package Nucleo.ia;

import Nucleo.jugadas.Jugada;

import java.time.Duration;
import java.util.Objects;

/**
 * Lo que decidió una IA: la jugada y cuánto tardaría en jugarla una persona de su nivel, contando desde
 * que empezó a pensar. Quien la hace jugar espera hasta ese momento para mover.
 */
public record DecisionDeJuego(Jugada jugada, Duration tiempoDeRespuesta) {
    public DecisionDeJuego {
        Objects.requireNonNull(jugada, "jugada");
        Objects.requireNonNull(tiempoDeRespuesta, "tiempoDeRespuesta");
        if (tiempoDeRespuesta.isNegative()) throw new IllegalArgumentException("El tiempo no puede ser negativo");
    }
}
