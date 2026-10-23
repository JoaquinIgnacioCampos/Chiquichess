package Nucleo.juego;

import Nucleo.piezas.*;
import Nucleo.tablero.DisposicionInicial;
import Nucleo.tablero.Tablero;

/** Posición inicial clásica del ajedrez en un tablero de 8x8. */
public class DisposicionEstandar implements DisposicionInicial {

    private static final int TAMANIO = 8;

    @Override
    public Tablero crearTablero() {
        Tablero tablero = new Tablero(TAMANIO, TAMANIO);
        for (Color color : Color.values()) {
            int filaBase = color == Color.BLANCO ? 0 : TAMANIO - 1;
            int filaPeones = color == Color.BLANCO ? 1 : TAMANIO - 2;

            Pieza[] filaDePiezas = {
                    new Torre(color), new Caballo(color), new Alfil(color), new Reina(color),
                    new Rey(color), new Alfil(color), new Caballo(color), new Torre(color)
            };
            for (int columna = 0; columna < TAMANIO; columna++) {
                tablero.colocar(new Posicion(columna, filaBase), filaDePiezas[columna]);
                tablero.colocar(new Posicion(columna, filaPeones), new Peon(color));
            }
        }
        return tablero;
    }
}
