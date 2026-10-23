package Nucleo.reglas;

import Nucleo.movimientos.Deslizamiento;
import Nucleo.movimientos.ReglaCompuesta;
import Nucleo.movimientos.ReglaMovimiento;
import Nucleo.movimientos.SaltoFijo;

public class ReglasBase {

    private static final int[][] ORTOGONALES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONALES = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] SALTOS_CABALLO = {
            {1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}};

    private ReglasBase() {
    }

    public static ReglaMovimiento ortogonales(int maxPasos) {
        return deslizamientos(ORTOGONALES, maxPasos);
    }

    public static ReglaMovimiento diagonales(int maxPasos) {
        return deslizamientos(DIAGONALES, maxPasos);
    }

    public static ReglaMovimiento saltosDeCaballo() {
        return new SaltoFijo(SALTOS_CABALLO);
    }

    private static ReglaMovimiento deslizamientos(int[][] direcciones, int maxPasos) {
        ReglaMovimiento[] reglas = new ReglaMovimiento[direcciones.length];
        for (int i = 0; i < direcciones.length; i++) {
            reglas[i] = new Deslizamiento(direcciones[i][0], direcciones[i][1], maxPasos);
        }
        return new ReglaCompuesta(reglas);
    }
}