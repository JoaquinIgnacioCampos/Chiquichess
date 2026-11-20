package core.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void inProgressAndCheckNotEndGame() {
        // Assert
        assertFalse(GameState.IN_PROGRESS.isFinished());
        assertFalse(GameState.CHECK.isFinished());
        assertFalse(GameState.IN_PROGRESS.isDraw());
        assertFalse(GameState.CHECK.isDraw());
    }

    @Test
    void checkMateEndsGameButNotIsDraw() {
        // Assert
        assertTrue(GameState.CHECK_MATE.isFinished());
        assertFalse(GameState.CHECK_MATE.isDraw());
    }

    @Test
    void allDrawEndGame() {
        // Assert
        for (GameState state : new GameState[]{
                GameState.DRAW_STALEMATE,
                GameState.DRAW_REPETITION,
                GameState.DRAW_RULE_50_MOVEMENTS}) {
            assertTrue(state.isFinished(), state.name());
            assertTrue(state.isDraw(), state.name());
        }
    }
}
