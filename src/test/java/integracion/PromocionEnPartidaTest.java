package integracion;

import Nucleo.bando.Bando;
import Nucleo.juego.Jugador;
import Nucleo.juego.Movimiento;
import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.juego.Partida;
import Nucleo.jugadas.HuellaDePosicion;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tablero.Tablero;
import Nucleo.tiempo.SinReloj;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/** La promoción jugada en partidas reales, con las reglas clásicas y el deshacer habilitado. */
class PromocionEnPartidaTest {

    /** Peón blanco en a7 a punto de promover; torre negra en b8 para poder capturar al promover. */
    private Partida conPeonEnA7() {
        Tablero tablero = TableroFake.desde(
                ".t..r...",
                "P.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");
        return new Partida(tablero, new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                ReglasDePartida.clasicas().conDeshacer(), new SinReloj());
    }

    @Test
    void sinElegirElPeonSeConvierteEnDama() {
        // Arrange
        Partida partida = conPeonEnA7();

        // Act
        partida.mover(mov("a7", "a8"));

        // Assert
        Pieza nueva = partida.tablero().piezaEn(pos("a8")).orElseThrow();
        assertEquals('D', nueva.codigo());
        assertEquals(Bando.BLANCO, nueva.color());
        assertEquals(List.of(Movimiento.conPromocion(pos("a7"), pos("a8"), 'D')), partida.historial());
    }

    @Test
    void sePuedeElegirOtraPieza() {
        // Arrange
        Partida partida = conPeonEnA7();

        // Act
        partida.mover(Movimiento.conPromocion(pos("a7"), pos("a8"), 'C'));

        // Assert
        assertEquals('C', partida.tablero().piezaEn(pos("a8")).orElseThrow().codigo());
    }

    @Test
    void sePuedePromoverCapturando() {
        // Arrange
        Partida partida = conPeonEnA7();

        // Act
        var resultado = partida.mover(Movimiento.conPromocion(pos("a7"), pos("b8"), 'T'));

        // Assert
        assertTrue(resultado.piezaCapturada().orElseThrow().esTorre());
        assertEquals('T', partida.tablero().piezaEn(pos("b8")).orElseThrow().codigo());
        assertEquals(Bando.BLANCO, partida.tablero().piezaEn(pos("b8")).orElseThrow().color());
    }

    @Test
    void noSePuedePromoverAUnaPiezaQueNoEstaEntreLasOpciones() {
        // Arrange
        Partida partida = conPeonEnA7();

        // Act
        MovimientoInvalidoException rechazo = assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(Movimiento.conPromocion(pos("a7"), pos("a8"), 'R')));

        // Assert
        assertEquals(MovimientoInvalidoException.Motivo.MOVIMIENTO_NO_PERMITIDO, rechazo.motivo());
        assertTrue(partida.tablero().piezaEn(pos("a7")).orElseThrow().esPeon());
    }

    @Test
    void pedirPromocionEnUnaJugadaQueNoPromueveSeRechaza() {
        // Arrange
        Partida partida = conPeonEnA7();

        // Act + Assert
        assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(Movimiento.conPromocion(pos("e1"), pos("e2"), 'D')));
    }

    @Test
    void laDamaNuevaPuedeDarJaque() {
        // Arrange: el rey negro está en e8, en la fila de promoción
        Partida partida = conPeonEnA7();

        // Act: el peón captura la torre de b8 y se convierte en dama, que ataca e8 por la fila
        partida.mover(Movimiento.conPromocion(pos("a7"), pos("b8"), 'D'));

        // Assert
        assertEquals(EstadoPartida.JAQUE, partida.estado());
    }

    @Test
    void lasOpcionesDePromocionSonLasCuatroPiezasEnOrden() {
        // Arrange
        Partida partida = conPeonEnA7();

        // Act
        List<Character> avanzando = codigos(partida.opcionesDePromocion(pos("a7"), pos("a8")));
        List<Character> capturando = codigos(partida.opcionesDePromocion(pos("a7"), pos("b8")));

        // Assert
        assertEquals(List.of('D', 'T', 'A', 'C'), avanzando);
        assertEquals(List.of('D', 'T', 'A', 'C'), capturando);
        assertTrue(partida.opcionesDePromocion(pos("a7"), pos("a8")).stream()
                .allMatch(pieza -> pieza.color() == Bando.BLANCO));
    }

    @Test
    void sinPromocionNoHayOpciones() {
        // Arrange
        Partida partida = conPeonEnA7();

        // Assert: una jugada común, un destino imposible y una pieza que no es del turno
        assertTrue(partida.opcionesDePromocion(pos("e1"), pos("e2")).isEmpty());
        assertTrue(partida.opcionesDePromocion(pos("a7"), pos("a6")).isEmpty());
        assertTrue(partida.opcionesDePromocion(pos("b8"), pos("b1")).isEmpty());
    }

    private static List<Character> codigos(List<Pieza> piezas) {
        return piezas.stream().map(Pieza::codigo).toList();
    }

    @Test
    void deshacerLaPromocionDevuelveElPeon() {
        // Arrange
        Partida partida = conPeonEnA7();
        Pieza peon = partida.tablero().piezaEn(pos("a7")).orElseThrow();
        long huellaAntes = HuellaDePosicion.de(partida.tablero(), partida.turno());
        partida.mover(Movimiento.conPromocion(pos("a7"), pos("b8"), 'D'));

        // Act
        partida.deshacer();

        // Assert
        assertSame(peon, partida.tablero().piezaEn(pos("a7")).orElseThrow());
        assertTrue(partida.tablero().piezaEn(pos("b8")).orElseThrow().esTorre());
        assertEquals(huellaAntes, HuellaDePosicion.de(partida.tablero(), partida.turno()));
    }
}
