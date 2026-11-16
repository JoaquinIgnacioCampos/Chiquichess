package adaptadores;

import Nucleo.bando.Bando;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;


import java.util.Optional;

/**
 * Adaptador de salida: dibuja el tablero como texto. Blancas en mayúscula, negras en minúscula.
 * En el modo fútbol la pelota suelta se dibuja con 'o', y la pieza que la lleva, con un '*' al lado;
 * las casillas vacías del arco, con '#'.
 */
public class RenderizadorConsola {


    public String dibujar(ITablero tablero) {
        StringBuilder sb = new StringBuilder();
        for (int fila = tablero.alto() - 1; fila >= 0; fila--) {
            sb.append(String.format("%2d ", fila + 1));
            for (int columna = 0; columna < tablero.ancho(); columna++) {
                Posicion casilla = new Posicion(columna, fila);
                Optional<Pieza> pieza = tablero.piezaEn(casilla);
                boolean pelota = tablero.pelota().filter(casilla::equals).isPresent();
                char vacia = pelota ? 'o' : tablero.arcoEn(casilla).isPresent() ? '#' : '.';
                sb.append(pieza.isPresent() ? simbolo(pieza.get()) : vacia)
                        .append(pelota && pieza.isPresent() ? '*' : ' ');
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
        return pieza.color() == Bando.BLANCO
                ? Character.toUpperCase(pieza.codigo())
                : Character.toLowerCase(pieza.codigo());
    }
}
