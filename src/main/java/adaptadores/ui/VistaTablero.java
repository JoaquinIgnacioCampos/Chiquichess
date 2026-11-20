package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.piezas.Peon;
import Nucleo.piezas.Pieza;
import Nucleo.piezas.Rey;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Proyección del tablero actual. Solo guarda referencias a sus dependencias y nodos gráficos.
 *
 * <p>Animaciones: cada vez que se redibuja compara dónde estaba cada pieza (el núcleo mueve el mismo
 * objeto) con dónde está ahora, y desliza las que cambiaron de casilla. Así se animan las jugadas, el
 * enroque, la promoción, deshacer y rehacer sin que el núcleo sepa nada de la vista. Las capturadas
 * estallan en una capa aparte cuando llega la pieza que las come. El estado de cada animación vive en
 * propiedades que sobreviven a los redibujos, así un clic a mitad de camino no la corta.
 */
public final class VistaTablero extends Region {
    private static final Duration DESLIZAMIENTO = Duration.millis(280);
    private static final Duration ESTALLIDO = Duration.millis(320);
    private static final Interpolator SUAVE = Interpolator.SPLINE(0.3, 0.1, 0.25, 1);
    private final ITablero tablero;
    private final TemaTablero tema;
    private final RepresentacionPiezas representacion;
    private final GridPane grilla = new GridPane();
    private final Consumer<Posicion> alPulsar;
    /** Bando que se ve del lado de abajo: blancas salvo que se elija mirar desde las negras. */
    private Nucleo.bando.Bando abajo = Nucleo.bando.Bando.BLANCO;
    /** Capa sobre la grilla para las piezas capturadas que se desvanecen. */
    private final Pane capa = new Pane();
    /** Dónde estaba cada pieza en el último dibujo (por identidad: el núcleo mueve el mismo objeto). */
    private final Map<Pieza, Posicion> anteriores = new IdentityHashMap<>();
    private boolean dibujado;
    /** Piezas en movimiento o apareciendo, con su animación en curso. */
    private final Map<Pieza, Recorrido> recorridos = new IdentityHashMap<>();
    private Optional<Posicion> pelotaAnterior = Optional.empty();
    private Recorrido recorridoPelota;
    /** Latido continuo (0 a 1 y vuelta) del brillo del rey en jaque. */
    private final DoubleProperty latido = new SimpleDoubleProperty();
    /** La pieza elegida se levanta un poco: de 0 (apoyada) a 1 (levantada). */
    private final DoubleProperty alzado = new SimpleDoubleProperty();
    private Optional<Posicion> elegidaAnterior = Optional.empty();
    /** Bando cuyo rey está en jaque (o en jaque mate), si lo hay. */
    private Optional<Bando> enJaque = Optional.empty();
    private boolean mate;
    /** El rey que acaba de quedar en jaque se sacude; el que recibe el mate cae de costado. */
    private final DoubleProperty sacudida = new SimpleDoubleProperty();
    private final DoubleProperty caida = new SimpleDoubleProperty();
    private Pieza reyAnimado;
    private boolean reyAnimadoEnMate;
    private Timeline animacionRey;
    private final Timeline pulso = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(latido, 0)),
            new KeyFrame(Duration.millis(650), new KeyValue(latido, 1, Interpolator.EASE_BOTH)));

    /**
     * Desplazamiento de una pieza (en casillas, desde su casilla nueva) y su escala, que se animan hasta
     * 0 y 1. Los nodos que se crean en cada redibujo se atan a estas propiedades.
     */
    private record Recorrido(DoubleProperty columnas, DoubleProperty filas, DoubleProperty escala) {
        static Recorrido de(double columnas, double filas, double escala) {
            return new Recorrido(new SimpleDoubleProperty(columnas), new SimpleDoubleProperty(filas),
                    new SimpleDoubleProperty(escala));
        }
    }

    public VistaTablero(ITablero tablero, TemaTablero tema, RepresentacionPiezas representacion) {
        this(tablero, tema, representacion, posicion -> { });
    }

    public VistaTablero(ITablero tablero, TemaTablero tema, RepresentacionPiezas representacion,
                        Consumer<Posicion> alPulsar) {
        this.tablero = Objects.requireNonNull(tablero, "tablero");
        this.tema = Objects.requireNonNull(tema, "tema");
        this.representacion = Objects.requireNonNull(representacion, "representacion");
        this.alPulsar = Objects.requireNonNull(alPulsar, "alPulsar");
        setMinSize(0, 0);
        capa.setMouseTransparent(true);
        capa.setManaged(false);
        getChildren().addAll(grilla, capa);
        pulso.setAutoReverse(true);
        pulso.setCycleCount(Animation.INDEFINITE);
        // El latido arranca al redibujar con el tablero a la vista y se corta al sacarlo de la escena.
        sceneProperty().addListener((p, antes, escena) -> {
            if (escena == null) pulso.stop();
        });
        redibujar();
    }

    /**
     * Solo se anima con el tablero a la vista, en una ventana abierta. Si no (por ejemplo, en los tests,
     * sin ventana), cada cambio salta directo a su estado final.
     */
    private boolean animable() {
        return getScene() != null && getScene().getWindow() != null && getScene().getWindow().isShowing();
    }

    /** Reproduce la animación, o si no se puede animar, la lleva al final y avisa que terminó. */
    private void reproducir(Timeline linea) {
        if (animable()) {
            linea.play();
            return;
        }
        // Los valores del último cuadro son el estado final de la animación.
        linea.getKeyFrames().stream().max(java.util.Comparator.comparing(KeyFrame::getTime)).ifPresent(ultimo -> {
            for (KeyValue valor : ultimo.getValues()) {
                @SuppressWarnings("unchecked")
                var destino = (javafx.beans.value.WritableValue<Object>) valor.getTarget();
                destino.setValue(valor.getEndValue());
            }
        });
        if (linea.getOnFinished() != null) linea.getOnFinished().handle(null);
    }

    /**
     * Qué rey señalar: el del bando en jaque late en rojo; si es mate, además cae de costado. Se llama
     * antes de redibujar.
     */
    public void senalarRey(Optional<Bando> bando, boolean esMate) {
        enJaque = Objects.requireNonNull(bando, "bando");
        mate = esMate && bando.isPresent();
    }

    /** Gira el tablero para que ese bando quede abajo, como lo vería quien juega con él. */
    public void mirarDesde(Nucleo.bando.Bando bando) {
        abajo = Objects.requireNonNull(bando, "bando");
        redibujar();
    }

    /** Reconstruye la representación consultando el núcleo, sin almacenar piezas por separado. */
    public void redibujar() {
        redibujar(Optional.empty(), Set.of());
    }

    /** La selección puede estar vacía y los destinos ya vienen calculados por el núcleo. */
    public void redibujar(Optional<Posicion> seleccionada, Set<Posicion> destinos) {
        Objects.requireNonNull(seleccionada, "seleccionada");
        Objects.requireNonNull(destinos, "destinos");
        // Color de la pieza elegida: un destino ocupado por una pieza del mismo color es un pase.
        Optional<Nucleo.bando.Bando> colorElegido = seleccionada.flatMap(tablero::piezaEn).map(Nucleo.piezas.Pieza::color);
        if (animable() && pulso.getStatus() != Animation.Status.RUNNING) pulso.play();
        animarCambios();
        animarSeleccion(seleccionada);
        animarRey();
        grilla.getChildren().clear();
        grilla.getColumnConstraints().clear();
        grilla.getRowConstraints().clear();
        for (int columna = 0; columna < tablero.ancho(); columna++) {
            ColumnConstraints restriccion = new ColumnConstraints();
            restriccion.setPercentWidth(100.0 / tablero.ancho());
            grilla.getColumnConstraints().add(restriccion);
        }
        for (int fila = 0; fila < tablero.alto(); fila++) {
            RowConstraints restriccion = new RowConstraints();
            restriccion.setPercentHeight(100.0 / tablero.alto());
            grilla.getRowConstraints().add(restriccion);
            for (int columna = 0; columna < tablero.ancho(); columna++) {
                Posicion posicion = new Posicion(columna, fila);
                StackPane casilla = crearCasilla(posicion,
                        seleccionada.filter(posicion::equals).isPresent(), destinos.contains(posicion), colorElegido);
                boolean desdeBlancas = abajo == Nucleo.bando.Bando.BLANCO;
                grilla.add(casilla, desdeBlancas ? columna : tablero.ancho() - 1 - columna,
                        desdeBlancas ? tablero.alto() - 1 - fila : fila);
            }
        }
        requestLayout();
    }

    private StackPane crearCasilla(Posicion posicion, boolean seleccionada, boolean destino,
                                   Optional<Nucleo.bando.Bando> colorElegido) {
        StackPane casilla = new StackPane();
        casilla.setMinSize(0, 0);
        casilla.setId("casilla-" + posicion.columna() + "-" + posicion.fila());
        casilla.getStyleClass().add("casilla");
        boolean oscura = (posicion.columna() + posicion.fila()) % 2 == 0;
        Color colorCasilla = oscura ? tema.casillaOscura() : tema.casillaClara();
        if (seleccionada) {
            casilla.getStyleClass().add("casilla-seleccionada");
            colorCasilla = tema.casillaSeleccionada();
        }
        casilla.setBackground(Background.fill(colorCasilla));
        if (tema.textura() != null) {
            ImageView pasto = new ImageView(tema.textura());
            pasto.setManaged(false);
            pasto.setMouseTransparent(true);
            pasto.setOpacity(seleccionada ? 0.15 : 0.28);
            pasto.fitWidthProperty().bind(casilla.widthProperty());
            pasto.fitHeightProperty().bind(casilla.heightProperty());
            casilla.getChildren().add(pasto);
        }
        casilla.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY) {
                alPulsar.accept(posicion);
            }
        });
        casilla.setAccessibleText("Columna " + (posicion.columna() + 1) + ", fila " + (posicion.fila() + 1));
        tablero.arcoEn(posicion).ifPresent(defensor -> agregarArco(casilla, posicion));
        tablero.piezaEn(posicion).ifPresent(pieza -> {
            boolean reySenalado = pieza instanceof Rey && enJaque.filter(pieza.color()::equals).isPresent();
            if (reySenalado) agregarBrilloDeJaque(casilla);
            Node figura = representacion.crear(pieza, tema);
            DoubleExpression factor = new SimpleDoubleProperty(1);
            Recorrido recorrido = recorridos.get(pieza);
            if (recorrido != null) {
                factor = recorrido.escala();
                atarDesplazamiento(figura, casilla, recorrido);
            }
            if (seleccionada) {
                // Por encima de las vecinas, para que la sombra de la pieza levantada no quede tapada.
                casilla.setViewOrder(-0.5);
                factor = factor.multiply(alzado.multiply(0.12).add(1));
                DropShadow sombra = new DropShadow(0, 0, 0, Color.web("#000000", 0.45));
                sombra.radiusProperty().bind(alzado.multiply(12));
                sombra.offsetYProperty().bind(alzado.multiply(7));
                figura.setEffect(sombra);
                figura.translateYProperty().bind(alzado.multiply(casilla.heightProperty()).multiply(-0.06));
            }
            if (reySenalado) {
                figura.rotateProperty().bind(mate ? caida.multiply(78) : sacudida.multiply(14));
            }
            figura.scaleXProperty().bind(casilla.widthProperty().divide(RepresentacionPiezas.LADO_BASE).multiply(factor));
            figura.scaleYProperty().bind(casilla.heightProperty().divide(RepresentacionPiezas.LADO_BASE).multiply(factor));
            casilla.getChildren().add(figura);
        });
        if (tablero.pelota().filter(posicion::equals).isPresent()) {
            agregarPelota(casilla, tablero.piezaEn(posicion).isPresent());
        }
        if (destino) {
            boolean pase = tablero.piezaEn(posicion)
                    .filter(pieza -> colorElegido.filter(color -> color == pieza.color()).isPresent())
                    .isPresent();
            if (pase) {
                agregarIndicadorDePase(casilla);
            } else {
                agregarIndicador(casilla, tablero.piezaEn(posicion).isPresent());
            }
        }
        return casilla;
    }

    /**
     * Arco del modo fútbol: una red (líneas cruzadas sobre un velo claro) y la línea de gol gruesa del
     * lado del borde del tablero. Va debajo de la pieza para no taparla.
     */
    private void agregarArco(StackPane casilla, Posicion posicion) {
        Pane arco = new Pane();
        arco.setId("arco-" + posicion.columna() + "-" + posicion.fila());
        arco.getStyleClass().add("arco");
        arco.setMouseTransparent(true);
        arco.setMinSize(0, 0);
        arco.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Color.web("#FFFFFF", 0.30), CornerRadii.EMPTY, Insets.EMPTY)));
        // La línea de gol va del lado del borde que se ve, según hacia dónde esté girado el tablero.
        int filaDeArriba = abajo == Nucleo.bando.Bando.BLANCO ? tablero.alto() - 1 : 0;
        boolean arriba = posicion.fila() == filaDeArriba;
        double grosor = 5;
        arco.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.WHITE, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(arriba ? grosor : 0, 0, arriba ? 0 : grosor, 0))));
        javafx.scene.paint.Color hilo = javafx.scene.paint.Color.web("#FFFFFF", 0.55);
        for (int i = 1; i < 4; i++) {
            double fraccion = i / 4.0;
            javafx.scene.shape.Line vertical = new javafx.scene.shape.Line();
            vertical.startXProperty().bind(arco.widthProperty().multiply(fraccion));
            vertical.endXProperty().bind(arco.widthProperty().multiply(fraccion));
            vertical.endYProperty().bind(arco.heightProperty());
            javafx.scene.shape.Line horizontal = new javafx.scene.shape.Line();
            horizontal.startYProperty().bind(arco.heightProperty().multiply(fraccion));
            horizontal.endYProperty().bind(arco.heightProperty().multiply(fraccion));
            horizontal.endXProperty().bind(arco.widthProperty());
            for (javafx.scene.shape.Line linea : new javafx.scene.shape.Line[]{vertical, horizontal}) {
                linea.setStroke(hilo);
                linea.setStrokeWidth(1);
                linea.setManaged(false);
                arco.getChildren().add(linea);
            }
        }
        casilla.getChildren().add(arco);
    }

    /**
     * Pelota del modo fútbol: suelta, va en el centro de la casilla; si una pieza la lleva, va más
     * chica en la esquina de abajo a la derecha, para que la pieza se siga viendo.
     */
    private void agregarPelota(StackPane casilla, boolean conPieza) {
        Circle pelota = new Circle();
        pelota.setId("pelota");
        pelota.getStyleClass().add("pelota");
        pelota.setManaged(false);
        pelota.setMouseTransparent(true);
        var lado = Bindings.min(casilla.widthProperty(), casilla.heightProperty());
        pelota.radiusProperty().bind(lado.multiply(conPieza ? 0.13 : 0.22));
        double posicion = conPieza ? 0.80 : 0.5;
        pelota.centerXProperty().bind(casilla.widthProperty().multiply(posicion));
        pelota.centerYProperty().bind(casilla.heightProperty().multiply(posicion));
        pelota.setFill(javafx.scene.paint.Color.WHITE);
        pelota.setStroke(javafx.scene.paint.Color.web("#1b1b1b"));
        pelota.strokeWidthProperty().bind(lado.multiply(0.035));
        if (recorridoPelota != null) atarDesplazamiento(pelota, casilla, recorridoPelota);
        casilla.getChildren().add(pelota);
    }

    /** Brillo rojo debajo del rey en jaque: late mientras dure el jaque y queda fijo en el mate. */
    private void agregarBrilloDeJaque(StackPane casilla) {
        Region brillo = new Region();
        brillo.getStyleClass().add("brillo-jaque");
        brillo.setMouseTransparent(true);
        brillo.setMinSize(0, 0);
        brillo.setBackground(new Background(new BackgroundFill(new javafx.scene.paint.RadialGradient(0, 0, 0.5, 0.5,
                0.6, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#FF2A2A", 0.95)),
                new javafx.scene.paint.Stop(0.55, Color.web("#E01818", 0.55)),
                new javafx.scene.paint.Stop(1, Color.web("#B00000", 0))), CornerRadii.EMPTY, Insets.EMPTY)));
        if (mate) {
            brillo.setOpacity(0.9);
        } else {
            brillo.opacityProperty().bind(latido.multiply(0.55).add(0.35));
        }
        casilla.getChildren().add(brillo);
    }

    /** Ata la posición de un nodo a su recorrido: la casilla con el nodo en viaje va por encima del resto. */
    private void atarDesplazamiento(Node nodo, StackPane casilla, Recorrido recorrido) {
        nodo.translateXProperty().bind(recorrido.columnas().multiply(casilla.widthProperty()));
        nodo.translateYProperty().bind(recorrido.filas().multiply(casilla.heightProperty()));
        casilla.setViewOrder(-1);
    }

    /** Columna y fila en pantalla de una casilla, según hacia dónde esté girado el tablero. */
    private double[] enPantalla(Posicion posicion) {
        boolean desdeBlancas = abajo == Nucleo.bando.Bando.BLANCO;
        return new double[]{desdeBlancas ? posicion.columna() : tablero.ancho() - 1 - posicion.columna(),
                desdeBlancas ? tablero.alto() - 1 - posicion.fila() : posicion.fila()};
    }

    /**
     * Compara las piezas con las del dibujo anterior: las que cambiaron de casilla se deslizan, las que
     * desaparecieron estallan y las nuevas aparecen. Un peón que desaparece y una pieza nueva de su
     * color que aparece son una promoción: la pieza nueva sale de donde estaba el peón.
     */
    private void animarCambios() {
        Map<Pieza, Posicion> actuales = new IdentityHashMap<>();
        tablero.piezas().forEach((posicion, pieza) -> actuales.put(pieza, posicion));
        Optional<Posicion> pelota = tablero.pelota();
        if (dibujado) {
            List<Map.Entry<Pieza, Posicion>> desaparecidas = new ArrayList<>();
            anteriores.forEach((pieza, posicion) -> {
                if (!actuales.containsKey(pieza)) desaparecidas.add(Map.entry(pieza, posicion));
            });
            actuales.forEach((pieza, hasta) -> {
                Posicion desde = anteriores.get(pieza);
                if (desde == null) {
                    var peon = desaparecidas.stream()
                            .filter(e -> e.getKey() instanceof Peon && e.getKey().color() == pieza.color())
                            .findFirst();
                    peon.ifPresentOrElse(e -> {
                        desaparecidas.remove(e);
                        deslizar(pieza, e.getValue(), hasta);
                    }, () -> aparecer(pieza));
                } else if (!desde.equals(hasta)) {
                    deslizar(pieza, desde, hasta);
                }
            });
            desaparecidas.forEach(e -> estallar(e.getKey(), e.getValue()));
            if (pelotaAnterior.isPresent() && pelota.isPresent() && !pelotaAnterior.equals(pelota)) {
                Recorrido recorrido = recorridoDesde(pelotaAnterior.get(), pelota.get());
                recorridoPelota = recorrido;
                animarHastaElLugar(recorrido, () -> {
                    if (recorridoPelota == recorrido) recorridoPelota = null;
                });
            }
        }
        anteriores.clear();
        anteriores.putAll(actuales);
        pelotaAnterior = pelota;
        dibujado = true;
    }

    private Recorrido recorridoDesde(Posicion desde, Posicion hasta) {
        double[] origen = enPantalla(desde);
        double[] destino = enPantalla(hasta);
        return Recorrido.de(origen[0] - destino[0], origen[1] - destino[1], 1);
    }

    private void deslizar(Pieza pieza, Posicion desde, Posicion hasta) {
        Recorrido recorrido = recorridoDesde(desde, hasta);
        recorridos.put(pieza, recorrido);
        animarHastaElLugar(recorrido, () -> recorridos.remove(pieza, recorrido));
    }

    private void aparecer(Pieza pieza) {
        Recorrido recorrido = Recorrido.de(0, 0, 0.2);
        recorridos.put(pieza, recorrido);
        animarHastaElLugar(recorrido, () -> recorridos.remove(pieza, recorrido));
    }

    private void animarHastaElLugar(Recorrido recorrido, Runnable alTerminar) {
        Timeline linea = new Timeline(new KeyFrame(DESLIZAMIENTO,
                new KeyValue(recorrido.columnas(), 0, SUAVE),
                new KeyValue(recorrido.filas(), 0, SUAVE),
                new KeyValue(recorrido.escala(), 1, Interpolator.EASE_OUT)));
        linea.setOnFinished(evento -> alTerminar.run());
        reproducir(linea);
    }

    /**
     * Pieza capturada: se queda quieta hasta que llega la que la come y entonces se agranda, gira y se
     * desvanece. Va en una capa aparte, así los redibujos no la borran.
     */
    private void estallar(Pieza pieza, Posicion posicion) {
        double lado = grilla.getWidth() / tablero.ancho();
        if (!animable()) return;
        if (lado <= 0) return;
        double[] celda = enPantalla(posicion);
        Node figura = representacion.crear(pieza, tema);
        figura.setScaleX(lado / RepresentacionPiezas.LADO_BASE);
        figura.setScaleY(lado / RepresentacionPiezas.LADO_BASE);
        StackPane lugar = new StackPane(figura);
        lugar.resizeRelocate(grilla.getLayoutX() + celda[0] * lado, grilla.getLayoutY() + celda[1] * lado, lado, lado);
        capa.getChildren().add(lugar);
        Duration llegada = DESLIZAMIENTO.multiply(0.8);
        Timeline linea = new Timeline(
                new KeyFrame(llegada,
                        new KeyValue(lugar.opacityProperty(), 1),
                        new KeyValue(lugar.scaleXProperty(), 1),
                        new KeyValue(lugar.scaleYProperty(), 1),
                        new KeyValue(lugar.rotateProperty(), 0)),
                new KeyFrame(llegada.add(ESTALLIDO),
                        new KeyValue(lugar.opacityProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(lugar.scaleXProperty(), 1.45, Interpolator.EASE_OUT),
                        new KeyValue(lugar.scaleYProperty(), 1.45, Interpolator.EASE_OUT),
                        new KeyValue(lugar.rotateProperty(), pieza.color() == Bando.BLANCO ? -25 : 25)));
        linea.setOnFinished(evento -> capa.getChildren().remove(lugar));
        linea.play();
    }

    /** La pieza recién elegida se levanta; al soltarla vuelve a apoyarse. */
    private void animarSeleccion(Optional<Posicion> seleccionada) {
        if (seleccionada.equals(elegidaAnterior)) return;
        elegidaAnterior = seleccionada;
        alzado.set(0);
        if (seleccionada.isEmpty()) return;
        reproducir(new Timeline(new KeyFrame(Duration.millis(140), new KeyValue(alzado, 1, Interpolator.EASE_OUT))));
    }

    /** Al empezar un jaque el rey se sacude una vez; en el mate cae de costado y queda así. */
    private void animarRey() {
        Pieza rey = enJaque.flatMap(bando -> tablero.piezas().values().stream()
                .filter(pieza -> pieza instanceof Rey && pieza.color() == bando).findFirst()).orElse(null);
        if (rey == reyAnimado && mate == reyAnimadoEnMate) return;
        reyAnimado = rey;
        reyAnimadoEnMate = mate;
        if (animacionRey != null) animacionRey.stop();
        sacudida.set(0);
        caida.set(0);
        if (rey == null) return;
        animacionRey = new Timeline();
        if (mate) {
            animacionRey.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(250), new KeyValue(caida, 0)),
                    new KeyFrame(Duration.millis(900), new KeyValue(caida, 1, Interpolator.SPLINE(0.5, 0, 0.9, 0.6))),
                    new KeyFrame(Duration.millis(1030), new KeyValue(caida, 0.88, Interpolator.EASE_OUT)),
                    new KeyFrame(Duration.millis(1160), new KeyValue(caida, 1, Interpolator.EASE_IN)));
        } else {
            double[] valores = {1, -1, 0.7, -0.6, 0.3, 0};
            for (int i = 0; i < valores.length; i++) {
                animacionRey.getKeyFrames().add(new KeyFrame(Duration.millis(260 + 70 * i),
                        new KeyValue(sacudida, valores[i], Interpolator.EASE_BOTH)));
            }
        }
        reproducir(animacionRey);
    }

    /** Destino de un pase (modo fútbol): un anillo blanco punteado, distinto del de captura. */
    private void agregarIndicadorDePase(StackPane casilla) {
        Circle indicador = new Circle();
        indicador.setManaged(false);
        indicador.setMouseTransparent(true);
        indicador.getStyleClass().add("indicador-pase");
        indicador.centerXProperty().bind(casilla.widthProperty().divide(2));
        indicador.centerYProperty().bind(casilla.heightProperty().divide(2));
        var lado = Bindings.min(casilla.widthProperty(), casilla.heightProperty());
        indicador.radiusProperty().bind(lado.multiply(0.42));
        indicador.setFill(null);
        indicador.setStroke(javafx.scene.paint.Color.WHITE);
        indicador.strokeWidthProperty().bind(lado.multiply(0.06));
        indicador.getStrokeDashArray().addAll(10.0, 9.0);
        casilla.getChildren().add(indicador);
    }

    private void agregarIndicador(StackPane casilla, boolean captura) {
        Circle indicador = new Circle();
        indicador.setManaged(false);
        indicador.setMouseTransparent(true);
        indicador.centerXProperty().bind(casilla.widthProperty().divide(2));
        indicador.centerYProperty().bind(casilla.heightProperty().divide(2));
        var lado = Bindings.min(casilla.widthProperty(), casilla.heightProperty());
        indicador.radiusProperty().bind(lado.multiply(captura ? 0.43 : 0.16));
        if (captura) {
            indicador.getStyleClass().add("indicador-captura");
            indicador.setFill(null);
            indicador.setStroke(tema.destinoCaptura());
            indicador.strokeWidthProperty().bind(lado.multiply(0.08));
        } else {
            indicador.getStyleClass().add("indicador-movimiento");
            indicador.setFill(tema.destinoLibre());
        }
        casilla.getChildren().add(indicador);
    }

    @Override
    protected void layoutChildren() {
        double lado = Math.min(getWidth() / tablero.ancho(), getHeight() / tablero.alto());
        double ancho = lado * tablero.ancho();
        double alto = lado * tablero.alto();
        grilla.resizeRelocate((getWidth() - ancho) / 2, (getHeight() - alto) / 2, ancho, alto);
        capa.resizeRelocate(0, 0, getWidth(), getHeight());
    }

    @Override
    protected double computePrefWidth(double height) {
        return tablero.ancho() * RepresentacionPiezas.LADO_BASE;
    }

    @Override
    protected double computePrefHeight(double width) {
        return tablero.alto() * RepresentacionPiezas.LADO_BASE;
    }
}
