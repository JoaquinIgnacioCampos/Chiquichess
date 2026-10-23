package Nucleo.reglas;

import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;

import java.util.Optional;

/**
 * Foto de la partida que reciben las condiciones de finalización después de cada jugada
 * (y una vez al crear la partida, en cuyo caso piezaMovida está vacía).
 *
 * @param tablero              tablero ya actualizado (solo lectura)
 * @param turno                color que debe jugar a continuación
 * @param piezaMovida          pieza que acaba de moverse; vacío en la evaluación inicial
 * @param huboCaptura          true si la última jugada capturó una pieza
 * @param enJaque              true si el rey de quien tiene el turno está en jaque
 * @param tieneMovimientosLegales true si quien tiene el turno puede hacer al menos una jugada legal
 */
public record ContextoDeJugada(
        ITablero tablero,
        Color turno,
        Optional<Pieza> piezaMovida,
        boolean huboCaptura,
        boolean enJaque,
        boolean tieneMovimientosLegales) {
}
