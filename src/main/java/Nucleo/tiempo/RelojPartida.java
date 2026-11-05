package Nucleo.tiempo;

import Nucleo.piezas.Color;
import java.time.Duration;
import java.util.Optional;

/** Reloj de una sola partida. Cada orden y consulta se ejecuta secuencialmente. */
public interface RelojPartida {
    void iniciar(Color color);
    void cambiarTurno();
    void detener();

    /** Vacío indica que no existe límite de tiempo. */
    Optional<Duration> tiempoRestante(Color color);

    default boolean tiempoAgotado(Color color) {
        return tiempoRestante(color).map(tiempo -> tiempo.isZero()).orElse(false);
    }
}
