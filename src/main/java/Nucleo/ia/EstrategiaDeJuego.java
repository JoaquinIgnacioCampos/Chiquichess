package Nucleo.ia;

import Nucleo.jugadas.Jugada;

import java.time.Duration;

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
     * Si acepta las tablas que le ofrece el rival. {@code situacion.turno()} es el bando que ofrece, es
     * decir, el rival de quien responde.
     */
    boolean aceptaTablas(SituacionDePartida situacion);
}
