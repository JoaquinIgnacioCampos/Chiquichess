package dobles;

import Nucleo.movimientos.ReglaMovimiento;
import Nucleo.tablero.ITablero;

import java.util.Set;

import Nucleo.tablero.Posicion;
import Nucleo.bando.Bando;
/** Mock: cuenta cuántas veces se le pidieron destinos y casillas atacadas. Devuelve un conjunto fijo. */
public class MockReglaMovimiento implements ReglaMovimiento {

    private final Set<Posicion> respuesta;
    private int llamadasDestinos = 0;
    private int llamadasAtacadas = 0;

    public MockReglaMovimiento(Set<Posicion> respuesta) {
        this.respuesta = Set.copyOf(respuesta);
    }

    @Override
    public Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen, Bando color) {
        llamadasDestinos++;
        return respuesta;
    }

    @Override
    public Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen, Bando color) {
        llamadasAtacadas++;
        return respuesta;
    }

    public int llamadasDestinos() {
        return llamadasDestinos;
    }

    public int llamadasAtacadas() {
        return llamadasAtacadas;
    }
}
