package adaptadores.ui;

import Nucleo.juego.Jugador;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.control.Button;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import Nucleo.bando.Bando;
/** Identificación y acciones de presentación de un jugador, sin reglas del juego. */
final class TarjetaJugador extends StackPane {
    private enum Panel { NORMAL, RENDICION, OFERTA, RESPUESTA }
    private final TemaPanel tema;
    private final Bando color;
    private final Text nombre = new Text();
    private final Text nombreClub = new Text();
    private final Text bando = new Text();
    private final VBox identidad = new VBox();
    private final VistaReloj reloj;
    private final StackPane acciones = new StackPane();
    private Button rendirse;
    private Button ofrecerTablas;
    private final VBox contenido = new VBox(12);
    private final HBox barraAcciones = new HBox(8);
    private final Text esperando = new Text("Esperando respuesta");
    private ConfirmacionAccionJavaFx confirmacion;
    private ConfirmacionAccionJavaFx confirmarOferta;
    private ConfirmacionAccionJavaFx responderOferta;
    private Panel panelVisible = Panel.NORMAL;
    private final StackPane insignia;

    TarjetaJugador(Bando color, TemaPanel tema) {
        this(color, tema, null);
    }

    TarjetaJugador(Bando color, TemaPanel temaBase, PresentacionClub club) {
        TemaPanel tema = club == null ? temaBase : club.tema();
        this.tema = tema;
        this.color = color;
        setId(color == Bando.BLANCO ? "jugador-blanco" : "jugador-negro");
        nombre.setId(color == Bando.BLANCO ? "nombre-blanco" : "nombre-negro");
        nombre.setFill(tema.texto());
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER_LEFT);
        contenido.setAlignment(Pos.CENTER_LEFT);
        setBackground(new Background(new BackgroundFill(tema.tarjeta(), CornerRadii.EMPTY, Insets.EMPTY)));

        Text rol = new Text(color == Bando.BLANCO ? "LOCAL" : "VISITANTE");
        rol.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        rol.setFill(tema.textoSecundario());
        reloj = new VistaReloj(color, tema);
        reloj.prefWidthProperty().bind(widthProperty().multiply(0.35));
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(10, new Rectangle(4, 15, tema.acento()), rol, espacio, reloj);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Polygon escudo = new Polygon(32, 0, 62, 10, 60, 46, 51, 63, 32, 76, 13, 63, 4, 46, 2, 10);
        escudo.setFill(color == Bando.BLANCO ? tema.insigniaBlanca() : tema.insigniaNegra());
        escudo.setStroke(tema.borde());
        escudo.setStrokeWidth(2);
        Text peon = new Text(color == Bando.BLANCO ? "♙" : "♟");
        peon.setFont(Font.font("Segoe UI Symbol", 44));
        peon.setFill(color == Bando.BLANCO ? tema.insigniaNegra() : tema.insigniaBlanca());
        insignia = new StackPane();
        if (club == null) {
            insignia.getChildren().addAll(escudo, peon);
        } else {
            ImageView imagen = new ImageView(club.escudo());
            imagen.setId("escudo-" + sufijo());
            imagen.setAccessibleText("Escudo de " + club.equipo().nombre());
            imagen.setPreserveRatio(true);
            imagen.setFitWidth(64);
            imagen.setFitHeight(80);
            imagen.setSmooth(true);
            insignia.getChildren().add(imagen);
            nombreClub.setText(club.equipo().nombre());
        }
        insignia.setMinSize(64, 80);
        insignia.setPrefSize(64, 80);
        insignia.setMaxSize(64, 80);

        bando.setText(color == Bando.BLANCO ? "BLANCAS" : "NEGRAS");
        bando.setFill(tema.textoSecundario());
        bando.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        nombreClub.setId("club-" + sufijo());
        nombreClub.setAccessibleText(nombreClub.getText());
        nombreClub.setFill(tema.acento());
        nombreClub.setVisible(club != null);
        nombreClub.setManaged(club != null);
        identidad.getChildren().addAll(nombre, nombreClub, bando);
        identidad.setMinWidth(0);
        identidad.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(identidad, Priority.ALWAYS);
        HBox cuerpo = new HBox(16, new Group(insignia), identidad);
        cuerpo.setAlignment(Pos.CENTER_LEFT);
        cuerpo.setMinHeight(0);
        acciones.getChildren().add(cuerpo);
        VBox.setVgrow(acciones, Priority.ALWAYS);
        barraAcciones.setAlignment(Pos.CENTER_RIGHT);
        barraAcciones.setVisible(false);
        barraAcciones.setManaged(false);
        esperando.setId("esperando-tablas-" + sufijo());
        esperando.setFill(tema.acento());
        esperando.setFont(Font.font("System", FontWeight.BOLD, 12));
        esperando.setVisible(false);
        esperando.setManaged(false);
        contenido.getChildren().addAll(cabecera, acciones, barraAcciones);
        getChildren().add(contenido);
        widthProperty().addListener((propiedad, antes, ahora) -> ajustarNombre());
        heightProperty().addListener((propiedad, antes, ahora) -> ajustarDistribucion());
        ajustarDistribucion();
        ajustarNombre();
    }

    private void ajustarNombre() {
        nombre.setFont(Font.font("System", FontWeight.BOLD, Math.max(17,
                Math.min(getHeight() < 200 ? 20 : 32, getWidth() * 0.075))));
        nombre.setWrappingWidth(Math.max(1, getWidth() - 120));
    }

    private void ajustarDistribucion() {
        boolean compacta = getHeight() < 200;
        setPadding(new Insets(compacta ? 10 : 14, compacta ? 16 : 20, compacta ? 10 : 14, compacta ? 16 : 20));
        contenido.setSpacing(compacta ? 5 : 8);
        identidad.setSpacing(compacta ? 3 : 5);
        nombreClub.setFont(Font.font("System", FontWeight.BOLD, compacta ? 11 : 14));
        bando.setFont(Font.font("Consolas", FontWeight.BOLD, compacta ? 11 : 13));
        insignia.setScaleX(compacta ? 0.6 : 1);
        insignia.setScaleY(compacta ? 0.6 : 1);
        ajustarNombre();
    }

    private String sufijo() {
        return color == Bando.BLANCO ? "blanco" : "negro";
    }

    void actualizar(Jugador jugador, boolean activo) {
        nombre.setText(jugador.nombre());
        nombre.setAccessibleText(jugador.nombre());
        getStyleClass().remove("jugador-activo");
        if (activo) getStyleClass().add("jugador-activo");
        setBorder(new Border(new BorderStroke(activo ? tema.activo() : tema.borde(),
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(activo ? 2 : 1))));
        setEffect(activo ? new DropShadow(14, tema.activo().deriveColor(0, 1, 1, 0.2)) : null);
    }

    void actualizarReloj(java.util.Optional<java.time.Duration> restante, boolean activo) {
        reloj.actualizar(restante, activo);
    }

    /** Las acciones se conectan desde el ensamblado; la tarjeta solo emite el clic. */
    void configurarRendicion(Runnable solicitar) {
        if (rendirse != null) throw new IllegalStateException("Rendición ya configurada");
        rendirse = new Button("Rendirse");
        rendirse.setId(getId().equals("jugador-blanco") ? "rendirse-blanco" : "rendirse-negro");
        rendirse.setAccessibleText(getId().equals("jugador-blanco") ? "Rendirse: blancas" : "Rendirse: negras");
        rendirse.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        rendirse.setTextFill(tema.alerta());
        rendirse.setBackground(new Background(new BackgroundFill(tema.tarjeta(), new CornerRadii(4), Insets.EMPTY)));
        rendirse.setBorder(new Border(new BorderStroke(tema.alerta(), BorderStrokeStyle.SOLID,
                new CornerRadii(4), new BorderWidths(1))));
        rendirse.setPadding(new Insets(5, 12, 5, 12));
        rendirse.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        rendirse.setCursor(javafx.scene.Cursor.HAND);
        rendirse.focusedProperty().addListener((p, a, foco) -> rendirse.setBorder(new Border(
                new BorderStroke(foco ? tema.acento() : tema.alerta(), BorderStrokeStyle.SOLID,
                        new CornerRadii(4), new BorderWidths(foco ? 2 : 1)))));
        confirmacion = new ConfirmacionAccionJavaFx(tema, "¿Seguro que querés\nrendirte?", "Rendirse", "Cancelar",
                "rendicion", tema.alerta(), () -> {
            mostrar(Panel.NORMAL);
            solicitar.run();
        }, () -> mostrar(Panel.NORMAL));
        confirmacion.setId(getId().equals("jugador-blanco") ? "confirmacion-blanco" : "confirmacion-negro");
        confirmacion.setVisible(false);
        confirmacion.setManaged(false);
        getChildren().add(confirmacion);
        rendirse.setOnAction(evento -> mostrar(Panel.RENDICION));
        barraAcciones.getChildren().add(rendirse);
        barraAcciones.setVisible(true);
        barraAcciones.setManaged(true);
    }

    void habilitarRendicion(boolean habilitada) {
        if (rendirse != null) rendirse.setDisable(!habilitada);
        if (!habilitada && panelVisible == Panel.RENDICION) mostrar(Panel.NORMAL);
    }

    void configurarTablas(Runnable ofrecer, Runnable aceptar, Runnable rechazar) {
        if (ofrecerTablas != null) throw new IllegalStateException("Tablas ya configuradas");
        ofrecerTablas = new Button("Ofrecer tablas");
        ofrecerTablas.setId("ofrecer-tablas-" + sufijo());
        ofrecerTablas.setAccessibleText("Ofrecer tablas: " + (color == Bando.BLANCO ? "blancas" : "negras"));
        ofrecerTablas.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        ofrecerTablas.setTextFill(tema.acento());
        ofrecerTablas.setPadding(new Insets(5, 8, 5, 8));
        ofrecerTablas.setBackground(new Background(new BackgroundFill(tema.tarjeta(), new CornerRadii(4), Insets.EMPTY)));
        ofrecerTablas.setBorder(new Border(new BorderStroke(tema.acento(), BorderStrokeStyle.SOLID,
                new CornerRadii(4), new BorderWidths(1))));
        ofrecerTablas.setCursor(javafx.scene.Cursor.HAND);
        ofrecerTablas.focusedProperty().addListener((p, antes, foco) -> ofrecerTablas.setBorder(new Border(
                new BorderStroke(foco ? tema.texto() : tema.acento(), BorderStrokeStyle.SOLID,
                        new CornerRadii(4), new BorderWidths(foco ? 2 : 1)))));
        confirmarOferta = new ConfirmacionAccionJavaFx(tema, "¿Querés ofrecer\ntablas al rival?",
                "Ofrecer tablas", "Cancelar", "oferta-tablas", tema.acento(), () -> {
            mostrar(Panel.NORMAL);
            ofrecer.run();
        }, () -> mostrar(Panel.NORMAL));
        confirmarOferta.setId("confirmar-oferta-" + sufijo());
        responderOferta = new ConfirmacionAccionJavaFx(tema, "Te ofrecieron tablas",
                "Aceptar", "Rechazar", "respuesta-tablas", tema.activo(), aceptar, rechazar);
        responderOferta.setId("respuesta-tablas-" + sufijo());
        for (var panel : new ConfirmacionAccionJavaFx[]{confirmarOferta, responderOferta}) {
            panel.setVisible(false);
            panel.setManaged(false);
            getChildren().add(panel);
        }
        ofrecerTablas.setOnAction(e -> mostrar(Panel.OFERTA));
        barraAcciones.getChildren().addAll(0, java.util.List.of(ofrecerTablas, esperando));
        barraAcciones.setVisible(true);
        barraAcciones.setManaged(true);
    }

    /** Los permisos y la oferta vigente se reciben del estado actual del núcleo. */
    void actualizarTablas(boolean puedeOfrecer, boolean ofertaPropia, boolean ofertaRival) {
        if (ofrecerTablas == null) return;
        ofrecerTablas.setDisable(!puedeOfrecer);
        ofrecerTablas.setVisible(!ofertaPropia);
        ofrecerTablas.setManaged(!ofertaPropia);
        esperando.setVisible(ofertaPropia);
        esperando.setManaged(ofertaPropia);
        if (ofertaRival) {
            mostrar(Panel.RESPUESTA);
        } else if (panelVisible == Panel.RESPUESTA || panelVisible == Panel.OFERTA && !puedeOfrecer) {
            mostrar(Panel.NORMAL);
        }
    }

    private void mostrar(Panel panel) {
        if (panelVisible == panel) return;
        panelVisible = panel;
        contenido.setVisible(panel == Panel.NORMAL);
        ConfirmacionAccionJavaFx enfocar = null;
        if (confirmacion != null) {
            confirmacion.setVisible(panel == Panel.RENDICION);
            confirmacion.setManaged(panel == Panel.RENDICION);
            if (panel == Panel.RENDICION) enfocar = confirmacion;
        }
        if (confirmarOferta != null) {
            confirmarOferta.setVisible(panel == Panel.OFERTA);
            confirmarOferta.setManaged(panel == Panel.OFERTA);
            responderOferta.setVisible(panel == Panel.RESPUESTA);
            responderOferta.setManaged(panel == Panel.RESPUESTA);
            if (panel == Panel.OFERTA) enfocar = confirmarOferta;
            if (panel == Panel.RESPUESTA) enfocar = responderOferta;
        }
        if (enfocar != null) enfocar.enfocarCancelar();
    }
}
