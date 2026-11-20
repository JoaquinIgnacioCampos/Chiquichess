package adapters.ui;

import core.game.GameInProgress;
import adapters.GameAssembler;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/** Los botones "Deshacer" y "Rehacer" de la cabecera, conectados a una partida real. */
class UndoInViewTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // otro test ya lo inició
        }
    }

    private GameView view(GameInProgress game) {
        BoardTheme theme = new BoardTheme(
                javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
                javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
                javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.GRAY,
                javafx.scene.paint.Color.YELLOW, javafx.scene.paint.Color.PURPLE,
                javafx.scene.paint.Color.BLUE);
        GameView view = new GameView(new BoardController(game), theme,
                new UnicodeRepresentation(), PanelTheme.scoreboard());
        view.configureUndo();
        return view;
    }

    private void press(GameView view, String square) {
        var position = pos(square);
        StackPane node = (StackPane) view.lookup("#casilla-" + position.column() + "-" + position.row());
        node.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, true, null));
    }

    private Button button(GameView view, String id) {
        return (Button) view.lookup("#" + id);
    }

    @Test
    void withoutOptionNotHasButtons() {
        GameView view = view(new GameAssembler().createWithoutClock());
        assertNull(view.lookup("#deshacer"));
        assertNull(view.lookup("#rehacer"));
    }

    @Test
    void onStartButtonsAreDisabled() {
        GameView view = view(new GameAssembler().createWithoutClockWithUndo());
        assertTrue(button(view, "deshacer").isDisable());
        assertTrue(button(view, "rehacer").isDisable());
    }

    @Test
    void undoAndRedoAMoveFromButtons() {
        GameInProgress game = new GameAssembler().createWithoutClockWithUndo();
        GameView view = view(game);
        press(view, "e2");
        press(view, "e4");
        assertFalse(button(view, "deshacer").isDisable());

        button(view, "deshacer").fire();
        assertTrue(game.history().isEmpty());
        assertTrue(game.board().pieceIn(pos("e2")).isPresent());
        assertTrue(button(view, "deshacer").isDisable());
        assertFalse(button(view, "rehacer").isDisable());

        button(view, "rehacer").fire();
        assertEquals(1, game.history().size());
        assertTrue(button(view, "rehacer").isDisable());
    }

    @Test
    void undoDiscardsSelection() {
        GameInProgress game = new GameAssembler().createWithoutClockWithUndo();
        GameView view = view(game);
        press(view, "e2");
        press(view, "e4");
        press(view, "e7");

        button(view, "deshacer").fire();

        assertEquals(0, view.lookupAll(".indicador-movimiento").size());
    }
}
