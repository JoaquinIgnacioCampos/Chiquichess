package doubles;

import core.game.MoveRequest;
import core.board.Position;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ayuda para escribir casillas como "e4" en los tests del núcleo, sin depender del adaptador
 * AlgebraicNotation (el núcleo se prueba sin adaptadores). a1 = columna 0, fila 0.
 */
public final class Squares {

    private Squares() {
    }

    public static Position pos(String square) {
        return new Position(square.charAt(0) - 'a', Integer.parseInt(square.substring(1)) - 1);
    }

    public static Set<Position> positions(String... squares) {
        return Arrays.stream(squares).map(Squares::pos).collect(Collectors.toSet());
    }

    public static MoveRequest move(String origin, String destination) {
        return new MoveRequest(pos(origin), pos(destination));
    }
}
