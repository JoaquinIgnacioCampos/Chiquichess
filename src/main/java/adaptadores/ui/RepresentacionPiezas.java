package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.piezas.Pieza;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Objects;

/** Traduce pieza y bando a un nodo visual; concentra la futura sustitución por sprites. */
public class RepresentacionPiezas {
    static final double LADO_BASE = 80;

    public Node crear(Pieza pieza, TemaTablero tema) {
        Objects.requireNonNull(pieza, "pieza");
        Objects.requireNonNull(tema, "tema");
        Text figura = new Text(simbolo(pieza));
        figura.setFont(Font.font("Segoe UI Symbol", LADO_BASE * 0.7));
        figura.setFill(pieza.color() == Bando.BLANCO ? tema.piezaBlanca() : tema.piezaNegra());
        figura.setStroke(tema.contornoPiezas());
        figura.setStrokeWidth(0.8);
        figura.setMouseTransparent(true);
        figura.setAccessibleText(pieza.nombre() + " " + pieza.color());
        return figura;
    }

    private String simbolo(Pieza pieza) {
        int blanco = switch (pieza.codigo()) {
            case 'R' -> 0x2654;
            case 'D' -> 0x2655;
            case 'T' -> 0x2656;
            case 'A' -> 0x2657;
            case 'C' -> 0x2658;
            case 'P' -> 0x2659;
            default -> -1;
        };
        if (blanco == -1) {
            return String.valueOf(pieza.codigo());
        }
        return Character.toString(blanco + (pieza.color() == Bando.NEGRO ? 6 : 0));
    }
}
