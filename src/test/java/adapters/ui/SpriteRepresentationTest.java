package adapters.ui;

import core.side.Side;
import core.pieces.Pawn;
import core.pieces.Piece;
import core.pieces.King;
import core.board.Position;
import core.board.Board;
import adapters.configuration.GameConfiguration.Team;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Los archivos de prueba: rey-blanco.png mide 2x2 y rey-negro.png 4x4; el resto no existe. */
class SpriteRepresentationTest {
    private final BoardTheme theme = new BoardTheme(Color.BEIGE, Color.BROWN, Color.WHITE, Color.BLACK,
            Color.BLACK, Color.GRAY);
    private final SpriteCatalog catalog = new SpriteCatalog("/sprites-de-prueba");
    private final PieceRepresentation representation = new SpriteRepresentation(
            Map.of(Side.WHITE, catalog, Side.BLACK, catalog), new UnicodeRepresentation());

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // otro test ya lo inició
        }
    }

    /** El sprite va dentro de un contenedor del tamaño de una casilla: se busca la imagen adentro. */
    private static ImageView imageOf(javafx.scene.Node figure) {
        return assertInstanceOf(ImageView.class, figure.lookup(".image-view"));
    }

    @Test
    void spriteStickingOutTowardUpAndRestsItsBaseInSquare() {
        var figure = representation.create(new King(Side.WHITE), theme);
        var image = imageOf(figure);
        assertEquals(PieceRepresentation.SIDE_BASE, figure.prefWidth(-1));
        assertEquals(PieceRepresentation.SIDE_BASE, figure.prefHeight(-1));
        assertTrue(image.getFitHeight() > PieceRepresentation.SIDE_BASE);
        assertTrue(image.getLayoutY() < 0);
        assertTrue(image.getLayoutY() + image.getFitHeight() <= PieceRepresentation.SIDE_BASE);
    }

    @Test
    void eachColorUsesItsOwnVersionOfSprite() {
        ImageView white = imageOf(representation.create(new King(Side.WHITE), theme));
        ImageView black = imageOf(representation.create(new King(Side.BLACK), theme));
        assertEquals(2, white.getImage().getWidth());
        assertEquals(4, black.getImage().getWidth());
    }

    @Test
    void catalogLoadsEachImageAAloneTime() {
        assertSame(catalog.sprite('R', Side.WHITE).orElseThrow(), catalog.sprite('R', Side.WHITE).orElseThrow());
    }

    @Test
    void ifMissingSpriteUsesRepresentationOfFallback() {
        Text figure = assertInstanceOf(Text.class, representation.create(new Pawn(Side.WHITE), theme));
        assertEquals("♟", figure.getText());
    }

    @Test
    void aSideWithoutCatalogUsesRepresentationOfFallback() {
        PieceRepresentation onlyWhite = new SpriteRepresentation(
                Map.of(Side.WHITE, catalog), new UnicodeRepresentation());
        imageOf(onlyWhite.create(new King(Side.WHITE), theme));
        assertInstanceOf(Text.class, onlyWhite.create(new King(Side.BLACK), theme));
    }

    @Test
    void aNewPieceWithoutFileNotBreaksView() {
        Piece newValue = new Piece(Side.WHITE, new core.movements.CompositeRule()) {
            @Override public String displayName() { return "Nueva"; }
            @Override public char code() { return 'X'; }
            @Override public int value() { return 0; }
        };
        assertEquals("X", assertInstanceOf(Text.class, representation.create(newValue, theme)).getText());
    }

    @Test
    void eachClubSearchesInItsOwnFolder() {
        for (Team team : Team.values()) {
            assertTrue(SpriteCatalog.de(team).sprite('X', Side.WHITE).isEmpty());
        }
    }

    @Test
    void spriteKeepsDescriptionAndNotCoversClicksOfSquare() {
        Board board = new Board(2, 2);
        board.place(new Position(0, 0), new King(Side.WHITE));
        List<Position> pressed = new ArrayList<>();
        BoardView view = new BoardView(board, theme, representation, pressed::add);
        StackPane square = (StackPane) view.lookup("#casilla-0-0");
        javafx.scene.Node sprite = square.getChildren().getFirst();
        imageOf(sprite);
        assertTrue(sprite.isMouseTransparent());
        assertEquals("Rey BLANCO", sprite.getAccessibleText());
        square.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, true, null));
        assertEquals(List.of(new Position(0, 0)), pressed);
    }
}
