package adapters.ui;

import core.board.IBoard;
import core.board.Position;

import java.util.Objects;

/**
 * Líneas de la cancha: los costados, las líneas de fondo, la del medio y las dos áreas de cada
 * arco (la chica y la grande). Solo decide dónde va cada línea, sobre los bordes entre casillas; dibujarlas es
 * cosa de la vista.
 *
 * <p>Las líneas van sobre los bordes de la grilla. Un borde horizontal {@code b} es el que está arriba de la
 * fila {@code b - 1} y abajo de la fila {@code b} (el 0 es el borde de abajo de todo y el alto del tablero, el
 * de arriba); un borde vertical {@code b} es el que está a la izquierda de la columna {@code b}.
 */
final class PitchMarkings {
    /** Cuántas filas entran en el área chica y en la grande, contando desde la línea de fondo. */
    static final int SMALL_AREA_DEPTH = 1;
    static final int LARGE_AREA_DEPTH = 2;

    private final int width;
    private final int height;
    /** Columnas del arco; el área chica sobra una columna a cada lado y la grande dos. */
    private final int firstNet;
    private final int lastNet;

    private PitchMarkings(int width, int height, int firstNet, int lastNet) {
        this.width = width;
        this.height = height;
        this.firstNet = firstNet;
        this.lastNet = lastNet;
    }

    /**
     * Las líneas de ese tablero. Los arcos van donde los marca el tablero (modo fútbol); si no tiene, como en el
     * ajedrez clásico, se toman las dos columnas del medio (o la del medio, si el ancho es impar).
     */
    static PitchMarkings of(IBoard board) {
        Objects.requireNonNull(board, "tablero");
        int first = Integer.MAX_VALUE;
        int last = Integer.MIN_VALUE;
        for (int column = 0; column < board.width(); column++) {
            for (int row = 0; row < board.height(); row++) {
                if (board.netIn(new Position(column, row)).isPresent()) {
                    first = Math.min(first, column);
                    last = Math.max(last, column);
                }
            }
        }
        if (last < first) {
            first = (board.width() - 1) / 2;
            last = board.width() / 2;
        }
        return new PitchMarkings(board.width(), board.height(), first, last);
    }

    /** true si hay línea sobre ese borde horizontal, a lo largo de esa columna. */
    boolean horizontal(int boundary, int column) {
        if (boundary == 0 || boundary == height) return true;                       // líneas de fondo
        if (boundary * 2 == height) return true;                                    // línea del medio
        return inArea(boundary, column, SMALL_AREA_DEPTH, 1) || inArea(boundary, column, LARGE_AREA_DEPTH, 2);
    }

    /** true si hay línea sobre ese borde vertical, a lo largo de esa fila. */
    boolean vertical(int boundary, int row) {
        if (boundary == 0 || boundary == width) return true;                        // costados
        return besideArea(boundary, row, SMALL_AREA_DEPTH, 1) || besideArea(boundary, row, LARGE_AREA_DEPTH, 2);
    }

    /** El lado de adelante de un área: sobre el borde que está {@code depth} filas adentro, entre sus columnas. */
    private boolean inArea(int boundary, int column, int depth, int margin) {
        boolean frontLine = boundary == depth || boundary == height - depth;
        return frontLine && column >= firstNet - margin && column <= lastNet + margin;
    }

    /** Los lados de un área: los bordes verticales de sus columnas extremas, a lo largo de su profundidad. */
    private boolean besideArea(int boundary, int row, int depth, int margin) {
        boolean sideLine = boundary == firstNet - margin || boundary == lastNet + margin + 1;
        return sideLine && (row < depth || row >= height - depth);
    }
}
