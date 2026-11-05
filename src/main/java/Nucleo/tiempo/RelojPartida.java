package Nucleo.tiempo;

import java.time.Duration;
import java.util.Optional;

import Nucleo.bando.Bando;
/** Reloj de una sola partida. Cada orden y consulta se ejecuta secuencialmente. */
public interface RelojPartida {
    void iniciar(Bando color);
    void cambiarTurno();
    void detener();

    /** Vacío indica que no existe límite de tiempo. */
    Optional<Duration> tiempoRestante(Bando color);

    default boolean tiempoAgotado(Bando color) {
        return tiempoRestante(color).map(tiempo -> tiempo.isZero()).orElse(false);
    }
}
