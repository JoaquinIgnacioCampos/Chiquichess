package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.tablero.Posicion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
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
    private final HBox titulo = new HBox(16);
    private final StackPane areaTablero = new StackPane();
    private final TemaTablero tema;
    private final RepresentacionPiezas representacion;
    /** Se crea recién la primera vez que un peón promueve: la mayoría de las partidas no lo necesita. */
    private SelectorPromocion selectorPromocion;
    private final TemaPanel temaPanel;
    private final MensajesDeLaInterfaz mensajes;
    private NavegacionPartida navegacion;
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
        setMinSize(0, 0);
        setBackground(new Background(new BackgroundFill(tema.fondo(), CornerRadii.EMPTY, Insets.EMPTY)));
        BorderPane juego = new BorderPane();
        juego.setId("area-juego");
        juego.setMinSize(0, 0);
        juego.setPadding(new Insets(24));
        Text chiqui = new Text("CHIQUI");
        chiqui.setFill(temaPanel.texto());
        chiqui.setFont(Font.font("System", FontWeight.BOLD, 22));
        Text chess = new Text("CHESS");
        chess.setFill(temaPanel.acento());
        chess.setFont(Font.font("System", FontWeight.BOLD, 22));
        titulo.getChildren().add(new HBox(chiqui, chess));
        titulo.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(titulo, new Insets(0, 0, 16, 0));
        juego.setTop(titulo);
        areaTablero.setMinSize(0, 0);
        this.tema = tema;
        this.representacion = representacion;
        areaTablero.getChildren().add(tablero);
        juego.setCenter(areaTablero);
        setCenter(juego);
        panel = new PanelEstadoPartida(controlador, temaPanel, clubes, mensajes);
        panel.prefWidthProperty().bind(widthProperty().multiply(0.35));
        setRight(panel);
        mensaje.setId("mensaje-movimiento");
        mensaje.setFill(temaPanel.texto());
        mensaje.setFont(Font.font(16));
        mensaje.wrappingWidthProperty().bind(juego.widthProperty().subtract(48));
        mensaje.managedProperty().bind(mensaje.textProperty().isNotEmpty());
        mensaje.visibleProperty().bind(mensaje.textProperty().isNotEmpty());
        BorderPane.setMargin(mensaje, new Insets(12, 0, 0, 0));
        juego.setBottom(mensaje);
        redibujar();
    }

    private void pulsar(Posicion posicion) {
        mensaje.setText(controlador.pulsar(posicion).map(mensajes::rechazoDeMovimiento).orElse(""));
        redibujar();
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

    public void redibujar() {
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
        deshacer = BotonDeCabecera.crear("Deshacer", "deshacer", temaPanel.tarjeta(), temaPanel.texto());
        rehacer = BotonDeCabecera.crear("Rehacer", "rehacer", temaPanel.tarjeta(), temaPanel.texto());
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
