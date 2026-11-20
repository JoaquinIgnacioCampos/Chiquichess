package adapters.ui;

import adapters.configuration.GameConfiguration.Team;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Todo equipo tiene escudo y colores propios, y se puede elegir en el menú. */
class ClubPresentationTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // otro test ya lo inició
        }
    }

    @Test
    void eachTeamHasItsCrestAndTheirColors() {
        for (Team team : Team.values()) {
            ClubPresentation club = ClubPresentation.de(team, PanelTheme.scoreboard());
            assertFalse(club.crest().isError(), team.name());
            assertTrue(club.crest().getWidth() > 0, team.name());
            assertNotNull(club.theme().accent(), team.name());
        }
    }

    @Test
    void crestCallsAsTeamInLowercase() {
        assertEquals("argentina.png", ClubPresentation.crestFile(Team.ARGENTINA));
        assertEquals("Argentina", Team.ARGENTINA.displayName());
    }

    @Test
    void argentinaCanChooseForTwoPlayers() {
        GameMenuView menu = new GameMenuView(PanelTheme.scoreboard(), configuration -> { });
        var options = menu;
        assertNotNull(options.lookup("#jugador-1-argentina"));
        assertNotNull(options.lookup("#jugador-2-argentina"));
    }
}
