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
        Objects.requireNonNull(passer, "pasador");
        Objects.requireNonNull(from, "desde");
        Objects.requireNonNull(receiver, "receptor");
        Objects.requireNonNull(toward, "hacia");
        if (from.equals(toward)) {
            throw new IllegalArgumentException("Un pase tiene que ir a otra casilla");
        }
        if (passer.color() != receiver.color()) {
            throw new IllegalArgumentException("Solo se le puede pasar la pelota a un compañero");
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
            throw new IllegalStateException("Las piezas del pase no están en " + from + " y " + toward);
        }
        if (!board.ball().filter(de::equals).isPresent()) {
            throw new IllegalStateException("La pelota no está en " + de);
        }
        board.placeBall(a);
    }
}
