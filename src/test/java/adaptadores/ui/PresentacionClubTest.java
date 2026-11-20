package adaptadores.ui;

import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Todo equipo tiene escudo y colores propios, y se puede elegir en el menú. */
class PresentacionClubTest {

    @BeforeAll
    static void iniciarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaIniciado) {
            // otro test ya lo inició
        }
    }

    @Test
    void cadaEquipoTieneSuEscudoYSusColores() {
        for (Equipo equipo : Equipo.values()) {
            PresentacionClub club = PresentacionClub.de(equipo, TemaPanel.marcador());
            assertFalse(club.escudo().isError(), equipo.name());
            assertTrue(club.escudo().getWidth() > 0, equipo.name());
            assertNotNull(club.tema().acento(), equipo.name());
        }
    }

    @Test
    void elEscudoSeLlamaComoElEquipoEnMinusculas() {
        assertEquals("argentina.png", PresentacionClub.archivoDelEscudo(Equipo.ARGENTINA));
        assertEquals("Argentina", Equipo.ARGENTINA.nombre());
    }

    @Test
    void argentinaSePuedeElegirParaLosDosJugadores() {
        VistaMenuPartida menu = new VistaMenuPartida(TemaPanel.marcador(), configuracion -> { });
        var opciones = menu;
        assertNotNull(opciones.lookup("#jugador-1-argentina"));
        assertNotNull(opciones.lookup("#jugador-2-argentina"));
    }
}
