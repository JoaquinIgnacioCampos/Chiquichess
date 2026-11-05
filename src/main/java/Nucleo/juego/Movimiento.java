package Nucleo.juego;

import Nucleo.tablero.Posicion;

import java.util.Objects;
import java.util.Optional;

/**
 * Intención de mover la pieza que está en origen hacia destino.
 *
 * @param promocion código de la pieza elegida si el peón promueve (D, T, A o C en el ajedrez clásico);
 *                  vacío si no se eligió, en cuyo caso un peón que promueve se convierte en dama
 */
public record Movimiento(Posicion origen, Posicion destino, Optional<Character> promocion) {

    public Movimiento {
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
        Objects.requireNonNull(promocion, "promocion");
    }

    public Movimiento(Posicion origen, Posicion destino) {
        this(origen, destino, Optional.empty());
    }

    /** Movimiento de un peón que promueve a la pieza con ese código. */
    public static Movimiento conPromocion(Posicion origen, Posicion destino, char codigo) {
        return new Movimiento(origen, destino, Optional.of(codigo));
    }
}
