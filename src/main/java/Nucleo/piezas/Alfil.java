package Nucleo.piezas;

import Nucleo.movimientos.Deslizamiento;
import Nucleo.bando.Bando;
import Nucleo.movimientos.ReglasBase;
public class Alfil extends Pieza {

    public Alfil(Bando color) {
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
