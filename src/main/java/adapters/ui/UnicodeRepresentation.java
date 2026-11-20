package adapters.ui;

import core.side.Side;
import core.pieces.Piece;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Objects;

/**
 * Dibuja cada pieza con su símbolo de ajedrez Unicode. Las dos usan la versión rellena del símbolo: el
 * bando lo da el color de relleno, con el contorno oscuro marcando la forma de la pieza blanca.
 */
public class UnicodeRepresentation implements PieceRepresentation {
    @Override
    public Node create(Piece piece, BoardTheme theme) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(theme, "theme");
        Text figure = new Text(symbol(piece));
        figure.setFont(Font.font("Segoe UI Symbol", SIDE_BASE * 0.7));
        figure.setFill(piece.color() == Side.WHITE ? theme.whitePiece() : theme.blackPiece());
        figure.setStroke(theme.outlinePieces());
        figure.setStrokeWidth(0.8);
        figure.setMouseTransparent(true);
        figure.setAccessibleText(piece.displayName() + " " + piece.color());
        return figure;
    }

    private String symbol(Piece piece) {
        int fill = switch (piece.code()) {
            case 'K' -> 0x265A;
            case 'Q' -> 0x265B;
            case 'R' -> 0x265C;
            case 'B' -> 0x265D;
            case 'N' -> 0x265E;
            case 'P' -> 0x265F;
            default -> -1;
        };
        return fill == -1 ? String.valueOf(piece.code()) : Character.toString(fill);
    }
}
