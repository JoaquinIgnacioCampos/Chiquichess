package Nucleo.jugadas;

import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.Objects;

/**
 * Lleva la pelota del modo fútbol de una casilla a otra. Al revertir, la devuelve.
 *
 * Si la jugada es un gol, la pelota no queda en el arco: va directo a la casilla del saque, y el
 * efecto lo marca con {@code gol}. Así el marcador se puede contar mirando las jugadas, y deshacer
 * un gol es deshacer este efecto. En un gol el saque puede coincidir con la casilla de donde salió
 * la pelota, por eso origen y destino pueden ser iguales.
 */
public record MoverPelota(Posicion desde, Posicion hacia, boolean gol) implements Efecto {

    public MoverPelota {
        Objects.requireNonNull(desde, "desde");
        Objects.requireNonNull(hacia, "hacia");
    }

    @Override
    public void aplicar(Tablero tablero) {
        mover(tablero, desde, hacia);
    }

    @Override
    public void revertir(Tablero tablero) {
        mover(tablero, hacia, desde);
    }

    private static void mover(Tablero tablero, Posicion de, Posicion a) {
        if (!tablero.pelota().filter(de::equals).isPresent()) {
            throw new IllegalStateException("La pelota no está en " + de);
        }
        tablero.colocarPelota(a);
    }
}
