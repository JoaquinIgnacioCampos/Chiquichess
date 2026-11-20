package core.game;

import core.side.Side;
import core.pieces.*;
import core.board.InitialLayout;
import core.board.Position;
import core.board.Board;

/** Posición inicial clásica del ajedrez en un tablero de 8x8. */
public class StandardLayout implements InitialLayout {

    private static final int SIZE = 8;

    @Override
    public Board createBoard() {
        Board board = new Board(SIZE, SIZE);
        for (Side color : Side.values()) {
            int rowBase = color == Side.WHITE ? 0 : SIZE - 1;
            int rowPawns = color == Side.WHITE ? 1 : SIZE - 2;

            Piece[] pieceRow = {
                    new Rook(color), new Knight(color), new Bishop(color), new Queen(color),
                    new King(color), new Bishop(color), new Knight(color), new Rook(color)
            };
            for (int column = 0; column < SIZE; column++) {
                board.place(new Position(column, rowBase), pieceRow[column]);
                board.place(new Position(column, rowPawns), new Pawn(color));
            }
        }
        return board;
    }
}
