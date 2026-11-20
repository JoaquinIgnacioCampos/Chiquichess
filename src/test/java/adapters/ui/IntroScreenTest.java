package adapters.ui;

import adapters.audio.VolumeControl;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** La pantalla con la que arranca el juego: logo, botón para comenzar y control de sonido. */
class IntroScreenTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // otro test ya lo inició
        }
    }

    private int started;
    private final IntroScreen screen =
            new IntroScreen(PanelTheme.scoreboard(), new VolumeControl(), () -> started++);

    @Test
    void showsLogoAndAButtonForStart() {
        ImageView logo = assertInstanceOf(ImageView.class, screen.lookup("#logo-intro"));
        assertFalse(logo.getImage().isError());
        Button button = assertInstanceOf(Button.class, screen.lookup("#comenzar"));
        assertEquals("COMENZAR", button.getText());
    }

    @Test
    void playStartNotifiesAAloneTime() {
        Button button = (Button) screen.lookup("#comenzar");
        button.fire();
        button.fire();
        assertEquals(1, started);
    }

    @Test
    void hasControlOfSoundForMuteVideo() {
        assertNotNull(screen.lookup("#silencio-intro"));
        assertNotNull(screen.lookup("#volumen-intro"));
    }

    @Test
    void withoutPlayNotHasVideoAndStopNotFails() {
        assertNull(((javafx.scene.media.MediaView) screen.lookup("#video-intro")).getMediaPlayer());
        assertDoesNotThrow(screen::stop);
    }
}
