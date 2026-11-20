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
    void firstRunOfAudioStartsWholeAndAllEndInSilence() {
        assertEquals(1, IntroScreen.gain(0, 10, false), 1e-9);
        assertEquals(1, IntroScreen.gain(5, 10, false), 1e-9);
        assertEquals(0, IntroScreen.gain(10, 10, false), 1e-9);
    }

    @Test
    void runsNextEnterWithAFade() {
        assertEquals(0, IntroScreen.gain(0, 10, true), 1e-9);
        assertEquals(1, IntroScreen.gain(IntroScreen.FADE, 10, true), 1e-9);
        double half = IntroScreen.gain(IntroScreen.FADE / 2, 10, true);
        assertTrue(half > 0 && half < 1);
    }

    @Test
    void onOverlapEndOfARunWithStartOfNextPowerKeeps() {
        for (double t = 0; t <= IntroScreen.FADE; t += 0.1) {
            double leaves = IntroScreen.gain(10 - IntroScreen.FADE + t, 10, false);
            double enters = IntroScreen.gain(t, 10, true);
            assertEquals(1, leaves * leaves + enters * enters, 1e-9, "t=" + t);
        }
    }

    @Test
    void withoutPlayNotHasVideoAndStopNotFails() {
        assertNull(((javafx.scene.media.MediaView) screen.lookup("#video-intro")).getMediaPlayer());
        assertDoesNotThrow(screen::stop);
    }
}
