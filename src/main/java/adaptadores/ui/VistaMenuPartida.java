package adaptadores.ui;

import Nucleo.ia.Dificultad;
import adaptadores.audio.ControlDeVolumen;
import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.ConfiguracionPartida.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

/** Menú de selección: emite una configuración completa; no crea partidas ni relojes. */
public final class VistaMenuPartida extends GridPane {
    /** Altura desde la que las opciones entran sin juntarlas. */
    private static final double ALTO_COMODO = 810;

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
    private final Button jugar = new Button("Jugar");
    /** La imagen del personaje de la portada (se balancea al abrir el juego). */
    private ImageView figura;
    private HBox pie;
    private final Label pendientes = new Label();

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
        RowConstraints fila = new RowConstraints();
        fila.setPercentHeight(100);
        getRowConstraints().add(fila);
        add(portada(), 0, 0);

        VBox opciones = new VBox(18);
        opciones.setPadding(new Insets(28, 32, 28, 32));
        opciones.setMinWidth(0);
        VBox titulo = new VBox(5, texto("Selección de partida", 28, tema.texto(), true),
                texto("Completá las opciones para empezar.", 14, tema.textoSecundario(), false));
        opciones.getChildren().add(titulo);
        opciones.getChildren().add(seccion("MODO", fila(opcion("Clásico", "modo-clasico", Modo.CLASICO, modo),
                opcion("Fútbol · Posesión y gol", "modo-futbol", Modo.FUTBOL, modo))));
        limiteDeJugadas.setId("limite-jugadas");
        limiteDeJugadas.setEditable(true);
        limiteDeJugadas.setPrefWidth(110);
        limiteDeJugadas.getEditor().setFont(Font.font("System", FontWeight.BOLD, 13));
        limiteDeJugadas.setStyle("-fx-background-color: " + css(tema.borde()) + "; -fx-background-radius: 5;");
        limiteDeJugadas.getEditor().setStyle("-fx-background-color: " + css(tema.tarjeta())
                + "; -fx-text-fill: " + css(tema.texto()) + "; -fx-background-radius: 5 0 0 5;");
        // Lo escrito a mano se toma al salir del campo (y también al apretar Jugar).
        limiteDeJugadas.getEditor().focusedProperty().addListener((p, antes, foco) -> {
            if (!foco) confirmarLimiteEscrito();
        });
        avisoLimite.setId("aviso-limite");
        avisoLimite.setFont(Font.font("System", 12));
        avisoLimite.setTextFill(tema.textoSecundario());
        avisoLimite.setWrapText(true);
        opciones.getChildren().add(seccion("LÍMITE DE JUGADAS (FÚTBOL)", new VBox(6, limiteDeJugadas, avisoLimite)));
        FlowPane tiempos = fila();
        for (Tiempo valor : Tiempo.values()) tiempos.getChildren().add(opcion(valor.texto(),
                "tiempo-" + valor.name().toLowerCase(java.util.Locale.ROOT), valor, tiempo));
        opciones.getChildren().add(seccion("TIEMPO POR JUGADOR", tiempos));
        permitirDeshacer = opcion("Permitir deshacer", "permitir-deshacer", Boolean.TRUE, null);
        avisoDeshacer.setId("aviso-deshacer");
        avisoDeshacer.setFont(Font.font("System", 12));
        avisoDeshacer.setTextFill(tema.textoSecundario());
        opciones.getChildren().add(seccion("DESHACER JUGADAS", new VBox(6, fila(permitirDeshacer), avisoDeshacer)));
        opciones.getChildren().add(seccion("RIVAL", fila(
                opcion("Amigo · misma pantalla", "rival-amigo", Rival.AMIGO, rival),
                opcion("IA · la computadora", "rival-ia", Rival.IA, rival))));
        avisoDificultad.setId("aviso-dificultad");
        avisoDificultad.setFont(Font.font("System", 12));
        avisoDificultad.setTextFill(tema.textoSecundario());
        avisoDificultad.setWrapText(true);
        opciones.getChildren().add(seccion("DIFICULTAD DE LA IA", new VBox(6, fila(
                opcion("Fácil", "dificultad-facil", Dificultad.FACIL, dificultad),
                opcion("Medio", "dificultad-medio", Dificultad.MEDIO, dificultad),
                opcion("Difícil", "dificultad-dificil", Dificultad.DIFICIL, dificultad)), avisoDificultad)));
        VBox equipos = new VBox(10, seleccionEquipo("Jugador 1", "jugador-1", equipo1),
                seleccionEquipo("Jugador 2", "jugador-2", equipo2));
        opciones.getChildren().add(seccion("EQUIPOS", equipos));
        opciones.getChildren().add(seccion("QUIÉN JUEGA CON BLANCAS", fila(
                opcion("Jugador 1 blancas", "color-jugador-1", Colores.JUGADOR_1_BLANCAS, colores),
                opcion("Jugador 2 blancas", "color-jugador-2", Colores.JUGADOR_2_BLANCAS, colores),
                opcion("Aleatorio", "color-aleatorio", Colores.ALEATORIO, colores))));

        pendientes.setId("selecciones-pendientes");
        pendientes.setFont(Font.font("System", 13));
        pendientes.setTextFill(tema.textoSecundario());
        pendientes.setWrapText(true);
        // Puede necesitar varios renglones en ventanas angostas: crece en alto en vez de cortarse.
        pendientes.setMinHeight(Region.USE_PREF_SIZE);
        pendientes.setMinWidth(0);
        pendientes.setMaxWidth(Double.MAX_VALUE);
        jugar.setId("jugar");
        jugar.setFont(Font.font("System", FontWeight.BOLD, 17));
        jugar.setTextFill(tema.fondo());
        jugar.setBackground(fondo(tema.acento()));
        jugar.setPadding(new Insets(12, 38, 12, 38));
        jugar.setMinWidth(150);
        jugar.setPrefWidth(150);
        jugar.setMaxWidth(150);
        jugar.setCursor(Cursor.HAND);
        jugar.focusedProperty().addListener((p, antes, foco) -> pintarJugar());
        pintarJugar();
        jugar.setOnAction(e -> emitirConfiguracion());
        pie = new HBox(16, pendientes, jugar);
        pie.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(pendientes, Priority.ALWAYS);
        pie.setBackground(fondo(tema.tarjeta()));

        ScrollPane desplazamiento = new ScrollPane(opciones);
        desplazamiento.setId("opciones-menu");
        desplazamiento.setFitToWidth(true);
        desplazamiento.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamiento.setMinSize(0, 0);
        desplazamiento.setStyle("-fx-background: " + css(tema.fondo()) + "; -fx-background-color: " + css(tema.fondo()) + ";");
        // "Jugar" queda fijo debajo de las opciones: se ve siempre, aunque haya que desplazarlas.
        VBox columnaOpciones = new VBox(desplazamiento, pie);
        columnaOpciones.setMinSize(0, 0);
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);
        add(columnaOpciones, 1, 0);
        heightProperty().addListener((p, antes, alto) -> ajustarDensidad(opciones, pie, alto.doubleValue()));
        ajustarDensidad(opciones, pie, getHeight());
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

    /**
     * Agrega al pie el botón de silencio, con el mismo control de volumen que la partida: lo que se elige
     * acá sigue valiendo al jugar.
     */
    public void configurarSonido(ControlDeVolumen control) {
        Objects.requireNonNull(control, "control");
        if (pie.lookup("#silencio-menu") != null) throw new IllegalStateException("Sonido ya configurado");
        Button silencio = BotonDeCabecera.crear(textoSilencio(control), "silencio-menu", tema.borde(), tema.texto());
        // El texto de "Falta elegir" ocupa el resto: el botón nunca se achica hasta cortarse.
        silencio.setMinWidth(Region.USE_PREF_SIZE);
        silencio.setOnAction(e -> {
            control.silenciar(!control.silenciado());
            silencio.setText(textoSilencio(control));
        });
        pie.getChildren().addFirst(silencio);
    }

    private static String textoSilencio(ControlDeVolumen control) {
        return control.silenciado() ? "Sonido: no" : "Sonido: sí";
    }

    /** En pantallas bajas junta las opciones para que entren sin desplazarlas. */
    private void ajustarDensidad(VBox opciones, HBox pie, double alto) {
        boolean compacto = alto < ALTO_COMODO;
        opciones.setSpacing(compacto ? 11 : 18);
        opciones.setPadding(compacto ? new Insets(16, 32, 8, 32) : new Insets(28, 32, 12, 32));
        pie.setPadding(compacto ? new Insets(8, 32, 12, 32) : new Insets(12, 32, 20, 32));
    }

    private static Image cargarPersonaje() {
        var recurso = Objects.requireNonNull(VistaMenuPartida.class.getResource("/personaje/chiquichess.png"),
                "Falta la imagen del personaje");
        Image imagen = new Image(recurso.toExternalForm(), false);
        if (imagen.isError()) throw new IllegalStateException("No se pudo cargar la imagen del personaje", imagen.getException());
        return imagen;
    }

    private VBox portada() {
        Text marca = texto("CHIQUICHESS", 32, tema.texto(), true);
        Text subtitulo = texto("AJEDREZ ARGENTINO", 13, tema.acento(), true);
        figura = new ImageView(cargarPersonaje());
        figura.setId("personaje");
        figura.setPreserveRatio(true);
        figura.setSmooth(true);
        StackPane personaje = new StackPane(figura);
        figura.fitWidthProperty().bind(javafx.beans.binding.Bindings.max(1, personaje.widthProperty().subtract(24)));
        figura.fitHeightProperty().bind(javafx.beans.binding.Bindings.max(1, personaje.heightProperty().subtract(24)));
        personaje.setId("espacio-personaje");
        personaje.setMinSize(0, 0);
        personaje.setPrefHeight(370);
        personaje.setMaxHeight(Double.MAX_VALUE);
        personaje.setBackground(new Background(new BackgroundFill(tema.fondo(), new CornerRadii(12), Insets.EMPTY)));
        personaje.setBorder(new Border(new BorderStroke(tema.borde(), BorderStrokeStyle.DASHED,
                new CornerRadii(12), new BorderWidths(1))));
        personaje.setAccessibleText("Personaje de ChiquiChess");
        Text leyenda = texto("Elegí tu equipo.\nJugá tu clásico.", 23, tema.texto(), true);
        leyenda.wrappingWidthProperty().bind(javafx.beans.binding.Bindings.max(1, widthProperty().multiply(.4).subtract(64)));
        marca.fontProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(() ->
                Font.font("System", FontWeight.BOLD, Math.max(21, Math.min(34, getWidth() * .027))), widthProperty()));
        VBox portada = new VBox(14, marca, subtitulo, personaje, leyenda);
        portada.setId("portada-menu");
        portada.setPadding(new Insets(32));
        portada.setMinWidth(0);
        portada.setBackground(fondo(tema.tarjeta()));
        portada.setBorder(new Border(new BorderStroke(tema.borde(), BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(0, 1, 0, 0))));
        VBox.setVgrow(personaje, Priority.ALWAYS);
        return portada;
    }

    private VBox seleccionEquipo(String jugador, String id, ToggleGroup grupo) {
        FlowPane botones = fila();
        for (Equipo equipo : Equipo.values()) {
            ToggleButton boton = opcion(equipo.nombre(),
                    id + "-" + equipo.name().toLowerCase(java.util.Locale.ROOT), equipo, grupo);
            var imagen = new javafx.scene.image.ImageView(PresentacionClub.de(equipo, tema).escudo());
            imagen.setFitWidth(26);
            imagen.setFitHeight(30);
            imagen.setPreserveRatio(true);
            imagen.setSmooth(true);
            imagen.setMouseTransparent(true);
            boton.setGraphic(imagen);
            boton.setGraphicTextGap(7);
            botones.getChildren().add(boton);
        }
        return new VBox(6, texto(jugador, 14, tema.texto(), true), botones);
    }

    private ToggleButton opcion(String texto, String id, Object valor, ToggleGroup grupo) {
        ToggleButton boton = new ToggleButton(texto);
        boton.setId(id);
        boton.setUserData(valor);
        boton.setToggleGroup(grupo);
        boton.setFont(Font.font("System", FontWeight.BOLD, 13));
        boton.setPadding(new Insets(9, 12, 9, 12));
        boton.setCursor(Cursor.HAND);
        boton.selectedProperty().addListener((p, antes, ahora) -> pintarOpcion(boton));
        boton.focusedProperty().addListener((p, antes, ahora) -> pintarOpcion(boton));
        pintarOpcion(boton);
        return boton;
    }

    private void pintarOpcion(ToggleButton boton) {
        boton.setStyle("-fx-background-color: " + css(boton.isSelected() ? tema.acento() : tema.tarjeta())
                + "; -fx-text-fill: " + css(boton.isSelected() ? tema.fondo() : tema.texto())
                + "; -fx-border-color: " + css(boton.isFocused() ? tema.texto() :
                boton.isSelected() ? tema.acento() : tema.borde())
                + "; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-width: 1;");
    }

    private void pintarJugar() {
        jugar.setStyle("-fx-background-color: " + css(tema.acento()) + "; -fx-text-fill: " + css(tema.fondo())
                + "; -fx-border-color: " + css(jugar.isFocused() ? tema.texto() : tema.acento())
                + "; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-width: 2;");
    }

    private void actualizarSeleccion() {
        bloquearEquipo(equipo1, equipo2);
        bloquearEquipo(equipo2, equipo1);
        // Deshacer solo existe en partidas sin tiempo: con un tiempo elegido se apaga y se deshabilita.
        boolean sinTiempo = tiempo.getSelectedToggle() != null
                && tiempo.getSelectedToggle().getUserData() == Tiempo.SIN_TIEMPO;
        if (!sinTiempo) permitirDeshacer.setSelected(false);
        permitirDeshacer.setDisable(!sinTiempo);
        avisoDeshacer.setText(sinTiempo ? "Cualquiera de los dos podrá deshacer y rehacer jugadas."
                : "Disponible solo en partidas sin tiempo.");
        // La dificultad solo existe contra la IA: con otro rival se borra y se deshabilita.
        boolean contraLaIa = rival.getSelectedToggle() != null && rival.getSelectedToggle().getUserData() == Rival.IA;
        if (!contraLaIa && dificultad.getSelectedToggle() != null) dificultad.selectToggle(null);
        for (Toggle opcion : dificultad.getToggles()) ((ToggleButton) opcion).setDisable(!contraLaIa);
        avisoDificultad.setText(contraLaIa ? "La IA juega como Jugador 2, con su equipo."
                : "Disponible solo contra la IA.");
        boolean futbol = modo.getSelectedToggle() != null && modo.getSelectedToggle().getUserData() == Modo.FUTBOL;
        limiteDeJugadas.setDisable(!futbol);
        avisoLimite.setText(futbol
                ? "Jugadas de los dos juntos (de " + ConfiguracionPartida.LIMITE_MINIMO + " a "
                + ConfiguracionPartida.LIMITE_MAXIMO + "). Al llegar, gana quien tenga más goles."
                : "Disponible solo en el modo fútbol.");
        var faltantes = new ArrayList<String>();
        if (modo.getSelectedToggle() == null) faltantes.add("modo");
        if (tiempo.getSelectedToggle() == null) faltantes.add("tiempo");
        if (rival.getSelectedToggle() == null) faltantes.add("rival");
        if (contraLaIa && dificultad.getSelectedToggle() == null) faltantes.add("dificultad");
        if (equipo1.getSelectedToggle() == null) faltantes.add("equipo de Jugador 1");
        if (equipo2.getSelectedToggle() == null) faltantes.add("equipo de Jugador 2");
        if (colores.getSelectedToggle() == null) faltantes.add("colores");
        jugar.setDisable(!faltantes.isEmpty());
        pendientes.setText(faltantes.isEmpty() ? "Todo listo para jugar." : "Falta elegir: " + String.join(", ", faltantes) + ".");
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

    private VBox seccion(String titulo, javafx.scene.Node contenido) {
        return new VBox(8, texto(titulo, 12, tema.textoSecundario(), true), contenido);
    }

    private FlowPane fila(javafx.scene.Node... botones) {
        return new FlowPane(8, 8, botones);
    }

    private Text texto(String valor, double tamano, javafx.scene.paint.Color color, boolean negrita) {
        Text texto = new Text(valor);
        texto.setFont(Font.font("System", negrita ? FontWeight.BOLD : FontWeight.NORMAL, tamano));
        texto.setFill(color);
        return texto;
    }

    private Background fondo(javafx.scene.paint.Color color) {
        return new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY));
    }

    private String css(javafx.scene.paint.Color color) {
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
    }
}
