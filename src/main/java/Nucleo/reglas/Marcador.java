package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Goles de cada bando en el modo fútbol.
 *
 * No se lleva un contador: se cuenta en las jugadas ejecutadas, que ya guardan si fueron gol (ver
 * {@link Nucleo.jugadas.MoverPelota}). Así el marcador siempre coincide con el historial, también
 * después de deshacer o rehacer un gol, sin necesidad de un Memento propio.
 */
public record Marcador(int blancas, int negras) {

    public Marcador {
        if (blancas < 0 || negras < 0) {
            throw new IllegalArgumentException("Los goles no pueden ser negativos");
        }
    }

    /** Marcador que resulta de esas jugadas. En una partida sin pelota es 0 a 0. */
    public static Marcador de(List<Jugada> jugadas) {
        int blancas = 0;
        int negras = 0;
        for (Jugada jugada : Objects.requireNonNull(jugadas, "jugadas")) {
            if (jugada.esGol()) {
                if (jugada.piezaMovida().color() == Bando.BLANCO) {
                    blancas++;
                } else {
                    negras++;
                }
            }
        }
        return new Marcador(blancas, negras);
    }

    public int goles(Bando bando) {
        return Objects.requireNonNull(bando, "bando") == Bando.BLANCO ? blancas : negras;
    }

    /** El bando que va ganando; vacío si están empatados. */
    public Optional<Bando> lider() {
        if (blancas == negras) {
            return Optional.empty();
        }
        return Optional.of(blancas > negras ? Bando.BLANCO : Bando.NEGRO);
    }
}
