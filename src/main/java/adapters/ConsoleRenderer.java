package adapters;

import core.side.Side;
import core.pieces.Piece;
import core.board.IBoard;
import core.board.Position;


import java.util.Optional;

/**
 * Adaptador de salida: dibuja el tablero como texto. Blancas en mayúscula, negras en minúscula.
 * En el modo fútbol la pelota suelta se dibuja con 'o', y la pieza que la lleva, con un '*' al lado;
 * las casillas vacías del arco, con '#'.
 */
public class ConsoleRenderer {


    public String draw(IBoard board) {
        StringBuilder sb = new StringBuilder();
        for (int row = board.height() - 1; row >= 0; row--) {
            sb.append(String.format("%2d ", row + 1));
            for (int column = 0; column < board.width(); column++) {
                Position square = new Position(column, row);
                Optional<Piece> piece = board.pieceIn(square);
                boolean ball = board.ball().filter(square::equals).isPresent();
                char empty = ball ? 'o' : board.netIn(square).isPresent() ? '#' : '.';
                sb.append(piece.isPresent() ? symbol(piece.get()) : empty)
                        .append(ball && piece.isPresent() ? '*' : ' ');
            }
            sb.append('\n');
        }
        sb.append("   ");
        for (int column = 0; column < board.width(); column++) {
            sb.append((char) ('a' + column)).append(' ');
        }
        return sb.toString();
    }

    private char symbol(Piece piece) {
        return piece.color() == Side.WHITE
                ? Character.toUpperCase(piece.code())
                : Character.toLowerCase(piece.code());
    }
}
