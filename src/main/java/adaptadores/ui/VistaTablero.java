package adaptadores.ui;

import Nucleo.juego.Posicion;
import Nucleo.tablero.ITablero;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Proyección del tablero actual. Solo guarda referencias a sus dependencias y nodos gráficos. */
public final class VistaTablero extends Region {
    private final ITablero tablero;
    private final TemaTablero tema;
    private final RepresentacionPiezas representacion;
    private final GridPane grilla = new GridPane();
    private final Consumer<Posicion> alPulsar;

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
        getChildren().add(grilla);
        redibujar();
    }

    /** Reconstruye la representación consultando el núcleo, sin almacenar piezas por separado. */
    public void redibujar() {
        redibujar(null, Set.of());
    }

    /** La selección puede estar vacía y los destinos ya vienen calculados por el núcleo. */
    public void redibujar(Posicion seleccionada, Set<Posicion> destinos) {
        Objects.requireNonNull(destinos, "destinos");
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
                        posicion.equals(seleccionada), destinos.contains(posicion));
                grilla.add(casilla, columna, tablero.alto() - 1 - fila);
            }
        }
        requestLayout();
    }

    private StackPane crearCasilla(Posicion posicion, boolean seleccionada, boolean destino) {
        StackPane casilla = new StackPane();
        casilla.setMinSize(0, 0);
        casilla.setId("casilla-" + posicion.columna() + "-" + posicion.fila());
        casilla.getStyleClass().add("casilla");
        boolean oscura = (posicion.columna() + posicion.fila()) % 2 == 0;
        Color color = oscura ? tema.casillaOscura() : tema.casillaClara();
        if (seleccionada) {
            casilla.getStyleClass().add("casilla-seleccionada");
            color = tema.casillaSeleccionada();
        }
        casilla.setBackground(Background.fill(color));
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
        var ocupante = tablero.piezaEn(posicion);
        ocupante.ifPresent(pieza -> {
            Node figura = representacion.crear(pieza, tema);
            figura.scaleXProperty().bind(casilla.widthProperty().divide(RepresentacionPiezas.LADO_BASE));
            figura.scaleYProperty().bind(casilla.heightProperty().divide(RepresentacionPiezas.LADO_BASE));
            casilla.getChildren().add(figura);
        });
        if (destino) {
            agregarIndicador(casilla, ocupante.isPresent());
        }
        return casilla;
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
