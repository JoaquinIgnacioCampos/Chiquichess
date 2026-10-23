package Nucleo.piezas;

import Nucleo.movimientos.Deslizamiento;
import Nucleo.movimientos.ReglaCompuesta;
import Nucleo.reglas.ReglasBase;

/** Reina = torre + alfil, ambos sin límite de pasos. */
public class Reina extends Pieza {

    public Reina(Color color) {
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
