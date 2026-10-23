package Nucleo.piezas;

import Nucleo.movimientos.Deslizamiento;
import Nucleo.reglas.ReglasBase;

public class Alfil extends Pieza {

    public Alfil(Color color) {
        super(color, ReglasBase.diagonales(Deslizamiento.SIN_LIMITE));
    }

    @Override
    public String nombre() {
        return "Alfil";
    }

    @Override
    public char codigo() {
        return 'A';
    }
}
