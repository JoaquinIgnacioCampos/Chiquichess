package adaptadores.ui;

import adaptadores.audio.ControlDeVolumen;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** La pantalla con la que arranca el juego: logo, botón para comenzar y control de sonido. */
class PantallaDeIntroTest {

    @BeforeAll
    static void iniciarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaIniciado) {
            // otro test ya lo inició
        }
    }

    private int comenzadas;
    private final PantallaDeIntro pantalla =
            new PantallaDeIntro(TemaPanel.marcador(), new ControlDeVolumen(), () -> comenzadas++);

    @Test
    void muestraElLogoYUnBotonParaComenzar() {
        ImageView logo = assertInstanceOf(ImageView.class, pantalla.lookup("#logo-intro"));
        assertFalse(logo.getImage().isError());
        Button boton = assertInstanceOf(Button.class, pantalla.lookup("#comenzar"));
        assertEquals("COMENZAR", boton.getText());
    }

    @Test
    void tocarComenzarAvisaUnaSolaVez() {
        Button boton = (Button) pantalla.lookup("#comenzar");
        boton.fire();
        boton.fire();
        assertEquals(1, comenzadas);
    }

    @Test
    void tieneElControlDeSonidoParaSilenciarElVideo() {
        assertNotNull(pantalla.lookup("#silencio-intro"));
        assertNotNull(pantalla.lookup("#volumen-intro"));
    }

    @Test
    void laPrimeraPasadaDelAudioArrancaEnteraYTodasTerminanEnSilencio() {
        assertEquals(1, PantallaDeIntro.ganancia(0, 10, false), 1e-9);
        assertEquals(1, PantallaDeIntro.ganancia(5, 10, false), 1e-9);
        assertEquals(0, PantallaDeIntro.ganancia(10, 10, false), 1e-9);
    }

    @Test
    void lasPasadasSiguientesEntranConUnFundido() {
        assertEquals(0, PantallaDeIntro.ganancia(0, 10, true), 1e-9);
        assertEquals(1, PantallaDeIntro.ganancia(PantallaDeIntro.FUNDIDO, 10, true), 1e-9);
        double mitad = PantallaDeIntro.ganancia(PantallaDeIntro.FUNDIDO / 2, 10, true);
        assertTrue(mitad > 0 && mitad < 1);
    }

    @Test
    void alSolaparseElFinDeUnaPasadaConElInicioDeLaSiguienteLaPotenciaSeMantiene() {
        for (double t = 0; t <= PantallaDeIntro.FUNDIDO; t += 0.1) {
            double sale = PantallaDeIntro.ganancia(10 - PantallaDeIntro.FUNDIDO + t, 10, false);
            double entra = PantallaDeIntro.ganancia(t, 10, true);
            assertEquals(1, sale * sale + entra * entra, 1e-9, "t=" + t);
        }
    }

    @Test
    void sinReproducirNoHayVideoYDetenerNoFalla() {
        assertNull(((javafx.scene.media.MediaView) pantalla.lookup("#video-intro")).getMediaPlayer());
        assertDoesNotThrow(pantalla::detener);
    }
}
