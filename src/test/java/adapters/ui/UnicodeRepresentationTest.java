package adapters.ui;

import core.side.Side;
import core.movements.CompositeRule;
import core.pieces.*;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnicodeRepresentationTest {
    private final BoardTheme theme = new BoardTheme(
            javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
            javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
            javafx.scene.paint.Color.DARKSLATEGRAY, javafx.scene.paint.Color.GRAY);
    private final PieceRepresentation representation = new UnicodeRepresentation();

    @Test
    void translatesSixTypesAndBothColorsInSymbolCorrect() {
        for (Side color : Side.values()) {
            List<Piece> pieces = List.of(new King(color), new Queen(color), new Rook(color),
                    new Bishop(color), new Knight(color), new Pawn(color));
            // Los dos bandos usan el símbolo relleno; los distingue el color (ver el test siguiente).
            List<String> expected = List.of("♚", "♛", "♜", "♝", "♞", "♟");
            for (int i = 0; i < pieces.size(); i++) {
                assertEquals(expected.get(i), ((Text) representation.create(pieces.get(i), theme)).getText());
            }
        }
    }

    @Test
    void usesColorsOfPiecesAndOutlineReceivedInTheme() {
        Text white = (Text) representation.create(new Rook(Side.WHITE), theme);
        Text black = (Text) representation.create(new Rook(Side.BLACK), theme);
        assertEquals(javafx.scene.paint.Color.WHITE, white.getFill());
        assertEquals(javafx.scene.paint.Color.BLACK, black.getFill());
        assertEquals(javafx.scene.paint.Color.DARKSLATEGRAY, white.getStroke());
        assertEquals(javafx.scene.paint.Color.DARKSLATEGRAY, black.getStroke());
    }

    @Test
    void aNewPieceCanShowWithItsCodeWithoutBreakView() {
        Piece newValue = new Piece(Side.WHITE, new CompositeRule()) {
            @Override public String displayName() { return "Nueva"; }
            @Override public char code() { return 'X'; }
            @Override public int value() { return 0; }
        };
        assertEquals("X", ((Text) representation.create(newValue, theme)).getText());
    }
}
