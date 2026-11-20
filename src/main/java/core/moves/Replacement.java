package core.moves;

import core.pieces.Piece;
import core.board.Position;
import core.board.Board;

import java.util.Objects;

/** Cambia la pieza de una casilla por otra (la promoción del peón). Al revertir, vuelve la anterior. */
public record Replacement(Piece previous, Piece newValue, Position position) implements Effect {

    public Replacement {
        Objects.requireNonNull(previous, "anterior");
        Objects.requireNonNull(newValue, "nueva");
        Objects.requireNonNull(position, "posicion");
    }

    @Override
    public void apply(Board board) {
        change(board, previous, newValue);
    }

    @Override
    public void revert(Board board) {
        change(board, newValue, previous);
    }

    private void change(Board board, Piece expected, Piece replacement) {
        if (board.pieceIn(position).orElse(null) != expected) {
            throw new IllegalStateException("No está la pieza esperada en " + position);
        }
        board.place(position, replacement);
    }
}
