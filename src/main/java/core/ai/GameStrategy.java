package core.ai;

import core.game.GameSituation;
import core.moves.Move;

import java.time.Duration;
import java.util.Optional;

/**
 * Strategy: cómo decide sus jugadas un jugador que no es humano. Los adaptadores (la ventana, la
 * consola) solo conocen esta interfaz: le piden una jugada y la juegan con {@code Partida.mover}, igual
 * que si la hubiera elegido una persona. Hoy la implementa {@link JugadorArtificial}.
 */
public interface GameStrategy {

    /**
     * La jugada elegida para quien tiene el turno en la situación.
     *
     * @param tiempoMaximo cuánto puede pensar como mucho (por ejemplo, para no perder por tiempo con reloj)
     * @throws IllegalStateException si no hay ninguna jugada legal (la partida ya debería haber terminado)
     */
    Move chooseMove(GameSituation situation, Duration timeMaximum);

    /**
     * La jugada y cuánto tardaría en jugarla una persona (los adaptadores esperan hasta ese momento para
     * mover). Por defecto responde sin demora, pensando como mucho lo que permite el reloj.
     *
     * @param tiempoRestante lo que le queda en el reloj; vacío si la partida no tiene tiempo
     */
    default GameDecision decide(GameSituation situation, Optional<Duration> timeRemaining) {
        Duration maximum = HumanRhythm.clockLimit(timeRemaining).orElse(Duration.ofMinutes(10));
        return new GameDecision(chooseMove(situation, maximum), Duration.ZERO);
    }

    /**
     * Si acepta las tablas que le ofrece el rival. {@code situacion.turno()} es el bando que ofrece, es
     * decir, el rival de quien responde.
     */
    boolean acceptsDraw(GameSituation situation);
}
