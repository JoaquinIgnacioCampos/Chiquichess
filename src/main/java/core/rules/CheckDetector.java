package core.rules;

import core.side.Side;
import core.pieces.Piece;
import core.board.IBoard;
import core.board.Position;
import java.util.Map;
import java.util.Optional;

/** Responsabilidad única: decir si el rey de un color está siendo atacado. */
public class CheckDetector implements ICheckDetector {

    @Override
    public boolean isInCheck(IBoard board, Side color) {
        Optional<Position> positionKing = searchKing(board, color);
        if (positionKing.isEmpty()) {
            return false;
        }
        for (Map.Entry<Position, Piece> input : board.pieces().entrySet()) {
            Piece piece = input.getValue();
            if (piece.color() != color
                    && piece.squaresAttacked(board, input.getKey()).contains(positionKing.get())) {
                return true;
            }
        }
        return false;
    }

    private Optional<Position> searchKing(IBoard board, Side color) {
        // Ciclo y no stream: se llama por cada jugada que se prueba (la IA prueba muchísimas).
        for (Map.Entry<Position, Piece> input : board.pieces().entrySet()) {
            if (input.getValue().isKing() && input.getValue().color() == color) {
                return Optional.of(input.getKey());
            }
        }
        return Optional.empty();
    }
}
