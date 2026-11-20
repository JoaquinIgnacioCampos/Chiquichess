package adapters.ui;

import adapters.GameAssembler;
import core.game.GameInProgress;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El logo reemplaza al nombre escrito en el menú y en la cabecera de la partida. */
class LogoTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // otro test ya lo inició
        }
    }

    private final BoardTheme theme = new BoardTheme(
            javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
            javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
            javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.GRAY);

    private GameView view() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        return new GameView(new BoardController(game), theme, new UnicodeRepresentation(), PanelTheme.scoreboard());
    }

    @Test
    void theHeaderShowsTheLogoInsteadOfTheWrittenName() {
        GameView view = view();
        ImageView logo = assertInstanceOf(ImageView.class, view.lookup("#logo-partida"));
        assertFalse(logo.getImage().isError());
        assertEquals("ChiquiChess", logo.getAccessibleText());
        assertTrue(logo.getFitHeight() > 0);
    }

    @Test
    void theMenuShowsTheLogoInsteadOfTheLedTitle() {
        GameMenuView menu = new GameMenuView(PanelTheme.scoreboard(), configuration -> { });
        ImageView logo = assertInstanceOf(ImageView.class, menu.lookup("#logo-menu"));
        assertFalse(logo.getImage().isError());
    }
}
