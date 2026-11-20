package adapters.ui;

import adapters.configuration.GameConfiguration;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** La opción "Permitir deshacer" del menú: solo se habilita sin tiempo y viaja en la configuración. */
class GameMenuViewTest {

    /** Los controles de JavaFX (botones, etiquetas) necesitan el toolkit iniciado; si ya lo estaba, no pasa nada. */
    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // otro test ya lo inició
        }
    }

    private final List<GameConfiguration> emitted = new ArrayList<>();
    private final GameMenuView menu = new GameMenuView(PanelTheme.scoreboard(), emitted::add);

    /** Busca un control de las opciones del menú. */
    private javafx.scene.Node searchInOptions(String id) {
        return menu.lookup("#options-menu").lookup("#" + id);
    }

    private javafx.scene.Node search(String id) {
        javafx.scene.Node inOptions = searchInOptions(id);
        return inOptions != null ? inOptions : menu.lookup("#" + id);
    }

    private ToggleButton button(String id) {
        return (ToggleButton) search(id);
    }

    private void chooseAllLessTime() {
        button("mode-classic").setSelected(true);
        button("rival-friend").setSelected(true);
        button("player-1-boca").setSelected(true);
        button("player-2-river").setSelected(true);
        button("color-player-1").setSelected(true);
    }

    @Test
    void coverShowsImageOfCharacter() {
        var space = (javafx.scene.layout.StackPane) menu.lookup("#space-character");
        var figure = assertInstanceOf(javafx.scene.image.ImageView.class, space.getChildren().getFirst());
        assertFalse(figure.getImage().isError());
        assertEquals(500, figure.getImage().getWidth());
    }

    @Test
    void playStaysOutsideOfOffsetForBeSeenAlways() {
        assertNull(searchInOptions("play"));
        assertNotNull(menu.lookup("#play"));
    }

    @Test
    void withoutChooseTimeOptionIsDisabled() {
        assertTrue(button("allow-undo").isDisable());
    }

    @Test
    void withWithoutTimeEnablesAndTravelsInConfiguration() {
        chooseAllLessTime();
        button("time-without_time").setSelected(true);
        assertFalse(button("allow-undo").isDisable());

        button("allow-undo").setSelected(true);
        ((Button) search("play")).fire();

        assertEquals(1, emitted.size());
        assertTrue(emitted.get(0).allowUndo());
    }

    @Test
    void chooseATimeTurnsOffAndDisablesOption() {
        chooseAllLessTime();
        button("time-without_time").setSelected(true);
        button("allow-undo").setSelected(true);

        button("time-five_minutes").setSelected(true);

        assertTrue(button("allow-undo").isDisable());
        assertFalse(button("allow-undo").isSelected());
        ((Button) search("play")).fire();
        assertFalse(emitted.get(0).allowUndo());
    }

    @Test
    void optionNotIsRequiredForPlay() {
        chooseAllLessTime();
        button("time-without_time").setSelected(true);
        assertFalse(search("play").isDisable());
    }

    @Test
    void modeFootballTravelsInConfiguration() {
        chooseAllLessTime();
        button("mode-football").setSelected(true);
        button("time-without_time").setSelected(true);

        ((Button) search("play")).fire();

        assertEquals(GameConfiguration.Mode.FOOTBALL, emitted.get(0).mode());
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.control.Spinner<Integer> limit() {
        return (javafx.scene.control.Spinner<Integer>) search("limit-moves");
    }

    @Test
    void limitOfMovesOnlyEnablesInModeFootball() {
        assertTrue(limit().isDisable());
        button("mode-football").setSelected(true);
        assertFalse(limit().isDisable());
        button("mode-classic").setSelected(true);
        assertTrue(limit().isDisable());
    }

    @Test
    void limitWrittenTravelsInConfiguration() {
        chooseAllLessTime();
        button("mode-football").setSelected(true);
        button("time-without_time").setSelected(true);
        limit().getEditor().setText("40");

        ((Button) search("play")).fire();

        assertEquals(40, emitted.get(0).moveLimit());
    }

    @Test
    void aLimitOutsideOfRangeAdjustsAndATextInvalidDiscards() {
        chooseAllLessTime();
        button("mode-football").setSelected(true);
        button("time-without_time").setSelected(true);

        limit().getEditor().setText("5000");
        ((Button) search("play")).fire();
        limit().getEditor().setText("muchas");
        ((Button) search("play")).fire();

        assertEquals(GameConfiguration.LIMIT_MAXIMUM, emitted.get(0).moveLimit());
        assertEquals(GameConfiguration.LIMIT_MAXIMUM, emitted.get(1).moveLimit());
    }

    // ---------------- Rival IA ----------------

    @Test
    void difficultyOnlyEnablesAgainstAiAndIsRequired() {
        chooseAllLessTime();
        button("time-without_time").setSelected(true);
        assertTrue(button("difficulty-middle").isDisable());

        button("rival-ai").setSelected(true);

        assertFalse(button("difficulty-middle").isDisable());
        assertTrue(search("play").isDisable());
        assertTrue(((javafx.scene.control.Label) menu.lookup("#selections-pending")).getText().contains("difficulty"));
    }

    @Test
    void difficultyChosenTravelsInConfiguration() {
        chooseAllLessTime();
        button("time-without_time").setSelected(true);
        button("rival-ai").setSelected(true);
        button("difficulty-hard").setSelected(true);

        ((Button) search("play")).fire();

        assertEquals(GameConfiguration.Rival.IA, emitted.getFirst().rival());
        assertEquals(java.util.Optional.of(core.ia.Difficulty.HARD), emitted.getFirst().difficulty());
    }

    @Test
    void returnToChooseFriendDeletesDifficulty() {
        chooseAllLessTime();
        button("time-without_time").setSelected(true);
        button("rival-ai").setSelected(true);
        button("difficulty-easy").setSelected(true);

        button("rival-friend").setSelected(true);

        assertFalse(button("difficulty-easy").isSelected());
        ((Button) search("play")).fire();
        assertTrue(emitted.getFirst().difficulty().isEmpty());
    }

    // ---------------- Portada y sonido ----------------

    @Test
    void onOpenCharacterSwaysSomeSecondsAndReturnsToStayStill() {
        var laugh = menu.animateCharacter();
        var character = search("character");

        assertEquals(javafx.animation.Animation.Status.RUNNING, laugh.getStatus());
        double seconds = laugh.getTotalDuration().toSeconds();
        assertTrue(seconds > 2 && seconds < 4, "lasts " + seconds + " s");
        laugh.stop();
        laugh.getOnFinished().handle(null);
        assertEquals(0, character.getRotate());
        assertEquals(1, character.getScaleX());
    }

    @Test
    void buttonOfSoundOfMenuUsesSameControlThatGame() {
        var control = new adapters.audio.VolumeControl();
        menu.configureSound(control);
        Button silence = (Button) menu.lookup("#silence-menu");

        silence.fire();

        assertTrue(control.muted());
        assertEquals("Sound: off", silence.getAccessibleText());
    }

    @Test
    void playAOptionNotifiesAndSelectionsOfOwnMenuNot() {
        int[] notices = {0};
        menu.onChooseOption(() -> notices[0]++);

        button("mode-classic").fire();
        button("player-1-argentina").fire();
        button("rival-friend").setSelected(true);   // lo hace el programa, no un clic: no avisa

        assertEquals(2, notices[0]);
    }
    @Test
    void selectionUsualOnlyLeavesPendingTeams() {
        menu.preselect(adapters.configuration.MenuSelection.usual());
        assertTrue(button("mode-classic").isSelected());
        assertTrue(button("time-ten_minutes").isSelected());
        assertTrue(button("rival-friend").isSelected());
        assertTrue(button("color-random").isSelected());
        assertFalse(button("allow-undo").isSelected());
        assertTrue(search("play").isDisable());
        assertEquals("Still to choose: team of Player 1, team of Player 2.",
                ((javafx.scene.control.Label) search("selections-pending")).getText());

        button("player-1-boca").setSelected(true);
        button("player-2-river").setSelected(true);
        ((Button) search("play")).fire();
        assertEquals(GameConfiguration.Colors.RANDOM, emitted.getFirst().colors());
        assertEquals(GameConfiguration.Time.TEN_MINUTES, emitted.getFirst().time());
    }

    @Test
    void aMenuNewRecoversLastSelectionComplete() {
        var previous = new GameConfiguration(GameConfiguration.Mode.FOOTBALL, GameConfiguration.Time.WITHOUT_TIME,
                GameConfiguration.Rival.IA, GameConfiguration.Team.RIVER, GameConfiguration.Team.BOCA,
                GameConfiguration.Colors.PLAYER_2_WHITE, true, 120,
                java.util.Optional.of(core.ia.Difficulty.HARD));
        menu.preselect(adapters.configuration.MenuSelection.de(previous));
        ((Button) search("play")).fire();
        assertEquals(List.of(previous), emitted);
    }

    @Test
    void volumeOfMenuChangesControlShared() {
        var control = new adapters.audio.VolumeControl();
        menu.configureSound(control);
        ((javafx.scene.control.Slider) menu.lookup("#volume-menu")).setValue(0.2);
        assertEquals(0.2, control.volume(), 1e-9);
    }
    @Test
    void silenceCarriesVolumeToZeroAndRaiseItItReactivates() {
        var control = new adapters.audio.VolumeControl();
        menu.configureSound(control);
        Button silence = (Button) menu.lookup("#silence-menu");
        var volume = (javafx.scene.control.Slider) menu.lookup("#volume-menu");
        double before = volume.getValue();

        silence.fire();
        assertEquals(0, volume.getValue());
        assertEquals("Sound: off", silence.getAccessibleText());

        volume.setValue(0.4);
        assertFalse(control.muted());
        assertEquals("Sound: on", silence.getAccessibleText());

        silence.fire();
        silence.fire();
        assertEquals(0.4, volume.getValue(), 1e-9);
        assertNotEquals(before, volume.getValue());
    }
}
