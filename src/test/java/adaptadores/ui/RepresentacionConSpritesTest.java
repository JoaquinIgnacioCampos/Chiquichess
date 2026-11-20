package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.piezas.Peon;
import Nucleo.piezas.Pieza;
import Nucleo.piezas.Rey;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Los archivos de prueba: rey-blanco.png mide 2x2 y rey-negro.png 4x4; el resto no existe. */
class RepresentacionConSpritesTest {
    private final TemaTablero tema = new TemaTablero(Color.BEIGE, Color.BROWN, Color.WHITE, Color.BLACK,
            Color.BLACK, Color.GRAY);
    private final CatalogoSprites catalogo = new CatalogoSprites("/sprites-de-prueba");
    private final RepresentacionPiezas representacion = new RepresentacionConSprites(
            Map.of(Bando.BLANCO, catalogo, Bando.NEGRO, catalogo), new RepresentacionUnicode());

    @BeforeAll
    static void iniciarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaIniciado) {
            // otro test ya lo inició
        }
    }

    /** El sprite va dentro de un contenedor del tamaño de una casilla: se busca la imagen adentro. */
    private static ImageView imagenDe(javafx.scene.Node figura) {
        return assertInstanceOf(ImageView.class, figura.lookup(".image-view"));
    }

    @Test
    void elSpriteSobresaleHaciaArribaYApoyaSuBaseEnLaCasilla() {
        var figura = representacion.crear(new Rey(Bando.BLANCO), tema);
        var imagen = imagenDe(figura);
        assertEquals(RepresentacionPiezas.LADO_BASE, figura.prefWidth(-1));
        assertEquals(RepresentacionPiezas.LADO_BASE, figura.prefHeight(-1));
        assertTrue(imagen.getFitHeight() > RepresentacionPiezas.LADO_BASE);
        assertTrue(imagen.getLayoutY() < 0);
        assertTrue(imagen.getLayoutY() + imagen.getFitHeight() <= RepresentacionPiezas.LADO_BASE);
    }

    @Test
    void cadaColorUsaSuPropiaVersionDelSprite() {
        ImageView blanco = imagenDe(representacion.crear(new Rey(Bando.BLANCO), tema));
        ImageView negro = imagenDe(representacion.crear(new Rey(Bando.NEGRO), tema));
        assertEquals(2, blanco.getImage().getWidth());
        assertEquals(4, negro.getImage().getWidth());
    }

    @Test
    void elCatalogoCargaCadaImagenUnaSolaVez() {
        assertSame(catalogo.sprite('R', Bando.BLANCO).orElseThrow(), catalogo.sprite('R', Bando.BLANCO).orElseThrow());
    }

    @Test
    void siFaltaElSpriteUsaLaRepresentacionDeRespaldo() {
        Text figura = assertInstanceOf(Text.class, representacion.crear(new Peon(Bando.BLANCO), tema));
        assertEquals("♟", figura.getText());
    }

    @Test
    void unBandoSinCatalogoUsaLaRepresentacionDeRespaldo() {
        RepresentacionPiezas soloBlancas = new RepresentacionConSprites(
                Map.of(Bando.BLANCO, catalogo), new RepresentacionUnicode());
        imagenDe(soloBlancas.crear(new Rey(Bando.BLANCO), tema));
        assertInstanceOf(Text.class, soloBlancas.crear(new Rey(Bando.NEGRO), tema));
    }

    @Test
    void unaPiezaNuevaSinArchivoNoRompeLaVista() {
        Pieza nueva = new Pieza(Bando.BLANCO, new Nucleo.movimientos.ReglaCompuesta()) {
            @Override public String nombre() { return "Nueva"; }
            @Override public char codigo() { return 'X'; }
            @Override public int valor() { return 0; }
        };
        assertEquals("X", assertInstanceOf(Text.class, representacion.crear(nueva, tema)).getText());
    }

    @Test
    void cadaClubBuscaEnSuPropiaCarpeta() {
        for (Equipo equipo : Equipo.values()) {
            assertTrue(CatalogoSprites.de(equipo).sprite('X', Bando.BLANCO).isEmpty());
        }
    }

    @Test
    void elSpriteConservaLaDescripcionYNoTapaLosClicsDeLaCasilla() {
        Tablero tablero = new Tablero(2, 2);
        tablero.colocar(new Posicion(0, 0), new Rey(Bando.BLANCO));
        List<Posicion> pulsadas = new ArrayList<>();
        VistaTablero vista = new VistaTablero(tablero, tema, representacion, pulsadas::add);
        StackPane casilla = (StackPane) vista.lookup("#casilla-0-0");
        javafx.scene.Node sprite = casilla.getChildren().getFirst();
        imagenDe(sprite);
        assertTrue(sprite.isMouseTransparent());
        assertEquals("Rey BLANCO", sprite.getAccessibleText());
        casilla.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, true, null));
        assertEquals(List.of(new Posicion(0, 0)), pulsadas);
    }
}
