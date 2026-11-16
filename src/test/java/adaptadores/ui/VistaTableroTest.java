package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.piezas.*;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import adaptadores.EnsambladorPartida;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

class VistaTableroTest {
    private final TemaTablero tema = new TemaTablero(
            javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
            javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
            javafx.scene.paint.Color.DARKSLATEGRAY, javafx.scene.paint.Color.GRAY);

    private StackPane casilla(javafx.scene.Node vista, int columna, int fila) {
        return (StackPane) vista.lookup("#casilla-" + columna + "-" + fila);
    }

    @Test
    void usaLasDimensionesRealesInclusoEnTablerosRectangulares() {
        VistaTablero vista = new VistaTablero(new Tablero(10, 6), tema, new RepresentacionPiezas());
        assertEquals(60, vista.lookupAll(".casilla").size());
        assertNotNull(casilla(vista, 9, 5));
        assertNull(casilla(vista, 10, 5));
        assertNull(casilla(vista, 9, 6));
        assertEquals(5, GridPane.getRowIndex(casilla(vista, 0, 0)));
        assertEquals(0, GridPane.getRowIndex(casilla(vista, 0, 5)));
        assertEquals(9, GridPane.getColumnIndex(casilla(vista, 9, 5)));
    }

    @Test
    void alternaDesdeLasCoordenadasDelNucleoConA1Oscura() {
        VistaTablero vista = new VistaTablero(new Tablero(3, 5), tema, new RepresentacionPiezas());
        assertEquals(javafx.scene.paint.Color.BROWN,
                casilla(vista, 0, 0).getBackground().getFills().getFirst().getFill());
        assertEquals(javafx.scene.paint.Color.BEIGE,
                casilla(vista, 1, 0).getBackground().getFills().getFirst().getFill());
        assertEquals(javafx.scene.paint.Color.BEIGE,
                casilla(vista, 0, 1).getBackground().getFills().getFirst().getFill());
        assertEquals(javafx.scene.paint.Color.BROWN,
                casilla(vista, 2, 4).getBackground().getFills().getFirst().getFill());
    }

    @Test
    void cambiaColoresAlInyectarOtroTemaSinModificarLaVista() {
        TemaTablero otroTema = new TemaTablero(javafx.scene.paint.Color.YELLOW,
                javafx.scene.paint.Color.BLUE, javafx.scene.paint.Color.WHITE,
                javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.BLACK,
                javafx.scene.paint.Color.NAVY);
        VistaTablero vista = new VistaTablero(new Tablero(2, 2), otroTema, new RepresentacionPiezas());
        assertEquals(javafx.scene.paint.Color.BLUE,
                casilla(vista, 0, 0).getBackground().getFills().getFirst().getFill());
        assertEquals(javafx.scene.paint.Color.YELLOW,
                casilla(vista, 1, 0).getBackground().getFills().getFirst().getFill());
    }

    @Test
    void blancasAbajoYNegrasArribaSegunLasPiezasDelNucleo() {
        Tablero tablero = new Tablero(4, 6);
        tablero.colocar(new Posicion(0, 0), new Rey(Bando.BLANCO));
        tablero.colocar(new Posicion(3, 5), new Rey(Bando.NEGRO));
        VistaTablero vista = new VistaTablero(tablero, tema, new RepresentacionPiezas());
        assertEquals("♔", ((Text) casilla(vista, 0, 0).getChildren().getFirst()).getText());
        assertEquals("♚", ((Text) casilla(vista, 3, 5).getChildren().getFirst()).getText());
        assertEquals(5, GridPane.getRowIndex(casilla(vista, 0, 0)));
        assertEquals(0, GridPane.getRowIndex(casilla(vista, 3, 5)));
    }

    @Test
    void redibujarConsultaElEstadoActualEnLugarDeUnaCopiaLocal() {
        var partida = new EnsambladorPartida().crearSinReloj();
        VistaPartida vista = new VistaPartida(new ControladorTablero(partida), tema,
                new RepresentacionPiezas(), TemaPanel.marcador());
        assertFalse(casilla(vista, 4, 1).getChildren().isEmpty());
        assertTrue(casilla(vista, 4, 3).getChildren().isEmpty());
        partida.mover(mov("e2", "e4"));
        vista.redibujar();
        assertTrue(casilla(vista, 4, 1).getChildren().isEmpty());
        assertEquals("♙", ((Text) casilla(vista, 4, 3).getChildren().getFirst()).getText());
        assertEquals(64, vista.lookupAll(".casilla").size());
    }

    @Test
    void alRedimensionarConservaCasillasCuadradasYElTableroCentrado() {
        VistaTablero vista = new VistaTablero(new Tablero(10, 6), tema, new RepresentacionPiezas());
        vista.resize(700, 700);
        vista.layout();
        GridPane grilla = (GridPane) vista.getChildrenUnmodifiable().getFirst();
        assertEquals(700, grilla.getWidth(), 0.01);
        assertEquals(420, grilla.getHeight(), 0.01);
        assertEquals(0, grilla.getLayoutX(), 0.01);
        assertEquals(140, grilla.getLayoutY(), 0.01);
        assertEquals(casilla(vista, 0, 0).getWidth(), casilla(vista, 0, 0).getHeight(), 0.01);
    }

    @Test
    void dibujaLaPelotaSoloEnSuCasilla() {
        Tablero tablero = new Tablero(8, 8);
        tablero.colocarPelota(new Posicion(3, 4));
        VistaTablero vista = new VistaTablero(tablero, tema, new RepresentacionPiezas());
        assertEquals(1, vista.lookupAll(".pelota").size());
        assertNotNull(casilla(vista, 3, 4).lookup("#pelota"));
    }

    @Test
    void laPelotaSigueALaPiezaQueLaLleva() {
        Tablero tablero = new Tablero(8, 8);
        tablero.colocar(new Posicion(3, 4), new Caballo(Bando.BLANCO));
        tablero.colocarPelota(new Posicion(3, 4));
        VistaTablero vista = new VistaTablero(tablero, tema, new RepresentacionPiezas());
        StackPane conPelota = casilla(vista, 3, 4);
        assertNotNull(conPelota.lookup("#pelota"));
        assertEquals(2, conPelota.getChildren().size()); // la pieza y la pelota
    }

    @Test
    void elAjedrezClasicoNoTienePelota() {
        VistaTablero vista = new VistaTablero(new Tablero(8, 8), tema, new RepresentacionPiezas());
        assertTrue(vista.lookupAll(".pelota").isEmpty());
    }

    @Test
    void marcaLasCasillasDelArco() {
        Tablero tablero = new Tablero(8, 8);
        tablero.marcarArco(new Posicion(3, 7), Bando.NEGRO);
        tablero.marcarArco(new Posicion(4, 0), Bando.BLANCO);
        VistaTablero vista = new VistaTablero(tablero, tema, new RepresentacionPiezas());
        assertEquals(2, vista.lookupAll(".arco").size());
        assertNotNull(casilla(vista, 3, 7).lookup("#arco-3-7"));
        assertNotNull(casilla(vista, 4, 0).lookup("#arco-4-0"));
    }

    @Test
    void elArcoQuedaDebajoDeLaPieza() {
        Tablero tablero = new Tablero(8, 8);
        tablero.marcarArco(new Posicion(4, 7), Bando.NEGRO);
        tablero.colocar(new Posicion(4, 7), new Rey(Bando.NEGRO));
        VistaTablero vista = new VistaTablero(tablero, tema, new RepresentacionPiezas());
        StackPane casilla = casilla(vista, 4, 7);
        assertTrue(casilla.getChildren().get(0).getStyleClass().contains("arco"));
        assertEquals(2, casilla.getChildren().size());
    }

    @Test
    void unDestinoOcupadoPorUnCompanieroSeMarcaComoPase() {
        Tablero tablero = new Tablero(8, 8);
        tablero.colocar(new Posicion(0, 0), new Torre(Bando.BLANCO));
        tablero.colocar(new Posicion(0, 4), new Caballo(Bando.BLANCO));
        tablero.colocar(new Posicion(0, 6), new Caballo(Bando.NEGRO));
        VistaTablero vista = new VistaTablero(tablero, tema, new RepresentacionPiezas());
        vista.redibujar(java.util.Optional.of(new Posicion(0, 0)),
                java.util.Set.of(new Posicion(0, 4), new Posicion(0, 6)));
        assertEquals(1, casilla(vista, 0, 4).lookupAll(".indicador-pase").size());
        assertTrue(casilla(vista, 0, 4).lookupAll(".indicador-captura").isEmpty());
        assertEquals(1, casilla(vista, 0, 6).lookupAll(".indicador-captura").size());
    }
}
