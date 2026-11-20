package core.game;

import core.moves.Move;
import core.pieces.Piece;
import core.board.Position;

import java.util.Objects;
import java.util.Optional;

/**
 * Intención de mover la pieza que está en origen hacia destino.
 *
 * @param promocion código de la pieza elegida si el peón promueve (D, T, A o C en el ajedrez clásico);
 *                  vacío si no se eligió, en cuyo caso un peón que promueve se convierte en dama
 */
public record MoveRequest(Position origin, Position destination, Optional<Character> promotion) {

    public MoveRequest {
        Objects.requireNonNull(origin, "origen");
        Objects.requireNonNull(destination, "destino");
        Objects.requireNonNull(promotion, "promocion");
    }

    public MoveRequest(Position origin, Position destination) {
        this(origin, destination, Optional.empty());
    }

    /** Movimiento de un peón que promueve a la pieza con ese código. */
    public static MoveRequest withPromotion(Position origin, Position destination, char code) {
        return new MoveRequest(origin, destination, Optional.of(code));
    }

    /** El movimiento que hay que pedirle a la partida para hacer esa jugada (por ejemplo, la que eligió la IA). */
    public static MoveRequest de(Move move) {
        Objects.requireNonNull(move, "jugada");
        return new MoveRequest(move.origin(), move.destination(), move.promotion().map(Piece::code));
    }
}
