package core.moves;

import core.pieces.Piece;
import core.board.Position;
import core.board.Board;

import java.util.Objects;

/**
 * Pase del modo fútbol: la pelota va de la pieza que la tiene a un compañero. Las piezas no se
 * mueven; al revertir, la pelota vuelve a quien pasó.
 *
 * Verifica que las dos piezas sigan en su lugar, así un pase no se puede aplicar sobre otra
 * posición. Si el pase termina en gol, la jugada le suma después un {@link MoverPelota} al saque.
 */
public record Pass(Piece passer, Position from, Piece receiver, Position toward) implements Effect {

    public Pass {
        Objects.requireNonNull(passer, "passer");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(toward, "toward");
        if (from.equals(toward)) {
            throw new IllegalArgumentException("A pass has to go to another square");
        }
        if (passer.color() != receiver.color()) {
            throw new IllegalArgumentException("The ball can only be passed to a teammate");
        }
    }

    @Override
    public void apply(Board board) {
        pass(board, from, toward);
    }

    @Override
    public void revert(Board board) {
        pass(board, toward, from);
    }

    private void pass(Board board, Position de, Position a) {
        if (board.pieceIn(from).orElse(null) != passer || board.pieceIn(toward).orElse(null) != receiver) {
            throw new IllegalStateException("The pieces of the pass are not at " + from + " and " + toward);
        }
        if (!board.ball().filter(de::equals).isPresent()) {
            throw new IllegalStateException("The ball is not at " + de);
        }
        board.placeBall(a);
    }
}
