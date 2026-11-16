package Nucleo.reglas;

import Nucleo.jugadas.Jugada;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.List;

/**
 * Strategy: una regla que cambia las jugadas que permite el movimiento propio de cada pieza.
 * Puede sumar jugadas nuevas (el enroque, la captura al paso) o transformar las existentes (la
 * promoción convierte el avance del peón a la última fila en una jugada por cada pieza posible).
 * Cada jugada es una {@link Jugada} con sus propios efectos, así ejecutarla y deshacerla funciona
 * igual que cualquier otra.
 *
 * Las jugadas devueltas cumplen las condiciones de la regla, pero todavía no se controló si dejan al
 * propio rey en jaque: eso lo hace MovimientosLegales para todas las jugadas por igual.
 */
public interface ReglaEspecial {

    /**
     * Jugadas especiales de la pieza que está en origen; vacío si no tiene ninguna.
     *
     * @param historial jugadas ejecutadas en la partida, en orden (para saber, por ejemplo, si una pieza ya se movió)
     * @param detector  el mismo detector de jaque que usa la partida
     */
    List<Jugada> jugadas(Tablero tablero, Posicion origen, List<Jugada> historial, IDetectorDeJaque detector);

    /**
     * Reemplaza una jugada ya generada por las variantes que exige la regla. Por defecto la deja igual.
     * MovimientosLegales lo aplica a todas las jugadas, comunes y especiales.
     */
    default List<Jugada> transformar(Tablero tablero, Jugada jugada) {
        return List.of(jugada);
    }
}
