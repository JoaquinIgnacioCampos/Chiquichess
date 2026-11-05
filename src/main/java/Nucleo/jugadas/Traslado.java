package Nucleo.jugadas;

import Nucleo.piezas.Pieza;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.Objects;

/** Lleva una pieza de una casilla a otra casilla vacía. */
public record Traslado(Pieza pieza, Posicion origen, Posicion destino) implements Efecto {

    public Traslado {
        Objects.requireNonNull(pieza, "pieza");
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
        if (origen.equals(destino)) {
            throw new IllegalArgumentException("El origen y el destino deben ser distintos");
        }
    }

    @Override
    public void aplicar(Tablero tablero) {
        mover(tablero, origen, destino);
    }

    @Override
    public void revertir(Tablero tablero) {
        mover(tablero, destino, origen);
    }

    private void mover(Tablero tablero, Posicion desde, Posicion hacia) {
        if (tablero.piezaEn(desde).orElse(null) != pieza) {
            throw new IllegalStateException("No está la pieza esperada en " + desde);
        }
        if (tablero.piezaEn(hacia).isPresent()) {
            throw new IllegalStateException("La casilla " + hacia + " no está vacía");
        }
        tablero.quitar(desde);
        tablero.colocar(hacia, pieza);
    }
}
