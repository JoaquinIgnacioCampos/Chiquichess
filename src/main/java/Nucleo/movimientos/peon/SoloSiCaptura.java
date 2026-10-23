package Nucleo.movimientos.peon;

import Nucleo.movimientos.ReglaMovimiento;
import Nucleo.piezas.Color;
import Nucleo.tablero.ITablero;
import Nucleo.juego.Posicion;

import java.util.HashSet;
import java.util.Set;

/**
 * Decorator: restringe los destinos a casillas con una pieza rival (solo se mueve si captura).
 * Las casillas atacadas son todas las que la regla interna alcanzaría, haya o no una pieza:
 * así el peón amenaza sus diagonales aunque estén vacías.
 */
public class SoloSiCaptura implements ReglaMovimiento {

    private final ReglaMovimiento regla;

    public SoloSiCaptura(ReglaMovimiento regla) {
        this.regla = regla;
    }

    @Override
    public Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen, Color color) {
        Set<Posicion> destinos = new HashSet<>(regla.destinosPosibles(tablero, origen, color));
        destinos.removeIf(destino -> tablero.piezaEn(destino).isEmpty());
        return destinos;
    }

    @Override
    public Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen, Color color) {
        return regla.casillasAtacadas(tablero, origen, color);
    }
}
