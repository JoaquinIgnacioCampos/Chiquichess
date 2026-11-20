package adapters.ui;

import core.side.Side;
import core.game.InvalidActionException;
import core.game.StandardLayout;
import core.game.Player;
import core.game.InvalidMoveException;
import core.game.Game;
import core.game.GameInProgress;
import core.rules.GameRules;
import core.time.NoClock;
import adapters.ArtificialRival;
import doubles.StrategyFalse;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Contra la IA, la interfaz solo deja actuar a la persona. La IA de estos tests no juega sola (no hay
 * un AiTurn): así se puede mirar qué pasa mientras le toca.
 */
class GameAgainstAiInViewTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // otro test ya lo inició
        }
    }

    private final GameInProgress game = new Game(new StandardLayout().createBoard(),
            new Player("Ana", Side.WHITE), new Player("IA (medio)", Side.BLACK),
            GameRules.classic().withUndo(), new NoClock());
    private final ArtificialRival rival = new ArtificialRival(game, Side.BLACK, new StrategyFalse(false));
    private final BoardController controller = new BoardController(game, Optional.of(rival));

    @Test
    void whileHimPlaysToAiBoardNotResponds() {
        // Arrange
        game.move(move("e2", "e4"));

        // Act
        var rejection = controller.press(pos("e7"));

        // Assert
        assertEquals(Optional.of(InvalidMoveException.Reason.NO_ES_SU_TURNO), rejection);
        assertTrue(controller.selected().isEmpty());
    }

    @Test
    void nobodyYieldsNorOffersDrawInNameOfAi() {
        // Arrange
        game.move(move("e2", "e4"));

        // Act + Assert
        assertEquals(Optional.of(InvalidActionException.Reason.NO_ES_SU_TURNO), controller.resign(Side.BLACK));
        assertEquals(Optional.of(InvalidActionException.Reason.NO_ES_SU_TURNO), controller.offerDraw(Side.BLACK));
        assertFalse(game.state().isFinished());
    }

    @Test
    void aiRespondsRightAOfferOfDraw() {
        // Act: la IA de prueba rechaza
        controller.offerDraw(Side.WHITE);

        // Assert
        assertTrue(game.offerDrawPending().isEmpty());
        assertFalse(game.state().isFinished());
    }

    @Test
    void undoReturnsToTurnOfPerson() {
        // Arrange
        game.move(move("e2", "e4"));
        rival.play();

        // Act
        controller.undo();

        // Assert
        assertTrue(game.history().isEmpty());
    }

    @Test
    void panelShowsThatAiThinksAndItsCardNotHasButtonsActive() {
        // Arrange
        GameView view = new GameView(controller, new BoardTheme(
                javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
                javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
                javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.GRAY),
                new UnicodeRepresentation(), PanelTheme.scoreboard());
        view.configureResignation();
        view.configureDraw();

        // Act
        game.move(move("e2", "e4"));

        // Assert
        assertEquals("IA PENSANDO…", ((Text) view.lookup("#estado-partida")).getText());
        assertTrue(((Button) view.lookup("#rendirse-negro")).isDisable());
        assertTrue(((Button) view.lookup("#ofrecer-tablas-negro")).isDisable());
    }
}
