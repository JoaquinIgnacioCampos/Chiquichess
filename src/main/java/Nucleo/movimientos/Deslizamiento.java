package Nucleo.movimientos;

import Nucleo.tablero.ITablero;
import Nucleo.juego.Posicion;
import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Movimiento base: avanzar en línea recta en una dirección (dc, df) hasta un máximo de pasos,
 * frenando ante la primera pieza. Si es rival se puede capturar; si es propia, no.
 */
public class Deslizamiento implements ReglaMovimiento {

    public static final int SIN_LIMITE = Integer.MAX_VALUE;

    private final int deltaColumna;
    private final int deltaFila;
    private final int maxPasos;

    public Deslizamiento(int deltaColumna, int deltaFila, int maxPasos) {
        if (deltaColumna == 0 && deltaFila == 0) {
            throw new IllegalArgumentException("La dirección no puede ser (0, 0)");
        }
        if (maxPasos < 1) {
            throw new IllegalArgumentException("maxPasos debe ser al menos 1");
        }
        this.deltaColumna = deltaColumna;
        this.deltaFila = deltaFila;
        this.maxPasos = maxPasos;
    }

    @Override
    public Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen, Color color) {
        Set<Posicion> destinos = new HashSet<>();
        Posicion actual = origen.desplazada(deltaColumna, deltaFila);
        int pasos = 1;
        while (pasos <= maxPasos && tablero.contiene(actual)) {
            Optional<Pieza> ocupante = tablero.piezaEn(actual);
            if (ocupante.isEmpty()) {
                destinos.add(actual);
            } else {
                if (ocupante.get().color() != color) {
                    destinos.add(actual); // captura
                }
                break; // cualquier pieza bloquea el camino
            }
            actual = actual.desplazada(deltaColumna, deltaFila);
            pasos++;
        }
        return destinos;
    }

    @Override
    public Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen, Color color) {
        return destinosPosibles(tablero, origen, color);
    }
}