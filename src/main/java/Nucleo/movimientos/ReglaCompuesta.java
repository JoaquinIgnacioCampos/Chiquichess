package Nucleo.movimientos;

import Nucleo.tablero.ITablero;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Nucleo.tablero.Posicion;
import Nucleo.bando.Bando;
/** Composite: une los destinos y ataques de varias reglas (ej. reina = torre + alfil). */
public class ReglaCompuesta implements ReglaMovimiento {

    private final List<ReglaMovimiento> reglas;

    public ReglaCompuesta(ReglaMovimiento... reglas) {
        this.reglas = List.copyOf(Arrays.asList(reglas));
    }

    @Override
    public Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen, Bando color) {
        Set<Posicion> resultado = new HashSet<>();
        for (ReglaMovimiento regla : reglas) {
            resultado.addAll(regla.destinosPosibles(tablero, origen, color));
        }
        return resultado;
    }

    @Override
    public Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen, Bando color) {
        Set<Posicion> resultado = new HashSet<>();
        for (ReglaMovimiento regla : reglas) {
            resultado.addAll(regla.casillasAtacadas(tablero, origen, color));
        }
        return resultado;
    }
}
