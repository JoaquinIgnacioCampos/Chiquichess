package core.moves;

import core.pieces.Piece;
import core.board.Position;
import core.board.Board;

import java.util.Objects;

/** Lleva una pieza de una casilla a otra casilla vacía. */
public record Transfer(Piece piece, Position origin, Position destination) implements Effect {

    public Transfer {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("The origin and the destination must be different");
        }
    }

    @Override
    public void apply(Board board) {
        move(board, origin, destination);
    }

    @Override
    public void revert(Board board) {
        move(board, destination, origin);
    }

    private void move(Board board, Position from, Position toward) {
        if (board.pieceIn(from).orElse(null) != piece) {
            throw new IllegalStateException("The expected piece is not at " + from);
        }
        if (board.pieceIn(toward).isPresent()) {
            throw new IllegalStateException("The square " + toward + " is not empty");
        }
        board.remove(from);
        board.place(toward, piece);
    }
}
