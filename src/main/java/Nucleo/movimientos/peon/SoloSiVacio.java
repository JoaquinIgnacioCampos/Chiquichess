package Nucleo.movimientos.peon;

import Nucleo.movimientos.ReglaMovimiento;
import Nucleo.tablero.ITablero;
import java.util.HashSet;
import java.util.Set;

import Nucleo.tablero.Posicion;
import Nucleo.bando.Bando;
/**
 * Decorator: restringe los destinos a casillas libres (no permite capturar).
 * Un avance no es un ataque, por eso no aporta casillas atacadas.
 */
public class SoloSiVacio implements ReglaMovimiento {

    private final ReglaMovimiento regla;

    public SoloSiVacio(ReglaMovimiento regla) {
        this.regla = regla;
    }

    @Override
    public Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen, Bando color) {
        Set<Posicion> destinos = new HashSet<>(regla.destinosPosibles(tablero, origen, color));
        destinos.removeIf(destino -> tablero.piezaEn(destino).isPresent());
        return destinos;
    }

    @Override
    public Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen, Bando color) {
        return Set.of();
    }
}
