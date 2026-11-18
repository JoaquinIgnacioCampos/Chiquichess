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
 */
public enum Dificultad {
    /**
     * Mira una sola jugada: ve lo que gana con su movimiento, pero no la respuesta del rival (por eso,
     * por ejemplo, se come un peón defendido con la dama y la pierde). Y una de cada cinco se distrae.
     */
    FACIL(1, Duration.ofMillis(300), 150, false, 0.2),
    MEDIO(3, Duration.ofMillis(800), 25, true, 0),
    DIFICIL(64, Duration.ofMillis(1500), 0, true, 0);

    private final int profundidadMaxima;
    private final Duration tiempoDeCalculo;
    private final int ruido;
    private final boolean conQuietud;
    private final double descuido;

    Dificultad(int profundidadMaxima, Duration tiempoDeCalculo, int ruido, boolean conQuietud, double descuido) {
        this.profundidadMaxima = profundidadMaxima;
        this.tiempoDeCalculo = tiempoDeCalculo;
        this.ruido = ruido;
        this.conQuietud = conQuietud;
        this.descuido = descuido;
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
