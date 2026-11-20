package adapters.ui;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Íconos de pixel art dibujados a partir de una grilla de texto: cada carácter es un píxel ({@code X} con
 * la tinta, {@code o} con el fondo, {@code a} con el acento, {@code .} vacío). Se pintan con cuadrados
 * nítidos, sin suavizado, como el resto del arte del juego.
 */
final class PixelIcon {
    /** Lado de cada píxel del ícono, en píxeles de pantalla. */
    static final double PIXEL = 2;

    static final String[] WHISTLE = {
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

    static final String[] UNDO = {
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

    static final String[] HOUSE = {
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

    static final String[] FLAG = {
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

    private PixelIcon() {
    }

    static Node create(String[] rows, Color ink, Color background, Color accent) {
        return create(rows, ink, background, accent, false);
    }

    /** Con {@code mirrored} se da vuelta de izquierda a derecha (por ejemplo, rehacer a partir de deshacer). */
    static Node create(String[] rows, Color ink, Color background, Color accent, boolean mirrored) {
        int columns = rows[0].length();
        Canvas lienzo = new Canvas(columns * PIXEL, rows.length * PIXEL);
        GraphicsContext drawing = lienzo.getGraphicsContext2D();
        drawing.setImageSmoothing(false);
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < columns; column++) {
                char pixel = rows[row].charAt(mirrored ? columns - 1 - column : column);
                Color color = switch (pixel) {
                    case 'X' -> ink;
                    case 'o' -> background;
                    case 'a' -> accent;
                    default -> null;
                };
                if (color == null) continue;
                drawing.setFill(color);
                drawing.fillRect(column * PIXEL, row * PIXEL, PIXEL, PIXEL);
            }
        }
        lienzo.setMouseTransparent(true);
        return lienzo;
    }

    /** El mismo ícono tachado: una diagonal del color de alerta, separada del dibujo por el fondo. */
    static Node crossed(String[] rows, Color ink, Color background, Color alerta) {
        int columns = rows[0].length();
        char[][] grid = new char[rows.length][];
        for (int row = 0; row < rows.length; row++) {
            grid[row] = rows[row].replace('a', '.').toCharArray();
        }
        Canvas lienzo = (Canvas) create(java.util.Arrays.stream(grid).map(String::new).toArray(String[]::new),
                ink, background, alerta);
        GraphicsContext drawing = lienzo.getGraphicsContext2D();
        for (int row = 0; row < rows.length; row++) {
            int column = Math.round((float) (rows.length - 1 - row) * (columns - 1) / (rows.length - 1));
            drawing.setFill(background);
            drawing.fillRect((column + 1) * PIXEL, row * PIXEL, PIXEL, PIXEL);
            drawing.setFill(alerta);
            drawing.fillRect(column * PIXEL, row * PIXEL, PIXEL, PIXEL);
        }
        return lienzo;
    }
}
