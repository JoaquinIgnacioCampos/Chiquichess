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
 */
public enum Dificultad {
    FACIL(2, Duration.ofMillis(300), 150, false),
    MEDIO(3, Duration.ofMillis(800), 25, true),
    DIFICIL(64, Duration.ofMillis(1500), 0, true);

    private final int profundidadMaxima;
    private final Duration tiempoDeCalculo;
    private final int ruido;
    private final boolean conQuietud;

    Dificultad(int profundidadMaxima, Duration tiempoDeCalculo, int ruido, boolean conQuietud) {
        this.profundidadMaxima = profundidadMaxima;
        this.tiempoDeCalculo = tiempoDeCalculo;
        this.ruido = ruido;
        this.conQuietud = conQuietud;
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
}
