package core.movements;

import core.side.Side;
import core.board.IBoard;
import core.pieces.Piece;
import core.board.Position;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Movimiento base: avanzar en línea recta en una dirección (dc, df) hasta un máximo de pasos,
 * frenando ante la primera pieza. Si es rival se puede capturar; si es propia, no.
 */
public class Slide implements MovementRule {

    public static final int WITHOUT_LIMIT = Integer.MAX_VALUE;

    private final int deltaColumn;
    private final int deltaRow;
    private final int maxSteps;

    public Slide(int deltaColumn, int deltaRow, int maxSteps) {
        if (deltaColumn == 0 && deltaRow == 0) {
            throw new IllegalArgumentException("La dirección no puede ser (0, 0)");
        }
        if (maxSteps < 1) {
            throw new IllegalArgumentException("maxPasos debe ser al menos 1");
        }
        this.deltaColumn = deltaColumn;
        this.deltaRow = deltaRow;
        this.maxSteps = maxSteps;
    }

    @Override
    public Set<Position> destinationsPossible(IBoard board, Position origin, Side color) {
        Set<Position> destinations = new HashSet<>();
        Position actual = origin.shifted(deltaColumn, deltaRow);
        int steps = 1;
        while (steps <= maxSteps && board.contains(actual)) {
            Optional<Piece> occupant = board.pieceIn(actual);
            if (occupant.isEmpty()) {
                destinations.add(actual);
            } else {
                if (occupant.get().color() != color) {
                    destinations.add(actual); // captura
                }
                break; // cualquier pieza bloquea el camino
            }
            actual = actual.shifted(deltaColumn, deltaRow);
            steps++;
        }
        return destinations;
    }

    @Override
    public Set<Position> squaresAttacked(IBoard board, Position origin, Side color) {
        return destinationsPossible(board, origin, color);
    }
}