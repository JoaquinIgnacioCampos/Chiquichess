package Nucleo.juego;

import Nucleo.tablero.DisposicionInicial;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.Objects;

/**
 * Decorator: la misma disposición inicial, con la pelota del modo fútbol en la casilla del saque.
 * Las piezas quedan como las pone la disposición de base.
 */
public class DisposicionConPelota implements DisposicionInicial {

    private final DisposicionInicial base;
    private final Posicion saque;

    public DisposicionConPelota(DisposicionInicial base, Posicion saque) {
        this.base = Objects.requireNonNull(base, "base");
        this.saque = Objects.requireNonNull(saque, "saque");
    }

    @Override
    public Tablero crearTablero() {
        Tablero tablero = base.crearTablero();
        tablero.colocarPelota(saque);
        return tablero;
    }
}
