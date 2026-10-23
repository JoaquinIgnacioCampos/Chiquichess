package Nucleo.piezas;

import Nucleo.movimientos.Deslizamiento;
import Nucleo.movimientos.ReglaCompuesta;
import Nucleo.movimientos.ReglaMovimiento;
import Nucleo.movimientos.peon.SoloSi;
import Nucleo.movimientos.peon.SoloSiCaptura;
import Nucleo.movimientos.peon.SoloSiVacio;
import Nucleo.tablero.ITablero;

public class Peon extends Pieza {

    public Peon(Color color) {
        super(color, construirRegla(color));
    }

    private static ReglaMovimiento construirRegla(Color color) {
        int adelante = color.direccionAvance();

        ReglaMovimiento avanceSimple = new SoloSiVacio(new Deslizamiento(0, adelante, 1));
        ReglaMovimiento avanceDoble = new SoloSi(
                (tablero, origen, c) -> origen.fila() == filaInicial(tablero, c),
                new SoloSiVacio(new Deslizamiento(0, adelante, 2)));
        ReglaMovimiento capturaIzquierda = new SoloSiCaptura(new Deslizamiento(-1, adelante, 1));
        ReglaMovimiento capturaDerecha = new SoloSiCaptura(new Deslizamiento(1, adelante, 1));

        return new ReglaCompuesta(avanceSimple, avanceDoble, capturaIzquierda, capturaDerecha);
    }

    private static int filaInicial(ITablero tablero, Color color) {
        return color == Color.BLANCO ? 1 : tablero.alto() - 2;
    }

    @Override
    public boolean esPeon() {
        return true;
    }

    @Override
    public String nombre() {
        return "Peon";
    }

    @Override
    public char codigo() {
        return 'P';
    }
}