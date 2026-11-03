package adaptadores.ui;

import Nucleo.juego.Posicion;
import Nucleo.piezas.*;
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
        tablero.colocar(new Posicion(0, 0), new Rey(Color.BLANCO));
        tablero.colocar(new Posicion(3, 5), new Rey(Color.NEGRO));
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
}
