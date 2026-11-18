package Nucleo.ia;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Cuánto tardaría una persona de ese nivel en responder, para que la IA no juegue "de golpe" ni
 * siempre al mismo ritmo. Es una función pura: el azar llega como números entre 0 y 1, así se prueba
 * sin esperar.
 *
 * <ul>
 *   <li><b>Jugada común:</b> entre la respuesta mínima y la máxima de la dificultad, más cerca del medio
 *       (promedio de dos azares).</li>
 *   <li><b>Una sola jugada posible:</b> ~1 s; no hay nada que pensar.</li>
 *   <li><b>En jaque con pocas salidas:</b> el mínimo de su rango.</li>
 *   <li><b>Apertura</b> (las primeras {@link #JUGADAS_DE_APERTURA} jugadas de la partida): la mitad,
 *       como quien juega de memoria.</li>
 *   <li><b>Posición complicada</b> (varias jugadas casi igual de buenas): hacia el máximo; ver
 *       {@link #alargarPorComplejidad}.</li>
 *   <li><b>Con reloj:</b> nunca más de 1/{@link #FRACCION_DEL_TIEMPO_RESTANTE} de lo que le queda.</li>
 * </ul>
 */
public final class RitmoHumano {
    public static final int FRACCION_DEL_TIEMPO_RESTANTE = 40;
    static final int JUGADAS_DE_APERTURA = 10;
    static final Duration JUGADA_FORZADA = Duration.ofMillis(900);
    static final Duration MINIMO_EN_APERTURA = Duration.ofMillis(800);

    private RitmoHumano() {
    }

    /**
     * Tiempo de respuesta antes de pensar la jugada.
     *
     * @param jugadasLegales cuántas jugadas puede hacer
     * @param jugadasHechas  jugadas de la partida hasta ahora (de los dos bandos)
     * @param azar1          número al azar entre 0 y 1
     * @param azar2          otro número al azar entre 0 y 1
     */
    public static Duration tiempo(Dificultad dificultad, int jugadasLegales, boolean enJaque, int jugadasHechas,
                                  Optional<Duration> tiempoRestante, double azar1, double azar2) {
        Objects.requireNonNull(dificultad, "dificultad");
        Objects.requireNonNull(tiempoRestante, "tiempoRestante");
        Duration tiempo;
        if (jugadasLegales <= 1) {
            tiempo = JUGADA_FORZADA.plusMillis(Math.round(300 * azar1));
        } else if (enJaque && jugadasLegales <= 3) {
            tiempo = dificultad.respuestaMinima();
        } else {
            tiempo = entre(dificultad, (azar1 + azar2) / 2);
            if (jugadasHechas < JUGADAS_DE_APERTURA) {
                tiempo = maximo(tiempo.dividedBy(2), MINIMO_EN_APERTURA);
            }
        }
        return conReloj(tiempo, tiempoRestante);
    }

    /**
     * En una posición complicada (varias jugadas casi igual de buenas), una persona duda: el tiempo sube
     * por lo menos a un punto al azar de la mitad alta de su rango (para que no sea siempre el mismo),
     * sin pasarse del reloj. En la apertura no: ahí casi todas las jugadas son parecidas y se juega
     * rápido igual.
     *
     * @param azar número al azar entre 0 y 1
     */
    public static Duration alargarPorComplejidad(Duration tiempo, Dificultad dificultad, int jugadasHechas,
                                                 Optional<Duration> tiempoRestante, double azar) {
        if (jugadasHechas < JUGADAS_DE_APERTURA) return tiempo;
        return conReloj(maximo(tiempo, entre(dificultad, 0.5 + 0.5 * azar)), tiempoRestante);
    }

    /** Lo más que puede tardar con ese tiempo restante; sin reloj, no hay límite. */
    public static Optional<Duration> limiteDelReloj(Optional<Duration> tiempoRestante) {
        return tiempoRestante.map(restante -> restante.dividedBy(FRACCION_DEL_TIEMPO_RESTANTE));
    }

    private static Duration entre(Dificultad dificultad, double fraccion) {
        long minimo = dificultad.respuestaMinima().toMillis();
        long maximo = dificultad.respuestaMaxima().toMillis();
        return Duration.ofMillis(minimo + Math.round((maximo - minimo) * Math.max(0, Math.min(1, fraccion))));
    }

    private static Duration conReloj(Duration tiempo, Optional<Duration> tiempoRestante) {
        return limiteDelReloj(tiempoRestante).filter(limite -> limite.compareTo(tiempo) < 0).orElse(tiempo);
    }

    private static Duration maximo(Duration una, Duration otra) {
        return una.compareTo(otra) >= 0 ? una : otra;
    }
}
