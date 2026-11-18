package Nucleo.ia;

import java.time.Duration;

/**
 * Las tres dificultades usan el mismo buscador Minimax. Cambian en cuánto piensan y en cuánto se
 * pueden equivocar:
 *
 * @param profundidadMaxima cuántas jugadas hacia adelante mira, como máximo (la suya, la del rival...)
 * @param tiempoDeCalculo   cuánto puede pensar; se queda con la búsqueda más profunda que terminó a tiempo
 * @param ruido             error en centésimos de peón que se suma al azar al puntaje de cada jugada:
 *                          con 150 puede elegir una jugada hasta un peón y medio peor que la mejor, pero
 *                          nunca regala una pieza que vio
 * @param conQuietud        si al llegar al límite sigue mirando los cambios de piezas en curso, para no
 *                          cortar el análisis en medio de una captura (lo que haría un buen jugador)
 * @param descuido          probabilidad de distraerse en una jugada: juega cualquiera de sus jugadas que
 *                          no le den mate en 1, sin pensar cuál conviene (como un principiante)
 * @param respuestaMinima   lo que tarda en responder, como una persona de ese nivel, en una jugada común:
 *                          entre este mínimo y {@code respuestaMaxima} (ver {@link RitmoHumano})
 * @param respuestaMaxima   lo más que tarda en una jugada común
 * @param calculaMientrasEspera si usa todo el tiempo de respuesta para calcular (mira más hondo) en
 *                          lugar de calcular {@code tiempoDeCalculo} y esperar el resto
 */
public enum Dificultad {
    /**
     * Mira una sola jugada: ve lo que gana con su movimiento, pero no la respuesta del rival (por eso,
     * por ejemplo, se come un peón defendido con la dama y la pierde). Y una de cada cinco se distrae.
     */
    FACIL(1, Duration.ofMillis(300), 150, false, 0.2, Duration.ofSeconds(1), Duration.ofSeconds(3), false),
    MEDIO(3, Duration.ofMillis(800), 25, true, 0, Duration.ofSeconds(2), Duration.ofSeconds(5), false),
    /** Piensa de verdad todo lo que tarda: por eso mira más hondo que los otros dos. */
    DIFICIL(64, Duration.ofMillis(1500), 0, true, 0, Duration.ofSeconds(3), Duration.ofSeconds(7), true);

    private final int profundidadMaxima;
    private final Duration tiempoDeCalculo;
    private final int ruido;
    private final boolean conQuietud;
    private final double descuido;
    private final Duration respuestaMinima;
    private final Duration respuestaMaxima;
    private final boolean calculaMientrasEspera;

    Dificultad(int profundidadMaxima, Duration tiempoDeCalculo, int ruido, boolean conQuietud, double descuido,
               Duration respuestaMinima, Duration respuestaMaxima, boolean calculaMientrasEspera) {
        this.profundidadMaxima = profundidadMaxima;
        this.tiempoDeCalculo = tiempoDeCalculo;
        this.ruido = ruido;
        this.conQuietud = conQuietud;
        this.descuido = descuido;
        this.respuestaMinima = respuestaMinima;
        this.respuestaMaxima = respuestaMaxima;
        this.calculaMientrasEspera = calculaMientrasEspera;
    }

    public Duration respuestaMinima() {
        return respuestaMinima;
    }

    public Duration respuestaMaxima() {
        return respuestaMaxima;
    }

    public boolean calculaMientrasEspera() {
        return calculaMientrasEspera;
    }

    public int profundidadMaxima() {
        return profundidadMaxima;
    }

    public Duration tiempoDeCalculo() {
        return tiempoDeCalculo;
    }

    public int ruido() {
        return ruido;
    }

    public boolean conQuietud() {
        return conQuietud;
    }

    public double descuido() {
        return descuido;
    }
}
