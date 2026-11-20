package adapters.audio;

import core.side.Side;
import core.game.GameInProgress;
import adapters.GameAssembler;
import adapters.configuration.GameConfiguration.Team;
import adapters.configuration.GameConfiguration.Mode;
import doubles.PlayerFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static doubles.Squares.move;
import static org.junit.jupiter.api.Assertions.*;

/** El adaptador de sonido enganchado a partidas reales, con un reproductor que no hace ruido. */
class GameSoundsTest {

    @TempDir
    Path folder;
    private final PlayerFalse player = new PlayerFalse();
    private final VolumeControl control = new VolumeControl();
    private static final Map<Side, Team> TEAMS = Map.of(Side.WHITE, Team.BOCA, Side.BLACK, Team.RIVER);

    @BeforeEach
    void soundsCommon() throws IOException {
        for (Sound sound : Sound.values()) {
            create("comunes/" + sound.file() + ".wav");
        }
    }

    private void create(String relative) throws IOException {
        Path file = folder.resolve("sonidos").resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{0});
    }

    private GameSounds connect(GameInProgress game, boolean football) {
        return GameSounds.connect(game, new SoundCatalog(Optional.of(folder.resolve("sonidos")), false), player,
                TEAMS, control, football);
    }

    private static GameInProgress classic() {
        return new GameAssembler().createWithoutClockWithUndo(Mode.CLASICO);
    }

    private static GameInProgress football() {
        return new GameAssembler().createWithoutClockWithUndo(Mode.FUTBOL, 10);
    }

    private static void play(GameInProgress game, String... moves) {
        for (String move : moves) {
            game.move(move(move.substring(0, 2), move.substring(2, 4)));
        }
    }

    @Test
    void eachMovePlaysItsSound() {
        // Arrange
        GameInProgress game = classic();
        connect(game, false);

        // Act: mate del loco
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");

        // Assert: el silbato de arranque, tres movimientos, el que da mate y la victoria de las negras (sin
        // archivo propio de River)
        assertEquals(List.of("comunes/silbato-inicial.wav", "comunes/movimiento.wav", "comunes/movimiento.wav",
                "comunes/movimiento.wav", "comunes/movimiento.wav", "comunes/victoria.wav"), player.played);
    }

    @Test
    void victorySoundsWithFileOfClubWinner() throws IOException {
        // Arrange
        create("clubes/river/victoria.wav");
        create("clubes/boca/victoria.wav");
        GameInProgress game = classic();
        connect(game, false);

        // Act: ganan las negras, que son River
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");

        // Assert
        assertEquals("clubes/river/victoria.wav", player.played.getLast());
    }

    @Test
    void classicNotHasCrowdAndOnlyStartsWithWhistle() {
        // Act
        connect(classic(), false);

        // Assert
        assertTrue(player.tracks.isEmpty());
        assertEquals(List.of("comunes/silbato-inicial.wav"), player.played);
    }

    @Test
    void footballStartsWithWhistleAndACrowdIfTwoClubsShare() {
        // Act
        connect(football(), true);

        // Assert: una sola pista, porque ninguno tiene hinchada propia
        assertEquals(List.of("comunes/silbato-inicial.wav"), player.played);
        assertEquals(1, player.sounding().size());
    }

    @Test
    void eachClubWithCrowdOwnHasItsTrack() throws IOException {
        // Arrange
        create("clubes/boca/hinchada.wav");
        create("clubes/river/hinchada.wav");

        // Act
        connect(football(), true);

        // Assert
        assertEquals(List.of("clubes/boca/hinchada.wav", "clubes/river/hinchada.wav"),
                player.sounding().stream().map(p -> p.file).toList());
    }

    @Test
    void crowdRisesWithMovesAndDownToUndo() {
        // Arrange
        GameInProgress game = football();
        connect(game, true);
        PlayerFalse.TrackFalse crowd = player.sounding().getFirst();
        double onStart = crowd.volume;

        // Act + Assert
        play(game, "e2e4", "e7e5", "g1f3");
        double after = crowd.volume;
        assertTrue(after > onStart);
        game.undo();
        assertTrue(crowd.volume < after);
    }

    @Test
    void onEndSilencesCrowdAndToCloseDetaches() {
        // Arrange
        GameInProgress game = football();
        GameSounds sounds = connect(game, true);

        // Act
        game.resign(Side.WHITE);

        // Assert: silbato final y victoria de River; la hinchada se detuvo
        assertEquals(List.of("comunes/silbato-inicial.wav", "comunes/silbato-final.wav", "comunes/victoria.wav"),
                player.played);
        assertTrue(player.sounding().isEmpty());
        sounds.close();
        int before = player.played.size();
        assertThrows(RuntimeException.class, () -> game.move(move("e2", "e4")));
        assertEquals(before, player.played.size());
    }

    @Test
    void silenceCarriesAllToZeroAndRecoversWithVolumeChosen() {
        // Arrange
        GameInProgress game = football();
        connect(game, true);
        PlayerFalse.TrackFalse crowd = player.sounding().getFirst();
        control.changeVolume(0.5);
        double withVolume = crowd.volume;

        // Act
        control.mute(true);
        play(game, "e2e4");

        // Assert
        assertEquals(0.0, crowd.volume);
        assertEquals(0.0, player.volumes.getLast());
        control.mute(false);
        assertTrue(crowd.volume > 0 && withVolume > 0);
    }

    @Test
    void closeLeavesOfListenVolume() {
        // Arrange
        GameSounds sounds = connect(football(), true);
        PlayerFalse.TrackFalse crowd = player.tracks.getFirst();

        // Act
        sounds.close();
        double onClose = crowd.volume;
        control.changeVolume(0.1);

        // Assert
        assertTrue(crowd.stopped);
        assertEquals(onClose, crowd.volume);
    }

    @Test
    void chooseAPieceSoundsAClickMoreSoftThatAMovement() {
        // Arrange
        GameSounds sounds = connect(classic(), false);

        // Act
        sounds.selectedPiece();

        // Assert: tras el silbato de arranque, el clic suave
        assertEquals(List.of("comunes/silbato-inicial.wav", "comunes/seleccion.wav"), player.played);
        assertEquals(control.effective() * GameSounds.VOLUME_SELECTION, player.volumes.getLast(), 1e-9);
    }
}
