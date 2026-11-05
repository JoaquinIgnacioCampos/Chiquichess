package Nucleo.juego;

import Nucleo.piezas.Color;

import java.util.Objects;
import java.util.Optional;

/** Resultado final inmutable. Sin ganador significa tablas, no partida en curso. */
public record ResultadoPartida(Optional<Color> ganador, MotivoFinalizacion motivo) {
    public ResultadoPartida {
        Objects.requireNonNull(ganador, "ganador");
        Objects.requireNonNull(motivo, "motivo");
        if (motivo.esTablas() == ganador.isPresent()) {
            throw new IllegalArgumentException("Las tablas no tienen ganador; una victoria sí");
        }
    }
}
