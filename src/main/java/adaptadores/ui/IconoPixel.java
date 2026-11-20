package adaptadores.ui;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Íconos de pixel art dibujados a partir de una grilla de texto: cada carácter es un píxel ({@code X} con
 * la tinta, {@code o} con el fondo, {@code a} con el acento, {@code .} vacío). Se pintan con cuadrados
 * nítidos, sin suavizado, como el resto del arte del juego.
 */
final class IconoPixel {
    /** Lado de cada píxel del ícono, en píxeles de pantalla. */
    static final double PIXEL = 2;

    static final String[] SILBATO = {
            "........a.a..",
            ".......a..a.a",
            ".............",
            "..XXXXXXXXXXX",
            ".XXXXXXXXXXXX",
            "XXXooXXXX....",
            "XXooooXXX....",
            "XXooooXXX....",
            "XXXooXXXX....",
            ".XXXXXXX.....",
            "..XXXXX......"};

    static final String[] DESHACER = {
            "...X........",
            "..XX........",
            ".XXXXXXXX...",
            "XXXXXXXXXX..",
            ".XXXXXXXXXX.",
            "..XX.....XX.",
            "...X.....XX.",
            ".........XX.",
            "......XXXX..",
            "......XXX..."};

    static final String[] CASA = {
            ".....XX.....",
            "....XXXX....",
            "...XXXXXX...",
            "..XXXXXXXX..",
            ".XXXXXXXXXX.",
            "XXXXXXXXXXXX",
            ".XXXXooXXXX.",
            ".XXXXooXXXX.",
            ".XXXXooXXXX.",
            ".XXXXooXXXX."};

    static final String[] BANDERA = {
            "XX.........",
            "XXXXXXXXX..",
            "XXXXXXXXXXX",
            "XXXXXXXXXXX",
            "XXXXXXXXX..",
            "XX.........",
            "XX.........",
            "XX.........",
            "XX.........",
            "XX........."};

    private IconoPixel() {
    }

    static Node crear(String[] filas, Color tinta, Color fondo, Color acento) {
        return crear(filas, tinta, fondo, acento, false);
    }

    /** Con {@code espejado} se da vuelta de izquierda a derecha (por ejemplo, rehacer a partir de deshacer). */
    static Node crear(String[] filas, Color tinta, Color fondo, Color acento, boolean espejado) {
        int columnas = filas[0].length();
        Canvas lienzo = new Canvas(columnas * PIXEL, filas.length * PIXEL);
        GraphicsContext dibujo = lienzo.getGraphicsContext2D();
        dibujo.setImageSmoothing(false);
        for (int fila = 0; fila < filas.length; fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                char pixel = filas[fila].charAt(espejado ? columnas - 1 - columna : columna);
                Color color = switch (pixel) {
                    case 'X' -> tinta;
                    case 'o' -> fondo;
                    case 'a' -> acento;
                    default -> null;
                };
                if (color == null) continue;
                dibujo.setFill(color);
                dibujo.fillRect(columna * PIXEL, fila * PIXEL, PIXEL, PIXEL);
            }
        }
        lienzo.setMouseTransparent(true);
        return lienzo;
    }

    /** El mismo ícono tachado: una diagonal del color de alerta, separada del dibujo por el fondo. */
    static Node tachado(String[] filas, Color tinta, Color fondo, Color alerta) {
        int columnas = filas[0].length();
        char[][] grilla = new char[filas.length][];
        for (int fila = 0; fila < filas.length; fila++) {
            grilla[fila] = filas[fila].replace('a', '.').toCharArray();
        }
        Canvas lienzo = (Canvas) crear(java.util.Arrays.stream(grilla).map(String::new).toArray(String[]::new),
                tinta, fondo, alerta);
        GraphicsContext dibujo = lienzo.getGraphicsContext2D();
        for (int fila = 0; fila < filas.length; fila++) {
            int columna = Math.round((float) (filas.length - 1 - fila) * (columnas - 1) / (filas.length - 1));
            dibujo.setFill(fondo);
            dibujo.fillRect((columna + 1) * PIXEL, fila * PIXEL, PIXEL, PIXEL);
            dibujo.setFill(alerta);
            dibujo.fillRect(columna * PIXEL, fila * PIXEL, PIXEL, PIXEL);
        }
        return lienzo;
    }
}
