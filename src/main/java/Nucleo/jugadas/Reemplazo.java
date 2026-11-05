package Nucleo.jugadas;

import Nucleo.piezas.Pieza;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.Objects;

/** Cambia la pieza de una casilla por otra (la promoción del peón). Al revertir, vuelve la anterior. */
public record Reemplazo(Pieza anterior, Pieza nueva, Posicion posicion) implements Efecto {

    public Reemplazo {
        Objects.requireNonNull(anterior, "anterior");
        Objects.requireNonNull(nueva, "nueva");
        Objects.requireNonNull(posicion, "posicion");
    }

    @Override
    public void aplicar(Tablero tablero) {
        cambiar(tablero, anterior, nueva);
    }

    @Override
    public void revertir(Tablero tablero) {
        cambiar(tablero, nueva, anterior);
    }

    private void cambiar(Tablero tablero, Pieza esperada, Pieza reemplazo) {
        if (tablero.piezaEn(posicion).orElse(null) != esperada) {
            throw new IllegalStateException("No está la pieza esperada en " + posicion);
        }
        tablero.colocar(posicion, reemplazo);
    }
}
