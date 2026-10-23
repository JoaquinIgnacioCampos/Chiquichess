package Nucleo.reglas;

import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;
import Nucleo.juego.Posicion;

import java.util.Map;
import java.util.Optional;

/** Responsabilidad única: decir si el rey de un color está siendo atacado. */
public class DetectorDeJaque implements IDetectorDeJaque {

    @Override
    public boolean estaEnJaque(ITablero tablero, Color color) {
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

    private Optional<Posicion> buscarRey(ITablero tablero, Color color) {
        return tablero.piezas().entrySet().stream()
                .filter(e -> e.getValue().esRey() && e.getValue().color() == color)
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
