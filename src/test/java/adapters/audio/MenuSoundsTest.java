package adapters.audio;

import doubles.PlayerFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** La risa al abrir el juego y el ambiente del menú, con un reproductor que no hace ruido. */
class MenuSoundsTest {

    @TempDir
    Path folder;
    private final PlayerFalse player = new PlayerFalse();
    private final VolumeControl control = new VolumeControl();
    private MenuSounds sounds;

    @BeforeEach
    void prepare() throws IOException {
        for (String displayName : List.of("risa", "menu", "seleccion")) {
            Path file = folder.resolve("sonidos/comunes/" + displayName + ".wav");
            Files.createDirectories(file.getParent());
            Files.write(file, new byte[]{0});
        }
        sounds = new MenuSounds(new SoundCatalog(Optional.of(folder.resolve("sonidos")), false),
                player, control);
    }

    @Test
    void onOpenGameLaughsATimeAndStartsAmbient() {
        // Act
        sounds.openGame();

        // Assert
        assertEquals(List.of("comunes/risa.wav"), player.played);
        assertEquals(List.of("comunes/menu.wav"), player.sounding().stream().map(p -> p.file).toList());
    }

    @Test
    void laughNotRepeatsToReturnToMenu() {
        // Act: se abre, se juega una partida y se vuelve al menú dos veces
        sounds.openGame();
        sounds.leaveMenu();
        sounds.enterMenu();
        sounds.leaveMenu();
        sounds.openGame();

        // Assert
        assertEquals(1, player.played.size());
        assertEquals(1, player.sounding().size());
    }

    @Test
    void onStartAGameAmbientSilencesAndToReturnFollows() {
        // Arrange
        sounds.openGame();

        // Act + Assert
        sounds.leaveMenu();
        assertTrue(player.sounding().isEmpty());
        sounds.enterMenu();
        assertEquals(1, player.sounding().size());
    }

    @Test
    void ambientSoundsLowAndFollowsSilence() {
        // Arrange
        sounds.openGame();
        PlayerFalse.TrackFalse ambient = player.sounding().getFirst();

        // Act + Assert
        assertEquals(VolumeControl.DEFAULT_VOLUME * MenuSounds.VOLUME_AMBIENT, ambient.volume, 1e-9);
        control.mute(true);
        assertEquals(0.0, ambient.volume);
        control.mute(false);
        assertTrue(ambient.volume > 0);
    }

    @Test
    void withoutFilesNotSoundsNothingAndNotFails() {
        // Arrange
        MenuSounds withoutFiles = new MenuSounds(new SoundCatalog(Optional.empty(), false),
                player, control);

        // Act
        withoutFiles.openGame();

        // Assert
        assertTrue(player.played.isEmpty());
        assertTrue(player.tracks.isEmpty());
    }

    @Test
    void chooseAOptionSoundsSameClickThatChooseAPiece() {
        // Act
        sounds.chosenOption();

        // Assert
        assertEquals(List.of("comunes/seleccion.wav"), player.played);
        assertEquals(control.effective() * GameSounds.VOLUME_SELECTION, player.volumes.getFirst(), 1e-9);
    }
}
