package dobles;

import Nucleo.juego.Posicion;
import Nucleo.movimientos.ReglaMovimiento;
import Nucleo.piezas.Color;
import Nucleo.tablero.ITablero;

import java.util.Set;

/** Mock: cuenta cuántas veces se le pidieron destinos y casillas atacadas. Devuelve un conjunto fijo. */
public class MockReglaMovimiento implements ReglaMovimiento {

    private final Set<Posicion> respuesta;
    private int llamadasDestinos = 0;
    private int llamadasAtacadas = 0;

    public MockReglaMovimiento(Set<Posicion> respuesta) {
        this.respuesta = Set.copyOf(respuesta);
    }

    @Override
    public Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen, Color color) {
        llamadasDestinos++;
        return respuesta;
    }

    @Override
    public Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen, Color color) {
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
