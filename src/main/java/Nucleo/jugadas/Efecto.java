package Nucleo.jugadas;

import Nucleo.tablero.Tablero;

/**
 * Un cambio elemental sobre el tablero que se puede aplicar y revertir exactamente.
 * Una jugada es una lista de efectos: así una captura común, un enroque (dos traslados) o una
 * captura al paso (un retiro en otra casilla) se representan con las mismas dos piezas.
 *
 * Cada efecto verifica que el tablero esté en el estado esperado antes de tocarlo y, si no lo está,
 * lanza IllegalStateException sin modificar nada.
 */
public sealed interface Efecto permits Traslado, Retiro {

    void aplicar(Tablero tablero);

    void revertir(Tablero tablero);
}
