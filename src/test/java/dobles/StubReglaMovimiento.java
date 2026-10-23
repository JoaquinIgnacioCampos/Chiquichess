package dobles;

import Nucleo.juego.Posicion;
import Nucleo.movimientos.ReglaMovimiento;
import Nucleo.piezas.Color;
import Nucleo.tablero.ITablero;

import java.util.Set;

/** Stub: devuelve siempre los mismos destinos y casillas atacadas, sin mirar el tablero. */
public class StubReglaMovimiento implements ReglaMovimiento {

    private final Set<Posicion> destinos;
    private final Set<Posicion> atacadas;

    public StubReglaMovimiento(Set<Posicion> destinos, Set<Posicion> atacadas) {
        this.destinos = Set.copyOf(destinos);
        this.atacadas = Set.copyOf(atacadas);
    }

    /** Variante en la que ataca las mismas casillas a las que puede ir. */
    public StubReglaMovimiento(Set<Posicion> destinos) {
        this(destinos, destinos);
    }

    @Override
    public Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen, Color color) {
        return destinos;
    }

    @Override
    public Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen, Color color) {
        return atacadas;
    }
}
