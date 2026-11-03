package adaptadores.ui;

import Nucleo.juego.MotivoFinalizacion;
import Nucleo.piezas.Color;
import Nucleo.reglas.EstadoPartida;
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

/** Marcador de lectura: traduce el estado del núcleo a información visual. */
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

    public PanelEstadoPartida(ControladorTablero controlador, TemaPanel tema) {
        this.controlador = Objects.requireNonNull(controlador, "controlador");
        this.tema = Objects.requireNonNull(tema, "tema");
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
        negras = new TarjetaJugador(Color.NEGRO, tema);
        blancas = new TarjetaJugador(Color.BLANCO, tema);
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
        Text modalidad = new Text("Partida local");
        modalidad.setFill(tema.textoSecundario());
        modalidad.setFont(Font.font("Consolas", 12));
        Region separador = new Region();
        separador.setMinHeight(1);
        separador.setPrefHeight(1);
        separador.setMaxHeight(1);
        separador.setBackground(new Background(new BackgroundFill(tema.borde(), CornerRadii.EMPTY, Insets.EMPTY)));
        VBox informacion = new VBox(12, titulo, encabezado, principal, estadoActual, separador, motivo, modalidad);
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
        negras.actualizar(controlador.jugador(Color.NEGRO), !finalizada && controlador.turno() == Color.NEGRO);
        blancas.actualizar(controlador.jugador(Color.BLANCO), !finalizada && controlador.turno() == Color.BLANCO);
        if (controlador.resultado().isPresent()) {
            var resultado = controlador.resultado().orElseThrow();
            titulo.setText("RESULTADO FINAL");
            encabezado.setTexto(resultado.ganador().isPresent() ? "GANAN LAS" : "PARTIDA");
            principal.setTexto(resultado.ganador().map(this::bando).orElse("TABLAS"));
            estado.setText(resultado.ganador().isPresent() ? "VICTORIA" : "EMPATE");
            estado.setFill(resultado.ganador().isPresent() ? tema.activo() : tema.acento());
            motivo.setText(describir(resultado.motivo()));
        } else {
            boolean jaque = controlador.estado() == EstadoPartida.JAQUE;
            titulo.setText("ESTADO DE LA PARTIDA");
            encabezado.setTexto("TURNO DE");
            principal.setTexto(bando(controlador.turno()));
            estado.setText(jaque ? "¡JAQUE!" : "EN JUEGO");
            estado.setFill(jaque ? tema.alerta() : tema.activo());
            motivo.setText("");
        }
        punto.setFill(estado.getFill());
        actualizarRelojes();
    }

    /** Refresco ligero que conserva tarjetas, tablero, selección y sus nodos. */
    public void actualizarRelojes() {
        boolean enCurso = !controlador.estado().estaFinalizada();
        negras.actualizarReloj(controlador.tiempoRestante(Color.NEGRO), enCurso && controlador.turno() == Color.NEGRO);
        blancas.actualizarReloj(controlador.tiempoRestante(Color.BLANCO), enCurso && controlador.turno() == Color.BLANCO);
    }

    private String bando(Color color) {
        return color == Color.BLANCO ? "BLANCAS" : "NEGRAS";
    }

    private String describir(MotivoFinalizacion motivo) {
        return switch (motivo) {
            case JAQUE_MATE -> "Por jaque mate";
            case AHOGADO -> "Por ahogado";
            case REPETICION -> "Por repetición";
            case REGLA_50_MOVIMIENTOS -> "Regla de los 50 movimientos";
            case RENDICION -> "Por rendición";
            case TIEMPO_AGOTADO -> "Por tiempo agotado";
            case ACUERDO -> "Por acuerdo";
        };
    }
}
