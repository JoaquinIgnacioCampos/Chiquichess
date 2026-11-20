package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.tablero.Posicion;
import adaptadores.audio.ControlDeVolumen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.Objects;

/** Vista preparada para dibujar y enviar órdenes a la partida recibida. */
public final class VistaPartida extends BorderPane {
    private final ControladorTablero controlador;
    private final VistaTablero tablero;
    private final Text mensaje = new Text();
    private final PanelEstadoPartida panel;
    /** Marca y botones de la cabecera; si no entran en una fila, los botones bajan a la siguiente. */
    private final FlowPane titulo = new FlowPane(16, 10);
    private final StackPane areaTablero = new StackPane();
    private final TemaTablero tema;
    private final RepresentacionPiezas representacion;
    /** Se crea recién la primera vez que un peón promueve: la mayoría de las partidas no lo necesita. */
    private SelectorPromocion selectorPromocion;
    private final TemaPanel temaPanel;
    private final MensajesDeLaInterfaz mensajes;
    private NavegacionPartida navegacion;
    private Runnable alSeleccionarPieza = () -> { };
    private final java.util.Map<Bando, PresentacionClub> clubes;
    /** Celebración de gol sobre el tablero (modo fútbol). */
    private final CartelDeGol cartelDeGol;
    private javafx.scene.control.Button deshacer;
    private javafx.scene.control.Button rehacer;

    public VistaPartida(ControladorTablero controlador, TemaTablero tema,
                        RepresentacionPiezas representacion, TemaPanel temaPanel) {
        this(controlador, tema, representacion, temaPanel, java.util.Map.of());
    }

    public VistaPartida(ControladorTablero controlador, TemaTablero tema,
                        RepresentacionPiezas representacion, TemaPanel temaPanel,
                        java.util.Map<Bando, PresentacionClub> clubes) {
        this(controlador, tema, representacion, temaPanel, clubes, new MensajesDeLaInterfaz());
    }

    public VistaPartida(ControladorTablero controlador, TemaTablero tema,
                        RepresentacionPiezas representacion, TemaPanel temaPanel,
                        java.util.Map<Bando, PresentacionClub> clubes, MensajesDeLaInterfaz mensajes) {
        this.controlador = Objects.requireNonNull(controlador, "controlador");
        this.mensajes = Objects.requireNonNull(mensajes, "mensajes");
        Objects.requireNonNull(tema, "tema");
        this.tablero = new VistaTablero(controlador.tablero(), tema, representacion, this::pulsar);
        this.temaPanel = Objects.requireNonNull(temaPanel, "temaPanel");
        this.clubes = java.util.Map.copyOf(Objects.requireNonNull(clubes, "clubes"));
        this.cartelDeGol = new CartelDeGol(temaPanel);
        setMinSize(0, 0);
        setBackground(new Background(new BackgroundFill(tema.fondo(), CornerRadii.EMPTY, Insets.EMPTY)));
        BorderPane juego = new BorderPane();
        juego.setId("area-juego");
        juego.setMinSize(0, 0);
        juego.setPadding(new Insets(24));
        Text chiqui = new Text("CHIQUI");
        chiqui.setFill(temaPanel.texto());
        chiqui.setFont(EstiloPixel.fuente(22, true));
        Text chess = new Text("CHESS");
        chess.setFill(temaPanel.acento());
        chess.setFont(EstiloPixel.fuente(22, true));
        titulo.getChildren().add(new HBox(chiqui, chess));
        titulo.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(titulo, new Insets(0, 0, 16, 0));
        juego.setTop(titulo);
        areaTablero.setMinSize(0, 0);
        this.tema = tema;
        this.representacion = representacion;
        areaTablero.getChildren().addAll(tablero, cartelDeGol);
        juego.setCenter(areaTablero);
        setCenter(juego);
        panel = new PanelEstadoPartida(controlador, temaPanel, clubes, mensajes);
        panel.prefWidthProperty().bind(widthProperty().multiply(0.35));
        setRight(panel);
        mensaje.setId("mensaje-movimiento");
        mensaje.setFill(temaPanel.texto());
        mensaje.setFont(EstiloPixel.fuente(16, false));
        mensaje.wrappingWidthProperty().bind(javafx.beans.binding.Bindings.min(520,
                areaTablero.widthProperty().multiply(0.8)));
        mensaje.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        // Cartel flotante sobre la parte de abajo del tablero: se ve sin correr ni achicar nada.
        // No toma clics, así el tablero que queda debajo se sigue pudiendo usar.
        StackPane cartel = new StackPane(mensaje);
        cartel.setId("cartel-mensaje");
        cartel.setMouseTransparent(true);
        cartel.setPadding(new Insets(10, 18, 10, 18));
        cartel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cartel.setBackground(new Background(new BackgroundFill(
                temaPanel.fondo().deriveColor(0, 1, 1, 0.9), CornerRadii.EMPTY, Insets.EMPTY)));
        cartel.setBorder(new Border(new BorderStroke(temaPanel.acento(), BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(2))));
        cartel.visibleProperty().bind(mensaje.textProperty().isNotEmpty());
        StackPane.setAlignment(cartel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(cartel, new Insets(0, 0, 18, 0));
        areaTablero.getChildren().add(cartel);
        // Después del panel: el panel anota el evento y recién entonces se redibuja todo.
        controlador.escuchar(evento -> {
            if (evento instanceof Nucleo.juego.EventoDePartida.JugadaHecha hecha
                    && hecha.tiene(Nucleo.juego.EventoDePartida.Rasgo.GOL)) {
                celebrarGol(hecha.bando());
            }
            redibujar();
        });
        redibujar();
    }

    private void pulsar(Posicion posicion) {
        var antes = controlador.seleccionada();
        mensaje.setText(controlador.pulsar(posicion).map(mensajes::rechazoDeMovimiento).orElse(""));
        // Una pieza recién elegida (no deseleccionar, no mover): lo puede aprovechar el sonido.
        var ahora = controlador.seleccionada();
        if (ahora.isPresent() && !ahora.equals(antes)) alSeleccionarPieza.run();
        redibujar();
    }

    /** El cartel de gol con el escudo, el color y el nombre del equipo de ese bando (o del bando, sin clubes). */
    private javafx.animation.Animation celebrarGol(Bando bando) {
        PresentacionClub club = clubes.get(bando);
        if (club == null) {
            return cartelDeGol.celebrar(bando == Bando.BLANCO ? "Blancas" : "Negras", temaPanel.acento(),
                    java.util.Optional.empty());
        }
        return cartelDeGol.celebrar(club.equipo().nombre(), club.tema().acento(), java.util.Optional.of(club.escudo()));
    }

    /** Qué hacer cuando la persona elige una pieza para mover (por ejemplo, un clic de sonido). */
    public void alSeleccionarPieza(Runnable accion) {
        alSeleccionarPieza = Objects.requireNonNull(accion, "accion");
    }

    private void promover(char codigo) {
        mensaje.setText(controlador.promover(codigo).map(mensajes::rechazoDeMovimiento).orElse(""));
        redibujar();
    }

    private void cancelarPromocion() {
        controlador.cancelarPromocion();
        redibujar();
    }

    private void mostrarPromocion(java.util.List<Nucleo.piezas.Pieza> opciones) {
        if (selectorPromocion == null) {
            if (opciones.isEmpty()) return;
            selectorPromocion = new SelectorPromocion(tema, representacion, temaPanel,
                    this::promover, this::cancelarPromocion);
            // Justo encima del tablero, debajo de la confirmación de "Volver al menú" si la hay.
            areaTablero.getChildren().add(1, selectorPromocion);
        }
        selectorPromocion.mostrar(opciones);
    }

    /** Tablero y tarjetas vistos desde ese bando: sus piezas y su tarjeta quedan abajo. */
    public void mirarDesde(Nucleo.bando.Bando bando) {
        tablero.mirarDesde(bando);
        panel.mirarDesde(bando);
    }

    public void redibujar() {
        var estado = controlador.estado();
        boolean mate = estado == Nucleo.reglas.EstadoPartida.JAQUE_MATE;
        tablero.senalarRey(mate || estado == Nucleo.reglas.EstadoPartida.JAQUE
                ? java.util.Optional.of(controlador.turno()) : java.util.Optional.empty(), mate);
        tablero.redibujar(controlador.seleccionada(), controlador.destinosLegales());
        mostrarPromocion(controlador.promocionPendiente());
        panel.actualizar();
        if (navegacion != null) navegacion.actualizar();
        if (deshacer != null) {
            deshacer.setDisable(!controlador.puedeDeshacer());
            rehacer.setDisable(!controlador.puedeRehacer());
        }
    }

    /**
     * Agrega a la cabecera los botones "Deshacer" y "Rehacer", solo si la partida lo permite. Los dos
     * jugadores pueden usarlos, por eso van en la cabecera y no en una tarjeta. Se llama antes de
     * configurarNavegacion, así quedan a la izquierda de "Volver al menú".
     */
    public void configurarDeshacer() {
        if (deshacer != null) throw new IllegalStateException("Deshacer ya configurado");
        if (!controlador.permiteDeshacer()) return;
        deshacer = BotonDeCabecera.crear("Deshacer", "deshacer", temaPanel.tarjeta(), temaPanel.texto(),
                IconoPixel.DESHACER, false);
        rehacer = BotonDeCabecera.crear("Rehacer", "rehacer", temaPanel.tarjeta(), temaPanel.texto(),
                IconoPixel.DESHACER, true);
        deshacer.setOnAction(e -> mostrarRespuestaDeshacer(controlador.deshacer()));
        rehacer.setOnAction(e -> mostrarRespuestaDeshacer(controlador.rehacer()));
        titulo.getChildren().addAll(deshacer, rehacer);
        redibujar();
    }

    private void mostrarRespuestaDeshacer(java.util.Optional<Nucleo.juego.AccionInvalidaException.Motivo> rechazo) {
        mensaje.setText(rechazo
                .map(motivo -> mensajes.rechazoDeAccion(MensajesDeLaInterfaz.Accion.DESHACER, motivo))
                .orElse(""));
        redibujar();
    }

    /**
     * Agrega a la cabecera el botón de silencio y el volumen. El control se comparte entre partidas, así
     * que lo que se elige acá sigue valiendo en la revancha. Se llama antes de configurarNavegacion.
     */
    public void configurarSonido(ControlDeVolumen control) {
        Objects.requireNonNull(control, "control");
        if (titulo.lookup("#silencio") != null) throw new IllegalStateException("Sonido ya configurado");
        titulo.getChildren().addAll(ControlesDeSonido.crear(control, "silencio", "volumen",
                temaPanel.tarjeta(), temaPanel.texto()));
    }

    public void actualizarRelojes() {
        panel.actualizarRelojes();
    }

    public void configurarNavegacion(Runnable alMenu, Runnable alRevancha) {
        if (navegacion != null) throw new IllegalStateException("Navegación ya configurada");
        navegacion = new NavegacionPartida(controlador, temaPanel, alMenu, alRevancha);
        titulo.getChildren().add(navegacion.volver());
        areaTablero.getChildren().add(navegacion.confirmacion());
        addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, navegacion::cancelarConEscape);
        panel.configurarRevancha(navegacion.revancha());
    }

    /** La tarjeta confirma la intención; el controlador envía la orden al núcleo. */
    public void configurarRendicion() {
        panel.configurarRendicion(color -> {
            controlador.verificarTiempo();
            if (controlador.estado().estaFinalizada()) {
                redibujar();
                return;
            }
            mensaje.setText(controlador.rendirse(color)
                    .map(motivo -> mensajes.rechazoDeAccion(MensajesDeLaInterfaz.Accion.RENDIRSE, motivo))
                    .orElse(""));
            redibujar();
        });
    }

    public void configurarTablas() {
        panel.configurarTablas(color -> mostrarRespuestaTablas(controlador.ofrecerTablas(color)),
                color -> mostrarRespuestaTablas(controlador.aceptarTablas(color)),
                color -> mostrarRespuestaTablas(controlador.rechazarTablas(color)));
    }

    private void mostrarRespuestaTablas(java.util.Optional<Nucleo.juego.AccionInvalidaException.Motivo> rechazo) {
        mensaje.setText(rechazo
                .map(motivo -> mensajes.rechazoDeAccion(MensajesDeLaInterfaz.Accion.TABLAS, motivo))
                .orElse(""));
        redibujar();
    }
}
