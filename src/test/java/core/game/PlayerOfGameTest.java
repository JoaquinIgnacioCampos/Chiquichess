package core.game;

import core.side.Side;
import core.rules.CheckDetector;
import core.rules.GameRules;
import core.time.NoClock;
import org.junit.jupiter.api.Test;
import java.util.List;
import static doubles.Squares.move;
import static org.junit.jupiter.api.Assertions.*;

class PlayerOfGameTest {
    private final Player white = new Player("Lucía", Side.WHITE);
    private final Player black = new Player("Tomás", Side.BLACK);
    private final Game game = new Game(new StandardLayout().createBoard(),
            white, black, new GameRules(new CheckDetector(), List.of()), new NoClock());

    @Test
    void queryBothSidesWithoutDependOfTurn() {
        assertEquals(white, game.player(Side.WHITE));
        assertEquals(black, game.player(Side.BLACK));
        game.move(move("e2", "e4"));
        assertEquals(white, game.player(Side.WHITE));
        assertEquals(black, game.player(Side.BLACK));
        assertEquals(game.player(Side.BLACK), game.currentPlayer());
    }

    @Test
    void queryNotModifiesGameAndRejectsColorNull() {
        assertThrows(NullPointerException.class, () -> game.player(null));
        assertEquals(Side.WHITE, game.turn());
        assertTrue(game.history().isEmpty());
        assertTrue(game.result().isEmpty());
    }
}
