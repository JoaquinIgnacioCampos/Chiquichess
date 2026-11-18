package Nucleo.ia;

import Nucleo.jugadas.Jugada;

import java.time.Duration;
import java.util.Optional;

/**
 * Strategy: cómo decide sus jugadas un jugador que no es humano. Los adaptadores (la ventana, la
 * consola) solo conocen esta interfaz: le piden una jugada y la juegan con {@code Partida.mover}, igual
 * que si la hubiera elegido una persona. Hoy la implementa {@link JugadorArtificial}.
 */
public interface EstrategiaDeJuego {

    /**
     * La jugada elegida para quien tiene el turno en la situación.
     *
     * @param tiempoMaximo cuánto puede pensar como mucho (por ejemplo, para no perder por tiempo con reloj)
     * @throws IllegalStateException si no hay ninguna jugada legal (la partida ya debería haber terminado)
     */
    Jugada elegirJugada(SituacionDePartida situacion, Duration tiempoMaximo);

    /**
     * La jugada y cuánto tardaría en jugarla una persona (los adaptadores esperan hasta ese momento para
     * mover). Por defecto responde sin demora, pensando como mucho lo que permite el reloj.
     *
     * @param tiempoRestante lo que le queda en el reloj; vacío si la partida no tiene tiempo
     */
    default DecisionDeJuego decidir(SituacionDePartida situacion, Optional<Duration> tiempoRestante) {
        Duration maximo = RitmoHumano.limiteDelReloj(tiempoRestante).orElse(Duration.ofMinutes(10));
        return new DecisionDeJuego(elegirJugada(situacion, maximo), Duration.ZERO);
    }

    /**
     * Si acepta las tablas que le ofrece el rival. {@code situacion.turno()} es el bando que ofrece, es
     * decir, el rival de quien responde.
     */
    boolean aceptaTablas(SituacionDePartida situacion);
}
