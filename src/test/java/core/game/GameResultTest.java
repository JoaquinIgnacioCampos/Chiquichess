package core.game;

import core.side.Side;
import core.rules.*;
import core.rules.FinishReason;
import core.time.NoClock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GameResultTest {
    private Game game(EndCondition... conditions) {
        return new Game(new StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                new GameRules(new CheckDetector(), List.of(conditions)), new NoClock());
    }

    @Test
    void inProgressNotHasResult() {
        assertTrue(game().result().isEmpty());
    }

    @Test
    void mateSeparatesWinnerAndReasonKeepingQueryPrevious() {
        Game game = game(c -> Optional.of(GameState.CHECK_MATE));
        GameResult result = game.result().orElseThrow();
        assertEquals(Optional.of(Side.BLACK), result.winner());
        assertEquals(FinishReason.CHECK_MATE, result.reason());
        assertEquals(Side.BLACK, game.winner().orElseThrow().color());
    }

    @Test
    void drawAutomaticKeepTheirReasonsWithoutWinner() {
        var cases = java.util.Map.of(
                GameState.DRAW_STALEMATE, FinishReason.STALEMATE,
                GameState.DRAW_REPETITION, FinishReason.REPETITION,
                GameState.DRAW_RULE_50_MOVEMENTS, FinishReason.RULE_50_MOVEMENTS);
        cases.forEach((state, reason) -> {
            Game game = game(c -> Optional.of(state));
            assertEquals(state, game.state());
            assertEquals(new GameResult(Optional.empty(), reason), game.result().orElseThrow());
            assertTrue(game.winner().isEmpty());
        });
    }

    @Test
    void resultRejectsCombinationsInconsistent() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameResult(Optional.of(Side.WHITE), FinishReason.AGREEMENT));
        assertThrows(IllegalArgumentException.class,
                () -> new GameResult(Optional.empty(), FinishReason.RESIGNATION));
    }
}
