package adaptadores.audio;

import Nucleo.bando.Bando;
import Nucleo.reglas.Marcador;

import java.util.Objects;
import java.util.Optional;

/**
 * Qué tan fuerte canta cada hinchada, de 0 a 1. Es una función del estado de la partida y no un
 * contador que se va sumando: así, al deshacer, la hinchada baja sola, y el cálculo se prueba sin
 * parlantes. El pico de un gol no sale de acá: lo pone el propio sonido del gol.
 *
 * <ul>
 *   <li>Arranca en {@link #BASE} y crece hasta {@link #POR_AVANCE} más a medida que se acerca el límite de jugadas.</li>
 *   <li>Si el partido está parejo (a lo sumo un gol de diferencia), suma hasta {@link #POR_PAREJO} más,
 *       también según el avance: un final apretado es lo que más se escucha.</li>
 *   <li>La hinchada del que va ganando canta completa; la del que pierde, a {@link #FACTOR_PERDEDOR};
 *       con empate, las dos a {@link #FACTOR_EMPATE}.</li>
 * </ul>
 */
public final class IntensidadDeHinchada {
    static final double BASE = 0.3;
    static final double POR_AVANCE = 0.5;
    static final double POR_PAREJO = 0.2;
    static final double FACTOR_PERDEDOR = 0.6;
    static final double FACTOR_EMPATE = 0.85;

    private IntensidadDeHinchada() {
    }

    /**
     * @param jugadasHechas jugadas de los dos bandos en lo que va de la partida
     * @param limite        jugadas a las que termina la partida; sin límite, el avance no cuenta
     */
    public static double de(Bando hinchada, int jugadasHechas, Optional<Integer> limite, Marcador marcador) {
        Objects.requireNonNull(hinchada, "hinchada");
        Objects.requireNonNull(limite, "limite");
        Objects.requireNonNull(marcador, "marcador");
        double avance = limite.filter(l -> l > 0)
                .map(l -> Math.min(1.0, Math.max(0, jugadasHechas) / (double) l))
                .orElse(0.0);
        boolean parejo = Math.abs(marcador.blancas() - marcador.negras()) <= 1;
        double general = BASE + POR_AVANCE * avance + (parejo ? POR_PAREJO * avance : 0);
        double factor = marcador.lider()
                .map(lider -> lider == hinchada ? 1.0 : FACTOR_PERDEDOR)
                .orElse(FACTOR_EMPATE);
        return Math.min(1.0, general * factor);
    }
}
