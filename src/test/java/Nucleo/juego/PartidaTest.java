package Nucleo.juego;

import Nucleo.juego.MovimientoInvalidoException.Motivo;
import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;
import Nucleo.piezas.Torre;
import Nucleo.reglas.*;
import Nucleo.tablero.Tablero;
import dobles.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de Partida: sus colaboradores (detector de jaque, condiciones de fin,
 * movimientos legales) se reemplazan por dobles para probar solo la lógica de Partida.
 */
class PartidaTest {

    private final Jugador blanco = new Jugador("Ana", Color.BLANCO);
    private final Jugador negro = new Jugador("Beto", Color.NEGRO);

    private Partida partida(Tablero tablero, IDetectorDeJaque detector, CondicionDeFinalizacion... condiciones) {
        return new Partida(tablero, blanco, negro, detector, List.of(condiciones));
    }

    /** Partida desde la posición inicial, sin jaques ni condiciones de fin. */
    private Partida partidaSimple(CondicionDeFinalizacion... condiciones) {
        return partida(new DisposicionEstandar().crearTablero(), StubDetectorDeJaque.nuncaEnJaque(), condiciones);
    }

    // ---------------- Construcción ----------------

    @Test
    void rechazaJugadoresConLosColoresCambiados() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Partida(
                new DisposicionEstandar().crearTablero(), negro, blanco,
                StubDetectorDeJaque.nuncaEnJaque(), List.of()));
    }

    @Test
    void rechazaDependenciasNulas() {
        // Act + Assert
        assertThrows(NullPointerException.class, () -> new Partida(
                null, blanco, negro, StubDetectorDeJaque.nuncaEnJaque(), List.of()));
        assertThrows(NullPointerException.class, () -> new Partida(
                new DisposicionEstandar().crearTablero(), blanco, negro, null, List.of()));
    }

    @Test
    void sinJaqueNiCondicionesLaPartidaEmpiezaEnCurso() {
        // Act
        Partida partida = partidaSimple();

        // Assert
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
    }

    // ---------------- Turnos ----------------

    @Test
    void empiezanLasBlancas() {
        // Act
        Partida partida = partidaSimple();

        // Assert
        assertEquals(Color.BLANCO, partida.turno());
        assertEquals(blanco, partida.jugadorActual());
    }

    @Test
    void despuesDeUnMovimientoValidoLeTocaAlOtroJugador() {
        // Arrange
        Partida partida = partidaSimple();

        // Act
        partida.mover(mov("e2", "e4"));

        // Assert
        assertEquals(Color.NEGRO, partida.turno());
        assertEquals(negro, partida.jugadorActual());
    }

    @Test
    void losTurnosAlternanEntreAmbosJugadores() {
        // Arrange
        Partida partida = partidaSimple();

        // Act
        partida.mover(mov("e2", "e4"));
        partida.mover(mov("e7", "e5"));

        // Assert
        assertEquals(Color.BLANCO, partida.turno());
    }

    // ---------------- Ejecución del movimiento ----------------

    @Test
    void moverLlevaLaPiezaDelOrigenAlDestino() {
        // Arrange
        Partida partida = partidaSimple();
        Pieza peon = partida.tablero().piezaEn(pos("e2")).orElseThrow();

        // Act
        ResultadoMovimiento resultado = partida.mover(mov("e2", "e4"));

        // Assert
        assertTrue(partida.tablero().piezaEn(pos("e2")).isEmpty());
        assertSame(peon, partida.tablero().piezaEn(pos("e4")).orElseThrow());
        assertTrue(resultado.piezaCapturada().isEmpty());
        assertEquals(EstadoPartida.EN_CURSO, resultado.estado());
    }

    @Test
    void alCapturarDevuelveLaPiezaCapturadaYLaSacaDelTablero() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "t.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T.......");
        Pieza torreNegra = tablero.piezaEn(pos("a8")).orElseThrow();
        Partida partida = partida(tablero, StubDetectorDeJaque.nuncaEnJaque());

        // Act
        ResultadoMovimiento resultado = partida.mover(mov("a1", "a8"));

        // Assert
        assertEquals(Optional.of(torreNegra), resultado.piezaCapturada());
        assertEquals(Color.BLANCO, partida.tablero().piezaEn(pos("a8")).orElseThrow().color());
        assertEquals(1, partida.tablero().piezas().size());
    }

    // ---------------- Validaciones (cada Motivo de error) ----------------

    @Test
    void sinPiezaEnElOrigenLanzaSinPiezaEnOrigen() {
        // Arrange
        Partida partida = partidaSimple();

        // Act
        MovimientoInvalidoException e = assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(mov("e4", "e5")));

        // Assert
        assertEquals(Motivo.SIN_PIEZA_EN_ORIGEN, e.motivo());
    }

    @Test
    void moverUnaPiezaDelRivalLanzaNoEsSuTurno() {
        // Arrange
        Partida partida = partidaSimple();

        // Act
        MovimientoInvalidoException e = assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(mov("e7", "e5")));

        // Assert
        assertEquals(Motivo.NO_ES_SU_TURNO, e.motivo());
    }

    @Test
    void unDestinoQueLaPiezaNoAlcanzaLanzaMovimientoNoPermitido() {
        // Arrange
        Partida partida = partidaSimple();

        // Act
        MovimientoInvalidoException e = assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(mov("a1", "a3")));

        // Assert
        assertEquals(Motivo.MOVIMIENTO_NO_PERMITIDO, e.motivo());
    }

    @Test
    void siLaJugadaDejaAlReyEnJaqueLanzaDejaAlReyEnJaque() {
        // Arrange: el stub dice que el rey queda en jaque haga lo que haga
        Partida partida = partida(new DisposicionEstandar().crearTablero(), StubDetectorDeJaque.siempreEnJaque());

        // Act
        MovimientoInvalidoException e = assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(mov("e2", "e4")));

        // Assert
        assertEquals(Motivo.DEJA_AL_REY_EN_JAQUE, e.motivo());
    }

    @Test
    void conLaPartidaTerminadaLanzaPartidaFinalizada() {
        // Arrange: una condición que termina la partida apenas se crea
        Partida partida = partidaSimple(StubCondicion.siempre(EstadoPartida.TABLAS_AHOGADO));

        // Act
        MovimientoInvalidoException e = assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(mov("e2", "e4")));

        // Assert
        assertEquals(Motivo.PARTIDA_FINALIZADA, e.motivo());
    }

    @Test
    void unMovimientoInvalidoNoModificaNiElTableroNiElTurno() {
        // Arrange
        Partida partida = partidaSimple();
        Map<Posicion, Pieza> antes = new HashMap<>(partida.tablero().piezas());

        // Act
        assertThrows(MovimientoInvalidoException.class, () -> partida.mover(mov("a1", "a5")));

        // Assert
        assertEquals(antes, partida.tablero().piezas());
        assertEquals(Color.BLANCO, partida.turno());
    }

    // ---------------- Estado y condiciones de fin (stubs y mocks) ----------------

    @Test
    void siElReyEstaEnJaqueYNingunaCondicionTerminaElEstadoEsJaque() {
        // Act
        Partida partida = partida(new DisposicionEstandar().crearTablero(), StubDetectorDeJaque.siempreEnJaque());

        // Assert
        assertEquals(EstadoPartida.JAQUE, partida.estado());
    }

    @Test
    void elEstadoEsElQueDevuelveLaCondicionQueSeCumple() {
        // Act
        Partida partida = partidaSimple(StubCondicion.siempre(EstadoPartida.TABLAS_REPETICION));

        // Assert
        assertEquals(EstadoPartida.TABLAS_REPETICION, partida.estado());
        assertTrue(partida.estado().estaFinalizada());
    }

    @Test
    void lasCondicionesSeConsultanUnaVezAlCrearLaPartidaSinPiezaMovida() {
        // Arrange
        MockCondicion mock = new MockCondicion();

        // Act
        partidaSimple(mock);

        // Assert
        assertEquals(1, mock.cantidadDeLlamadas());
        assertTrue(mock.ultimoContexto().piezaMovida().isEmpty());
        assertEquals(Color.BLANCO, mock.ultimoContexto().turno());
    }

    @Test
    void despuesDeCadaJugadaLaCondicionRecibeElContextoCorrecto() {
        // Arrange
        MockCondicion mock = new MockCondicion();
        Partida partida = partidaSimple(mock);

        // Act
        partida.mover(mov("e2", "e4"));

        // Assert
        assertEquals(2, mock.cantidadDeLlamadas());
        ContextoDeJugada contexto = mock.ultimoContexto();
        assertEquals(Color.NEGRO, contexto.turno(), "El contexto es desde el punto de vista de quien juega ahora");
        assertTrue(contexto.piezaMovida().orElseThrow().esPeon());
        assertFalse(contexto.huboCaptura());
        assertFalse(contexto.enJaque());
        assertTrue(contexto.tieneMovimientosLegales());
    }

    @Test
    void laCondicionSeEnteraDeLasCapturas() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "t.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T.......");
        MockCondicion mock = new MockCondicion();
        Partida partida = partida(tablero, StubDetectorDeJaque.nuncaEnJaque(), mock);

        // Act
        partida.mover(mov("a1", "a8"));

        // Assert
        assertTrue(mock.ultimoContexto().huboCaptura());
    }

    @Test
    void unaJugadaInvalidaNoConsultaLasCondiciones() {
        // Arrange
        MockCondicion mock = new MockCondicion();
        Partida partida = partidaSimple(mock);

        // Act
        assertThrows(MovimientoInvalidoException.class, () -> partida.mover(mov("e2", "e5")));

        // Assert
        assertEquals(1, mock.cantidadDeLlamadas());
    }

    @Test
    void laPrimeraCondicionQueSeCumpleGanaYLasSiguientesNoSeConsultan() {
        // Arrange
        MockCondicion siguiente = new MockCondicion();

        // Act
        Partida partida = partidaSimple(StubCondicion.siempre(EstadoPartida.TABLAS_AHOGADO), siguiente);

        // Assert
        assertEquals(EstadoPartida.TABLAS_AHOGADO, partida.estado());
        assertEquals(0, siguiente.cantidadDeLlamadas());
    }

    @Test
    void siNingunaCondicionSeCumpleSeConsultanTodas() {
        // Arrange
        MockCondicion primera = new MockCondicion();
        MockCondicion segunda = new MockCondicion();

        // Act
        partidaSimple(primera, StubCondicion.nunca(), segunda);

        // Assert
        assertEquals(1, primera.cantidadDeLlamadas());
        assertEquals(1, segunda.cantidadDeLlamadas());
    }

    // ---------------- MovimientosLegales inyectado (stub) ----------------

    @Test
    void sinJugadasLegalesNiJaqueLaPartidaTerminaEnAhogado() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();
        IDetectorDeJaque detector = StubDetectorDeJaque.nuncaEnJaque();

        // Act
        Partida partida = new Partida(tablero, blanco, negro, detector,
                new StubMovimientosLegales(false), List.of(new JaqueMate(), new Ahogado()));

        // Assert
        assertEquals(EstadoPartida.TABLAS_AHOGADO, partida.estado());
    }

    @Test
    void enJaqueYSinJugadasLegalesLaPartidaTerminaEnJaqueMate() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();
        IDetectorDeJaque detector = StubDetectorDeJaque.siempreEnJaque();

        // Act
        Partida partida = new Partida(tablero, blanco, negro, detector,
                new StubMovimientosLegales(false), List.of(new JaqueMate(), new Ahogado()));

        // Assert
        assertEquals(EstadoPartida.JAQUE_MATE, partida.estado());
    }

    // ---------------- Ganador ----------------

    @Test
    void enJaqueMateGanaElJugadorQueNoTieneElTurno() {
        // Arrange: el mate se declara con el turno de las blancas, así que pierden ellas
        Partida partida = partidaSimple(StubCondicion.siempre(EstadoPartida.JAQUE_MATE));

        // Act
        Optional<Jugador> ganador = partida.ganador();

        // Assert
        assertEquals(Optional.of(negro), ganador);
    }

    @Test
    void enTablasNoHayGanador() {
        // Arrange
        Partida partida = partidaSimple(StubCondicion.siempre(EstadoPartida.TABLAS_REPETICION));

        // Act + Assert
        assertTrue(partida.ganador().isEmpty());
    }

    @Test
    void conLaPartidaEnCursoNoHayGanador() {
        // Arrange
        Partida partida = partidaSimple();

        // Act + Assert
        assertTrue(partida.ganador().isEmpty());
    }

    // ---------------- Consulta de movimientos legales ----------------

    @Test
    void devuelveLosDestinosLegalesDeUnaPiezaPropia() {
        // Arrange
        Partida partida = partidaSimple();

        // Act
        Set<Posicion> destinos = partida.movimientosLegales(pos("b1"));

        // Assert
        assertEquals(posiciones("a3", "c3"), destinos);
    }

    @Test
    void noDevuelveDestinosParaPiezasDelRivalNiCasillasVacias() {
        // Arrange
        Partida partida = partidaSimple();

        // Act + Assert
        assertTrue(partida.movimientosLegales(pos("b8")).isEmpty());
        assertTrue(partida.movimientosLegales(pos("e4")).isEmpty());
    }

    @Test
    void conLaPartidaTerminadaNoHayMovimientosLegales() {
        // Arrange
        Partida partida = partidaSimple(StubCondicion.siempre(EstadoPartida.TABLAS_AHOGADO));

        // Act
        Set<Posicion> destinos = partida.movimientosLegales(pos("b1"));

        // Assert
        assertTrue(destinos.isEmpty());
    }

    @Test
    void elTableroQueExponeEsDeSoloLectura() {
        // Arrange
        Partida partida = partidaSimple();

        // Act + Assert
        assertThrows(UnsupportedOperationException.class,
                () -> partida.tablero().piezas().put(pos("e4"), new Torre(Color.BLANCO)));
    }
}
