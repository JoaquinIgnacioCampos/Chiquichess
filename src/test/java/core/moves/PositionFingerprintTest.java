package core.moves;

import core.side.Side;
import core.game.StandardLayout;
import core.pieces.Knight;
import core.pieces.King;
import core.pieces.Rook;
import core.board.Board;
import doubles.MovesOfTest;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.List;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

class PositionFingerprintTest {

    @Test
    void samePiecesInSameSquaresGiveSameFingerprintAlthoughBuildDifferent() {
        // Arrange: el mismo dibujo, pero un tablero armado pieza por pieza en otro orden
        Board drawn = BoardFake.from(
                "....k...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....K.N.");
        Board toHand = BoardFake.empty();
        toHand.place(pos("g1"), new Knight(Side.WHITE));
        toHand.place(pos("e8"), new King(Side.BLACK));
        toHand.place(pos("e1"), new King(Side.WHITE));

        // Act + Assert
        assertEquals(PositionFingerprint.de(drawn, Side.WHITE), PositionFingerprint.de(toHand, Side.WHITE));
    }

    @Test
    void turnChangesFingerprint() {
        // Arrange
        Board board = new StandardLayout().createBoard();

        // Act + Assert
        assertNotEquals(PositionFingerprint.de(board, Side.WHITE), PositionFingerprint.de(board, Side.BLACK));
    }

    @Test
    void squareTypeAndSideOfPieceChangeFingerprint() {
        // Act
        long whiteRookInTo1 = PositionFingerprint.de(BoardFake.withAPiece("a1", new Rook(Side.WHITE)), Side.WHITE);
        long whiteRookInTo2 = PositionFingerprint.de(BoardFake.withAPiece("a2", new Rook(Side.WHITE)), Side.WHITE);
        long blackRookInTo1 = PositionFingerprint.de(BoardFake.withAPiece("a1", new Rook(Side.BLACK)), Side.WHITE);
        long whiteKnightInTo1 = PositionFingerprint.de(BoardFake.withAPiece("a1", new Knight(Side.WHITE)), Side.WHITE);

        // Assert
        assertEquals(4, List.of(whiteRookInTo1, whiteRookInTo2, blackRookInTo1, whiteKnightInTo1)
                .stream().distinct().count());
    }

    @Test
    void updateWithEachMoveGivesItSameThatCalculateFromZero() {
        // Arrange: apertura con capturas de los dos lados
        Board board = new StandardLayout().createBoard();
        long incremental = PositionFingerprint.de(board, Side.WHITE);

        // Act: de a una jugada, para comparar en cada paso
        Side turn = Side.WHITE;
        for (String texto : new String[]{"e2e4", "d7d5", "e4d5", "d8d5", "b1c3", "d5a5"}) {
            Move move = MovesOfTest.play(board, texto).get(0);
            incremental = PositionFingerprint.tras(incremental, move);
            turn = turn.opposite();
            // Assert
            assertEquals(PositionFingerprint.de(board, turn), incremental, "After " + texto);
        }
    }

    @Test
    void goAndReturnReturnsExactlySameFingerprint() {
        // Arrange
        Board board = new StandardLayout().createBoard();
        long initial = PositionFingerprint.de(board, Side.WHITE);

        // Act
        long fingerprint = initial;
        for (Move move : MovesOfTest.play(board, "g1f3", "g8f6", "f3g1", "f6g8")) {
            fingerprint = PositionFingerprint.tras(fingerprint, move);
        }

        // Assert
        assertEquals(initial, fingerprint);
    }

    @Test
    void updateWorksWithMovesOfSeveralEffects() {
        // Arrange: un enroque armado a mano (dos traslados)
        Board board = BoardFake.from(
                "....k...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....K..R");
        long before = PositionFingerprint.de(board, Side.WHITE);
        Move castling = new Move(pos("e1"), pos("g1"), List.of(
                new Transfer(board.pieceIn(pos("e1")).orElseThrow(), pos("e1"), pos("g1")),
                new Transfer(board.pieceIn(pos("h1")).orElseThrow(), pos("h1"), pos("f1"))));

        // Act
        castling.apply(board);

        // Assert
        assertEquals(PositionFingerprint.de(board, Side.BLACK), PositionFingerprint.tras(before, castling));
    }
}
