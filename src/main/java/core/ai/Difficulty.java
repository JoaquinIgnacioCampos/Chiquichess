package core.ia;

import java.time.Duration;

/**
 * Las tres dificultades usan el mismo buscador Minimax. Cambian en cuánto piensan y en cuánto se
 * pueden equivocar:
 *
 * @param depthMaximum cuántas jugadas hacia adelante mira, como máximo (la suya, la del rival...)
 * @param calculationTime   cuánto puede pensar; se queda con la búsqueda más profunda que terminó a tiempo
 * @param noise             error en centésimos de peón que se suma al azar al puntaje de cada jugada:
 *                          con 150 puede elegir una jugada hasta un peón y medio peor que la mejor, pero
 *                          nunca regala una pieza que vio
 * @param withStillness        si al llegar al límite sigue mirando los cambios de piezas en curso, para no
 *                          cortar el análisis en medio de una captura (lo que haría un buen jugador)
 * @param oversight          probabilidad de distraerse en una jugada: juega cualquiera de sus jugadas que
 *                          no le den mate en 1, sin pensar cuál conviene (como un principiante)
 * @param responseMinimum   lo que tarda en responder, como una persona de ese nivel, en una jugada común:
 *                          entre este mínimo y {@code responseMaximum} (ver {@link HumanRhythm})
 * @param responseMaximum   lo más que tarda en una jugada común
 * @param calculatesWhileWaits si usa todo el tiempo de respuesta para calcular (mira más hondo) en
 *                          lugar de calcular {@code calculationTime} y esperar el resto
 */
public enum Difficulty {
    /**
     * Mira una sola jugada: ve lo que gana con su movimiento, pero no la respuesta del rival (por eso,
     * por ejemplo, se come un peón defendido con la dama y la pierde). Y una de cada cinco se distrae.
     */
    FACIL(1, Duration.ofMillis(300), 150, false, 0.2, Duration.ofSeconds(1), Duration.ofSeconds(3), false),
    MEDIO(3, Duration.ofMillis(800), 25, true, 0, Duration.ofSeconds(2), Duration.ofSeconds(5), false),
    /** Piensa de verdad todo lo que tarda: por eso mira más hondo que los otros dos. */
    DIFICIL(64, Duration.ofMillis(1500), 0, true, 0, Duration.ofSeconds(3), Duration.ofSeconds(7), true);

    private final int depthMaximum;
    private final Duration calculationTime;
    private final int noise;
    private final boolean withStillness;
    private final double oversight;
    private final Duration responseMinimum;
    private final Duration responseMaximum;
    private final boolean calculatesWhileWaits;

    Difficulty(int depthMaximum, Duration calculationTime, int noise, boolean withStillness, double oversight,
               Duration responseMinimum, Duration responseMaximum, boolean calculatesWhileWaits) {
        this.depthMaximum = depthMaximum;
        this.calculationTime = calculationTime;
        this.noise = noise;
        this.withStillness = withStillness;
        this.oversight = oversight;
        this.responseMinimum = responseMinimum;
        this.responseMaximum = responseMaximum;
        this.calculatesWhileWaits = calculatesWhileWaits;
    }

    public Duration responseMinimum() {
        return responseMinimum;
    }

    public Duration responseMaximum() {
        return responseMaximum;
    }

    public boolean calculatesWhileWaits() {
        return calculatesWhileWaits;
    }

    public int depthMaximum() {
        return depthMaximum;
    }

    public Duration calculationTime() {
        return calculationTime;
    }

    public int noise() {
        return noise;
    }

    public boolean withStillness() {
        return withStillness;
    }

    public double oversight() {
        return oversight;
    }
}
