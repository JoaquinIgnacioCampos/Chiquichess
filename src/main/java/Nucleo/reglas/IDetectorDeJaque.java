package Nucleo.reglas;

import Nucleo.piezas.Color;
import Nucleo.tablero.ITablero;

/**
 * Contrato para saber si el rey de un color está siendo atacado.
 * Partida y MovimientosLegales dependen de esta interfaz y no de la implementación concreta,
 * así en los tests se puede reemplazar por un doble (stub/mock) sin armar posiciones reales.
 */
public interface IDetectorDeJaque {

    boolean estaEnJaque(ITablero tablero, Color color);
}
