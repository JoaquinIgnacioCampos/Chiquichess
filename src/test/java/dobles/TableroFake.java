package dobles;

import Nucleo.juego.Posicion;
import Nucleo.piezas.*;
import Nucleo.tablero.Tablero;

/**
 * Fake: arma un tablero en memoria a partir de un dibujo de texto, con las mismas letras
 * que muestra la consola (mayúscula = blancas, minúscula = negras, '.' = casilla vacía).
 * La primera línea es la fila de arriba (la 8 en un tablero estándar), igual que en pantalla.
 *
 * <pre>
 * TableroFake.desde(
 *     "....r...",   // fila 8
 *     "........",
 *     ...
 *     "....R...");  // fila 1
 * </pre>
 *
 * Letras: P peón, T torre, C caballo, A alfil, D dama (reina), R rey.
 * El tamaño sale del dibujo, así que también sirve para tableros que no son de 8x8.
 */
public final class TableroFake {

    private TableroFake() {
    }

    public static Tablero desde(String... filas) {
        int alto = filas.length;
        int ancho = filas[0].length();
        Tablero tablero = new Tablero(ancho, alto);
        for (int i = 0; i < alto; i++) {
            if (filas[i].length() != ancho) {
                throw new IllegalArgumentException("Todas las filas deben tener el mismo largo");
            }
            int fila = alto - 1 - i;
            for (int columna = 0; columna < ancho; columna++) {
                char letra = filas[i].charAt(columna);
                if (letra != '.') {
                    tablero.colocar(new Posicion(columna, fila), crear(letra));
                }
            }
        }
        return tablero;
    }

    /** Tablero de 8x8 sin piezas. */
    public static Tablero vacio() {
        return new Tablero(8, 8);
    }

    /** Tablero de 8x8 con una sola pieza, útil para contar destinos sin interferencias. */
    public static Tablero conUnaPieza(String casilla, Pieza pieza) {
        Tablero tablero = vacio();
        tablero.colocar(Casillas.pos(casilla), pieza);
        return tablero;
    }

    private static Pieza crear(char letra) {
        Color color = Character.isUpperCase(letra) ? Color.BLANCO : Color.NEGRO;
        return switch (Character.toUpperCase(letra)) {
            case 'P' -> new Peon(color);
            case 'T' -> new Torre(color);
            case 'C' -> new Caballo(color);
            case 'A' -> new Alfil(color);
            case 'D' -> new Reina(color);
            case 'R' -> new Rey(color);
            default -> throw new IllegalArgumentException("Letra de pieza desconocida: " + letra);
        };
    }
}
