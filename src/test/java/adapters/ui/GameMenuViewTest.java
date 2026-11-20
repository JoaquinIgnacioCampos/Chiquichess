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
        return menu.lookup("#opciones-menu").lookup("#" + id);
    }

    private javafx.scene.Node search(String id) {
        javafx.scene.Node inOptions = searchInOptions(id);
        return inOptions != null ? inOptions : menu.lookup("#" + id);
    }

    private ToggleButton button(String id) {
        return (ToggleButton) search(id);
    }

    private void chooseAllLessTime() {
        button("modo-clasico").setSelected(true);
        button("rival-amigo").setSelected(true);
        button("jugador-1-boca").setSelected(true);
        button("jugador-2-river").setSelected(true);
        button("color-jugador-1").setSelected(true);
    }

    @Test
    void coverShowsImageOfCharacter() {
        var space = (javafx.scene.layout.StackPane) menu.lookup("#espacio-personaje");
        var figure = assertInstanceOf(javafx.scene.image.ImageView.class, space.getChildren().getFirst());
        assertFalse(figure.getImage().isError());
        assertEquals(500, figure.getImage().getWidth());
    }

    @Test
    void playStaysOutsideOfOffsetForBeSeenAlways() {
        assertNull(searchInOptions("jugar"));
        assertNotNull(menu.lookup("#jugar"));
    }

    @Test
    void withoutChooseTimeOptionIsDisabled() {
        assertTrue(button("permitir-deshacer").isDisable());
    }

    @Test
    void withWithoutTimeEnablesAndTravelsInConfiguration() {
        chooseAllLessTime();
        button("tiempo-sin_tiempo").setSelected(true);
        assertFalse(button("permitir-deshacer").isDisable());

        button("permitir-deshacer").setSelected(true);
        ((Button) search("jugar")).fire();

        assertEquals(1, emitted.size());
        assertTrue(emitted.get(0).allowUndo());
    }

    @Test
    void chooseATimeTurnsOffAndDisablesOption() {
        chooseAllLessTime();
        button("tiempo-sin_tiempo").setSelected(true);
        button("permitir-deshacer").setSelected(true);

        button("tiempo-cinco_minutos").setSelected(true);

        assertTrue(button("permitir-deshacer").isDisable());
        assertFalse(button("permitir-deshacer").isSelected());
        ((Button) search("jugar")).fire();
        assertFalse(emitted.get(0).allowUndo());
    }

    @Test
    void optionNotIsRequiredForPlay() {
        chooseAllLessTime();
        button("tiempo-sin_tiempo").setSelected(true);
        assertFalse(search("jugar").isDisable());
    }

    @Test
    void modeFootballTravelsInConfiguration() {
        chooseAllLessTime();
        button("modo-futbol").setSelected(true);
        button("tiempo-sin_tiempo").setSelected(true);

        ((Button) search("jugar")).fire();

        assertEquals(GameConfiguration.Mode.FUTBOL, emitted.get(0).mode());
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.control.Spinner<Integer> limit() {
        return (javafx.scene.control.Spinner<Integer>) search("limite-jugadas");
    }

    @Test
    void limitOfMovesOnlyEnablesInModeFootball() {
        assertTrue(limit().isDisable());
        button("modo-futbol").setSelected(true);
        assertFalse(limit().isDisable());
        button("modo-clasico").setSelected(true);
        assertTrue(limit().isDisable());
    }

    @Test
    void limitWrittenTravelsInConfiguration() {
        chooseAllLessTime();
        button("modo-futbol").setSelected(true);
        button("tiempo-sin_tiempo").setSelected(true);
        limit().getEditor().setText("40");

        ((Button) search("jugar")).fire();

        assertEquals(40, emitted.get(0).moveLimit());
    }

    @Test
    void aLimitOutsideOfRangeAdjustsAndATextInvalidDiscards() {
        chooseAllLessTime();
        button("modo-futbol").setSelected(true);
        button("tiempo-sin_tiempo").setSelected(true);

        limit().getEditor().setText("5000");
        ((Button) search("jugar")).fire();
        limit().getEditor().setText("muchas");
        ((Button) search("jugar")).fire();

        assertEquals(GameConfiguration.LIMIT_MAXIMUM, emitted.get(0).moveLimit());
        assertEquals(GameConfiguration.LIMIT_MAXIMUM, emitted.get(1).moveLimit());
    }

    // ---------------- Rival IA ----------------

    @Test
    void difficultyOnlyEnablesAgainstAiAndIsRequired() {
        chooseAllLessTime();
        button("tiempo-sin_tiempo").setSelected(true);
        assertTrue(button("dificultad-medio").isDisable());

        button("rival-ia").setSelected(true);

        assertFalse(button("dificultad-medio").isDisable());
        assertTrue(search("jugar").isDisable());
        assertTrue(((javafx.scene.control.Label) menu.lookup("#selecciones-pendientes")).getText().contains("dificultad"));
    }

    @Test
    void difficultyChosenTravelsInConfiguration() {
        chooseAllLessTime();
        button("tiempo-sin_tiempo").setSelected(true);
        button("rival-ia").setSelected(true);
        button("dificultad-dificil").setSelected(true);

        ((Button) search("jugar")).fire();

        assertEquals(GameConfiguration.Rival.IA, emitted.getFirst().rival());
        assertEquals(java.util.Optional.of(core.ai.Difficulty.DIFICIL), emitted.getFirst().difficulty());
    }

    @Test
    void returnToChooseFriendDeletesDifficulty() {
        chooseAllLessTime();
        button("tiempo-sin_tiempo").setSelected(true);
        button("rival-ia").setSelected(true);
        button("dificultad-facil").setSelected(true);

        button("rival-amigo").setSelected(true);

        assertFalse(button("dificultad-facil").isSelected());
        ((Button) search("jugar")).fire();
        assertTrue(emitted.getFirst().difficulty().isEmpty());
    }

    // ---------------- Portada y sonido ----------------

    @Test
    void onOpenCharacterSwaysSomeSecondsAndReturnsToStayStill() {
        var laugh = menu.animateCharacter();
        var character = search("personaje");

        assertEquals(javafx.animation.Animation.Status.RUNNING, laugh.getStatus());
        double seconds = laugh.getTotalDuration().toSeconds();
        assertTrue(seconds > 2 && seconds < 4, "dura " + seconds + " s");
        laugh.stop();
        laugh.getOnFinished().handle(null);
        assertEquals(0, character.getRotate());
        assertEquals(1, character.getScaleX());
    }

    @Test
    void buttonOfSoundOfMenuUsesSameControlThatGame() {
        var control = new adapters.audio.VolumeControl();
        menu.configureSound(control);
        Button silence = (Button) menu.lookup("#silencio-menu");

        silence.fire();

        assertTrue(control.muted());
        assertEquals("Sonido: no", silence.getAccessibleText());
    }

    @Test
    void playAOptionNotifiesAndSelectionsOfOwnMenuNot() {
        int[] notices = {0};
        menu.onChooseOption(() -> notices[0]++);

        button("modo-clasico").fire();
        button("jugador-1-argentina").fire();
        button("rival-amigo").setSelected(true);   // lo hace el programa, no un clic: no avisa

        assertEquals(2, notices[0]);
    }
    @Test
    void selectionUsualOnlyLeavesPendingTeams() {
        menu.preselect(adapters.configuration.MenuSelection.usual());
        assertTrue(button("modo-clasico").isSelected());
        assertTrue(button("tiempo-diez_minutos").isSelected());
        assertTrue(button("rival-amigo").isSelected());
        assertTrue(button("color-aleatorio").isSelected());
        assertFalse(button("permitir-deshacer").isSelected());
        assertTrue(search("jugar").isDisable());
        assertEquals("Falta elegir: equipo de Jugador 1, equipo de Jugador 2.",
                ((javafx.scene.control.Label) search("selecciones-pendientes")).getText());

        button("jugador-1-boca").setSelected(true);
        button("jugador-2-river").setSelected(true);
        ((Button) search("jugar")).fire();
        assertEquals(GameConfiguration.Colors.ALEATORIO, emitted.getFirst().colors());
        assertEquals(GameConfiguration.Time.DIEZ_MINUTOS, emitted.getFirst().time());
    }

    @Test
    void aMenuNewRecoversLastSelectionComplete() {
        var previous = new GameConfiguration(GameConfiguration.Mode.FUTBOL, GameConfiguration.Time.SIN_TIEMPO,
                GameConfiguration.Rival.IA, GameConfiguration.Team.RIVER, GameConfiguration.Team.BOCA,
                GameConfiguration.Colors.JUGADOR_2_BLANCAS, true, 120,
                java.util.Optional.of(core.ai.Difficulty.DIFICIL));
        menu.preselect(adapters.configuration.MenuSelection.de(previous));
        ((Button) search("jugar")).fire();
        assertEquals(List.of(previous), emitted);
    }

    @Test
    void volumeOfMenuChangesControlShared() {
        var control = new adapters.audio.VolumeControl();
        menu.configureSound(control);
        ((javafx.scene.control.Slider) menu.lookup("#volumen-menu")).setValue(0.2);
        assertEquals(0.2, control.volume(), 1e-9);
    }
    @Test
    void silenceCarriesVolumeToZeroAndRaiseItItReactivates() {
        var control = new adapters.audio.VolumeControl();
        menu.configureSound(control);
        Button silence = (Button) menu.lookup("#silencio-menu");
        var volume = (javafx.scene.control.Slider) menu.lookup("#volumen-menu");
        double before = volume.getValue();

        silence.fire();
        assertEquals(0, volume.getValue());
        assertEquals("Sonido: no", silence.getAccessibleText());

        volume.setValue(0.4);
        assertFalse(control.muted());
        assertEquals("Sonido: sí", silence.getAccessibleText());

        silence.fire();
        silence.fire();
        assertEquals(0.4, volume.getValue(), 1e-9);
        assertNotEquals(before, volume.getValue());
    }

    @Test
    void theFooterHasAnExitButtonThatDoesNothingUntilItIsConnected() {
        Button exit = (Button) menu.lookup("#salir");
        assertNotNull(exit);
        assertEquals("SALIR", exit.getText());
        assertDoesNotThrow(exit::fire);
    }

    @Test
    void touchingExitCallsWhatWasConnected() {
        int[] calls = {0};
        menu.onExit(() -> calls[0]++);
        ((Button) menu.lookup("#salir")).fire();
        assertEquals(1, calls[0]);
    }
}
