package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.juego.AccionInvalidaException.Motivo;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.*;
import Nucleo.tablero.Posicion;
import Nucleo.tiempo.RelojAjedrez;
import Nucleo.tiempo.RelojPartida;
import Nucleo.tiempo.SinReloj;
import dobles.FuenteDeTiempoFalsa;
import dobles.MockCondicionConMemoria;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Deshacer y rehacer en Partida. Se usa el criterio del Command: la foto de la partida después de
 * deshacer es idéntica a la de antes de jugar, y después de rehacer, idéntica a la de después de jugar.
 */
class DeshacerRehacerTest {

    /** Todo lo observable de una partida en un momento. */
    private record Foto(Map<Posicion, Pieza> piezas, Bando turno, EstadoPartida estado,
                        Optional<ResultadoPartida> resultado, List<Movimiento> historial) {
    }

    private static Foto foto(Partida partida) {
        return new Foto(new HashMap<>(partida.tablero().piezas()), partida.turno(), partida.estado(),
                partida.resultado(), partida.historial());
    }

    private Partida partida(ReglasDePartida reglas, RelojPartida reloj) {
        return new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO), reglas, reloj);
    }

    /** Ajedrez clásico con deshacer habilitado y sin límite de tiempo. */
    private Partida conDeshacer() {
        return partida(ReglasDePartida.clasicas().conDeshacer(), new SinReloj());
    }

    private void jugar(Partida partida, String... jugadas) {
        for (String jugada : jugadas) {
            partida.mover(mov(jugada.substring(0, 2), jugada.substring(2, 4)));
        }
    }

    private AccionInvalidaException.Motivo rechazo(Runnable accion) {
        return assertThrows(AccionInvalidaException.class, accion::run).motivo();
    }

    // ---------------- Deshacer y rehacer ----------------

    @Test
    void deshacerUnaJugadaDejaLaPartidaExactamenteComoAntes() {
        // Arrange
        Partida partida = conDeshacer();
        jugar(partida, "e2e4");
        Foto antes = foto(partida);
        jugar(partida, "e7e5");

        // Act
        partida.deshacer();

        // Assert
        assertEquals(antes, foto(partida));
    }

    @Test
    void deshacerUnaCapturaDevuelveLaPiezaASuCasilla() {
        // Arrange
        Partida partida = conDeshacer();
        jugar(partida, "e2e4", "d7d5");
        Pieza peonNegro = partida.tablero().piezaEn(pos("d5")).orElseThrow();
        jugar(partida, "e4d5");

        // Act
        partida.deshacer();

        // Assert
        assertSame(peonNegro, partida.tablero().piezaEn(pos("d5")).orElseThrow());
        assertEquals(32, partida.tablero().piezas().size());
        assertEquals(Bando.BLANCO, partida.turno());
    }

    @Test
    void sePuedeDeshacerHastaElPrincipioYNoMas() {
        // Arrange
        Partida partida = conDeshacer();
        Foto inicial = foto(partida);
        jugar(partida, "e2e4", "e7e5", "g1f3");

        // Act
        partida.deshacer();
        partida.deshacer();
        partida.deshacer();

        // Assert
        assertEquals(inicial, foto(partida));
        assertFalse(partida.puedeDeshacer());
        assertEquals(Motivo.NADA_PARA_DESHACER, rechazo(partida::deshacer));
    }

    @Test
    void cualquieraDeLosDosJugadoresPuedeDeshacer() {
        // Arrange: juegan las blancas; ahora le toca a las negras, que deshacen la jugada rival
        Partida partida = conDeshacer();
        jugar(partida, "e2e4");

        // Act
        partida.deshacer();

        // Assert
        assertEquals(Bando.BLANCO, partida.turno());
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void rehacerVuelveAEjecutarLaJugadaDeshecha() {
        // Arrange
        Partida partida = conDeshacer();
        jugar(partida, "e2e4", "d7d5", "e4d5");
        Foto despues = foto(partida);
        partida.deshacer();

        // Act
        partida.rehacer();

        // Assert
        assertEquals(despues, foto(partida));
        assertFalse(partida.puedeRehacer());
        assertEquals(Motivo.NADA_PARA_REHACER, rechazo(partida::rehacer));
    }

    @Test
    void sePuedenRehacerVariasJugadasEnOrden() {
        // Arrange
        Partida partida = conDeshacer();
        jugar(partida, "e2e4", "e7e5", "g1f3");
        Foto despues = foto(partida);
        partida.deshacer();
        partida.deshacer();
        partida.deshacer();

        // Act
        partida.rehacer();
        partida.rehacer();
        partida.rehacer();

        // Assert
        assertEquals(despues, foto(partida));
    }

    @Test
    void unaJugadaNuevaBorraLoQueSePodiaRehacer() {
        // Arrange
        Partida partida = conDeshacer();
        jugar(partida, "e2e4", "e7e5");
        partida.deshacer();
        assertTrue(partida.puedeRehacer());

        // Act: las negras eligen otra jugada
        jugar(partida, "c7c5");

        // Assert
        assertFalse(partida.puedeRehacer());
        assertEquals(Motivo.NADA_PARA_REHACER, rechazo(partida::rehacer));
        assertEquals(List.of(mov("e2", "e4"), mov("c7", "c5")), partida.historial());
    }

    // ---------------- Finales ----------------

    @Test
    void deshacerElJaqueMateReanudaLaPartidaYRehacerloVuelveATerminarla() {
        // Arrange: mate del loco
        Partida partida = conDeshacer();
        jugar(partida, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals(EstadoPartida.JAQUE_MATE, partida.estado());
        assertTrue(partida.puedeDeshacer());

        // Act
        partida.deshacer();

        // Assert
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
        assertTrue(partida.resultado().isEmpty());
        assertTrue(partida.ganador().isEmpty());
        assertEquals(Bando.NEGRO, partida.turno());

        // Act + Assert: rehacer
        partida.rehacer();
        assertEquals(EstadoPartida.JAQUE_MATE, partida.estado());
        assertEquals("Beto", partida.ganador().orElseThrow().nombre());
    }

    @Test
    void despuesDeDeshacerElMateSePuedeJugarOtraCosa() {
        // Arrange
        Partida partida = conDeshacer();
        jugar(partida, "f2f3", "e7e5", "g2g4", "d8h4");
        partida.deshacer();

        // Act
        jugar(partida, "d8g5");

        // Assert
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
        assertFalse(partida.puedeRehacer());
    }

    @Test
    void noSeDeshaceUnaRendicion() {
        // Arrange
        Partida partida = conDeshacer();
        jugar(partida, "e2e4");
        partida.rendirse(Bando.NEGRO);

        // Act + Assert
        assertFalse(partida.puedeDeshacer());
        assertEquals(Motivo.PARTIDA_FINALIZADA, rechazo(partida::deshacer));
        assertEquals(EstadoPartida.RENDICION, partida.estado());
    }

    @Test
    void noSeDeshaceUnAcuerdoDeTablas() {
        // Arrange
        Partida partida = conDeshacer();
        jugar(partida, "e2e4");
        partida.ofrecerTablas(Bando.NEGRO);
        partida.aceptarTablas(Bando.BLANCO);

        // Act + Assert
        assertEquals(Motivo.PARTIDA_FINALIZADA, rechazo(partida::deshacer));
    }

    // ---------------- Cuándo está permitido ----------------

    @Test
    void sinHabilitarloEnLasReglasNoSePuedeDeshacer() {
        // Arrange
        Partida partida = partida(ReglasDePartida.clasicas(), new SinReloj());
        jugar(partida, "e2e4");

        // Act + Assert
        assertFalse(partida.puedeDeshacer());
        assertEquals(Motivo.DESHACER_NO_PERMITIDO, rechazo(partida::deshacer));
        assertEquals(Motivo.DESHACER_NO_PERMITIDO, rechazo(partida::rehacer));
    }

    @Test
    void conRelojNoSePuedeDeshacerAunqueLasReglasLoPermitan() {
        // Arrange
        RelojPartida reloj = new RelojAjedrez(Duration.ofMinutes(5), Duration.ofMinutes(5), new FuenteDeTiempoFalsa());
        Partida partida = partida(ReglasDePartida.clasicas().conDeshacer(), reloj);
        jugar(partida, "e2e4");

        // Act + Assert
        assertFalse(partida.puedeDeshacer());
        assertEquals(Motivo.DESHACER_NO_PERMITIDO, rechazo(partida::deshacer));
    }

    @Test
    void deshacerYRehacerBorranLaOfertaDeTablasPendiente() {
        // Arrange
        Partida partida = conDeshacer();
        jugar(partida, "e2e4");
        partida.ofrecerTablas(Bando.NEGRO);

        // Act + Assert: deshacer
        partida.deshacer();
        assertTrue(partida.ofertaTablasPendiente().isEmpty());

        // Act + Assert: rehacer
        partida.ofrecerTablas(Bando.BLANCO);
        partida.rehacer();
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
    }

    // ---------------- Condiciones con memoria ----------------

    @Test
    void deshacerYRehacerNoInventanUnaRepeticion() {
        // Arrange: la posición inicial aparece por segunda vez
        Partida partida = conDeshacer();
        jugar(partida, "g1f3", "g8f6", "f3g1", "f6g8");

        // Act: ir y volver con deshacer y rehacer no la cuenta de nuevo
        partida.deshacer();
        partida.rehacer();

        // Assert
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());

        // Act + Assert: una vuelta real más sí es la tercera
        jugar(partida, "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(EstadoPartida.TABLAS_REPETICION, partida.estado());
    }

    @Test
    void deshacerLasTablasPorRepeticionReanudaLaPartida() {
        // Arrange
        Partida partida = conDeshacer();
        jugar(partida, "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(EstadoPartida.TABLAS_REPETICION, partida.estado());

        // Act
        partida.deshacer();

        // Assert
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
    }

    @Test
    void laReglaDeLos50MovimientosRecuperaSuCuentaAlDeshacer() {
        // Arrange: límite reducido a 1 movimiento por jugador (2 jugadas sueltas)
        Partida partida = partida(new ReglasDePartida(new DetectorDeJaque(), List.of(
                new JaqueMate(), new Ahogado(), new TablasPorReglaDeLos50Movimientos(1))).conDeshacer(), new SinReloj());
        jugar(partida, "g1f3", "e7e6");    // el peón reinicia la cuenta

        // Act: deshacer el peón devuelve la cuenta a 1; otra jugada suelta llega a 2
        partida.deshacer();
        jugar(partida, "g8f6");

        // Assert
        assertEquals(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS, partida.estado());
    }

    @Test
    void cadaCondicionRecuperaSuPropioRecuerdoEnOrdenInverso() {
        // Arrange: dos condiciones con memoria que anotan en qué orden se las restaura
        List<String> orden = new ArrayList<>();
        MockCondicionConMemoria primera = new MockCondicionConMemoria() {
            @Override
            public void restaurar(Recuerdo recuerdo) {
                orden.add("primera");
                super.restaurar(recuerdo);
            }
        };
        MockCondicionConMemoria segunda = new MockCondicionConMemoria() {
            @Override
            public void restaurar(Recuerdo recuerdo) {
                orden.add("segunda");
                super.restaurar(recuerdo);
            }
        };
        Partida partida = partida(new ReglasDePartida(new DetectorDeJaque(), List.of(primera, segunda)).conDeshacer(),
                new SinReloj());
        jugar(partida, "e2e4");

        // Act
        partida.deshacer();

        // Assert: cada una recibe el recuerdo que entregó al registrar e2e4 (el segundo registro)
        assertEquals(List.of(primera.entregados().get(1)), primera.restaurados());
        assertEquals(List.of(segunda.entregados().get(1)), segunda.restaurados());
        assertEquals(List.of("segunda", "primera"), orden);
    }
}
