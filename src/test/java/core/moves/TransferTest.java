package core.moves;

import core.side.Side;
import core.pieces.Piece;
import core.pieces.Rook;
import core.board.Position;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

class TransferTest {

    private final Piece rook = new Rook(Side.WHITE);

    @Test
    void applyCarriesPieceToDestination() {
        // Arrange
        Board board = BoardFake.withAPiece("a1", rook);
        Transfer transfer = new Transfer(rook, pos("a1"), pos("a5"));

        // Act
        transfer.apply(board);

        // Assert
        assertTrue(board.pieceIn(pos("a1")).isEmpty());
        assertSame(rook, board.pieceIn(pos("a5")).orElseThrow());
    }

    @Test
    void revertReturnsToOrigin() {
        // Arrange
        Board board = BoardFake.withAPiece("a1", rook);
        Map<Position, Piece> before = new HashMap<>(board.pieces());
        Transfer transfer = new Transfer(rook, pos("a1"), pos("a5"));
        transfer.apply(board);

        // Act
        transfer.revert(board);

        // Assert
        assertEquals(before, board.pieces());
    }

    @Test
    void ifPieceNotIsInOriginNotPlaysBoard() {
        // Arrange: en a1 hay otra torre, no la del traslado
        Board board = BoardFake.withAPiece("a1", new Rook(Side.WHITE));
        Map<Position, Piece> before = new HashMap<>(board.pieces());

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> new Transfer(rook, pos("a1"), pos("a5")).apply(board));
        assertEquals(before, board.pieces());
    }

    @Test
    void ifDestinationIsOccupiedNotPlaysBoard() {
        // Arrange: las capturas se modelan con un Retiro previo, no pisando la pieza
        Board board = BoardFake.withAPiece("a1", rook);
        board.place(pos("a5"), new Rook(Side.BLACK));
        Map<Position, Piece> before = new HashMap<>(board.pieces());

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> new Transfer(rook, pos("a1"), pos("a5")).apply(board));
        assertEquals(before, board.pieces());
    }

    @Test
    void rejectsOriginEqualToDestination() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Transfer(rook, pos("a1"), pos("a1")));
    }
}
