package Nucleo.jugadas;

import Nucleo.piezas.Pieza;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.Objects;

/** Saca una pieza del tablero (una captura). Al revertir, la devuelve a su casilla. */
public record Retiro(Pieza pieza, Posicion posicion) implements Efecto {

    public Retiro {
        Objects.requireNonNull(pieza, "pieza");
        Objects.requireNonNull(posicion, "posicion");
    }

    @Override
    public void aplicar(Tablero tablero) {
        if (tablero.piezaEn(posicion).orElse(null) != pieza) {
            throw new IllegalStateException("No está la pieza esperada en " + posicion);
        }
        tablero.quitar(posicion);
    }

    @Override
    public void revertir(Tablero tablero) {
        if (tablero.piezaEn(posicion).isPresent()) {
            throw new IllegalStateException("La casilla " + posicion + " no está vacía");
        }
        tablero.colocar(posicion, pieza);
    }
}
