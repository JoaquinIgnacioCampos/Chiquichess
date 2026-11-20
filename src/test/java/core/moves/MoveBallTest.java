package core.moves;

import core.side.Side;
import core.game.FootballLayout;
import core.game.StandardLayout;
import core.pieces.Rook;
import core.rules.Scoreboard;
import core.rules.BallRule;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/** La pelota en el tablero, el efecto que la mueve, su huella y el marcador. */
class MoveBallTest {

    @Test
    void aNewBoardNotHasBall() {
        // Assert
        assertTrue(BoardFake.empty().ball().isEmpty());
    }

    @Test
    void ballNotCanPutOutsideOfBoard() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> BoardFake.empty().placeBall(pos("i1")));
    }

    @Test
    void copyBoardCopyBallAndNets() {
        // Arrange
        Board board = BoardFake.empty();
        board.placeBall(pos("d5"));
        board.markNet(pos("d8"), Side.BLACK);

        // Act
        Board copy = board.copy();
        copy.placeBall(pos("e4"));

        // Assert: la copia es independiente
        assertEquals(Optional.of(Side.BLACK), copy.netIn(pos("d8")));
        assertEquals(Optional.of(pos("d5")), board.ball());
        assertEquals(Optional.of(pos("e4")), copy.ball());
    }

    @Test
    void applyAndRevertMoveBall() {
        // Arrange
        Board board = BoardFake.empty();
        board.placeBall(pos("d5"));
        MoveBall effect = new MoveBall(pos("d5"), pos("f6"), false);

        // Act + Assert
        effect.apply(board);
        assertEquals(Optional.of(pos("f6")), board.ball());
        effect.revert(board);
        assertEquals(Optional.of(pos("d5")), board.ball());
    }

    @Test
    void failsWithoutPlayNothingIfBallNotIsWhereWaits() {
        // Arrange
        Board board = BoardFake.empty();
        board.placeBall(pos("e4"));

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> new MoveBall(pos("d5"), pos("f6"), false).apply(board));
        assertThrows(IllegalStateException.class,
                () -> new MoveBall(pos("d5"), pos("f6"), false).apply(BoardFake.empty()));
        assertEquals(Optional.of(pos("e4")), board.ball());
    }

    @Test
    void layoutOfFootballMarksPitchWithoutChangePieces() {
        // Act
        Board board = new FootballLayout(new StandardLayout(), new BallRule()).createBoard();

        // Assert: pelota en d5, arcos en d/e de cada fila de fondo, y las mismas 32 piezas
        assertEquals(Optional.of(pos("d5")), board.ball());
        assertEquals(32, board.pieces().size());
        assertEquals(Optional.of(Side.WHITE), board.netIn(pos("d1")));
        assertEquals(Optional.of(Side.WHITE), board.netIn(pos("e1")));
        assertEquals(Optional.of(Side.BLACK), board.netIn(pos("d8")));
        assertEquals(Optional.of(Side.BLACK), board.netIn(pos("e8")));
        assertTrue(board.netIn(pos("c8")).isEmpty());
        assertTrue(board.netIn(pos("d2")).isEmpty());
    }

    @Test
    void squareOfBallChangesFingerprint() {
        // Arrange
        Board withoutBall = new StandardLayout().createBoard();
        Board inD5 = new FootballLayout(new StandardLayout(), new BallRule()).createBoard();
        Board inD4 = new FootballLayout(new StandardLayout(), new BallRule(
                java.util.Set.of(3, 4), pos("d4"), pos("d5"), pos("d4"))).createBoard();

        // Act
        long fingerprintWithout = PositionFingerprint.de(withoutBall, Side.WHITE);
        long fingerprintD5 = PositionFingerprint.de(inD5, Side.WHITE);
        long fingerprintD4 = PositionFingerprint.de(inD4, Side.WHITE);

        // Assert
        assertNotEquals(fingerprintWithout, fingerprintD5);
        assertNotEquals(fingerprintD5, fingerprintD4);
    }

    @Test
    void fingerprintUpdatedWithAGoalMatchesWithCalculatedFromZero() {
        // Arrange: torre blanca con la pelota en d2, a punto de hacer gol
        Board board = BoardFake.from(
                ".......k",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...R....",
                "....K...");
        new core.rules.BallRule().preparePitch(board);
        board.placeBall(pos("d2"));
        long before = PositionFingerprint.de(board, Side.WHITE);
        Move goal = new BallRule().transform(board, Move.normal(board, pos("d2"), pos("d8"))).get(0);

        // Act
        goal.apply(board);

        // Assert
        assertTrue(goal.isGoal());
        assertEquals(PositionFingerprint.de(board, Side.BLACK), PositionFingerprint.tras(before, goal));
    }

    @Test
    void scoreboardAccountGoalsOfEachSide() {
        // Arrange
        Rook white = new Rook(Side.WHITE);
        Rook black = new Rook(Side.BLACK);
        Move goalWhite = new Move(pos("d2"), pos("d8"), List.of(
                new Transfer(white, pos("d2"), pos("d8")), new MoveBall(pos("d2"), pos("d5"), true)));
        Move goalBlack = new Move(pos("d7"), pos("d1"), List.of(
                new Transfer(black, pos("d7"), pos("d1")), new MoveBall(pos("d7"), pos("d4"), true)));
        Move withoutGoal = new Move(pos("d5"), pos("d6"), List.of(
                new Transfer(white, pos("d5"), pos("d6")), new MoveBall(pos("d5"), pos("d6"), false)));

        // Act
        Scoreboard scoreboard = Scoreboard.de(List.of(goalWhite, withoutGoal, goalBlack, goalWhite));

        // Assert
        assertEquals(new Scoreboard(2, 1), scoreboard);
        assertEquals(2, scoreboard.goals(Side.WHITE));
        assertEquals(Optional.of(Side.WHITE), scoreboard.leader());
        assertEquals(Optional.empty(), Scoreboard.de(List.of(goalWhite, goalBlack)).leader());
    }

    @Test
    void passMovesBallWithoutMovePiecesAndReverts() {
        // Arrange
        Board board = BoardFake.empty();
        Rook passer = new Rook(Side.WHITE);
        Rook receiver = new Rook(Side.WHITE);
        board.place(pos("a1"), passer);
        board.place(pos("a5"), receiver);
        board.placeBall(pos("a1"));
        Pass pass = new Pass(passer, pos("a1"), receiver, pos("a5"));

        // Act + Assert
        pass.apply(board);
        assertEquals(Optional.of(pos("a5")), board.ball());
        assertSame(passer, board.pieceIn(pos("a1")).orElseThrow());
        pass.revert(board);
        assertEquals(Optional.of(pos("a1")), board.ball());
    }

    @Test
    void passFailsIfMissingPartnerOrIsARival() {
        // Arrange
        Board board = BoardFake.empty();
        Rook passer = new Rook(Side.WHITE);
        board.place(pos("a1"), passer);
        board.placeBall(pos("a1"));

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> new Pass(passer, pos("a1"), new Rook(Side.WHITE), pos("a5")).apply(board));
        assertEquals(Optional.of(pos("a1")), board.ball());
        assertThrows(IllegalArgumentException.class,
                () -> new Pass(passer, pos("a1"), new Rook(Side.BLACK), pos("a5")));
    }

    @Test
    void aMoveOfPassHasAsPieceMoveToThatPassesAndFingerprintUpdates() {
        // Arrange
        Board board = BoardFake.empty();
        Rook passer = new Rook(Side.WHITE);
        Rook receiver = new Rook(Side.WHITE);
        board.place(pos("a1"), passer);
        board.place(pos("a5"), receiver);
        board.placeBall(pos("a1"));
        Move pass = new Move(pos("a1"), pos("a5"), List.of(new Pass(passer, pos("a1"), receiver, pos("a5"))));
        long before = PositionFingerprint.de(board, Side.WHITE);

        // Act
        pass.apply(board);

        // Assert
        assertTrue(pass.isPass());
        assertSame(passer, pass.pieceMoved());
        assertEquals(PositionFingerprint.de(board, Side.BLACK), PositionFingerprint.tras(before, pass));
        assertThrows(IllegalArgumentException.class, () -> new Move(pos("b1"), pos("a5"),
                List.of(new Pass(passer, pos("a1"), receiver, pos("a5")))));
    }
}
