package adaptadores.ui;

import Nucleo.juego.Posicion;
import Nucleo.tablero.ITablero;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;

import java.util.Objects;

/** Proyección del tablero actual. Solo guarda referencias a sus dependencias y nodos gráficos. */
public final class VistaTablero extends Region {
    private final ITablero tablero;
    private final TemaTablero tema;
    private final RepresentacionPiezas representacion;
    private final GridPane grilla = new GridPane();

    public VistaTablero(ITablero tablero, TemaTablero tema, RepresentacionPiezas representacion) {
        this.tablero = Objects.requireNonNull(tablero, "tablero");
        this.tema = Objects.requireNonNull(tema, "tema");
        this.representacion = Objects.requireNonNull(representacion, "representacion");
        setMinSize(0, 0);
        getChildren().add(grilla);
        redibujar();
    }

    /** Reconstruye la representación consultando el núcleo, sin almacenar piezas por separado. */
    public void redibujar() {
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
                StackPane casilla = crearCasilla(posicion);
                grilla.add(casilla, columna, tablero.alto() - 1 - fila);
            }
        }
        requestLayout();
    }

    private StackPane crearCasilla(Posicion posicion) {
        StackPane casilla = new StackPane();
        casilla.setMinSize(0, 0);
        casilla.setId("casilla-" + posicion.columna() + "-" + posicion.fila());
        casilla.getStyleClass().add("casilla");
        boolean oscura = (posicion.columna() + posicion.fila()) % 2 == 0;
        casilla.setBackground(new Background(new BackgroundFill(
                oscura ? tema.casillaOscura() : tema.casillaClara(), CornerRadii.EMPTY, Insets.EMPTY)));
        casilla.setAccessibleText("Columna " + (posicion.columna() + 1) + ", fila " + (posicion.fila() + 1));
        tablero.piezaEn(posicion).ifPresent(pieza -> {
            Node figura = representacion.crear(pieza, tema);
            figura.scaleXProperty().bind(casilla.widthProperty().divide(RepresentacionPiezas.LADO_BASE));
            figura.scaleYProperty().bind(casilla.heightProperty().divide(RepresentacionPiezas.LADO_BASE));
            casilla.getChildren().add(figura);
        });
        return casilla;
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
