package integracion;

import Nucleo.bando.Bando;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Jugador;
import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.juego.Partida;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tiempo.SinReloj;
import adaptadores.EnsambladorPartida;
import org.junit.jupiter.api.Test;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/** El enroque jugado en partidas reales, con las reglas clásicas y el deshacer habilitado. */
class EnroqueEnPartidaTest {

    private Partida partida() {
        return new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("Beto", Bando.NEGRO), ReglasDePartida.clasicas().conDeshacer(), new SinReloj());
    }

    private void jugar(Partida partida, String... jugadas) {
        for (String jugada : jugadas) {
            partida.mover(mov(jugada.substring(0, 2), jugada.substring(2, 4)));
        }
    }

    /** Los dos bandos sacan el caballo y el alfil del flanco de rey: quedan listos para el enroque corto. */
    private static final String[] FLANCOS_DE_REY_LIBRES = {"e2e4", "e7e5", "g1f3", "g8f6", "f1c4", "f8c5"};

    @Test
    void elReyMuestraElEnroqueEntreSusDestinos() {
        // Arrange
        Partida partida = partida();
        jugar(partida, FLANCOS_DE_REY_LIBRES);

        // Act + Assert
        assertTrue(partida.movimientosLegales(pos("e1")).contains(pos("g1")));
    }

    @Test
    void ambosBandosEnrocanCorto() {
        // Arrange
        Partida partida = partida();
        jugar(partida, FLANCOS_DE_REY_LIBRES);
        Pieza torreBlanca = partida.tablero().piezaEn(pos("h1")).orElseThrow();

        // Act
        jugar(partida, "e1g1", "e8g8");

        // Assert
        assertTrue(partida.tablero().piezaEn(pos("g1")).orElseThrow().esRey());
        assertSame(torreBlanca, partida.tablero().piezaEn(pos("f1")).orElseThrow());
        assertTrue(partida.tablero().piezaEn(pos("g8")).orElseThrow().esRey());
        assertTrue(partida.tablero().piezaEn(pos("f8")).orElseThrow().esTorre());
        assertEquals(mov("e8", "g8"), partida.historial().getLast());
        assertEquals(Bando.BLANCO, partida.turno());
    }

    @Test
    void lasBlancasEnrocanLargo() {
        // Arrange
        Partida partida = partida();
        jugar(partida, "d2d4", "d7d5", "b1c3", "b8c6", "c1f4", "c8f5", "d1d2", "d8d7");

        // Act
        jugar(partida, "e1c1");

        // Assert
        assertTrue(partida.tablero().piezaEn(pos("c1")).orElseThrow().esRey());
        assertTrue(partida.tablero().piezaEn(pos("d1")).orElseThrow().esTorre());
        assertTrue(partida.tablero().piezaEn(pos("a1")).isEmpty());
    }

    @Test
    void deshacerElEnroqueDevuelveReyYTorreYElDerechoAEnrocar() {
        // Arrange
        Partida partida = partida();
        jugar(partida, FLANCOS_DE_REY_LIBRES);
        jugar(partida, "e1g1");

        // Act
        partida.deshacer();

        // Assert
        assertTrue(partida.tablero().piezaEn(pos("e1")).orElseThrow().esRey());
        assertTrue(partida.tablero().piezaEn(pos("h1")).orElseThrow().esTorre());
        assertTrue(partida.movimientosLegales(pos("e1")).contains(pos("g1")));
    }

    @Test
    void siElReyFueYVolvioPierdeElDerechoAEnrocar() {
        // Arrange
        Partida partida = partida();
        jugar(partida, FLANCOS_DE_REY_LIBRES);
        jugar(partida, "e1e2", "f6g8", "e2e1", "g8f6");

        // Act
        MovimientoInvalidoException rechazo = assertThrows(MovimientoInvalidoException.class,
                () -> jugar(partida, "e1g1"));

        // Assert
        assertEquals(MovimientoInvalidoException.Motivo.MOVIMIENTO_NO_PERMITIDO, rechazo.motivo());
        assertFalse(partida.movimientosLegales(pos("e1")).contains(pos("g1")));
    }

    @Test
    void laPartidaArmadaPorElEnsambladorPermiteEnrocar() {
        // Arrange
        Partida partida = new EnsambladorPartida().crearSinReloj();
        jugar(partida, FLANCOS_DE_REY_LIBRES);

        // Act
        jugar(partida, "e1g1");

        // Assert
        assertTrue(partida.tablero().piezaEn(pos("g1")).orElseThrow().esRey());
    }
}
