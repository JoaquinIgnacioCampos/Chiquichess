package Nucleo.juego;

import Nucleo.reglas.ReglaDeLaPelota;
import Nucleo.tablero.DisposicionInicial;
import Nucleo.tablero.Tablero;

import java.util.Objects;

/**
 * Decorator: la misma disposición inicial, con la cancha del modo fútbol marcada (los arcos y la
 * pelota en el saque inicial). Las piezas quedan como las pone la disposición de base.
 */
public class DisposicionDeFutbol implements DisposicionInicial {

    private final DisposicionInicial base;
    private final ReglaDeLaPelota pelota;

    public DisposicionDeFutbol(DisposicionInicial base, ReglaDeLaPelota pelota) {
        this.base = Objects.requireNonNull(base, "base");
        this.pelota = Objects.requireNonNull(pelota, "pelota");
    }

    @Override
    public Tablero crearTablero() {
        Tablero tablero = base.crearTablero();
        pelota.prepararCancha(tablero);
        return tablero;
    }
}
