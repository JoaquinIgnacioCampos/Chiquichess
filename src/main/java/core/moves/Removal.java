package core.moves;

import core.pieces.Piece;
import core.board.Position;
import core.board.Board;

import java.util.Objects;

/** Saca una pieza del tablero (una captura). Al revertir, la devuelve a su casilla. */
public record Removal(Piece piece, Position position) implements Effect {

    public Removal {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(position, "position");
    }

    @Override
    public void apply(Board board) {
        if (board.pieceIn(position).orElse(null) != piece) {
            throw new IllegalStateException("The expected piece is not at " + position);
        }
        board.remove(position);
    }

    @Override
    public void revert(Board board) {
        if (board.pieceIn(position).isPresent()) {
            throw new IllegalStateException("The square " + position + " is not empty");
        }
        board.place(position, piece);
    }
}
