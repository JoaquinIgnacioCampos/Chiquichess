package core.moves;

import core.side.Side;
import core.pieces.Pawn;
import core.pieces.Piece;
import core.pieces.Rook;
import core.board.Position;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del Command: se compara el tablero antes de aplicar, después de aplicar y después de
 * revertir. Después de revertir tiene que quedar idéntico a como estaba antes de aplicar.
 */
class MoveTest {

    private static Map<Position, Piece> photo(Board board) {
        return new HashMap<>(board.pieces());
    }

    @Test
    void aMoveNormalWithoutCaptureIsOnlyATransfer() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....P...",
                "........");
        Piece pawn = board.pieceIn(pos("e2")).orElseThrow();

        // Act
        Move move = Move.normal(board, pos("e2"), pos("e4"));

        // Assert
        assertEquals(List.of(new Transfer(pawn, pos("e2"), pos("e4"))), move.effects());
        assertSame(pawn, move.pieceMoved());
        assertTrue(move.captured().isEmpty());
    }

    @Test
    void aMoveNormalWithCaptureFirstWithdrawsAndAfterMoves() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "...p....",
                "....P...",
                "........",
                "........",
                "........");
        Piece pawn = board.pieceIn(pos("e4")).orElseThrow();
        Piece rival = board.pieceIn(pos("d5")).orElseThrow();

        // Act
        Move move = Move.normal(board, pos("e4"), pos("d5"));

        // Assert
        assertEquals(List.of(new Removal(rival, pos("d5")), new Transfer(pawn, pos("e4"), pos("d5"))),
                move.effects());
        assertSame(rival, move.captured().orElseThrow());
    }

    @Test
    void revertACaptureLeavesBoardAsBeforeOfApply() {
        // Arrange
        Board board = BoardFake.from(
                "....k...",
                "........",
                "........",
                "...p....",
                "....P...",
                "........",
                "........",
                "....K...");
        Map<Position, Piece> before = photo(board);
        Move move = Move.normal(board, pos("e4"), pos("d5"));

        // Act
        move.apply(board);
        Map<Position, Piece> after = photo(board);
        move.revert(board);

        // Assert
        assertNotEquals(before, after);
        assertEquals(3, after.size());
        assertEquals(before, photo(board));
    }

    @Test
    void aMoveCanMoveTwoPiecesAsACastling() {
        // Arrange: el rey va de e1 a g1 y la torre de h1 a f1
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....K..R");
        Piece king = board.pieceIn(pos("e1")).orElseThrow();
        Piece rook = board.pieceIn(pos("h1")).orElseThrow();
        Map<Position, Piece> before = photo(board);
        Move castling = new Move(pos("e1"), pos("g1"), List.of(
                new Transfer(king, pos("e1"), pos("g1")),
                new Transfer(rook, pos("h1"), pos("f1"))));

        // Act
        castling.apply(board);

        // Assert
        assertSame(king, board.pieceIn(pos("g1")).orElseThrow());
        assertSame(rook, board.pieceIn(pos("f1")).orElseThrow());
        assertSame(king, castling.pieceMoved());
        assertTrue(castling.captured().isEmpty());

        // Act + Assert: deshacer
        castling.revert(board);
        assertEquals(before, photo(board));
    }

    @Test
    void aMoveCanCaptureInOtherSquareAsCaptureToStep() {
        // Arrange: el peón blanco va de e5 a d6 y el negro capturado está en d5
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "...pP...",
                "........",
                "........",
                "........",
                "........");
        Piece white = board.pieceIn(pos("e5")).orElseThrow();
        Piece black = board.pieceIn(pos("d5")).orElseThrow();
        Map<Position, Piece> before = photo(board);
        Move onStep = new Move(pos("e5"), pos("d6"), List.of(
                new Removal(black, pos("d5")),
                new Transfer(white, pos("e5"), pos("d6"))));

        // Act
        onStep.apply(board);

        // Assert
        assertEquals(1, board.pieces().size());
        assertSame(white, board.pieceIn(pos("d6")).orElseThrow());
        assertSame(black, onStep.captured().orElseThrow());

        // Act + Assert: deshacer
        onStep.revert(board);
        assertEquals(before, photo(board));
    }

    @Test
    void ifAEffectFailsToHalfOfPathBoardStaysAsWas() {
        // Arrange: el segundo traslado espera una torre en h1 que no está
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....K...");
        Piece king = board.pieceIn(pos("e1")).orElseThrow();
        Piece rookAbsent = new Rook(Side.WHITE);
        Map<Position, Piece> before = photo(board);
        Move invalid = new Move(pos("e1"), pos("g1"), List.of(
                new Transfer(king, pos("e1"), pos("g1")),
                new Transfer(rookAbsent, pos("h1"), pos("f1"))));

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> invalid.apply(board));
        assertEquals(before, photo(board));
    }

    @Test
    void notCanRevertOnABoardThatNotIsAfterOfMove() {
        // Arrange: la jugada nunca se aplicó
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....P...",
                "........");
        Map<Position, Piece> before = photo(board);
        Move move = Move.normal(board, pos("e2"), pos("e4"));

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> move.revert(board));
        assertEquals(before, photo(board));
    }

    @Test
    void rejectsAMoveThatNotMovesPieceOfOrigin() {
        // Arrange
        Piece pawn = new Pawn(Side.WHITE);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Move(pos("e2"), pos("e4"),
                List.of(new Transfer(pawn, pos("d2"), pos("d4")))));
        assertThrows(IllegalArgumentException.class, () -> new Move(pos("e2"), pos("e4"), List.of()));
    }

    @Test
    void savesACopyUnmodifiableOfEffects() {
        // Arrange
        Piece pawn = new Pawn(Side.WHITE);
        List<Effect> effects = new ArrayList<>(List.of(new Transfer(pawn, pos("e2"), pos("e4"))));
        Move move = new Move(pos("e2"), pos("e4"), effects);

        // Act
        effects.add(new Removal(pawn, pos("a1")));

        // Assert
        assertEquals(1, move.effects().size());
        assertThrows(UnsupportedOperationException.class, () -> move.effects().add(new Removal(pawn, pos("a1"))));
    }

    @Test
    void aMoveNormalNeedsAPieceInOrigin() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> Move.normal(BoardFake.empty(), pos("e2"), pos("e4")));
    }
}
