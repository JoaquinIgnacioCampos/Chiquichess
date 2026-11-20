package core.movements;

import core.side.Side;
import core.pieces.Piece;
import core.board.IBoard;
import core.board.Position;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Movimiento base: saltar a desplazamientos fijos, sin importar lo que haya en el medio (caballo). */
public class FixedJump implements MovementRule {

    private final int[][] offsets;

    public FixedJump(int[][] offsets) {
        this.offsets = new int[offsets.length][];
        for (int i = 0; i < offsets.length; i++) {
            this.offsets[i] = offsets[i].clone();
        }
    }

    @Override
    public Set<Position> destinationsPossible(IBoard board, Position origin, Side color) {
        Set<Position> destinations = new HashSet<>();
        for (int[] d : offsets) {
            Position destination = origin.shifted(d[0], d[1]);
            if (!board.contains(destination)) {
                continue;
            }
            Optional<Piece> occupant = board.pieceIn(destination);
            if (occupant.isEmpty() || occupant.get().color() != color) {
                destinations.add(destination);
            }
        }
        return destinations;
    }

    @Override
    public Set<Position> squaresAttacked(IBoard board, Position origin, Side color) {
        return destinationsPossible(board, origin, color);
    }
}
