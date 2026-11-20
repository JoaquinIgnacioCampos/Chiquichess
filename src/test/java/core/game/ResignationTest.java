package core.game;

import core.side.Side;
import core.rules.*;
import core.rules.FinishReason;
import core.time.NoClock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static doubles.Squares.move;
import static org.junit.jupiter.api.Assertions.*;

class ResignationTest {
    private Game game() {
        return new Game(new StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                new GameRules(new CheckDetector(), List.of(new Checkmate(), new Stalemate())), new NoClock());
    }

    @Test
    void yieldWhiteAndWinBlack() {
        Game game = game();
        game.resign(Side.WHITE);
        assertEquals(Optional.of(Side.BLACK), game.result().orElseThrow().winner());
        assertEquals(FinishReason.RESIGNATION, game.result().orElseThrow().reason());
        assertEquals(Side.BLACK, game.winner().orElseThrow().color());
        assertTrue(game.state().isFinished());
        assertFalse(game.state().isDraw());
    }

    @Test
    void yieldBlackInItsTurnAndWinWhite() {
        Game game = game();
        game.move(move("e2", "e4"));
        game.resign(Side.BLACK);
        assertEquals(Optional.of(Side.WHITE), game.result().orElseThrow().winner());
        assertEquals(Side.WHITE, game.winner().orElseThrow().color());
    }

    @Test
    void whiteNotCanResignDuringBlackTurn() {
        Game game = game();
        game.move(move("e2", "e4"));
        var error = assertThrows(InvalidActionException.class, () -> game.resign(Side.WHITE));
        assertEquals(InvalidActionException.Reason.NOT_YOUR_TURN, error.reason());
        assertTrue(game.result().isEmpty());
        assertEquals(Side.BLACK, game.turn());
        assertEquals(List.of(move("e2", "e4")), game.history());
    }

    @Test
    void blackNotCanResignDuringWhiteTurn() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        var error = assertThrows(InvalidActionException.class, () -> game.resign(Side.BLACK));
        assertEquals(InvalidActionException.Reason.NOT_YOUR_TURN, error.reason());
        assertTrue(game.result().isEmpty());
        assertEquals(GameState.IN_PROGRESS, game.state());
        assertEquals(Optional.of(Side.WHITE), game.offerDrawPending());
    }

    @Test
    void resignAfterOfMateRejectsWithoutChangeResult() {
        Game game = game();
        game.move(move("f2", "f3"));
        game.move(move("e7", "e5"));
        game.move(move("g2", "g4"));
        game.move(move("d8", "h4"));
        var result = game.result();
        var error = assertThrows(InvalidActionException.class, () -> game.resign(Side.BLACK));
        assertEquals(InvalidActionException.Reason.FINISHED_GAME, error.reason());
        assertEquals(result, game.result());
    }

    @Test
    void moveAfterOfResignationRejects() {
        Game game = game();
        game.resign(Side.WHITE);
        var error = assertThrows(InvalidMoveException.class,
                () -> game.move(move("e2", "e4")));
        assertEquals(InvalidMoveException.Reason.FINISHED_GAME, error.reason());
        assertTrue(game.movementsLegal(doubles.Squares.pos("e2")).isEmpty());
    }

    @Test
    void resignTwoTimesRejects() {
        Game game = game();
        game.resign(Side.WHITE);
        assertThrows(InvalidActionException.class, () -> game.resign(Side.BLACK));
        assertEquals(Side.BLACK, game.winner().orElseThrow().color());
    }

    @Test
    void resignationNullNotFinishesGame() {
        Game game = game();
        assertThrows(NullPointerException.class, () -> game.resign(null));
        assertEquals(GameState.IN_PROGRESS, game.state());
    }
}
