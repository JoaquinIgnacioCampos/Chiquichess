package core.moves;

import core.pieces.Piece;
import core.board.Position;
import core.board.Board;

import java.util.Objects;

/** Lleva una pieza de una casilla a otra casilla vacía. */
public record Transfer(Piece piece, Position origin, Position destination) implements Effect {

    public Transfer {
        Objects.requireNonNull(piece, "pieza");
        Objects.requireNonNull(origin, "origen");
        Objects.requireNonNull(destination, "destino");
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("El origen y el destino deben ser distintos");
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
            throw new IllegalStateException("No está la pieza esperada en " + from);
        }
        if (board.pieceIn(toward).isPresent()) {
            throw new IllegalStateException("La casilla " + toward + " no está vacía");
        }
        board.remove(from);
        board.place(toward, piece);
    }
}
