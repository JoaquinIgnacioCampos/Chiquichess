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
        return url.map(u -> u.getPath().substring(u.getPath().indexOf("/common/") >= 0
                ? u.getPath().indexOf("/common/") + 1 : u.getPath().indexOf("/clubs/") + 1)).orElse("none");
    }

    @Test
    void goalOfClubHasPriorityOnCommon() throws IOException {
        // Arrange
        create("common/goal.wav");
        create("clubs/boca/goal.wav");

        // Act + Assert
        assertEquals("clubs/boca/goal.wav", path(catalog().search(Sound.GOAL, Optional.of(Team.BOCA))));
        assertEquals("common/goal.wav", path(catalog().search(Sound.GOAL, Optional.of(Team.RIVER))));
    }

    @Test
    void soundsCommonNotLookFolderOfClub() throws IOException {
        // Arrange: aunque el club tenga un movimiento propio, el movimiento es igual para todos
        create("common/movement.wav");
        create("clubs/boca/movement.wav");

        // Act + Assert
        assertEquals("common/movement.wav", path(catalog().search(Sound.MOVEMENT, Optional.of(Team.BOCA))));
    }

    @Test
    void ifMissingASoundUsesItsAlternative() throws IOException {
        // Arrange
        create("common/movement.wav");

        // Act + Assert
        assertEquals("common/movement.wav", path(catalog().search(Sound.CASTLING, Optional.empty())));
    }

    @Test
    void ifNotHasNoFileNotSounds() {
        assertTrue(catalog().search(Sound.GOAL, Optional.of(Team.BOCA)).isEmpty());
        assertTrue(catalog().search(Sound.CAPTURE, Optional.empty()).isEmpty());
    }

    @Test
    void acceptsAiffAndPrefersWav() throws IOException {
        // Arrange
        create("common/check.aiff");
        create("common/draw.aiff");
        create("common/draw.wav");

        // Act + Assert
        assertEquals("common/check.aiff", path(catalog().search(Sound.CHECK, Optional.empty())));
        assertEquals("common/draw.wav", path(catalog().search(Sound.DRAW, Optional.empty())));
    }
}
