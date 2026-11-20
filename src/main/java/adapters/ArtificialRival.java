package adapters;

import core.side.Side;
import core.ia.GameDecision;
import core.ia.GameStrategy;
import core.game.GameSituation;
import core.game.MoveRequest;
import core.game.GameInProgress;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * La IA sentada a la mesa: un jugador más de la partida, del lado de los adaptadores. Le pide jugadas a
 * su {@link GameStrategy} y las juega con {@code Game.move}, igual que una persona; por eso el
 * sonido, el panel y los eventos funcionan sin saber que es una IA. No depende de JavaFX: la usan la
 * consola (que la espera) y la ventana (que la hace pensar en otro hilo).
 *
 * También decide lo que en una partida entre personas decide el otro jugador: si acepta las tablas. Y
 * como deshacer contra la IA tiene que devolverle el turno a la persona, deshace de a dos jugadas.
 */
public final class ArtificialRival {
    /**
     * Lo que decidió la IA: el movimiento y cuánto tardaría una persona en jugarlo, contando desde que
     * empezó a pensar. La consola y la ventana esperan hasta ese momento para mover.
     */
    public record Response(MoveRequest movement, Duration delay) {
    }

    private final GameInProgress game;
    private final Side side;
    private final GameStrategy strategy;

    public ArtificialRival(GameInProgress game, Side side, GameStrategy strategy) {
        this.game = Objects.requireNonNull(game, "partida");
        this.side = Objects.requireNonNull(side, "bando");
        this.strategy = Objects.requireNonNull(strategy, "estrategia");
    }

    /** Con qué color juega la IA. */
    public Side side() {
        return side;
    }

    /** true si la partida sigue y es el turno de la IA. */
    public boolean himPlays() {
        return !game.state().isFinished() && game.turn() == side;
    }

    /**
     * Elige la jugada para esa situación y cuánto tardaría una persona en jugarla. No toca la partida,
     * así que se puede llamar desde otro hilo con una situación tomada antes. Con reloj, nunca piensa ni
     * tarda más de una fracción de lo que le queda: no pierde por tiempo.
     *
     * @param timeRemaining lo que le queda en el reloj; vacío si la partida no tiene tiempo
     */
    public Response think(GameSituation situation, Optional<Duration> timeRemaining) {
        GameDecision decision = strategy.decide(situation, timeRemaining);
        return new Response(MoveRequest.de(decision.move()), decision.responseTime());
    }

    /** Piensa sobre la situación actual de la partida (en este mismo hilo). */
    public Response think() {
        return think(game.situation(), game.timeRemaining(side));
    }

    /** Piensa y juega enseguida, en este mismo hilo, sin esperar la demora. Devuelve lo que jugó. */
    public MoveRequest play() {
        if (!himPlays()) throw new IllegalStateException("No es el turno de la IA");
        MoveRequest movement = think().movement();
        game.move(movement);
        return movement;
    }

    /**
     * Si el rival le ofreció tablas, las acepta o las rechaza según su estrategia; si no hay oferta del
     * rival, no hace nada. Devuelve true si las aceptó.
     */
    public boolean respondDraw() {
        if (game.offerDrawPending().filter(offerer -> offerer != side).isEmpty()) return false;
        if (strategy.acceptsDraw(game.situation())) {
            game.acceptDraw(side);
            return true;
        }
        game.rejectDraw(side);
        return false;
    }

    /**
     * Deshace hasta que le toque a la persona: la última jugada de la IA y la de la persona. Si la IA
     * todavía no había respondido, deshace solo la de la persona.
     *
     * @throws core.game.InvalidActionException si la partida no deja deshacer
     */
    public void undo() {
        game.undo();
        if (game.turn() == side && game.canUndo()) game.undo();
    }

    /** Rehace la jugada de la persona y la respuesta de la IA, si la había. */
    public void redo() {
        game.redo();
        if (game.turn() == side && game.canRedo()) game.redo();
    }
}
