package core.game;

import core.side.Side;
import core.pieces.*;
import core.board.Position;
import core.board.Board;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

class StandardLayoutTest {

    private Board board;

    @BeforeEach
    void prepareBoard() {
        board = new StandardLayout().createBoard();
    }

    @Test
    void createsABoardOf8X8With32Pieces() {
        // Assert
        assertEquals(8, board.width());
        assertEquals(8, board.height());
        assertEquals(32, board.pieces().size());
    }

    @Test
    void eachColorHas16Pieces() {
        // Act
        long white = board.pieces().values().stream().filter(p -> p.color() == Side.WHITE).count();
        long black = board.pieces().values().stream().filter(p -> p.color() == Side.BLACK).count();

        // Assert
        assertEquals(16, white);
        assertEquals(16, black);
    }

    @Test
    void kingsAndQueensAreInTheirSquares() {
        // Assert
        assertPiece("e1", King.class, Side.WHITE);
        assertPiece("e8", King.class, Side.BLACK);
        assertPiece("d1", Queen.class, Side.WHITE);
        assertPiece("d8", Queen.class, Side.BLACK);
    }

    @Test
    void rowBaseHasRookKnightBishopQueenKingBishopKnightRook() {
        // Arrange
        Class<?>[] expected = {Rook.class, Knight.class, Bishop.class, Queen.class,
                King.class, Bishop.class, Knight.class, Rook.class};

        // Assert
        for (int column = 0; column < 8; column++) {
            assertInstanceOf(expected[column], board.pieceIn(new Position(column, 0)).orElseThrow());
            assertInstanceOf(expected[column], board.pieceIn(new Position(column, 7)).orElseThrow());
        }
    }

    @Test
    void pawnsOccupyRows2And7() {
        // Assert
        for (int column = 0; column < 8; column++) {
            Piece white = board.pieceIn(new Position(column, 1)).orElseThrow();
            Piece black = board.pieceIn(new Position(column, 6)).orElseThrow();
            assertTrue(white.isPawn());
            assertEquals(Side.WHITE, white.color());
            assertTrue(black.isPawn());
            assertEquals(Side.BLACK, black.color());
        }
    }

    @Test
    void rowsOf3To6AreEmpty() {
        // Assert
        for (int row = 2; row <= 5; row++) {
            for (int column = 0; column < 8; column++) {
                assertTrue(board.pieceIn(new Position(column, row)).isEmpty());
            }
        }
    }

    @Test
    void eachCalledCreatesANewBoardEIndependent() {
        // Act
        Board other = new StandardLayout().createBoard();
        other.remove(pos("e2"));

        // Assert
        assertTrue(board.pieceIn(pos("e2")).isPresent());
    }

    private void assertPiece(String square, Class<? extends Piece> type, Side color) {
        Piece piece = board.pieceIn(pos(square)).orElseThrow();
        assertInstanceOf(type, piece);
        assertEquals(color, piece.color());
    }
}
