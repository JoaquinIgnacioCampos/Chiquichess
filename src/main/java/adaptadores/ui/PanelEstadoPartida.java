package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.Marcador;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Objects;
import java.util.Optional;

/** Marcador: presenta el estado del núcleo y deriva las solicitudes de los jugadores. */
public final class PanelEstadoPartida extends GridPane {
    private final ControladorTablero controlador;
    private final TemaPanel tema;
    private final TarjetaJugador negras;
    private final TarjetaJugador blancas;
    private final Text titulo = new Text();
    private final TextoLed encabezado;
    private final TextoLed principal;
    private final Text estado = new Text();
    private final Text motivo = new Text();
    private final Circle punto = new Circle(5);
    /** Modo fútbol: los goles de cada bando y cuántas jugadas van del límite. Vacíos en el clásico. */
    private final Text goles = new Text();
    private final Text jugadas = new Text();
    /** Goles totales en el último refresco, para anunciar el gol recién hecho. */
    private int golesVistos = 0;
    private final VBox informacion;
    private final MensajesDeLaInterfaz mensajes;

    public PanelEstadoPartida(ControladorTablero controlador, TemaPanel tema) {
        this(controlador, tema, java.util.Map.of());
    }

    public PanelEstadoPartida(ControladorTablero controlador, TemaPanel tema,
                               java.util.Map<Bando, PresentacionClub> clubes) {
        this(controlador, tema, clubes, new MensajesDeLaInterfaz());
    }

    public PanelEstadoPartida(ControladorTablero controlador, TemaPanel tema,
                               java.util.Map<Bando, PresentacionClub> clubes, MensajesDeLaInterfaz mensajes) {
        this.controlador = Objects.requireNonNull(controlador, "controlador");
        this.mensajes = Objects.requireNonNull(mensajes, "mensajes");
        this.tema = Objects.requireNonNull(tema, "tema");
        Objects.requireNonNull(clubes, "clubes");
        setId("panel-estado");
        setMinSize(0, 0);
        setBackground(new Background(new BackgroundFill(tema.fondo(), CornerRadii.EMPTY, Insets.EMPTY)));
        ColumnConstraints columna = new ColumnConstraints();
        columna.setPercentWidth(100);
        getColumnConstraints().add(columna);
        for (double porcentaje : new double[]{27, 46, 27}) {
            RowConstraints fila = new RowConstraints();
            fila.setPercentHeight(porcentaje);
            fila.setMinHeight(0);
            getRowConstraints().add(fila);
        }
        negras = new TarjetaJugador(Bando.NEGRO, tema, clubes.get(Bando.NEGRO));
        blancas = new TarjetaJugador(Bando.BLANCO, tema, clubes.get(Bando.BLANCO));
        encabezado = new TextoLed(tema.acento());
        encabezado.setId("marcador-encabezado");
        principal = new TextoLed(tema.texto());
        principal.setId("marcador-principal");
        titulo.setId("titulo-estado");
        titulo.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        titulo.setFill(tema.textoSecundario());
        estado.setId("estado-partida");
        estado.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        HBox estadoActual = new HBox(10, punto, estado);
        estadoActual.setAlignment(Pos.CENTER);
        motivo.setId("motivo-final");
        motivo.setFill(tema.textoSecundario());
        motivo.setFont(Font.font("System", 14));
        motivo.setTextAlignment(TextAlignment.CENTER);
        motivo.managedProperty().bind(motivo.textProperty().isNotEmpty());
        motivo.visibleProperty().bind(motivo.textProperty().isNotEmpty());
        goles.setId("goles-partida");
        goles.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        goles.setFill(tema.texto());
        jugadas.setId("jugadas-futbol");
        jugadas.setFont(Font.font("Consolas", 12));
        jugadas.setFill(tema.textoSecundario());
        for (Text texto : new Text[]{goles, jugadas}) {
            texto.managedProperty().bind(texto.textProperty().isNotEmpty());
            texto.visibleProperty().bind(texto.textProperty().isNotEmpty());
        }
        Text modalidad = new Text(controlador.marcador().isPresent()
                ? "Partida local · Fútbol: posesión y gol" : "Partida local");
        modalidad.setFill(tema.textoSecundario());
        modalidad.setFont(Font.font("Consolas", 12));
        Region separador = new Region();
        separador.setMinHeight(1);
        separador.setPrefHeight(1);
        separador.setMaxHeight(1);
        separador.setBackground(new Background(new BackgroundFill(tema.borde(), CornerRadii.EMPTY, Insets.EMPTY)));
        informacion = new VBox(12, titulo, encabezado, principal, estadoActual, goles, jugadas, separador,
                motivo, modalidad);
        informacion.setPadding(new Insets(20));
        informacion.setAlignment(Pos.CENTER);
        informacion.setMinSize(0, 0);
        StackPane marcador = new StackPane();
        marcador.setMinSize(0, 0);
        Canvas trama = new Canvas();
        trama.setManaged(false);
        trama.setMouseTransparent(true);
        trama.widthProperty().bind(marcador.widthProperty());
        trama.heightProperty().bind(marcador.heightProperty());
        trama.widthProperty().addListener((p, a, n) -> dibujarTrama(trama));
        trama.heightProperty().addListener((p, a, n) -> dibujarTrama(trama));
        motivo.wrappingWidthProperty().bind(marcador.widthProperty().subtract(40));
        marcador.heightProperty().addListener((p, a, n) -> informacion.setSpacing(
                Math.max(7, Math.min(16, n.doubleValue() * 0.035))));
        marcador.getChildren().addAll(trama, informacion);
        add(negras, 0, 0);
        add(marcador, 0, 1);
        add(blancas, 0, 2);
        actualizar();
    }

    private void dibujarTrama(Canvas trama) {
        var dibujo = trama.getGraphicsContext2D();
        dibujo.clearRect(0, 0, trama.getWidth(), trama.getHeight());
        dibujo.setFill(tema.puntoApagado());
        for (double x = 3; x < trama.getWidth(); x += 8) {
            for (double y = 3; y < trama.getHeight(); y += 8) dibujo.fillOval(x, y, 2, 2);
        }
    }

    public void actualizar() {
        boolean finalizada = controlador.estado().estaFinalizada();
        boolean golRecien = actualizarGoles();
        negras.habilitarRendicion(!finalizada && controlador.turno() == Bando.NEGRO);
        blancas.habilitarRendicion(!finalizada && controlador.turno() == Bando.BLANCO);
        var oferta = controlador.ofertaTablasPendiente();
        negras.actualizarTablas(!finalizada && controlador.turno() == Bando.NEGRO && oferta.isEmpty(),
                oferta.filter(color -> color == Bando.NEGRO).isPresent(),
                oferta.filter(color -> color == Bando.BLANCO).isPresent());
        blancas.actualizarTablas(!finalizada && controlador.turno() == Bando.BLANCO && oferta.isEmpty(),
                oferta.filter(color -> color == Bando.BLANCO).isPresent(),
                oferta.filter(color -> color == Bando.NEGRO).isPresent());
        negras.actualizar(controlador.jugador(Bando.NEGRO), !finalizada && controlador.turno() == Bando.NEGRO);
        blancas.actualizar(controlador.jugador(Bando.BLANCO), !finalizada && controlador.turno() == Bando.BLANCO);
        if (controlador.resultado().isPresent()) {
            var resultado = controlador.resultado().orElseThrow();
            titulo.setText("RESULTADO FINAL");
            encabezado.setTexto(resultado.ganador().isPresent() ? "GANAN LAS" : "PARTIDA");
            principal.setTexto(resultado.ganador().map(this::bando).orElse("TABLAS"));
            estado.setText(resultado.ganador().isPresent() ? "VICTORIA" : "EMPATE");
            estado.setFill(resultado.ganador().isPresent() ? tema.activo() : tema.acento());
            motivo.setText(mensajes.motivoDeFinal(resultado.motivo()));
        } else {
            boolean jaque = controlador.estado() == EstadoPartida.JAQUE;
            titulo.setText("ESTADO DE LA PARTIDA");
            encabezado.setTexto("TURNO DE");
            principal.setTexto(bando(controlador.turno()));
            estado.setText(golRecien ? "¡GOL!" : jaque ? "¡JAQUE!" : "EN JUEGO");
            estado.setFill(golRecien ? tema.acento() : jaque ? tema.alerta() : tema.activo());
            motivo.setText("");
        }
        punto.setFill(estado.getFill());
        actualizarRelojes();
    }

    /** Muestra el marcador del modo fútbol; devuelve true si desde el refresco anterior hubo un gol. */
    private boolean actualizarGoles() {
        Optional<Marcador> marcador = controlador.marcador();
        if (marcador.isEmpty()) {
            goles.setText("");
            jugadas.setText("");
            return false;
        }
        Marcador actual = marcador.get();
        goles.setText("BLANCAS " + actual.blancas() + " - " + actual.negras() + " NEGRAS");
        jugadas.setText("Jugada " + controlador.jugadasHechas()
                + controlador.limiteDeJugadas().map(limite -> " de " + limite).orElse(""));
        int total = actual.blancas() + actual.negras();
        boolean hubo = total > golesVistos;
        golesVistos = total;
        return hubo;
    }

    /** Refresco ligero que conserva tarjetas, tablero, selección y sus nodos. */
    public void actualizarRelojes() {
        boolean enCurso = !controlador.estado().estaFinalizada();
        negras.actualizarReloj(controlador.tiempoRestante(Bando.NEGRO), enCurso && controlador.turno() == Bando.NEGRO);
        blancas.actualizarReloj(controlador.tiempoRestante(Bando.BLANCO), enCurso && controlador.turno() == Bando.BLANCO);
    }

    public void configurarRendicion(java.util.function.Consumer<Bando> solicitar) {
        negras.configurarRendicion(() -> solicitar.accept(Bando.NEGRO));
        blancas.configurarRendicion(() -> solicitar.accept(Bando.BLANCO));
        actualizar();
    }

    public void configurarTablas(java.util.function.Consumer<Bando> ofrecer,
                                 java.util.function.Consumer<Bando> aceptar,
                                 java.util.function.Consumer<Bando> rechazar) {
        negras.configurarTablas(() -> ofrecer.accept(Bando.NEGRO), () -> aceptar.accept(Bando.NEGRO),
                () -> rechazar.accept(Bando.NEGRO));
        blancas.configurarTablas(() -> ofrecer.accept(Bando.BLANCO), () -> aceptar.accept(Bando.BLANCO),
                () -> rechazar.accept(Bando.BLANCO));
        actualizar();
    }

    void configurarRevancha(javafx.scene.Node boton) {
        informacion.getChildren().add(boton);
    }

    private String bando(Bando color) {
        return color == Bando.BLANCO ? "BLANCAS" : "NEGRAS";
    }
}
