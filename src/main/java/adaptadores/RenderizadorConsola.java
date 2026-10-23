package adaptadores;

import Nucleo.juego.Posicion;
import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;


import java.util.Optional;

/** Adaptador de salida: dibuja el tablero como texto. Blancas en mayúscula, negras en minúscula. */
public class RenderizadorConsola {


    public String dibujar(ITablero tablero) {
        StringBuilder sb = new StringBuilder();
        for (int fila = tablero.alto() - 1; fila >= 0; fila--) {
            sb.append(String.format("%2d ", fila + 1));
            for (int columna = 0; columna < tablero.ancho(); columna++) {
                Optional<Pieza> pieza = tablero.piezaEn(new Posicion(columna, fila));
                sb.append(pieza.isPresent() ? simbolo(pieza.get()) : '.').append(' ');
            }
            sb.append('\n');
        }
        sb.append("   ");
        for (int columna = 0; columna < tablero.ancho(); columna++) {
            sb.append((char) ('a' + columna)).append(' ');
        }
        return sb.toString();
    }

    private char simbolo(Pieza pieza) {
        return pieza.color() == Color.BLANCO
                ? Character.toUpperCase(pieza.codigo())
                : Character.toLowerCase(pieza.codigo());
    }
}
