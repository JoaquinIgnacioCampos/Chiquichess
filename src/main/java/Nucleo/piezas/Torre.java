package Nucleo.piezas;

import Nucleo.bando.Bando;
import Nucleo.movimientos.Deslizamiento;
import Nucleo.movimientos.ReglasBase;

public class Torre extends Pieza {

    public Torre(Bando color) {
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
