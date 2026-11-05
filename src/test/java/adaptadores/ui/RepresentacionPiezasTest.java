package adaptadores.ui;

import Nucleo.movimientos.ReglaCompuesta;
import Nucleo.piezas.*;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import Nucleo.bando.Bando;
class RepresentacionPiezasTest {
    private final TemaTablero tema = new TemaTablero(
            javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
            javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
            javafx.scene.paint.Color.DARKSLATEGRAY, javafx.scene.paint.Color.GRAY);
    private final RepresentacionPiezas representacion = new RepresentacionPiezas();

    @Test
    void traduceLosSeisTiposYAmbosColoresEnElSimboloCorrecto() {
        for (Bando color : Bando.values()) {
            List<Pieza> piezas = List.of(new Rey(color), new Reina(color), new Torre(color),
                    new Alfil(color), new Caballo(color), new Peon(color));
            List<String> esperados = color == Bando.BLANCO
                    ? List.of("♔", "♕", "♖", "♗", "♘", "♙")
                    : List.of("♚", "♛", "♜", "♝", "♞", "♟");
            for (int i = 0; i < piezas.size(); i++) {
                assertEquals(esperados.get(i), ((Text) representacion.crear(piezas.get(i), tema)).getText());
            }
        }
    }

    @Test
    void usaLosColoresDePiezasYContornoRecibidosEnElTema() {
        Text blanca = (Text) representacion.crear(new Torre(Bando.BLANCO), tema);
        Text negra = (Text) representacion.crear(new Torre(Bando.NEGRO), tema);
        assertEquals(javafx.scene.paint.Color.WHITE, blanca.getFill());
        assertEquals(javafx.scene.paint.Color.BLACK, negra.getFill());
        assertEquals(javafx.scene.paint.Color.DARKSLATEGRAY, blanca.getStroke());
        assertEquals(javafx.scene.paint.Color.DARKSLATEGRAY, negra.getStroke());
    }

    @Test
    void unaPiezaNuevaSePuedeMostrarConSuCodigoSinRomperLaVista() {
        Pieza nueva = new Pieza(Bando.BLANCO, new ReglaCompuesta()) {
            @Override public String nombre() { return "Nueva"; }
            @Override public char codigo() { return 'X'; }
        };
        assertEquals("X", ((Text) representacion.crear(nueva, tema)).getText());
    }
}
