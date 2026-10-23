package Nucleo.movimientos.peon;

import Nucleo.movimientos.ReglaMovimiento;
import Nucleo.piezas.Color;
import Nucleo.tablero.ITablero;
import Nucleo.juego.Posicion;

import java.util.Set;

/** Decorator: la regla interna solo aplica si se cumple una condición (ej. el peón está en su fila inicial). */
public class SoloSi implements ReglaMovimiento {

    @FunctionalInterface
    public interface Condicion {
        boolean seCumple(ITablero tablero, Posicion origen, Color color);
    }

    private final Condicion condicion;
    private final ReglaMovimiento regla;

    public SoloSi(Condicion condicion, ReglaMovimiento regla) {
        this.condicion = condicion;
        this.regla = regla;
    }

    @Override
    public Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen, Color color) {
        return condicion.seCumple(tablero, origen, color)
                ? regla.destinosPosibles(tablero, origen, color)
                : Set.of();
    }

    @Override
    public Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen, Color color) {
        return condicion.seCumple(tablero, origen, color)
                ? regla.casillasAtacadas(tablero, origen, color)
                : Set.of();
    }
}
