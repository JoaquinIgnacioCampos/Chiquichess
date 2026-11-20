package core.rules;

import core.side.Side;
import core.game.StandardLayout;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Cada test arma (con el fake) una posición concreta que provoca o no un jaque. */
class CheckDetectorTest {

    private final CheckDetector detector = new CheckDetector();

    @Test
    void inInitialPositionNoKingIsInCheck() {
        // Arrange
        Board board = new StandardLayout().createBoard();

        // Act + Assert
        assertFalse(detector.isInCheck(board, Side.WHITE));
        assertFalse(detector.isInCheck(board, Side.BLACK));
    }

    @Test
    void aRookInSameColumnWithoutObstaclesGivesCheck() {
        // Arrange
        Board board = BoardFake.from(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....K...");

        // Act
        boolean inCheck = detector.isInCheck(board, Side.WHITE);

        // Assert
        assertTrue(inCheck);
    }

    @Test
    void aPieceInMiddleBlocksCheck() {
        // Arrange
        Board board = BoardFake.from(
                "....r...",
                "........",
                "........",
                "....P...",
                "........",
                "........",
                "........",
                "....K...");

        // Act
        boolean inCheck = detector.isInCheck(board, Side.WHITE);

        // Assert
        assertFalse(inCheck);
    }

    @Test
    void aBishopInDiagonalGivesCheck() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                ".b......",
                "........",
                "........",
                "....K...");

        // Act
        boolean inCheck = detector.isInCheck(board, Side.WHITE);

        // Assert
        assertTrue(inCheck);
    }

    @Test
    void aKnightGivesCheckAlthoughKingThisSurrounded() {
        // Arrange: rey blanco en e2 rodeado de peones; el caballo negro en g3 salta hasta e2
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "...PPPn.",
                "...PKP..",
                "...PPP..");

        // Act
        boolean inCheck = detector.isInCheck(board, Side.WHITE);

        // Assert
        assertTrue(inCheck);
    }

    @Test
    void aKnightThatNotArrivesToKingNotGivesCheck() {
        // Arrange: el caballo negro en h2 ataca f1, f3 y g4, pero no e2
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "...PPP..",
                "...PKP.n",
                "...PPP..");

        // Act
        boolean inCheck = detector.isInCheck(board, Side.WHITE);

        // Assert
        assertFalse(inCheck);
    }

    @Test
    void aPawnGivesCheckInDiagonalButNotOfFront() {
        // Arrange
        Board inDiagonal = BoardFake.from(
                "........",
                "........",
                "........",
                "...p....",
                "....K...",
                "........",
                "........",
                "........");
        Board ofFront = BoardFake.from(
                "........",
                "........",
                "........",
                "....p...",
                "....K...",
                "........",
                "........",
                "........");

        // Act + Assert
        assertTrue(detector.isInCheck(inDiagonal, Side.WHITE));
        assertFalse(detector.isInCheck(ofFront, Side.WHITE));
    }

    @Test
    void piecesOwnNotGiveCheck() {
        // Arrange
        Board board = BoardFake.from(
                "....R...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....K...");

        // Act
        boolean inCheck = detector.isInCheck(board, Side.WHITE);

        // Assert
        assertFalse(inCheck);
    }

    @Test
    void onlyIsInCheckColorAttacked() {
        // Arrange
        Board board = BoardFake.from(
                "....k...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "K...R...");

        // Act + Assert
        assertTrue(detector.isInCheck(board, Side.BLACK));
        assertFalse(detector.isInCheck(board, Side.WHITE));
    }

    @Test
    void withoutKingNotHasCheck() {
        // Arrange
        Board board = BoardFake.from(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........");

        // Act
        boolean inCheck = detector.isInCheck(board, Side.WHITE);

        // Assert
        assertFalse(inCheck);
    }
}
