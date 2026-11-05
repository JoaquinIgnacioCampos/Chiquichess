package Nucleo.jugadas;

import Nucleo.tablero.Tablero;

/**
 * Un cambio elemental sobre el tablero que se puede aplicar y revertir exactamente.
 * Una jugada es una lista de efectos: así una captura común, un enroque (dos traslados), una
 * captura al paso (un retiro en otra casilla) o una promoción (un traslado y un reemplazo) se
 * representan con las mismas tres piezas.
 *
 * Cada efecto verifica que el tablero esté en el estado esperado antes de tocarlo y, si no lo está,
 * lanza IllegalStateException sin modificar nada.
 */
public sealed interface Efecto permits Traslado, Retiro, Reemplazo {

    void aplicar(Tablero tablero);

    void revertir(Tablero tablero);
}
