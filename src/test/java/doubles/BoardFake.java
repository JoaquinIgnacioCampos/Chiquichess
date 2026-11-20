package doubles;

import core.side.Side;
import core.pieces.*;
import core.board.Position;
import core.board.Board;

/**
 * Fake: arma un tablero en memoria a partir de un dibujo de texto, con las mismas letras
 * que muestra la consola (mayúscula = blancas, minúscula = negras, '.' = casilla vacía).
 * La primera línea es la fila de arriba (la 8 en un tablero estándar), igual que en pantalla.
 *
 * <pre>
 * TableroFake.desde(
 *     "....r...",   // fila 8
 *     "........",
 *     ...
 *     "....R...");  // fila 1
 * </pre>
 *
 * Letras: P peón, T torre, C caballo, A alfil, D dama (reina), R rey.
 * El tamaño sale del dibujo, así que también sirve para tableros que no son de 8x8.
 */
public final class BoardFake {

    private BoardFake() {
    }

    public static Board from(String... rows) {
        int height = rows.length;
        int width = rows[0].length();
        Board board = new Board(width, height);
        for (int i = 0; i < height; i++) {
            if (rows[i].length() != width) {
                throw new IllegalArgumentException("All rows must have the same length");
            }
            int row = height - 1 - i;
            for (int column = 0; column < width; column++) {
                char letter = rows[i].charAt(column);
                if (letter != '.') {
                    board.place(new Position(column, row), create(letter));
                }
            }
        }
        return board;
    }

    /** Tablero de 8x8 sin piezas. */
    public static Board empty() {
        return new Board(8, 8);
    }

    /** Tablero de 8x8 con una sola pieza, útil para contar destinos sin interferencias. */
    public static Board withAPiece(String square, Piece piece) {
        Board board = empty();
        board.place(Squares.pos(square), piece);
        return board;
    }

    private static Piece create(char letter) {
        Side color = Character.isUpperCase(letter) ? Side.WHITE : Side.BLACK;
        return switch (Character.toUpperCase(letter)) {
            case 'P' -> new Pawn(color);
            case 'R' -> new Rook(color);
            case 'N' -> new Knight(color);
            case 'B' -> new Bishop(color);
            case 'Q' -> new Queen(color);
            case 'K' -> new King(color);
            default -> throw new IllegalArgumentException("Unknown piece letter: " + letter);
        };
    }
}
