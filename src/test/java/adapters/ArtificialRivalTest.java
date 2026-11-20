package adapters;

import core.side.Side;
import core.game.StandardLayout;
import core.game.Player;
import core.game.Game;
import core.game.GameInProgress;
import core.rules.GameRules;
import core.time.NoClock;
import doubles.StrategyFalse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static doubles.Squares.move;
import static org.junit.jupiter.api.Assertions.*;

/** La IA como jugador de la partida: juega, responde tablas y deshace de a dos. */
class ArtificialRivalTest {

    private final GameInProgress game = new Game(new StandardLayout().createBoard(),
            new Player("Ana", Side.WHITE), new Player("IA", Side.BLACK),
            GameRules.classic().withUndo(), new NoClock());

    private ArtificialRival rival(boolean acceptsDraw) {
        return new ArtificialRival(game, Side.BLACK, new StrategyFalse(acceptsDraw));
    }

    @Test
    void playsOnlyWhenHimPlays() {
        // Arrange
        ArtificialRival ia = rival(false);

        // Act + Assert
        assertFalse(ia.himPlays());
        assertThrows(IllegalStateException.class, ia::play);
        game.move(move("e2", "e4"));
        assertTrue(ia.himPlays());
        ia.play();
        assertEquals(Side.WHITE, game.turn());
        assertEquals(2, game.history().size());
    }

    @Test
    void withClockThinksAFractionOfItThatHimStays() {
        // Arrange
        StrategyFalse strategy = new StrategyFalse(false);
        ArtificialRival ia = new ArtificialRival(game, Side.BLACK, strategy);
        game.move(move("e2", "e4"));

        // Act
        ia.think(game.situation(), Optional.of(Duration.ofSeconds(60)));

        // Assert
        assertEquals(Duration.ofSeconds(60).dividedBy(core.ia.HumanRhythm.REMAINING_TIME_FRACTION),
                strategy.timesReceived.getFirst());
    }

    @Test
    void undoReturnsUntilThatHimPlaysToPerson() {
        // Arrange
        ArtificialRival ia = rival(false);
        game.move(move("e2", "e4"));
        ia.play();

        // Act
        ia.undo();

        // Assert: se deshicieron la jugada de la IA y la de la persona
        assertTrue(game.history().isEmpty());
        assertEquals(Side.WHITE, game.turn());
    }

    @Test
    void ifAiYetNotRespondedUndoesOnlyMoveOfPerson() {
        // Arrange
        ArtificialRival ia = rival(false);
        game.move(move("e2", "e4"));
        game.move(move("e7", "e5"));
        game.move(move("g1", "f3"));

        // Act
        ia.undo();

        // Assert
        assertEquals(2, game.history().size());
        assertEquals(Side.WHITE, game.turn());
    }

    @Test
    void redoReturnsToPutMoveOfPersonAndResponse() {
        // Arrange
        ArtificialRival ia = rival(false);
        game.move(move("e2", "e4"));
        ia.play();
        ia.undo();

        // Act
        ia.redo();

        // Assert
        assertEquals(2, game.history().size());
        assertEquals(Side.WHITE, game.turn());
    }

    @Test
    void respondsDrawAccordingItsStrategy() {
        // Arrange
        GameInProgress other = new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("IA", Side.BLACK), GameRules.classic(), new NoClock());
        ArtificialRival accepts = new ArtificialRival(other, Side.BLACK, new StrategyFalse(true));
        ArtificialRival rejects = rival(false);

        // Act
        other.offerDraw(Side.WHITE);
        game.offerDraw(Side.WHITE);

        // Assert
        assertTrue(accepts.respondDraw());
        assertTrue(other.state().isDraw());
        assertFalse(rejects.respondDraw());
        assertTrue(game.offerDrawPending().isEmpty());
        assertFalse(game.state().isFinished());
    }

    @Test
    void withoutOfferOfRivalNotDoesNothing() {
        assertFalse(rival(true).respondDraw());
        assertFalse(game.state().isFinished());
    }
}
