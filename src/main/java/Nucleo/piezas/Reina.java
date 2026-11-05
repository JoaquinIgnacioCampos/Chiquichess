package Nucleo.piezas;

import Nucleo.bando.Bando;
import Nucleo.movimientos.Deslizamiento;
import Nucleo.movimientos.ReglaCompuesta;
import Nucleo.movimientos.ReglasBase;

/** Reina = torre + alfil, ambos sin límite de pasos. */
public class Reina extends Pieza {

    public Reina(Bando color) {
        super(color, new ReglaCompuesta(
                ReglasBase.ortogonales(Deslizamiento.SIN_LIMITE),
                ReglasBase.diagonales(Deslizamiento.SIN_LIMITE)));
    }

    @Override
    public String nombre() {
        return "Reina";
    }

    @Override
    public char codigo() {
        return 'D';
    }
}
