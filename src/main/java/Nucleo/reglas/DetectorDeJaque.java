package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;
import java.util.Map;
import java.util.Optional;

/** Responsabilidad única: decir si el rey de un color está siendo atacado. */
public class DetectorDeJaque implements IDetectorDeJaque {

    @Override
    public boolean estaEnJaque(ITablero tablero, Bando color) {
        Optional<Posicion> posicionRey = buscarRey(tablero, color);
        if (posicionRey.isEmpty()) {
            return false;
        }
        for (Map.Entry<Posicion, Pieza> entrada : tablero.piezas().entrySet()) {
            Pieza pieza = entrada.getValue();
            if (pieza.color() != color
                    && pieza.casillasAtacadas(tablero, entrada.getKey()).contains(posicionRey.get())) {
                return true;
            }
        }
        return false;
    }

    private Optional<Posicion> buscarRey(ITablero tablero, Bando color) {
        // Ciclo y no stream: se llama por cada jugada que se prueba (la IA prueba muchísimas).
        for (Map.Entry<Posicion, Pieza> entrada : tablero.piezas().entrySet()) {
            if (entrada.getValue().esRey() && entrada.getValue().color() == color) {
                return Optional.of(entrada.getKey());
            }
        }
        return Optional.empty();
    }
}
