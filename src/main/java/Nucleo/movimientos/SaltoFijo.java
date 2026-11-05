package Nucleo.movimientos;

import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import Nucleo.tablero.Posicion;
import Nucleo.bando.Bando;
/** Movimiento base: saltar a desplazamientos fijos, sin importar lo que haya en el medio (caballo). */
public class SaltoFijo implements ReglaMovimiento {

    private final int[][] desplazamientos;

    public SaltoFijo(int[][] desplazamientos) {
        this.desplazamientos = new int[desplazamientos.length][];
        for (int i = 0; i < desplazamientos.length; i++) {
            this.desplazamientos[i] = desplazamientos[i].clone();
        }
    }

    @Override
    public Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen, Bando color) {
        Set<Posicion> destinos = new HashSet<>();
        for (int[] d : desplazamientos) {
            Posicion destino = origen.desplazada(d[0], d[1]);
            if (!tablero.contiene(destino)) {
                continue;
            }
            Optional<Pieza> ocupante = tablero.piezaEn(destino);
            if (ocupante.isEmpty() || ocupante.get().color() != color) {
                destinos.add(destino);
            }
        }
        return destinos;
    }

    @Override
    public Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen, Bando color) {
        return destinosPosibles(tablero, origen, color);
    }
}
