package adapters.audio;

import adapters.configuration.GameConfiguration.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SoundCatalogTest {

    @TempDir
    Path folder;

    private SoundCatalog catalog() {
        return new SoundCatalog(Optional.of(folder), false);
    }

    private void create(String relative) throws IOException {
        Path file = folder.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{0});
    }

    private static String path(Optional<URL> url) {
        return url.map(u -> u.getPath().substring(u.getPath().indexOf("/comunes/") >= 0
                ? u.getPath().indexOf("/comunes/") + 1 : u.getPath().indexOf("/clubes/") + 1)).orElse("ninguno");
    }

    @Test
    void goalOfClubHasPriorityOnCommon() throws IOException {
        // Arrange
        create("comunes/gol.wav");
        create("clubes/boca/gol.wav");

        // Act + Assert
        assertEquals("clubes/boca/gol.wav", path(catalog().search(Sound.GOAL, Optional.of(Team.BOCA))));
        assertEquals("comunes/gol.wav", path(catalog().search(Sound.GOAL, Optional.of(Team.RIVER))));
    }

    @Test
    void soundsCommonNotLookFolderOfClub() throws IOException {
        // Arrange: aunque el club tenga un movimiento propio, el movimiento es igual para todos
        create("comunes/movimiento.wav");
        create("clubes/boca/movimiento.wav");

        // Act + Assert
        assertEquals("comunes/movimiento.wav", path(catalog().search(Sound.MOVEMENT, Optional.of(Team.BOCA))));
    }

    @Test
    void ifMissingASoundUsesItsAlternative() throws IOException {
        // Arrange
        create("comunes/movimiento.wav");

        // Act + Assert
        assertEquals("comunes/movimiento.wav", path(catalog().search(Sound.CASTLING, Optional.empty())));
    }

    @Test
    void ifNotHasNoFileNotSounds() {
        assertTrue(catalog().search(Sound.GOAL, Optional.of(Team.BOCA)).isEmpty());
        assertTrue(catalog().search(Sound.CAPTURE, Optional.empty()).isEmpty());
    }

    @Test
    void acceptsAiffAndPrefersWav() throws IOException {
        // Arrange
        create("comunes/jaque.aiff");
        create("comunes/tablas.aiff");
        create("comunes/tablas.wav");

        // Act + Assert
        assertEquals("comunes/jaque.aiff", path(catalog().search(Sound.CHECK, Optional.empty())));
        assertEquals("comunes/tablas.wav", path(catalog().search(Sound.DRAW, Optional.empty())));
    }
}
