package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.piezas.Pieza;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Objects;

/**
 * Dibuja cada pieza con su símbolo de ajedrez Unicode. Las dos usan la versión rellena del símbolo: el
 * bando lo da el color de relleno, con el contorno oscuro marcando la forma de la pieza blanca.
 */
public class RepresentacionUnicode implements RepresentacionPiezas {
    @Override
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
        int relleno = switch (pieza.codigo()) {
            case 'R' -> 0x265A;
            case 'D' -> 0x265B;
            case 'T' -> 0x265C;
            case 'A' -> 0x265D;
            case 'C' -> 0x265E;
            case 'P' -> 0x265F;
            default -> -1;
        };
        return relleno == -1 ? String.valueOf(pieza.codigo()) : Character.toString(relleno);
    }
}
