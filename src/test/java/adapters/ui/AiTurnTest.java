package adapters.ui;

import core.side.Side;
import core.game.StandardLayout;
import core.game.Player;
import core.game.Game;
import core.game.GameInProgress;
import core.rules.GameRules;
import core.time.NoClock;
import adapters.ArtificialRival;
import doubles.StrategyFalse;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

import static doubles.Squares.move;
import static org.junit.jupiter.api.Assertions.*;

/**
 * El turno de la IA en la ventana, sin hilos de verdad: pensar queda en una cola que el test ejecuta
 * cuando quiere, y "volver al hilo de la interfaz" es ejecutar en el momento.
 */
class AiTurnTest {

    private final Deque<Runnable> pending = new ArrayDeque<>();
    private final Executor inQueue = pending::add;

    private Game game() {
        return new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("IA", Side.BLACK), GameRules.classic().withUndo(), new NoClock());
    }

    private AiTurn turn(GameInProgress game, Side sideOfAi) {
        ArtificialRival rival = new ArtificialRival(game, sideOfAi, new StrategyFalse(false));
        return new AiTurn(game, rival, inQueue, Runnable::run);
    }

    private void endOfThink() {
        while (!pending.isEmpty()) pending.poll().run();
    }

    @Test
    void ifAiPlaysWithWhiteStartsBarelyStarts() {
        // Arrange
        GameInProgress game = game();
        AiTurn turn = turn(game, Side.WHITE);

        // Act
        turn.start();
        assertTrue(turn.thinking());
        endOfThink();

        // Assert
        assertEquals(1, game.history().size());
        assertEquals(Side.BLACK, game.turn());
        assertFalse(turn.thinking());
    }

    @Test
    void respondsAfterOfEachMoveOfPerson() {
        // Arrange
        GameInProgress game = game();
        turn(game, Side.BLACK).start();

        // Act
        game.move(move("e2", "e4"));
        endOfThink();
        game.move(move("d2", "d4"));
        endOfThink();

        // Assert
        assertEquals(4, game.history().size());
        assertEquals(Side.WHITE, game.turn());
    }

    @Test
    void ifGameChangesWhileThinksItThoughtDiscards() {
        // Arrange: la IA empieza a pensar su respuesta a e4
        GameInProgress game = game();
        turn(game, Side.BLACK).start();
        game.move(move("e2", "e4"));

        // Act: la persona se arrepiente antes de que la IA termine
        game.undo();
        endOfThink();

        // Assert: la IA no jugó sobre una posición que ya no existe
        assertTrue(game.history().isEmpty());
        assertEquals(Side.WHITE, game.turn());
    }

    @Test
    void afterOfCloseNotPlaysMore() {
        // Arrange
        GameInProgress game = game();
        AiTurn turn = turn(game, Side.BLACK);
        turn.start();
        game.move(move("e2", "e4"));

        // Act: se volvió al menú mientras pensaba
        turn.close();
        endOfThink();

        // Assert
        assertEquals(1, game.history().size());
    }
}
