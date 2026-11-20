package core.rules;

import core.side.Side;
import core.game.Player;
import core.game.Game;
import core.moves.Move;
import core.board.Board;
import core.time.NoClock;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/** Cómo se mueve la pelota, jugado en partidas reales con las reglas del modo fútbol. */
class BallRuleTest {

    private static Game game(String ball, String... rows) {
        Board board = BoardFake.from(rows);
        new core.rules.BallRule().preparePitch(board);
        board.placeBall(pos(ball));
        return new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.football().withUndo(), new NoClock());
    }

    private static Optional<?> ball(Game game) {
        return game.board().ball();
    }

    @Test
    void withoutBallNotChangesNoMove() {
        // Arrange
        Board board = new core.game.StandardLayout().createBoard();
        Move move = Move.normal(board, pos("e2"), pos("e4"));

        // Act + Assert
        assertEquals(List.of(move), new BallRule().transform(board, move));
    }

    @Test
    void movesThatNotPlayBallStayEqual() {
        // Arrange
        Board board = new core.game.StandardLayout().createBoard();
        board.placeBall(pos("d5"));
        Move move = Move.normal(board, pos("e2"), pos("e4"));

        // Act + Assert
        assertEquals(List.of(move), new BallRule().transform(board, move));
    }

    @Test
    void endInSquareOfBallTakesAndAfterCarries() {
        // Arrange
        Game game = game("d5",
                "....k...",
                "........",
                "........",
                "........",
                "........",
                "..N.....",
                "........",
                "....K...");

        // Act
        game.move(move("c3", "d5"));
        game.move(move("e8", "f8"));
        game.move(move("d5", "f6"));

        // Assert
        assertEquals(Optional.of(pos("f6")), ball(game));
    }

    @Test
    void passByAboveOfBallNotTakes() {
        // Arrange
        Game game = game("d4",
                "....k...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...RK...");

        // Act
        game.move(move("d1", "d7"));

        // Assert
        assertEquals(Optional.of(pos("d4")), ball(game));
    }

    @Test
    void captureToWhoCarriesBallRemoves() {
        // Arrange: el caballo negro de d5 tiene la pelota
        Game game = game("d5",
                "....k...",
                "........",
                "........",
                "...n....",
                "....P...",
                "........",
                "........",
                "....K...");

        // Act
        game.move(move("e4", "d5"));
        game.move(move("e8", "f8"));
        game.move(move("d5", "d6"));

        // Assert: el peón que capturó se la llevó
        assertEquals(Optional.of(pos("d6")), ball(game));
    }

    @Test
    void inCaptureToStepBallGoesWithWhoCapture() {
        // Arrange: el peón negro de d7 tiene la pelota y avanza dos casillas al lado del peón blanco
        Game game = game("d7",
                "....k...",
                "...p....",
                "........",
                "....P...",
                "........",
                "........",
                "........",
                "....K...");
        game.move(move("e1", "f1"));
        game.move(move("d7", "d5"));

        // Act
        game.move(move("e5", "d6"));

        // Assert
        assertTrue(game.board().pieceIn(pos("d5")).isEmpty());
        assertEquals(Optional.of(pos("d6")), ball(game));
    }

    @Test
    void carryBallToNetRivalIsGoalAndBallReturnsToKickoff() {
        // Arrange
        Game game = game("d2",
                ".......k",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...R....",
                "....K...");

        // Act
        game.move(move("d2", "d8"));

        // Assert: la torre queda en el arco y la pelota va a d5, el campo del que recibió el gol
        assertTrue(game.board().pieceIn(pos("d8")).orElseThrow().isRook());
        assertEquals(Optional.of(pos("d5")), ball(game));
        assertEquals(new Scoreboard(1, 0), game.scoreboard().orElseThrow());
        assertFalse(game.state().isFinished());
    }

    @Test
    void goalOfBlackReturnsToKickoffOfFieldWhite() {
        // Arrange
        Game game = game("d7",
                "k.......",
                "...r....",
                "........",
                "........",
                "........",
                "........",
                "........",
                ".......K");
        game.move(move("h1", "h2"));

        // Act
        game.move(move("d7", "d1"));

        // Assert
        assertEquals(Optional.of(pos("d4")), ball(game));
        assertEquals(new Scoreboard(0, 1), game.scoreboard().orElseThrow());
    }

    @Test
    void kingCanCarryBallButNotDoesGoal() {
        // Arrange
        Game game = game("d7",
                ".......k",
                "...K....",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........");

        // Act
        game.move(move("d7", "d8"));

        // Assert
        assertEquals(Optional.of(pos("d8")), ball(game));
        assertEquals(new Scoreboard(0, 0), game.scoreboard().orElseThrow());
    }

    @Test
    void aPawnCanPromoteAndDoGoalInSameMove() {
        // Arrange
        Game game = game("e7",
                ".......k",
                "....P...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "K.......");

        // Act
        game.move(move("e7", "e8"));

        // Assert
        assertTrue(game.board().pieceIn(pos("e8")).orElseThrow().code() == 'Q');
        assertEquals(Optional.of(pos("d5")), ball(game));
        assertEquals(new Scoreboard(1, 0), game.scoreboard().orElseThrow());
    }

    @Test
    void ifHasAPieceInKickoffStaysWithBall() {
        // Arrange: el caballo negro está parado en d5, el saque tras un gol blanco
        Game game = game("e2",
                "........",
                ".......k",
                "........",
                "...n....",
                "........",
                "........",
                "....R...",
                "K.......");
        game.move(move("e2", "e8"));

        // Act
        game.move(move("d5", "f4"));

        // Assert
        assertEquals(Optional.of(pos("f4")), ball(game));
    }

    @Test
    void undoAGoalReturnsBallAndScoreboard() {
        // Arrange
        Game game = game("d2",
                ".......k",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...R....",
                "....K...");
        game.move(move("d2", "d8"));

        // Act
        game.undo();

        // Assert
        assertEquals(Optional.of(pos("d2")), ball(game));
        assertEquals(new Scoreboard(0, 0), game.scoreboard().orElseThrow());

        // Act: rehacer vuelve a meter el gol
        game.redo();

        // Assert
        assertEquals(Optional.of(pos("d5")), ball(game));
        assertEquals(new Scoreboard(1, 0), game.scoreboard().orElseThrow());
    }

    @Test
    void withoutNetsMarkedInBoardNotHasGoal() {
        // Arrange: la pelota está, pero el tablero no tiene la cancha marcada
        Board board = BoardFake.from(
                ".......k",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...R....",
                "....K...");
        board.placeBall(pos("d2"));
        Game game = new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.football(), new NoClock());

        // Act
        game.move(move("d2", "d8"));

        // Assert: la torre se llevó la pelota a d8, pero no fue gol
        assertEquals(Optional.of(pos("d8")), ball(game));
        assertEquals(new Scoreboard(0, 0), game.scoreboard().orElseThrow());
    }
}
