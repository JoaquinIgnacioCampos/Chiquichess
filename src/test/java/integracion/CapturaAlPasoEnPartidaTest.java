package integracion;

import Nucleo.bando.Bando;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Jugador;
import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.juego.Partida;
import Nucleo.juego.ResultadoMovimiento;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tablero.Tablero;
import Nucleo.tiempo.SinReloj;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/** La captura al paso jugada en partidas reales, con las reglas clásicas y el deshacer habilitado. */
class CapturaAlPasoEnPartidaTest {

    private Partida partida(Tablero tablero) {
        return new Partida(tablero, new Jugador("Ana", Bando.BLANCO),
                new Jugador("Beto", Bando.NEGRO), ReglasDePartida.clasicas().conDeshacer(), new SinReloj());
    }

    private Partida desdeElPrincipio() {
        return partida(new DisposicionEstandar().crearTablero());
    }

    private ResultadoMovimiento jugar(Partida partida, String... jugadas) {
        ResultadoMovimiento ultimo = null;
        for (String jugada : jugadas) {
            ultimo = partida.mover(mov(jugada.substring(0, 2), jugada.substring(2, 4)));
        }
        return ultimo;
    }

    /** El peón blanco llega a e5 y el negro de d7 avanza dos casillas, quedando a su lado. */
    private static final String[] PEON_NEGRO_AL_LADO = {"e2e4", "a7a6", "e4e5", "d7d5"};

    @Test
    void elPeonCapturaAlPasoYElRivalSaleDeSuCasilla() {
        // Arrange
        Partida partida = desdeElPrincipio();
        jugar(partida, PEON_NEGRO_AL_LADO);
        Pieza peonNegro = partida.tablero().piezaEn(pos("d5")).orElseThrow();
        assertTrue(partida.movimientosLegales(pos("e5")).contains(pos("d6")));

        // Act
        ResultadoMovimiento resultado = jugar(partida, "e5d6");

        // Assert
        assertSame(peonNegro, resultado.piezaCapturada().orElseThrow());
        assertTrue(partida.tablero().piezaEn(pos("d5")).isEmpty());
        assertTrue(partida.tablero().piezaEn(pos("d6")).orElseThrow().esPeon());
        assertEquals(31, partida.tablero().piezas().size());
        assertEquals(mov("e5", "d6"), partida.historial().getLast());
    }

    @Test
    void siNoSeAprovechaEnseguidaSePierde() {
        // Arrange: las dos partes juegan otra cosa
        Partida partida = desdeElPrincipio();
        jugar(partida, PEON_NEGRO_AL_LADO);
        jugar(partida, "h2h3", "h7h6");

        // Act
        MovimientoInvalidoException rechazo = assertThrows(MovimientoInvalidoException.class,
                () -> jugar(partida, "e5d6"));

        // Assert
        assertEquals(MovimientoInvalidoException.Motivo.MOVIMIENTO_NO_PERMITIDO, rechazo.motivo());
    }

    @Test
    void deshacerLaCapturaDevuelveElPeonYLaPosibilidad() {
        // Arrange
        Partida partida = desdeElPrincipio();
        jugar(partida, PEON_NEGRO_AL_LADO);
        Pieza peonNegro = partida.tablero().piezaEn(pos("d5")).orElseThrow();
        jugar(partida, "e5d6");

        // Act
        partida.deshacer();

        // Assert
        assertSame(peonNegro, partida.tablero().piezaEn(pos("d5")).orElseThrow());
        assertTrue(partida.tablero().piezaEn(pos("e5")).orElseThrow().esPeon());
        assertEquals(32, partida.tablero().piezas().size());
        assertTrue(partida.movimientosLegales(pos("e5")).contains(pos("d6")));
    }

    @Test
    void noSePermiteSiAlSacarLosDosPeonesElReyQuedaExpuesto() {
        // Arrange: rey blanco en a5 y torre negra en h5. Si el peón de b5 captura al paso al de c5,
        // la fila 5 queda vacía y la torre da jaque.
        Partida partida = partida(TableroFake.desde(
                "....r...",
                "..p.....",
                "........",
                "RP.....t",
                "........",
                "........",
                ".......P",
                "........"));
        jugar(partida, "h2h3", "c7c5");

        // Act
        MovimientoInvalidoException rechazo = assertThrows(MovimientoInvalidoException.class,
                () -> jugar(partida, "b5c6"));

        // Assert
        assertEquals(MovimientoInvalidoException.Motivo.DEJA_AL_REY_EN_JAQUE, rechazo.motivo());
        assertFalse(partida.movimientosLegales(pos("b5")).contains(pos("c6")));
    }
}
