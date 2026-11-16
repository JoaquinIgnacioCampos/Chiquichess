package Nucleo.piezas;

import Nucleo.bando.Bando;
import Nucleo.movimientos.ReglasBase;

public class Caballo extends Pieza {

    public Caballo(Bando color) {
        super(color, ReglasBase.saltosDeCaballo());
    }

    @Override
    public String nombre() {
        return "Caballo";
    }

    @Override
    public char codigo() {
        return 'C';
    }
}
