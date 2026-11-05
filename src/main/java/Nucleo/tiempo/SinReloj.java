package Nucleo.tiempo;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import Nucleo.bando.Bando;
/** Null Object: partida sin límite; evita condicionales por ausencia de reloj. */
public class SinReloj implements RelojPartida {
    @Override
    public void iniciar(Bando color) {
        Objects.requireNonNull(color, "color");
    }

    @Override
    public void cambiarTurno() {
    }

    @Override
    public void detener() {
    }

    @Override
    public Optional<Duration> tiempoRestante(Bando color) {
        Objects.requireNonNull(color, "color");
        return Optional.empty();
    }
}
