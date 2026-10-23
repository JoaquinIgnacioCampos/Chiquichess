package Nucleo.piezas;

import Nucleo.movimientos.Deslizamiento;
import Nucleo.reglas.ReglasBase;

public class Torre extends Pieza {

    public Torre(Color color) {
        super(color, ReglasBase.ortogonales(Deslizamiento.SIN_LIMITE));
    }

    @Override
    public String nombre() {
        return "Torre";
    }

    @Override
    public char codigo() {
        return 'T';
    }
}
