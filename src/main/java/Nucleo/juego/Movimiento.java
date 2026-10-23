package Nucleo.juego;

import java.util.Objects;

/** Intención de mover la pieza que está en origen hacia destino. */
public record Movimiento(Posicion origen, Posicion destino) {

    public Movimiento {
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
    }
}
