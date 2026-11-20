package adapters.ui;

import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Letras de puntos 5x7, escaladas al espacio disponible sin una tipografía externa. */
public final class LedText extends Region {
    private static final String EMPTY = "00000/00000/00000/00000/00000/00000/00000";
    private static final Map<Character, String> GLYPHS = Map.ofEntries(
            Map.entry('0', "01110/10001/10011/10101/11001/10001/01110"),
            Map.entry('1', "00100/01100/00100/00100/00100/00100/01110"),
            Map.entry('2', "01110/10001/00001/00010/00100/01000/11111"),
            Map.entry('3', "11110/00001/00001/01110/00001/00001/11110"),
            Map.entry('4', "00010/00110/01010/10010/11111/00010/00010"),
            Map.entry('5', "11111/10000/10000/11110/00001/00001/11110"),
            Map.entry('6', "01110/10000/10000/11110/10001/10001/01110"),
            Map.entry('7', "11111/00001/00010/00100/01000/01000/01000"),
            Map.entry('8', "01110/10001/10001/01110/10001/10001/01110"),
            Map.entry('9', "01110/10001/10001/01111/00001/00001/01110"),
            Map.entry(':', "00000/00100/00100/00000/00100/00100/00000"),
            Map.entry('!', "00100/00100/00100/00100/00100/00000/00100"),
            Map.entry('¡', "00100/00000/00100/00100/00100/00100/00100"),
            Map.entry('A', "01110/10001/10001/11111/10001/10001/10001"),
            Map.entry('B', "11110/10001/10001/11110/10001/10001/11110"),
            Map.entry('C', "01111/10000/10000/10000/10000/10000/01111"),
            Map.entry('D', "11110/10001/10001/10001/10001/10001/11110"),
            Map.entry('E', "11111/10000/10000/11110/10000/10000/11111"),
            Map.entry('F', "11111/10000/10000/11110/10000/10000/10000"),
            Map.entry('G', "01111/10000/10000/10111/10001/10001/01110"),
            Map.entry('H', "10001/10001/10001/11111/10001/10001/10001"),
            Map.entry('I', "11111/00100/00100/00100/00100/00100/11111"),
            Map.entry('J', "00111/00010/00010/00010/10010/10010/01100"),
            Map.entry('K', "10001/10010/10100/11000/10100/10010/10001"),
            Map.entry('L', "10000/10000/10000/10000/10000/10000/11111"),
            Map.entry('M', "10001/11011/10101/10101/10001/10001/10001"),
            Map.entry('N', "10001/11001/10101/10011/10001/10001/10001"),
            Map.entry('O', "01110/10001/10001/10001/10001/10001/01110"),
            Map.entry('P', "11110/10001/10001/11110/10000/10000/10000"),
            Map.entry('Q', "01110/10001/10001/10001/10101/10010/01101"),
            Map.entry('R', "11110/10001/10001/11110/10100/10010/10001"),
            Map.entry('S', "01111/10000/10000/01110/00001/00001/11110"),
            Map.entry('T', "11111/00100/00100/00100/00100/00100/00100"),
            Map.entry('U', "10001/10001/10001/10001/10001/10001/01110"),
            Map.entry('V', "10001/10001/10001/10001/10001/01010/00100"),
            Map.entry('W', "10001/10001/10001/10101/10101/10101/01010"),
            Map.entry('X', "10001/10001/01010/00100/01010/10001/10001"),
            Map.entry('Y', "10001/10001/01010/00100/00100/00100/00100"),
            Map.entry('Z', "11111/00001/00010/00100/01000/10000/11111"));

    private final Canvas lienzo = new Canvas();
    private Color color;
    /** Color de los puntos apagados; null si no se dibujan (como en el panel). */
    private Color off;
    private String texto = "";

    public LedText(Color color) {
        this.color = Objects.requireNonNull(color, "color");
        setMinSize(0, 8);
        getChildren().add(lienzo);
        setMouseTransparent(true);
    }

    public void setText(String texto) {
        Objects.requireNonNull(texto, "texto");
        if (this.texto.equals(texto)) return;
        this.texto = texto;
        setAccessibleText(texto);
        requestLayout();
    }

    public void setColor(Color color) {
        Objects.requireNonNull(color, "color");
        if (this.color.equals(color)) return;
        this.color = color;
        requestLayout();
    }

    /** Dibuja también los puntos apagados, tenues, como un cartel de luces de estadio. */
    public void setColorOff(Color off) {
        this.off = Objects.requireNonNull(off, "apagado");
        requestLayout();
    }

    private String letters() {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
    }

    @Override public Orientation getContentBias() { return Orientation.HORIZONTAL; }

    @Override protected double computePrefWidth(double height) { return 300; }

    @Override protected double computePrefHeight(double width) {
        double columns = Math.max(1, letters().length() * 6 - 1);
        return Math.min(72, Math.max(18, (width < 0 ? 300 : width) * 7 / columns));
    }

    @Override protected void layoutChildren() {
        lienzo.setWidth(Math.max(0, getWidth()));
        lienzo.setHeight(Math.max(0, getHeight()));
        var drawing = lienzo.getGraphicsContext2D();
        drawing.clearRect(0, 0, getWidth(), getHeight());
        String letters = letters();
        if (letters.isEmpty()) return;
        int columns = letters.length() * 6 - 1;
        double step = Math.min(getWidth() / columns, getHeight() / 7);
        double startX = (getWidth() - columns * step) / 2;
        double startY = (getHeight() - 7 * step) / 2;
        double diameter = step * 0.72;
        drawing.setFill(color);
        for (int letter = 0; letter < letters.length(); letter++) {
            String glyph = GLYPHS.getOrDefault(letters.charAt(letter), EMPTY);
            for (int row = 0; row < 7; row++) {
                for (int column = 0; column < 5; column++) {
                    boolean on = glyph.charAt(row * 6 + column) == '1';
                    if (!on && off == null) continue;
                    drawing.setFill(on ? color : off);
                    drawing.fillOval(startX + (letter * 6 + column + 0.14) * step,
                            startY + (row + 0.14) * step, diameter, diameter);
                }
            }
        }
    }
}
