package adapters.ui;

import core.side.Side;
import core.game.Player;
import core.game.Game;
import core.game.GameInProgress;
import core.pieces.Piece;
import core.rules.GameRules;
import core.time.NoClock;
import doubles.BoardFake;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/** Elegir la pieza al promover: el controlador espera la elección y la vista muestra el selector. */
class PromotionInViewTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // otro test ya lo inició
        }
    }

    /** Peón blanco en a7 a punto de promover. */
    private final GameInProgress game = new Game(BoardFake.from(
            "....r...",
            "P.......",
            "........",
            "........",
            "........",
            "........",
            "........",
            "....R..."), new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
            GameRules.classic(), new NoClock());
    private final BoardController controller = new BoardController(game);

    private static List<Character> codes(List<Piece> pieces) {
        return pieces.stream().map(Piece::code).toList();
    }

    // ---------------- Controlador ----------------

    @Test
    void carryPawnToLastRowWaitsChoice() {
        // Act
        controller.press(pos("a7"));
        controller.press(pos("a8"));

        // Assert: todavía no movió
        assertEquals(List.of('D', 'T', 'A', 'C'), codes(controller.promotionPending()));
        assertTrue(game.history().isEmpty());
        assertTrue(game.board().pieceIn(pos("a7")).orElseThrow().isPawn());
    }

    @Test
    void choosePieceCompleteMove() {
        // Arrange
        controller.press(pos("a7"));
        controller.press(pos("a8"));

        // Act
        assertTrue(controller.promote('C').isEmpty());

        // Assert
        assertEquals('C', game.board().pieceIn(pos("a8")).orElseThrow().code());
        assertEquals(Side.BLACK, game.turn());
        assertTrue(controller.promotionPending().isEmpty());
    }

    @Test
    void cancelLeavesPawnWhereWas() {
        // Arrange
        controller.press(pos("a7"));
        controller.press(pos("a8"));

        // Act
        controller.cancelPromotion();

        // Assert
        assertTrue(controller.promotionPending().isEmpty());
        assertTrue(game.board().pieceIn(pos("a7")).orElseThrow().isPawn());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void pressOtherSquareDiscardsPromotion() {
        // Arrange
        controller.press(pos("a7"));
        controller.press(pos("a8"));

        // Act
        controller.press(pos("e1"));

        // Assert
        assertTrue(controller.promotionPending().isEmpty());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void promoteWithoutPromotionPendingIsAError() {
        assertThrows(IllegalStateException.class, () -> controller.promote('D'));
    }

    @Test
    void aCommonMoveNotAsksNothing() {
        // Act
        controller.press(pos("e1"));
        controller.press(pos("d1"));

        // Assert
        assertTrue(controller.promotionPending().isEmpty());
        assertEquals(1, game.history().size());
    }

    // ---------------- Vista ----------------

    private GameView view() {
        BoardTheme theme = new BoardTheme(Color.BEIGE, Color.BROWN, Color.WHITE, Color.BLACK,
                Color.BLACK, Color.GRAY, Color.YELLOW, Color.PURPLE, Color.BLUE);
        return new GameView(controller, theme, new UnicodeRepresentation(), PanelTheme.scoreboard());
    }

    private void press(GameView view, String square) {
        var position = pos(square);
        StackPane node = (StackPane) view.lookup("#casilla-" + position.column() + "-" + position.row());
        node.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, true, null));
    }

    @Test
    void selectorAppearsOnlyToPromote() {
        // Arrange
        GameView view = view();

        // Assert: al empezar no existe (se crea recién al promover)
        assertNull(view.lookup("#selector-promocion"));

        // Act
        press(view, "a7");
        press(view, "a8");

        // Assert
        assertTrue(view.lookup("#selector-promocion").isVisible());
        for (char code : List.of('D', 'T', 'A', 'C')) {
            assertNotNull(view.lookup("#promover-" + code), "Falta el botón de " + code);
        }
    }

    @Test
    void chooseInSelectorPromotesAndItCloses() {
        // Arrange
        GameView view = view();
        press(view, "a7");
        press(view, "a8");

        // Act
        ((Button) view.lookup("#promover-T")).fire();

        // Assert
        assertEquals('T', game.board().pieceIn(pos("a8")).orElseThrow().code());
        assertFalse(view.lookup("#selector-promocion").isVisible());
    }

    @Test
    void cancelInSelectorItClosesWithoutMove() {
        // Arrange
        GameView view = view();
        press(view, "a7");
        press(view, "a8");

        // Act
        ((Button) view.lookup("#cancelar-promocion")).fire();

        // Assert
        assertFalse(view.lookup("#selector-promocion").isVisible());
        assertTrue(game.board().pieceIn(pos("a7")).orElseThrow().isPawn());
    }
}
