package integracion;

import Nucleo.juego.*;
import Nucleo.juego.MovimientoInvalidoException.Motivo;
import Nucleo.piezas.Color;
import Nucleo.reglas.*;
import Nucleo.tablero.Tablero;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración: todas las clases reales trabajando juntas, armadas igual que en Main.
 * No hay dobles; se juegan partidas cortas y se verifica el resultado de punta a punta.
 */
class PartidaCompletaTest {

    private final Jugador blanco = new Jugador("Jugador 1", Color.BLANCO);
    private final Jugador negro = new Jugador("Jugador 2", Color.NEGRO);

    private Partida partidaEstandar() {
        return nuevaPartida(new DisposicionEstandar().crearTablero(), condicionesEstandar());
    }

    private Partida nuevaPartida(Tablero tablero, List<CondicionDeFinalizacion> condiciones) {
        return new Partida(tablero, blanco, negro, new DetectorDeJaque(), condiciones);
    }

    private List<CondicionDeFinalizacion> condicionesEstandar() {
        return List.of(new JaqueMate(), new Ahogado(),
                new TablasPorRepeticion(), new TablasPorReglaDeLos50Movimientos());
    }

    /** Juega una secuencia de jugadas escritas como "e2e4". Devuelve el resultado de la última. */
    private ResultadoMovimiento jugar(Partida partida, String... jugadas) {
        ResultadoMovimiento ultimo = null;
        for (String jugada : jugadas) {
            ultimo = partida.mover(mov(jugada.substring(0, 2), jugada.substring(2, 4)));
        }
        return ultimo;
    }

    @Test
    void mateDelLocoGananLasNegras() {
        // Arrange
        Partida partida = partidaEstandar();

        // Act
        ResultadoMovimiento resultado = jugar(partida, "f2f3", "e7e5", "g2g4", "d8h4");

        // Assert
        assertEquals(EstadoPartida.JAQUE_MATE, resultado.estado());
        assertEquals(Optional.of(negro), partida.ganador());
    }

    @Test
    void mateDelPastorGananLasBlancasCapturandoEnF7() {
        // Arrange
        Partida partida = partidaEstandar();

        // Act
        ResultadoMovimiento resultado = jugar(partida,
                "e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7");

        // Assert
        assertTrue(resultado.piezaCapturada().orElseThrow().esPeon());
        assertEquals(EstadoPartida.JAQUE_MATE, partida.estado());
        assertEquals(Optional.of(blanco), partida.ganador());
    }

    @Test
    void despuesDelMateNoSePuedeSeguirJugando() {
        // Arrange
        Partida partida = partidaEstandar();
        jugar(partida, "f2f3", "e7e5", "g2g4", "d8h4");

        // Act
        MovimientoInvalidoException e = assertThrows(MovimientoInvalidoException.class,
                () -> jugar(partida, "a2a3"));

        // Assert
        assertEquals(Motivo.PARTIDA_FINALIZADA, e.motivo());
    }

    @Test
    void unaCapturaSacaLaPiezaYPasaElTurno() {
        // Arrange
        Partida partida = partidaEstandar();
        jugar(partida, "e2e4", "d7d5");

        // Act
        ResultadoMovimiento resultado = jugar(partida, "e4d5");

        // Assert
        assertTrue(resultado.piezaCapturada().orElseThrow().esPeon());
        assertEquals(Color.NEGRO, resultado.piezaCapturada().get().color());
        assertEquals(31, partida.tablero().piezas().size());
        assertEquals(Color.NEGRO, partida.turno());
        assertEquals(EstadoPartida.EN_CURSO, resultado.estado());
    }

    @Test
    void enJaqueSoloSeAceptanJugadasQueLoResuelven() {
        // Arrange: la dama blanca en h5 da jaque por la diagonal abierta e8-h5
        Partida partida = partidaEstandar();
        jugar(partida, "e2e4", "f7f6", "d1h5");
        assertEquals(EstadoPartida.JAQUE, partida.estado());

        // Act: una jugada que ignora el jaque
        MovimientoInvalidoException e = assertThrows(MovimientoInvalidoException.class,
                () -> jugar(partida, "a7a6"));

        // Assert
        assertEquals(Motivo.DEJA_AL_REY_EN_JAQUE, e.motivo());
        assertEquals(Color.NEGRO, partida.turno());

        // Act: tapar el jaque con el peón
        ResultadoMovimiento resultado = jugar(partida, "g7g6");

        // Assert
        assertEquals(EstadoPartida.EN_CURSO, resultado.estado());
    }

    @Test
    void lasPiezasRespetanSusReglasDentroDeUnaPartidaReal() {
        // Arrange
        Partida partida = partidaEstandar();

        // Act + Assert: el alfil no puede saltar sus propios peones
        MovimientoInvalidoException e = assertThrows(MovimientoInvalidoException.class,
                () -> jugar(partida, "c1e3"));
        assertEquals(Motivo.MOVIMIENTO_NO_PERMITIDO, e.motivo());

        // Act + Assert: el caballo sí puede saltar
        assertDoesNotThrow(() -> jugar(partida, "g1f3"));
    }

    @Test
    void repetirTresVecesLaMismaPosicionSonTablas() {
        // Arrange: los caballos van y vuelven; la posición inicial se repite tras cada 4 jugadas
        Partida partida = partidaEstandar();
        jugar(partida, "g1f3", "g8f6", "f3g1", "f6g8");   // posición inicial por 2ª vez
        jugar(partida, "g1f3", "g8f6", "f3g1");
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());

        // Act
        ResultadoMovimiento resultado = jugar(partida, "f6g8"); // posición inicial por 3ª vez

        // Assert
        assertEquals(EstadoPartida.TABLAS_REPETICION, resultado.estado());
        assertTrue(partida.ganador().isEmpty());
    }

    @Test
    void sinCapturasNiMovimientosDePeonSeLlegaATablasPor50Movimientos() {
        // Arrange: límite reducido a 2 movimientos por jugador para no jugar 100 jugadas
        Partida partida = nuevaPartida(new DisposicionEstandar().crearTablero(),
                List.of(new JaqueMate(), new Ahogado(), new TablasPorReglaDeLos50Movimientos(2)));
        jugar(partida, "g1f3", "g8f6", "f3g1");
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());

        // Act
        ResultadoMovimiento resultado = jugar(partida, "f6g8");

        // Assert
        assertEquals(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS, resultado.estado());
    }

    @Test
    void unMovimientoDePeonReiniciaLaCuentaDeLos50Movimientos() {
        // Arrange
        Partida partida = nuevaPartida(new DisposicionEstandar().crearTablero(),
                List.of(new JaqueMate(), new Ahogado(), new TablasPorReglaDeLos50Movimientos(2)));
        jugar(partida, "g1f3", "g8f6", "f3g1");

        // Act: el peón reinicia la cuenta, y después hay solo 3 jugadas sueltas
        jugar(partida, "e7e6", "g1f3", "f6g8", "f3g1");

        // Assert
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
    }

    @Test
    void mateDelPasilloEnUnaPosicionArmada() {
        // Arrange
        Tablero tablero = dobles.TableroFake.desde(
                ".......r",
                "........",
                "......R.",
                "........",
                "........",
                "........",
                "........",
                "T.......");
        Partida partida = nuevaPartida(tablero, condicionesEstandar());

        // Act
        ResultadoMovimiento resultado = jugar(partida, "a1a8");

        // Assert
        assertEquals(EstadoPartida.JAQUE_MATE, resultado.estado());
        assertEquals(Optional.of(blanco), partida.ganador());
    }

    @Test
    void ahogadoEnUnaPosicionArmada() {
        // Arrange: rey negro en h8; la dama blanca va a f7 y lo deja sin jugadas pero sin jaque
        Tablero tablero = dobles.TableroFake.desde(
                ".......r",
                "....D...",
                "......R.",
                "........",
                "........",
                "........",
                "........",
                "........");
        Partida partida = nuevaPartida(tablero, condicionesEstandar());

        // Act
        ResultadoMovimiento resultado = jugar(partida, "e7f7");

        // Assert
        assertEquals(EstadoPartida.TABLAS_AHOGADO, resultado.estado());
        assertTrue(partida.ganador().isEmpty());
    }

    @Test
    void elReyNoPuedeCapturarUnaPiezaDefendida() {
        // Arrange: la torre negra en e2 está defendida por la torre negra en e8
        Tablero tablero = dobles.TableroFake.desde(
                "....t..r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....t...",
                "....R...");
        Partida partida = nuevaPartida(tablero, condicionesEstandar());
        assertEquals(EstadoPartida.JAQUE, partida.estado());

        // Act
        MovimientoInvalidoException e = assertThrows(MovimientoInvalidoException.class,
                () -> jugar(partida, "e1e2"));

        // Assert
        assertEquals(Motivo.DEJA_AL_REY_EN_JAQUE, e.motivo());
        assertFalse(partida.movimientosLegales(pos("e1")).contains(pos("e2")));
    }
}
