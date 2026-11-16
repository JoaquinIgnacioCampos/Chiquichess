package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.reglas.MotivoFinalizacion;

import java.util.Objects;
import java.util.Optional;

/** Resultado final inmutable. Sin ganador significa tablas, no partida en curso. */
public record ResultadoPartida(Optional<Bando> ganador, MotivoFinalizacion motivo) {
    public ResultadoPartida {
        Objects.requireNonNull(ganador, "ganador");
        Objects.requireNonNull(motivo, "motivo");
        if (motivo.esTablas() == ganador.isPresent()) {
            throw new IllegalArgumentException("Las tablas no tienen ganador; una victoria sí");
        }
    }
}
