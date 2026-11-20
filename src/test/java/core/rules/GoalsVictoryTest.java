package core.rules;

import core.side.Side;
import core.game.Player;
import core.game.Game;
import core.game.GameResult;
import core.board.Board;
import core.time.NoClock;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * El final por goles, jugado en partidas reales. Se usan límites chicos (1 gol, 2 jugadas) para no
 * tener que jugar partidas largas: la regla es la misma.
 */
class GoalsVictoryTest {

    /** La torre blanca de d2 tiene la pelota y puede hacer gol en d8. */
    private static Game rookInD2(GoalsVictory victory, String rowEight) {
        Board board = BoardFake.from(
                rowEight,
                "........",
                "........",
                "........",
                "........",
                "........",
                "...T....",
                "....R...");
        new core.rules.BallRule().preparePitch(board);
        board.placeBall(pos("d2"));
        ICheckDetector detector = new CheckDetector();
        GameRules rules = new GameRules(
                new LegalMoves(detector, List.of(new Promotion(), new BallRule())),
                List.of(new Checkmate(), victory, new Stalemate()), true);
        return new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                rules, new NoClock());
    }

    @Test
    void rejectsLimitsNotPositive() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new GoalsVictory(0, 90));
        assertThrows(IllegalArgumentException.class, () -> new GoalsVictory(3, 0));
    }

    @Test
    void winsWhoArrivesToGoals() {
        // Arrange
        Game game = rookInD2(new GoalsVictory(1, 90), ".......r");

        // Act
        game.move(move("d2", "d8"));

        // Assert
        assertEquals(GameState.VICTORY_BY_GOALS, game.state());
        assertEquals(Optional.of(new GameResult(Optional.of(Side.WHITE), FinishReason.GOALS)),
                game.result());
    }

    @Test
    void onArriveToLimitWinsWhoHasMoreGoalsAlthoughHimCueMove() {
        // Arrange
        Game game = rookInD2(new GoalsVictory(3, 2), ".......r");

        // Act: gol blanco y una jugada negra; se llega a 2 jugadas con el turno de las blancas
        game.move(move("d2", "d8"));
        game.move(move("h8", "h7"));

        // Assert: ganan las blancas, que son las que tienen que mover
        assertEquals(Side.WHITE, game.turn());
        assertEquals(GameState.VICTORY_BY_GOALS, game.state());
        assertEquals(Optional.of(Side.WHITE), game.result().orElseThrow().winner());
    }

    @Test
    void onArriveToLimitWithGoalsEqualIsTie() {
        // Arrange
        Game game = rookInD2(new GoalsVictory(3, 2), ".......r");

        // Act
        game.move(move("d2", "d3"));
        game.move(move("h8", "g8"));

        // Assert
        assertEquals(GameState.DRAW_TIED_ON_GOALS, game.state());
        assertEquals(Optional.of(new GameResult(Optional.empty(), FinishReason.TIED_ON_GOALS)),
                game.result());
    }

    @Test
    void checkMateWinsAlthoughMoveAlsoIsGoal() {
        // Arrange: rey negro en a8 y rey blanco en b6; la torre a d8 es mate y gol a la vez
        Board board = BoardFake.from(
                "r.......",
                "........",
                ".R......",
                "........",
                "........",
                "........",
                "...T....",
                "........");
        new core.rules.BallRule().preparePitch(board);
        board.placeBall(pos("d2"));
        ICheckDetector detector = new CheckDetector();
        GameRules rules = new GameRules(
                new LegalMoves(detector, List.of(new Promotion(), new BallRule())),
                List.of(new Checkmate(), new GoalsVictory(1, 90), new Stalemate()), true);
        Game game = new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                rules, new NoClock());

        // Act
        game.move(move("d2", "d8"));

        // Assert
        assertEquals(GameState.CHECK_MATE, game.state());
        assertEquals(new Scoreboard(1, 0), game.scoreboard().orElseThrow());
    }

    @Test
    void undoGoalWinnerReopensGame() {
        // Arrange
        Game game = rookInD2(new GoalsVictory(1, 90), ".......r");
        game.move(move("d2", "d8"));

        // Act
        game.undo();

        // Assert
        assertFalse(game.state().isFinished());
        assertTrue(game.result().isEmpty());
        assertEquals(new Scoreboard(0, 0), game.scoreboard().orElseThrow());
    }

    @Test
    void rulesOfFootballPutMateBeforeThatGoals() {
        // Act
        List<EndCondition> conditions = GameRules.football().conditions();

        // Assert
        assertInstanceOf(Checkmate.class, conditions.get(0));
        assertInstanceOf(GoalsVictory.class, conditions.get(1));
        assertTrue(conditions.stream().anyMatch(Stalemate.class::isInstance));
        assertTrue(conditions.stream().anyMatch(RepetitionDraw.class::isInstance));
        assertTrue(conditions.stream().anyMatch(FiftyMoveRuleDraw.class::isInstance));
    }

    @Test
    void limitOfMovesItChoosesWhoBuildsRules() {
        // Arrange
        Game football = new Game(new core.game.FootballLayout(new core.game.StandardLayout(),
                new BallRule()).createBoard(), new Player("Ana", Side.WHITE),
                new Player("Beto", Side.BLACK), GameRules.football(30), new NoClock());
        Game classic = new Game(new core.game.StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.classic(), new NoClock());

        // Act + Assert
        assertEquals(Optional.of(30), football.moveLimit());
        assertEquals(Optional.of(90), GameRules.football().conditions().get(1).moveLimit());
        assertEquals(Optional.empty(), classic.moveLimit());
    }

    @Test
    void onlyHasScoreboardIfRulesPlayWithGoals() {
        // Arrange: el mismo tablero con pelota, con y sin la condición de los goles
        Game football = new Game(new core.game.FootballLayout(new core.game.StandardLayout(),
                new BallRule()).createBoard(), new Player("Ana", Side.WHITE),
                new Player("Beto", Side.BLACK), GameRules.football(), new NoClock());
        Game withBallWithoutGoals = new Game(new core.game.FootballLayout(
                new core.game.StandardLayout(), new BallRule()).createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.classic(), new NoClock());

        // Act + Assert: lo decide la condición de fin, no la pelota del tablero
        assertEquals(Optional.of(new Scoreboard(0, 0)), football.scoreboard());
        assertEquals(Optional.empty(), withBallWithoutGoals.scoreboard());
    }
}
