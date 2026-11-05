package Nucleo.juego;

import Nucleo.piezas.Color;
import Nucleo.reglas.*;
import Nucleo.tiempo.RelojAjedrez;
import Nucleo.tiempo.RelojPartida;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Tablero;
import dobles.FuenteDeTiempoFalsa;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

class PartidaConRelojTest {
    private final FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();

    private Partida partida(CondicionDeFinalizacion... condiciones) {
        return conReloj(new RelojAjedrez(Duration.ofMinutes(5), Duration.ofMinutes(5), fuente), condiciones);
    }

    private Partida conReloj(RelojPartida reloj, CondicionDeFinalizacion... condiciones) {
        return new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Color.BLANCO), new Jugador("Beto", Color.NEGRO),
                new DetectorDeJaque(), List.of(condiciones), reloj);
    }

    private void avanzar(long segundos) {
        fuente.avanzar(Duration.ofSeconds(segundos));
    }

    private void assertTiempo(Partida partida, Color color, long segundos) {
        assertEquals(Optional.of(Duration.ofSeconds(segundos)), partida.tiempoRestante(color));
    }

    @Test
    void comienzaConBlancasYTrasMoverCorreElNegro() {
        Partida partida = partida();
        avanzar(30);
        assertTiempo(partida, Color.BLANCO, 270);
        assertTiempo(partida, Color.NEGRO, 300);
        partida.mover(mov("e2", "e4"));
        avanzar(10);
        assertTiempo(partida, Color.BLANCO, 270);
        assertTiempo(partida, Color.NEGRO, 290);
    }

    @Test
    void movimientoInvalidoNoCambiaNiReiniciaElReloj() {
        Partida partida = partida();
        avanzar(30);
        assertThrows(MovimientoInvalidoException.class, () -> partida.mover(mov("e2", "e5")));
        avanzar(10);
        assertEquals(Color.BLANCO, partida.turno());
        assertTiempo(partida, Color.BLANCO, 260);
        assertTiempo(partida, Color.NEGRO, 300);
    }

    @Test
    void exactoCeroVerificadoTerminaConVictoriaDelRival() {
        Partida partida = partida();
        avanzar(300);
        // Las lecturas son puras; finalizar es una orden explícita.
        assertTiempo(partida, Color.BLANCO, 0);
        assertTrue(partida.resultado().isEmpty());
        partida.verificarTiempo();
        assertEquals(EstadoPartida.TIEMPO_AGOTADO, partida.estado());
        assertEquals(new ResultadoPartida(Optional.of(Color.NEGRO), MotivoFinalizacion.TIEMPO_AGOTADO),
                partida.resultado().orElseThrow());
        assertEquals(Color.NEGRO, partida.ganador().orElseThrow().color());
        avanzar(60);
        partida.verificarTiempo();
        assertTiempo(partida, Color.BLANCO, 0);
        assertTiempo(partida, Color.NEGRO, 300);
    }

    @Test
    void negrasTambienPierdenPorTiempoDuranteSuTurno() {
        Partida partida = partida();
        partida.mover(mov("e2", "e4"));
        avanzar(301);
        partida.verificarTiempo();
        assertEquals(Color.BLANCO, partida.ganador().orElseThrow().color());
        assertTiempo(partida, Color.NEGRO, 0);
    }

    @Test
    void moverConTiempoVencidoFinalizaYNoEjecutaLaJugada() {
        Partida partida = partida();
        var tableroAntes = new HashMap<>(partida.tablero().piezas());
        avanzar(300);
        var error = assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(mov("e2", "e4")));
        assertEquals(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA, error.motivo());
        assertEquals(tableroAntes, partida.tablero().piezas());
        assertEquals(Color.BLANCO, partida.turno());
        assertEquals(MotivoFinalizacion.TIEMPO_AGOTADO, partida.resultado().orElseThrow().motivo());
    }

    @Test
    void siLaValidacionAgotaElTiempoSeRechazaAntesDeEjecutar() {
        IDetectorDeJaque detector = new DetectorDeJaque();
        MovimientosLegales validacion = new MovimientosLegales(detector) {
            @Override
            public boolean existeAlguno(Tablero tablero, Color color) {
                return true;
            }

            @Override
            public boolean dejaAlReyEnJaque(Tablero tablero, Pieza pieza, Posicion origen, Posicion destino) {
                fuente.avanzar(Duration.ofMinutes(5));
                return false;
            }
        };
        Partida partida = new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Color.BLANCO), new Jugador("Beto", Color.NEGRO),
                detector, validacion, List.of(),
                new RelojAjedrez(Duration.ofMinutes(5), Duration.ofMinutes(5), fuente));
        partida.ofrecerTablas(Color.BLANCO);
        var tableroAntes = new HashMap<>(partida.tablero().piezas());
        var error = assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(mov("e2", "e4")));
        assertEquals(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA, error.motivo());
        assertEquals(tableroAntes, partida.tablero().piezas());
        assertEquals(Color.BLANCO, partida.turno());
        assertTrue(partida.historial().isEmpty());
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        assertEquals(new ResultadoPartida(Optional.of(Color.NEGRO), MotivoFinalizacion.TIEMPO_AGOTADO),
                partida.resultado().orElseThrow());
        assertTiempo(partida, Color.BLANCO, 0);
        assertTiempo(partida, Color.NEGRO, 300);
    }

    @Test
    void verificarConTiempoDeSobraNoCambiaPartidaOfertaNiReloj() {
        Partida partida = partida();
        partida.ofrecerTablas(Color.BLANCO);
        avanzar(10);
        partida.verificarTiempo();
        partida.verificarTiempo();
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
        assertEquals(Optional.of(Color.BLANCO), partida.ofertaTablasPendiente());
        avanzar(10);
        assertTiempo(partida, Color.BLANCO, 280);
        assertTiempo(partida, Color.NEGRO, 300);
    }

    @Test
    void rendicionDetieneAmbosRelojesYNoSeReemplazaPorTiempo() {
        Partida partida = partida();
        avanzar(30);
        partida.rendirse(Color.BLANCO);
        avanzar(1000);
        partida.verificarTiempo();
        assertTiempo(partida, Color.BLANCO, 270);
        assertTiempo(partida, Color.NEGRO, 300);
        assertEquals(MotivoFinalizacion.RENDICION, partida.resultado().orElseThrow().motivo());
        assertEquals(Color.NEGRO, partida.ganador().orElseThrow().color());
    }

    @Test
    void acuerdoDetieneRelojMientrasQueOfertaYRechazoNoLoDetienen() {
        Partida partida = partida();
        avanzar(10);
        partida.ofrecerTablas(Color.BLANCO);
        avanzar(10);
        partida.rechazarTablas(Color.NEGRO);
        avanzar(10);
        partida.ofrecerTablas(Color.BLANCO);
        partida.aceptarTablas(Color.NEGRO);
        avanzar(100);
        assertTiempo(partida, Color.BLANCO, 270);
        assertTiempo(partida, Color.NEGRO, 300);
        assertEquals(MotivoFinalizacion.ACUERDO, partida.resultado().orElseThrow().motivo());
    }

    @Test
    void cadaFinalAutomaticoDetieneElRelojYBorraLaOferta() {
        for (EstadoPartida finalAutomatico : List.of(EstadoPartida.JAQUE_MATE,
                EstadoPartida.TABLAS_AHOGADO, EstadoPartida.TABLAS_REPETICION,
                EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS)) {
            Partida partida = partida(c -> c.piezaMovida().isPresent()
                    ? Optional.of(finalAutomatico) : Optional.empty());
            partida.ofrecerTablas(Color.BLANCO);
            avanzar(15);
            partida.mover(mov("e2", "e4"));
            avanzar(100);
            assertEquals(finalAutomatico, partida.estado());
            assertTrue(partida.ofertaTablasPendiente().isEmpty());
            assertTiempo(partida, Color.BLANCO, 285);
            assertTiempo(partida, Color.NEGRO, 300);
        }
    }

    @Test
    void unTableroInicialTerminalNoDejaElRelojCorriendo() {
        Partida partida = partida(c -> Optional.of(EstadoPartida.JAQUE_MATE));
        avanzar(300);
        assertTiempo(partida, Color.BLANCO, 300);
        assertTiempo(partida, Color.NEGRO, 300);
        partida.verificarTiempo();
        assertEquals(MotivoFinalizacion.JAQUE_MATE, partida.resultado().orElseThrow().motivo());
    }

    @Test
    void vencimientoBorraOfertaYNoPermiteAceptarNiRendirseDespues() {
        Partida partida = partida();
        partida.ofrecerTablas(Color.BLANCO);
        avanzar(300);
        assertThrows(AccionInvalidaException.class, () -> partida.aceptarTablas(Color.NEGRO));
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        assertEquals(MotivoFinalizacion.TIEMPO_AGOTADO, partida.resultado().orElseThrow().motivo());
        assertThrows(AccionInvalidaException.class, () -> partida.rendirse(Color.NEGRO));
    }

    @Test
    void constructorAnteriorSigueSinLimiteDeTiempo() {
        Partida partida = new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Color.BLANCO), new Jugador("Beto", Color.NEGRO),
                new DetectorDeJaque(), List.of());
        avanzar(100000);
        partida.verificarTiempo();
        assertTrue(partida.tiempoRestante(Color.BLANCO).isEmpty());
        assertTrue(partida.tiempoRestante(Color.NEGRO).isEmpty());
        partida.mover(mov("e2", "e4"));
        assertEquals(Color.NEGRO, partida.turno());
        assertTrue(partida.resultado().isEmpty());
    }

    @Test
    void relojNuloSeRechazaAlConstruir() {
        assertThrows(NullPointerException.class, () -> conReloj(null));
    }
}
