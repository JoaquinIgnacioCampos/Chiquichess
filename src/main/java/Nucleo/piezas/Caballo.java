package Nucleo.piezas;

import Nucleo.reglas.ReglasBase;

public class Caballo extends Pieza {

    public Caballo(Color color) {
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
