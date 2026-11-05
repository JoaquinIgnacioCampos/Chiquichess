package adaptadores.ui;

import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.ConfiguracionPartida.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

/** Menú de selección: emite una configuración completa; no crea partidas ni relojes. */
public final class VistaMenuPartida extends GridPane {
    private final TemaPanel tema;
    private final Consumer<ConfiguracionPartida> iniciar;
    private final ToggleGroup modo = new ToggleGroup();
    private final ToggleGroup tiempo = new ToggleGroup();
    private final ToggleGroup rival = new ToggleGroup();
    private final ToggleGroup equipo1 = new ToggleGroup();
    private final ToggleGroup equipo2 = new ToggleGroup();
    private final ToggleGroup colores = new ToggleGroup();
    private final ToggleButton permitirDeshacer;
    private final Label avisoDeshacer = new Label();
    private final Button jugar = new Button("Jugar");
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
        opciones.getChildren().add(seccion("MODO", fila(opcion("Clásico", "modo-clasico", Modo.CLASICO, modo))));
        FlowPane tiempos = fila();
        for (Tiempo valor : Tiempo.values()) tiempos.getChildren().add(opcion(valor.texto(),
                "tiempo-" + valor.name().toLowerCase(java.util.Locale.ROOT), valor, tiempo));
        opciones.getChildren().add(seccion("TIEMPO POR JUGADOR", tiempos));
        permitirDeshacer = opcion("Permitir deshacer", "permitir-deshacer", Boolean.TRUE, null);
        avisoDeshacer.setId("aviso-deshacer");
        avisoDeshacer.setFont(Font.font("System", 12));
        avisoDeshacer.setTextFill(tema.textoSecundario());
        opciones.getChildren().add(seccion("DESHACER JUGADAS", new VBox(6, fila(permitirDeshacer), avisoDeshacer)));
        ToggleButton ia = opcion("IA · Próximamente", "rival-ia", Rival.IA, rival);
        ia.setDisable(true);
        opciones.getChildren().add(seccion("RIVAL", fila(
                opcion("Amigo · misma pantalla", "rival-amigo", Rival.AMIGO, rival), ia)));
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
        HBox pie = new HBox(16, pendientes, jugar);
        pie.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(pendientes, Priority.ALWAYS);
        opciones.getChildren().add(pie);

        ScrollPane desplazamiento = new ScrollPane(opciones);
        desplazamiento.setId("opciones-menu");
        desplazamiento.setFitToWidth(true);
        desplazamiento.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamiento.setMinSize(0, 0);
        desplazamiento.setStyle("-fx-background: " + css(tema.fondo()) + "; -fx-background-color: " + css(tema.fondo()) + ";");
        add(desplazamiento, 1, 0);
        for (ToggleGroup grupo : new ToggleGroup[]{modo, tiempo, rival, equipo1, equipo2, colores}) {
            grupo.selectedToggleProperty().addListener((p, antes, ahora) -> actualizarSeleccion());
        }
        actualizarSeleccion();
    }

    private VBox portada() {
        Text marca = texto("CHIQUICHESS", 32, tema.texto(), true);
        Text subtitulo = texto("AJEDREZ ARGENTINO", 13, tema.acento(), true);
        Text simbolo = texto("♚", 190, tema.acento(), false);
        simbolo.setFont(Font.font("Segoe UI Symbol", 190));
        StackPane personaje = new StackPane(simbolo);
        personaje.setId("espacio-personaje");
        personaje.setMinSize(0, 0);
        personaje.setPrefHeight(370);
        personaje.setMaxHeight(Double.MAX_VALUE);
        personaje.setBackground(new Background(new BackgroundFill(tema.fondo(), new CornerRadii(12), Insets.EMPTY)));
        personaje.setBorder(new Border(new BorderStroke(tema.borde(), BorderStrokeStyle.DASHED,
                new CornerRadii(12), new BorderWidths(1))));
        personaje.setAccessibleText("Espacio reservado para el personaje de ChiquiChess");
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
        var faltantes = new ArrayList<String>();
        if (modo.getSelectedToggle() == null) faltantes.add("modo");
        if (tiempo.getSelectedToggle() == null) faltantes.add("tiempo");
        if (rival.getSelectedToggle() == null) faltantes.add("rival");
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
                (Colores) colores.getSelectedToggle().getUserData(), permitirDeshacer.isSelected());
        iniciar.accept(seleccion);
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
