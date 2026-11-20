package adaptadores.ui;

import Nucleo.ia.Dificultad;
import adaptadores.audio.ControlDeVolumen;
import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.ConfiguracionPartida.*;
import adaptadores.configuracion.SeleccionDelMenu;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Menú de selección: emite una configuración completa; no crea partidas ni relojes.
 *
 * Estilo de estadio en pixel art: títulos con las letras de puntos del marcador, tarjetas y botones de
 * bordes rectos con sombra dura, y un fondo con las franjas del pasto. Las opciones van en una grilla de
 * tarjetas para que todo entre sin desplazarse; las que dependen de otra aparecen dentro de su tarjeta.
 */
public final class VistaMenuPartida extends GridPane {
    private static final double ALTO_TITULO = 16;
    /** Clave con la que cada opción dependiente guarda el texto que la reemplaza cuando no aplica. */
    private static final String PISTA = "pista";

    private final TemaPanel tema;
    private final Consumer<ConfiguracionPartida> iniciar;
    private final ToggleGroup modo = new ToggleGroup();
    private final ToggleGroup tiempo = new ToggleGroup();
    private final ToggleGroup rival = new ToggleGroup();
    private final ToggleGroup equipo1 = new ToggleGroup();
    private final ToggleGroup equipo2 = new ToggleGroup();
    private final ToggleGroup colores = new ToggleGroup();
    /** Solo se habilita con el rival IA. */
    private final ToggleGroup dificultad = new ToggleGroup();
    private final Label avisoDificultad = new Label();
    private final ToggleButton permitirDeshacer;
    private final Label avisoDeshacer = new Label();
    /** Modo fútbol: jugadas (de los dos) a las que termina la partida. Solo se habilita con ese modo. */
    private final Spinner<Integer> limiteDeJugadas = new Spinner<>(ConfiguracionPartida.LIMITE_MINIMO,
            ConfiguracionPartida.LIMITE_MAXIMO, ConfiguracionPartida.LIMITE_POR_DEFECTO, 10);
    private final Label avisoLimite = new Label();
    private final Button jugar = new Button("JUGAR");
    /** La imagen del personaje de la portada (se balancea al abrir el juego). */
    private ImageView figura;
    private Runnable alElegirOpcion = () -> { };
    private final HBox pie;
    private final Label pendientes = new Label();
    /** Club elegido por cada jugador, al lado de su nombre. */
    private final Text clubJugador1 = new Text();
    private final Text clubJugador2 = new Text();
    /** Opciones que dependen de otra: se muestran dentro de su tarjeta solo cuando aplican. */
    private final VBox panelLimite;
    private final VBox panelDeshacer;
    private final VBox panelDificultad;

    public VistaMenuPartida(TemaPanel tema, Consumer<ConfiguracionPartida> iniciar) {
        this.tema = Objects.requireNonNull(tema, "tema");
        this.iniciar = Objects.requireNonNull(iniciar, "iniciar");
        setId("menu-partida");
        setMinSize(0, 0);
        setBackground(fondo(tema.fondo()));
        setStyle("-fx-base: " + css(tema.tarjeta()) + "; -fx-accent: " + css(tema.acento())
                + "; -fx-focus-color: " + css(tema.acento()) + "; -fx-faint-focus-color: transparent;");
        for (double porcentaje : new double[]{40, 60}) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(porcentaje);
            columna.setMinWidth(0);
            getColumnConstraints().add(columna);
        }
        RowConstraints filaUnica = new RowConstraints();
        filaUnica.setPercentHeight(100);
        getRowConstraints().add(filaUnica);
        add(portada(), 0, 0);

        configurarLimite();
        panelLimite = dependiente("Límite de jugadas", new VBox(6, limiteDeJugadas, avisoLimite));
        Node tarjetaModo = tarjeta("MODO", new VBox(fila(
                opcion("Clásico", "modo-clasico", Modo.CLASICO, modo),
                opcion("Fútbol", "modo-futbol", Modo.FUTBOL, modo)),
                ranura(panelLimite, "Con Fútbol se elige el límite de jugadas.")));

        FlowPane tiempos = fila();
        for (Tiempo valor : Tiempo.values()) tiempos.getChildren().add(opcion(textoDe(valor),
                "tiempo-" + valor.name().toLowerCase(java.util.Locale.ROOT), valor, tiempo));
        permitirDeshacer = opcion("Permitir deshacer", "permitir-deshacer", Boolean.TRUE, null);
        aviso(avisoDeshacer, "aviso-deshacer");
        avisoDeshacer.setText("Ambos pueden deshacer jugadas.");
        panelDeshacer = dependiente("Deshacer jugadas", new VBox(6, fila(permitirDeshacer), avisoDeshacer));
        Node tarjetaTiempo = tarjeta("TIEMPO", new VBox(tiempos,
                ranura(panelDeshacer, "Sin tiempo se pueden deshacer jugadas.")));

        aviso(avisoDificultad, "aviso-dificultad");
        avisoDificultad.setText("La IA es el Jugador 2.");
        panelDificultad = dependiente("Dificultad de la IA", new VBox(6, fila(
                opcion("Fácil", "dificultad-facil", Dificultad.FACIL, dificultad),
                opcion("Medio", "dificultad-medio", Dificultad.MEDIO, dificultad),
                opcion("Difícil", "dificultad-dificil", Dificultad.DIFICIL, dificultad)), avisoDificultad));
        Node tarjetaRival = tarjeta("RIVAL", new VBox(fila(
                opcion("Amigo", "rival-amigo", Rival.AMIGO, rival),
                opcion("IA", "rival-ia", Rival.IA, rival)),
                ranura(panelDificultad, "Contra la IA se elige la dificultad.")));

        Node tarjetaColores = tarjeta("CON BLANCAS", fila(
                opcion("Jugador 1", "color-jugador-1", Colores.JUGADOR_1_BLANCAS, colores),
                opcion("Jugador 2", "color-jugador-2", Colores.JUGADOR_2_BLANCAS, colores),
                opcion("Al azar", "color-aleatorio", Colores.ALEATORIO, colores)));

        Node tarjetaEquipos = tarjeta("EQUIPOS", new VBox(10,
                seleccionEquipo("Jugador 1", "jugador-1", equipo1, clubJugador1),
                seleccionEquipo("Jugador 2", "jugador-2", equipo2, clubJugador2)));

        GridPane grilla = new GridPane();
        grilla.setHgap(14);
        grilla.setVgap(10);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(50);
            columna.setMinWidth(0);
            grilla.getColumnConstraints().add(columna);
        }
        grilla.add(tarjetaModo, 0, 0);
        grilla.add(tarjetaTiempo, 1, 0);
        grilla.add(tarjetaRival, 0, 1);
        grilla.add(tarjetaColores, 1, 1);
        grilla.add(tarjetaEquipos, 0, 2, 2, 1);

        Text bajada = texto("Elegí cómo se juega y los equipos.", 13, tema.textoSecundario(), false);
        VBox opciones = new VBox(14, new VBox(6, titulo("ARMA LA PARTIDA", 22), bajada), grilla);
        opciones.setPadding(new Insets(16, 26, 12, 26));
        opciones.setMinWidth(0);

        pendientes.setId("selecciones-pendientes");
        pendientes.setFont(EstiloPixel.fuente(12, true));
        pendientes.setTextFill(tema.textoSecundario());
        pendientes.setWrapText(true);
        // Puede necesitar varios renglones en ventanas angostas: crece en alto en vez de cortarse.
        pendientes.setMinHeight(Region.USE_PREF_SIZE);
        pendientes.setMinWidth(0);
        pendientes.setMaxWidth(Double.MAX_VALUE);
        jugar.setId("jugar");
        jugar.setFont(EstiloPixel.fuente(18, true));
        jugar.setMinWidth(150);
        jugar.setPrefWidth(150);
        jugar.setMaxWidth(150);
        jugar.setCursor(Cursor.HAND);
        jugar.focusedProperty().addListener((p, antes, foco) -> pintarJugar());
        jugar.disabledProperty().addListener((p, antes, deshabilitado) -> pintarJugar());
        jugar.setOnAction(e -> emitirConfiguracion());
        pie = new HBox(14, pendientes, jugar);
        pie.setAlignment(Pos.CENTER_RIGHT);
        pie.setPadding(new Insets(10, 26, 14, 26));
        HBox.setHgrow(pendientes, Priority.ALWAYS);
        pie.setBackground(new Background(new BackgroundFill(tema.tarjeta(), CornerRadii.EMPTY, Insets.EMPTY)));
        pie.setBorder(new Border(new BorderStroke(tema.borde(), BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(3, 0, 0, 0))));

        // Si la ventana es muy baja, las opciones se achican para entrar sin desplazamiento; "Jugar"
        // queda siempre fijo debajo.
        AjusteAlAlto desplazamiento = new AjusteAlAlto(opciones);
        desplazamiento.setId("opciones-menu");
        String pasto = "linear-gradient(from 0px 0px to 112px 0px, repeat, #0C1712 50%, #0F1D16 50%)";
        desplazamiento.setStyle("-fx-background-color: " + pasto + ";");
        opciones.setStyle("-fx-background-color: transparent;");
        VBox columnaOpciones = new VBox(desplazamiento, pie);
        columnaOpciones.setMinSize(0, 0);
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);
        add(columnaOpciones, 1, 0);
        for (ToggleGroup grupo : new ToggleGroup[]{modo, tiempo, rival, equipo1, equipo2, colores, dificultad}) {
            grupo.selectedToggleProperty().addListener((p, antes, ahora) -> actualizarSeleccion());
        }
        actualizarSeleccion();
    }

    /**
     * El personaje se ríe: se balancea de un lado al otro, con un pequeño rebote, y vuelve a quedar
     * quieto. Dura unos 3 segundos, lo que dura la risa. Devuelve la animación (ya iniciada).
     */
    public javafx.animation.Animation animarPersonaje() {
        javafx.util.Duration vaivenDuracion = javafx.util.Duration.millis(260);
        var aLaIzquierda = new javafx.animation.RotateTransition(javafx.util.Duration.millis(150), figura);
        aLaIzquierda.setToAngle(-9);
        var vaiven = new javafx.animation.RotateTransition(vaivenDuracion, figura);
        vaiven.setFromAngle(-9);
        vaiven.setToAngle(9);
        vaiven.setCycleCount(10);
        vaiven.setAutoReverse(true);
        var rebote = new javafx.animation.ScaleTransition(vaivenDuracion, figura);
        rebote.setFromX(1);
        rebote.setFromY(1);
        rebote.setToX(1.06);
        rebote.setToY(1.06);
        rebote.setCycleCount(10);
        rebote.setAutoReverse(true);
        var alCentro = new javafx.animation.RotateTransition(javafx.util.Duration.millis(200), figura);
        alCentro.setToAngle(0);
        var risa = new javafx.animation.SequentialTransition(aLaIzquierda,
                new javafx.animation.ParallelTransition(vaiven, rebote), alCentro);
        risa.setOnFinished(evento -> {
            figura.setRotate(0);
            figura.setScaleX(1);
            figura.setScaleY(1);
        });
        risa.play();
        return risa;
    }

    /** Qué hacer cuando la persona toca una opción del menú (por ejemplo, un clic de sonido). */
    public void alElegirOpcion(Runnable accion) {
        alElegirOpcion = Objects.requireNonNull(accion, "accion");
    }

    /**
     * Agrega al pie el botón de silencio y el volumen, con el mismo control que la partida: lo que se elige
     * acá sigue valiendo al jugar.
     */
    public void configurarSonido(ControlDeVolumen control) {
        Objects.requireNonNull(control, "control");
        if (pie.lookup("#silencio-menu") != null) throw new IllegalStateException("Sonido ya configurado");
        pie.getChildren().addAll(0, ControlesDeSonido.crear(control, "silencio-menu", "volumen-menu",
                tema.borde(), tema.texto()));
    }

    /** Deja elegidas esas opciones, como si la persona las hubiera tocado (pero sin el clic de sonido). */
    public void preseleccionar(SeleccionDelMenu seleccion) {
        Objects.requireNonNull(seleccion, "seleccion");
        elegir(modo, seleccion.modo());
        // El tiempo y el rival van antes que deshacer y dificultad, que dependen de ellos.
        elegir(tiempo, seleccion.tiempo());
        elegir(rival, seleccion.rival());
        elegir(dificultad, seleccion.dificultad());
        elegir(equipo1, seleccion.equipoJugador1());
        elegir(equipo2, seleccion.equipoJugador2());
        elegir(colores, seleccion.colores());
        limiteDeJugadas.getValueFactory().setValue(seleccion.limiteDeJugadas());
        actualizarSeleccion();
        permitirDeshacer.setSelected(seleccion.permitirDeshacer() && !permitirDeshacer.isDisable());
    }

    private static void elegir(ToggleGroup grupo, java.util.Optional<?> valor) {
        grupo.selectToggle(grupo.getToggles().stream()
                .filter(opcion -> valor.isPresent() && valor.get().equals(opcion.getUserData()))
                .findFirst().orElse(null));
    }

    private static String textoDe(Tiempo valor) {
        return switch (valor) {
            case TRES_MINUTOS -> "3 min";
            case CINCO_MINUTOS -> "5 min";
            case DIEZ_MINUTOS -> "10 min";
            case SIN_TIEMPO -> "Sin tiempo";
        };
    }

    private void configurarLimite() {
        limiteDeJugadas.setId("limite-jugadas");
        limiteDeJugadas.setEditable(true);
        limiteDeJugadas.setPrefWidth(110);
        limiteDeJugadas.getEditor().setFont(EstiloPixel.fuente(13, true));
        limiteDeJugadas.setStyle("-fx-background-color: " + css(tema.borde()) + "; -fx-background-radius: 0;");
        limiteDeJugadas.getEditor().setStyle("-fx-background-color: " + css(tema.fondo())
                + "; -fx-text-fill: " + css(tema.texto()) + "; -fx-background-radius: 0;");
        // Lo escrito a mano se toma al salir del campo (y también al apretar Jugar).
        limiteDeJugadas.getEditor().focusedProperty().addListener((p, antes, foco) -> {
            if (!foco) confirmarLimiteEscrito();
        });
        aviso(avisoLimite, "aviso-limite");
        avisoLimite.setText("Jugadas de los dos juntos (de " + ConfiguracionPartida.LIMITE_MINIMO + " a "
                + ConfiguracionPartida.LIMITE_MAXIMO + "). Al llegar, gana quien tenga más goles.");
    }

    private void aviso(Label aviso, String id) {
        aviso.setId(id);
        aviso.setFont(EstiloPixel.fuente(12, false));
        aviso.setTextFill(tema.textoSecundario());
        aviso.setWrapText(true);
    }

    private static Image cargarPersonaje() {
        var recurso = Objects.requireNonNull(VistaMenuPartida.class.getResource("/personaje/chiquichess.png"),
                "Falta la imagen del personaje");
        Image imagen = new Image(recurso.toExternalForm(), false);
        if (imagen.isError()) throw new IllegalStateException("No se pudo cargar la imagen del personaje", imagen.getException());
        return imagen;
    }

    private VBox portada() {
        figura = new ImageView(cargarPersonaje());
        figura.setId("personaje");
        figura.setPreserveRatio(true);
        figura.setSmooth(true);
        StackPane personaje = new StackPane(figura);
        figura.fitWidthProperty().bind(javafx.beans.binding.Bindings.max(1, personaje.widthProperty().subtract(32)));
        figura.fitHeightProperty().bind(javafx.beans.binding.Bindings.max(1, personaje.heightProperty().subtract(32)));
        personaje.setId("espacio-personaje");
        personaje.setMinSize(0, 0);
        personaje.setPrefHeight(370);
        personaje.setMaxHeight(Double.MAX_VALUE);
        personaje.setStyle(estiloTarjeta(tema.fondo()));
        personaje.setAccessibleText("Personaje de ChiquiChess");
        Text leyenda = texto("Elegí tu equipo.\nJugá tu clásico.", 22, tema.texto(), true);
        leyenda.wrappingWidthProperty().bind(javafx.beans.binding.Bindings.max(1, widthProperty().multiply(.4).subtract(64)));
        VBox portada = new VBox(14, titulo("CHIQUICHESS", 30), titulo("AJEDREZ ARGENTINO", 12), personaje, leyenda);
        portada.setId("portada-menu");
        portada.setPadding(new Insets(28, 28, 24, 28));
        portada.setMinWidth(0);
        portada.setBackground(fondo(tema.tarjeta()));
        portada.setBorder(new Border(new BorderStroke(tema.borde(), BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(0, 3, 0, 0))));
        VBox.setVgrow(personaje, Priority.ALWAYS);
        return portada;
    }

    /** Fila de un jugador: su nombre con el club elegido y los escudos para elegirlo. */
    private HBox seleccionEquipo(String jugador, String id, ToggleGroup grupo, Text club) {
        FlowPane escudos = new FlowPane(6, 6);
        for (Equipo equipo : Equipo.values()) {
            ToggleButton boton = opcion("", id + "-" + equipo.name().toLowerCase(java.util.Locale.ROOT), equipo, grupo);
            var imagen = new ImageView(PresentacionClub.escudoDe(equipo));
            imagen.setFitWidth(54);
            imagen.setFitHeight(54);
            imagen.setPreserveRatio(true);
            imagen.setSmooth(true);
            imagen.setMouseTransparent(true);
            boton.setGraphic(imagen);
            boton.setAccessibleText(equipo.nombre());
            boton.setTooltip(new Tooltip(equipo.nombre()));
            boton.setMinSize(78, 70);
            boton.setPrefSize(78, 70);
            boton.setMaxSize(78, 70);
            escudos.getChildren().add(boton);
        }
        club.setFont(EstiloPixel.fuente(13, true));
        club.setFill(tema.acento());
        VBox nombre = new VBox(2, texto(jugador, 14, tema.texto(), true), club);
        nombre.setMinWidth(96);
        nombre.setPrefWidth(96);
        HBox fila = new HBox(10, nombre, escudos);
        fila.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(escudos, Priority.ALWAYS);
        return fila;
    }

    private ToggleButton opcion(String texto, String id, Object valor, ToggleGroup grupo) {
        // En un grupo siempre queda una opción elegida: tocar la elegida no la suelta. Las opciones
        // sueltas (sin grupo), como "Permitir deshacer", sí se activan y desactivan.
        ToggleButton boton = new ToggleButton(texto) {
            @Override
            public void fire() {
                if (getToggleGroup() == null || !isSelected()) super.fire();
            }
        };
        boton.setId(id);
        boton.setUserData(valor);
        boton.setToggleGroup(grupo);
        boton.setFont(EstiloPixel.fuente(12, true));
        boton.setCursor(Cursor.HAND);
        // Solo los clics de la persona (no las selecciones que hace el propio menú) avisan.
        boton.setOnAction(evento -> alElegirOpcion.run());
        boton.selectedProperty().addListener((p, antes, ahora) -> pintarOpcion(boton));
        boton.focusedProperty().addListener((p, antes, ahora) -> pintarOpcion(boton));
        boton.hoverProperty().addListener((p, antes, ahora) -> pintarOpcion(boton));
        boton.pressedProperty().addListener((p, antes, ahora) -> pintarOpcion(boton));
        pintarOpcion(boton);
        return boton;
    }

    /**
     * Botón de pixel art: sombra dura abajo a la derecha, borde de 2 píxeles y cara plana. Al estar
     * elegido o apretado se "hunde": pierde la sombra y se corre hacia ella.
     */
    private void pintarOpcion(ToggleButton boton) {
        boolean hundido = boton.isSelected() || boton.isPressed();
        Color cara = boton.isSelected() ? tema.acento()
                : boton.isHover() ? tema.tarjeta().interpolate(tema.borde(), 0.6) : tema.tarjeta();
        Color borde = boton.isFocused() ? tema.texto() : boton.isSelected() ? tema.acento().darker() : tema.borde();
        boton.setStyle(estiloBoton(cara, borde, hundido)
                + "-fx-text-fill: " + css(boton.isSelected() ? tema.fondo() : tema.texto()) + ";");
    }

    private void pintarJugar() {
        Color cara = jugar.isDisabled() ? tema.borde() : tema.acento();
        Color borde = jugar.isFocused() ? tema.texto() : cara.darker();
        jugar.setStyle(estiloBoton(cara, borde, false) + "-fx-text-fill: " + css(tema.fondo()) + ";"
                + "-fx-opacity: 1; -fx-padding: 10 18 14 14;");
    }

    private String estiloBoton(Color cara, Color borde, boolean hundido) {
        return EstiloPixel.boton(cara, borde, hundido);
    }

    /** Tarjeta de pixel art: sombra dura, borde grueso y cara oscura. */
    private String estiloTarjeta(Color cara) {
        return EstiloPixel.tarjeta(cara, tema.borde());
    }

    private VBox tarjeta(String titulo, Node contenido) {
        VBox tarjeta = new VBox(10, titulo(titulo, ALTO_TITULO), contenido);
        tarjeta.setPadding(new Insets(11, 19, 15, 15));
        tarjeta.setMinWidth(0);
        tarjeta.setStyle(estiloTarjeta(tema.tarjeta()));
        GridPane.setFillHeight(tarjeta, true);
        tarjeta.setMaxHeight(Double.MAX_VALUE);
        return tarjeta;
    }

    /** Título con las letras de puntos del marcador del estadio, alineado a la izquierda. */
    private Node titulo(String texto, double alto) {
        TextoLed led = new TextoLed(tema.acento());
        led.setColorApagado(tema.puntoApagado());
        led.setTexto(texto);
        double ancho = (texto.length() * 6 - 1) * alto / 7;
        led.setMinSize(ancho, alto);
        led.setPrefSize(ancho, alto);
        led.setMaxSize(ancho, alto);
        return led;
    }

    /** Opción que depende de otra, en un recuadro hundido dentro de su tarjeta. */
    private VBox dependiente(String titulo, Node contenido) {
        VBox panel = new VBox(6, texto(titulo, 12, tema.acento(), true), contenido);
        panel.setPadding(new Insets(8, 10, 10, 10));
        panel.setBackground(fondo(tema.fondo()));
        return panel;
    }

    /**
     * Lugar fijo para una opción dependiente: siempre ocupa lo mismo, así la tarjeta no cambia de alto.
     * Cuando la opción no aplica, en su lugar se lee qué la habilita.
     */
    private StackPane ranura(VBox panel, String pista) {
        Label explicacion = new Label(pista);
        explicacion.setFont(EstiloPixel.fuente(12, false));
        explicacion.setTextFill(tema.textoSecundario());
        explicacion.setWrapText(true);
        explicacion.setPadding(new Insets(8, 10, 0, 10));
        panel.getProperties().put(PISTA, explicacion);
        StackPane ranura = new StackPane(panel, explicacion);
        ranura.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(ranura, new Insets(10, 0, 0, 0));
        return ranura;
    }

    private static void mostrar(Node panel, boolean visible) {
        panel.setVisible(visible);
        ((Node) panel.getProperties().get(PISTA)).setVisible(!visible);
    }

    private void actualizarSeleccion() {
        bloquearEquipo(equipo1, equipo2);
        bloquearEquipo(equipo2, equipo1);
        clubJugador1.setText(nombreDelClub(equipo1));
        clubJugador2.setText(nombreDelClub(equipo2));
        // Deshacer solo existe en partidas sin tiempo: con un tiempo elegido se apaga y se deshabilita.
        boolean sinTiempo = tiempo.getSelectedToggle() != null
                && tiempo.getSelectedToggle().getUserData() == Tiempo.SIN_TIEMPO;
        if (!sinTiempo) permitirDeshacer.setSelected(false);
        permitirDeshacer.setDisable(!sinTiempo);
        mostrar(panelDeshacer, sinTiempo);
        // La dificultad solo existe contra la IA: con otro rival se borra y se deshabilita.
        boolean contraLaIa = rival.getSelectedToggle() != null && rival.getSelectedToggle().getUserData() == Rival.IA;
        if (!contraLaIa && dificultad.getSelectedToggle() != null) dificultad.selectToggle(null);
        for (Toggle opcion : dificultad.getToggles()) ((ToggleButton) opcion).setDisable(!contraLaIa);
        mostrar(panelDificultad, contraLaIa);
        boolean futbol = modo.getSelectedToggle() != null && modo.getSelectedToggle().getUserData() == Modo.FUTBOL;
        limiteDeJugadas.setDisable(!futbol);
        mostrar(panelLimite, futbol);
        var faltantes = new ArrayList<String>();
        if (modo.getSelectedToggle() == null) faltantes.add("modo");
        if (tiempo.getSelectedToggle() == null) faltantes.add("tiempo");
        if (rival.getSelectedToggle() == null) faltantes.add("rival");
        if (contraLaIa && dificultad.getSelectedToggle() == null) faltantes.add("dificultad");
        if (equipo1.getSelectedToggle() == null) faltantes.add("equipo de Jugador 1");
        if (equipo2.getSelectedToggle() == null) faltantes.add("equipo de Jugador 2");
        if (colores.getSelectedToggle() == null) faltantes.add("colores");
        jugar.setDisable(!faltantes.isEmpty());
        pintarJugar();
        pendientes.setText(faltantes.isEmpty() ? "Todo listo para jugar." : "Falta elegir: " + String.join(", ", faltantes) + ".");
    }

    private static String nombreDelClub(ToggleGroup grupo) {
        return grupo.getSelectedToggle() == null ? "Sin elegir" : ((Equipo) grupo.getSelectedToggle().getUserData()).nombre();
    }

    private void bloquearEquipo(ToggleGroup propio, ToggleGroup otro) {
        Object elegido = otro.getSelectedToggle() == null ? null : otro.getSelectedToggle().getUserData();
        for (Toggle opcion : propio.getToggles()) ((ToggleButton) opcion).setDisable(opcion.getUserData() == elegido);
    }

    private void emitirConfiguracion() {
        actualizarSeleccion();
        if (jugar.isDisabled()) return;
        ConfiguracionPartida seleccion = new ConfiguracionPartida((Modo) modo.getSelectedToggle().getUserData(),
                (Tiempo) tiempo.getSelectedToggle().getUserData(), (Rival) rival.getSelectedToggle().getUserData(),
                (Equipo) equipo1.getSelectedToggle().getUserData(), (Equipo) equipo2.getSelectedToggle().getUserData(),
                (Colores) colores.getSelectedToggle().getUserData(), permitirDeshacer.isSelected(),
                limiteElegido(), java.util.Optional.ofNullable(dificultad.getSelectedToggle())
                .map(elegida -> (Dificultad) elegida.getUserData()));
        iniciar.accept(seleccion);
    }

    /** El límite elegido, tomando lo que se haya escrito a mano en el campo. */
    private int limiteElegido() {
        confirmarLimiteEscrito();
        return limiteDeJugadas.getValue();
    }

    /** Pasa al valor lo escrito en el campo (fuera de rango, se ajusta al extremo); si no es un número, lo descarta. */
    private void confirmarLimiteEscrito() {
        var fabrica = limiteDeJugadas.getValueFactory();
        try {
            fabrica.setValue(fabrica.getConverter().fromString(limiteDeJugadas.getEditor().getText().trim()));
        } catch (RuntimeException noEsUnNumero) {
            // se queda el valor anterior
        }
        limiteDeJugadas.getEditor().setText(fabrica.getConverter().toString(fabrica.getValue()));
    }

    private FlowPane fila(Node... botones) {
        return new FlowPane(8, 8, botones);
    }

    private Text texto(String valor, double tamano, Color color, boolean negrita) {
        Text texto = new Text(valor);
        texto.setFont(EstiloPixel.fuente(tamano, negrita));
        texto.setFill(color);
        return texto;
    }

    private Background fondo(Color color) {
        return new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY));
    }

    private static String css(Color color) {
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
    }
}
