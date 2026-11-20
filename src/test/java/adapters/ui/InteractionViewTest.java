package adapters.ui;

import core.side.Side;
import core.game.GameInProgress;
import adapters.GameAssembler;
import doubles.FontOfTimeFalse;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static doubles.Squares.*;
import static org.junit.jupiter.api.Assertions.*;

class InteractionViewTest {
    private final BoardTheme theme = new BoardTheme(
            javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
            javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
            javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.GRAY,
            javafx.scene.paint.Color.YELLOW, javafx.scene.paint.Color.PURPLE,
            javafx.scene.paint.Color.BLUE);

    private GameView view(GameInProgress game) {
        return new GameView(new BoardController(game), theme,
                new UnicodeRepresentation(), PanelTheme.scoreboard());
    }

    private StackPane square(GameView view, String displayName) {
        var position = pos(displayName);
        return (StackPane) view.lookup("#casilla-" + position.column() + "-" + position.row());
    }

    private void press(GameView view, String displayName) {
        click(square(view, displayName), MouseButton.PRIMARY);
    }

    private void click(Node node, MouseButton button) {
        node.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, button, 1,
                false, false, false, false, false, false, false, false, false, true, null));
    }

    @Test
    void clickSelectsAndMarksOnlyDestinationsThatGivesCore() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        GameView view = view(game);
        press(view, "e2");
        assertEquals(2, view.lookupAll(".indicador-movimiento").size());
        assertNotNull(square(view, "e3").lookup(".indicador-movimiento"));
        assertNotNull(square(view, "e4").lookup(".indicador-movimiento"));
        assertEquals(javafx.scene.paint.Color.YELLOW,
                square(view, "e2").getBackground().getFills().getFirst().getFill());
        Circle point = (Circle) square(view, "e4").lookup(".indicador-movimiento");
        assertEquals(javafx.scene.paint.Color.PURPLE, point.getFill());
        assertTrue(point.isMouseTransparent());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void chooseOtherPieceReplacesAllIndicatorsPrevious() {
        GameView view = view(new GameAssembler().createWithoutClock());
        press(view, "e2");
        press(view, "g1");
        assertEquals(2, view.lookupAll(".indicador-movimiento").size());
        assertNull(square(view, "e4").lookup(".indicador-movimiento"));
        assertNotNull(square(view, "f3").lookup(".indicador-movimiento"));
        assertNotNull(square(view, "h3").lookup(".indicador-movimiento"));
    }

    @Test
    void emptySquareNotValidCancelsAllHighlighted() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        GameView view = view(game);
        press(view, "e2");
        press(view, "e6");
        assertTrue(view.lookupAll(".indicador-movimiento").isEmpty());
        assertTrue(view.lookupAll(".casilla-seleccionada").isEmpty());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void enemyNotCapturableAlsoCancelsSelection() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        GameView view = view(game);
        press(view, "e2");
        press(view, "a7");
        assertTrue(view.lookupAll(".indicador-movimiento").isEmpty());
        assertTrue(view.lookupAll(".casilla-seleccionada").isEmpty());
        assertEquals(32, game.board().pieces().size());
    }

    @Test
    void ringOverlappingMarksPieceCapturableWithoutHideIt() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        game.move(move("e2", "e4"));
        game.move(move("d7", "d5"));
        GameView view = view(game);
        press(view, "e4");
        view.resize(1000, 720);
        view.layout();
        StackPane rival = square(view, "d5");
        Circle ring = (Circle) rival.lookup(".indicador-captura");
        assertNotNull(ring);
        assertNull(ring.getFill());
        assertEquals(javafx.scene.paint.Color.BLUE, ring.getStroke());
        assertTrue(ring.getRadius() > 0);
        assertTrue(ring.getStrokeWidth() > 0);
        assertTrue(ring.isMouseTransparent());
        assertInstanceOf(Text.class, rival.getChildren().getFirst());
        assertSame(ring, rival.getChildren().getLast());
        assertNull(rival.lookup(".indicador-movimiento"));
        assertNotNull(square(view, "e5").lookup(".indicador-movimiento"));
    }

    @Test
    void clickInEnemyMarkedExecutesCaptureAndRedrawsFromCore() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        game.move(move("e2", "e4"));
        game.move(move("d7", "d5"));
        GameView view = view(game);
        press(view, "e4");
        press(view, "d5");
        assertEquals(Side.WHITE, game.board().pieceIn(pos("d5")).orElseThrow().color());
        assertEquals(31, game.board().pieces().size());
        assertEquals("♟", ((Text) square(view, "d5").getChildren().getFirst()).getText());
        assertTrue(square(view, "e4").getChildren().isEmpty());
        assertTrue(view.lookupAll(".indicador-captura").isEmpty());
        assertTrue(view.lookupAll(".indicador-movimiento").isEmpty());
        assertTrue(view.lookupAll(".casilla-seleccionada").isEmpty());
        assertEquals(move("e4", "d5"), game.history().getLast());
    }

    @Test
    void emptyMovementPassesTurnAndOnlySelectsNowPiecesBlack() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        GameView view = view(game);
        press(view, "e2");
        press(view, "e4");
        assertEquals(Side.BLACK, game.turn());
        press(view, "d2");
        assertTrue(view.lookupAll(".indicador-movimiento").isEmpty());
        press(view, "e7");
        assertNotNull(square(view, "e5").lookup(".indicador-movimiento"));
        assertNotNull(square(view, "e6").lookup(".indicador-movimiento"));
    }

    @Test
    void showsRejectionOfCoreIfTimeExpiresBetweenClicks() {
        FontOfTimeFalse font = new FontOfTimeFalse();
        GameInProgress game = new GameAssembler().createWithClock(Duration.ofSeconds(1), font);
        GameView view = view(game);
        press(view, "e2");
        font.advance(Duration.ofSeconds(1));
        press(view, "e4");
        Text message = (Text) view.lookup("#mensaje-movimiento");
        assertFalse(message.getText().isBlank());
        assertTrue(game.state().isFinished());
        assertTrue(game.history().isEmpty());
        assertTrue(view.lookupAll(".indicador-movimiento").isEmpty());
        assertFalse(square(view, "e2").getChildren().isEmpty());
    }

    @Test
    void clickSecondaryNotSendsOrdersNorSelects() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        GameView view = view(game);
        click(square(view, "e2"), MouseButton.SECONDARY);
        assertTrue(view.lookupAll(".indicador-movimiento").isEmpty());
        assertTrue(view.lookupAll(".casilla-seleccionada").isEmpty());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void notifiesOnlyWhenChoosesAPieceForMove() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        GameView view = view(game);
        int[] notices = {0};
        view.onSelectPiece(() -> notices[0]++);

        press(view, "e7");   // pieza rival: nada
        press(view, "e5");   // casilla vacía: nada
        assertEquals(0, notices[0]);
        press(view, "e2");   // elige un peón
        press(view, "g1");   // cambia al caballo
        assertEquals(2, notices[0]);
        press(view, "f3");   // mueve: suena el movimiento, no la selección
        assertEquals(2, notices[0]);
        assertEquals(1, game.history().size());
    }
}
