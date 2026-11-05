package Nucleo.reglas;

import Nucleo.jugadas.Jugada;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.List;

/**
 * Strategy: una regla que suma jugadas además de las que permite el movimiento propio de cada pieza
 * (por ejemplo, el enroque o la captura al paso). Cada jugada especial es una {@link Jugada} con sus
 * propios efectos, así ejecutarla y deshacerla funciona igual que cualquier otra.
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
}
