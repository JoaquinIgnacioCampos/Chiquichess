package Nucleo.piezas;

import Nucleo.movimientos.ReglaCompuesta;
import Nucleo.reglas.ReglasBase;

/**
 * Rey = torre + alfil limitados a 1 paso. Que no pueda moverse a una casilla atacada
 * no se resuelve acá sino en la validación de la Partida (no se permite dejar al propio rey en jaque).
 */
public class Rey extends Pieza {

    public Rey(Color color) {
        super(color, new ReglaCompuesta(
                ReglasBase.ortogonales(1),
                ReglasBase.diagonales(1)));
    }

    @Override
    public boolean esRey() {
        return true;
    }

    @Override
    public String nombre() {
        return "Rey";
    }

    @Override
    public char codigo() {
        return 'R';
    }
}
