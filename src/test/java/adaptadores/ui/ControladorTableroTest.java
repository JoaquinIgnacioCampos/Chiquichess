package adaptadores.ui;

import Nucleo.juego.*;
import Nucleo.piezas.Color;
import Nucleo.reglas.DetectorDeJaque;
import Nucleo.tiempo.RelojAjedrez;
import adaptadores.EnsambladorPartida;
import dobles.FuenteDeTiempoFalsa;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static dobles.Casillas.*;
import static org.junit.jupiter.api.Assertions.*;

class ControladorTableroTest {
    private final Partida partida = new EnsambladorPartida().crearSinReloj();
    private final ControladorTablero controlador = new ControladorTablero(partida);

    @Test
    void seleccionarConsultaLosDestinosRealesSinMover() {
        assertTrue(controlador.pulsar(pos("e2")).isEmpty());
        assertEquals(Optional.of(pos("e2")), controlador.seleccionada());
        assertEquals(posiciones("e3", "e4"), controlador.destinosLegales());
        assertTrue(partida.historial().isEmpty());
        assertEquals(Color.BLANCO, partida.turno());
    }

    @Test
    void otraPiezaPropiaReemplazaLaSeleccionYLosDestinos() {
        controlador.pulsar(pos("e2"));
        controlador.pulsar(pos("g1"));
        assertEquals(Optional.of(pos("g1")), controlador.seleccionada());
        assertEquals(posiciones("f3", "h3"), controlador.destinosLegales());
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void casillaVaciaNoPermitidaQuitaLaSeleccion() {
        controlador.pulsar(pos("e2"));
        assertTrue(controlador.pulsar(pos("e6")).isEmpty());
        assertTrue(controlador.seleccionada().isEmpty());
        assertTrue(controlador.destinosLegales().isEmpty());
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void piezaEnemigaNoCapturableQuitaLaSeleccion() {
        controlador.pulsar(pos("e2"));
        controlador.pulsar(pos("a7"));
        assertTrue(controlador.seleccionada().isEmpty());
        assertTrue(controlador.destinosLegales().isEmpty());
        assertEquals(32, partida.tablero().piezas().size());
    }

    @Test
    void noSeleccionaPiezasDelRival() {
        controlador.pulsar(pos("e7"));
        assertTrue(controlador.seleccionada().isEmpty());
        assertTrue(controlador.destinosLegales().isEmpty());
    }

    @Test
    void unDestinoPermitidoSeEjecutaPorElNucleoYPasaElTurno() {
        controlador.pulsar(pos("e2"));
        assertTrue(controlador.pulsar(pos("e4")).isEmpty());
        assertTrue(partida.tablero().piezaEn(pos("e2")).isEmpty());
        assertEquals(Color.BLANCO, partida.tablero().piezaEn(pos("e4")).orElseThrow().color());
        assertEquals(Color.NEGRO, partida.turno());
        assertEquals(List.of(mov("e2", "e4")), partida.historial());
        assertTrue(controlador.seleccionada().isEmpty());
        assertTrue(controlador.destinosLegales().isEmpty());
        controlador.pulsar(pos("e7"));
        assertEquals(posiciones("e5", "e6"), controlador.destinosLegales());
    }

    @Test
    void unaEnemigaCapturableEsUnDestinoYLaCapturaLaResuelvePartida() {
        partida.mover(mov("e2", "e4"));
        partida.mover(mov("d7", "d5"));
        controlador.pulsar(pos("e4"));
        assertEquals(posiciones("e5", "d5"), controlador.destinosLegales());
        controlador.pulsar(pos("d5"));
        assertTrue(partida.tablero().piezaEn(pos("e4")).isEmpty());
        assertEquals(Color.BLANCO, partida.tablero().piezaEn(pos("d5")).orElseThrow().color());
        assertEquals(31, partida.tablero().piezas().size());
        assertEquals(mov("e4", "d5"), partida.historial().getLast());
        assertEquals(Color.NEGRO, partida.turno());
        assertTrue(controlador.seleccionada().isEmpty());
    }

    @Test
    void losDestinosExcluyenJugadasQueDejanAlReyEnJaque() {
        Partida clavada = new Partida(TableroFake.desde(
                "r...t...", "........", "........", "........",
                "........", "........", "....T...", "....R..."),
                new Jugador("B", Color.BLANCO), new Jugador("N", Color.NEGRO),
                new DetectorDeJaque(), List.of());
        ControladorTablero interaccion = new ControladorTablero(clavada);
        interaccion.pulsar(pos("e2"));
        assertTrue(interaccion.destinosLegales().contains(pos("e3")));
        assertFalse(interaccion.destinosLegales().contains(pos("f2")));
        interaccion.pulsar(pos("f2"));
        assertTrue(interaccion.seleccionada().isEmpty());
        assertTrue(clavada.tablero().piezaEn(pos("e2")).isPresent());
        assertTrue(clavada.historial().isEmpty());
    }

    @Test
    void elNucleoPuedeRechazarUnaJugadaResaltadaPorTiempoAgotado() {
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        Partida cronometrada = new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("B", Color.BLANCO), new Jugador("N", Color.NEGRO),
                new DetectorDeJaque(), List.of(),
                new RelojAjedrez(Duration.ofSeconds(10), Duration.ofSeconds(10), fuente));
        ControladorTablero interaccion = new ControladorTablero(cronometrada);
        interaccion.pulsar(pos("e2"));
        var tableroAntes = new HashMap<>(cronometrada.tablero().piezas());
        fuente.avanzar(Duration.ofSeconds(10));
        assertEquals(Optional.of(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA),
                interaccion.pulsar(pos("e4")));
        assertEquals(MotivoFinalizacion.TIEMPO_AGOTADO, cronometrada.resultado().orElseThrow().motivo());
        assertEquals(tableroAntes, cronometrada.tablero().piezas());
        assertTrue(cronometrada.historial().isEmpty());
        assertTrue(interaccion.seleccionada().isEmpty());
    }

    @Test
    void noPermiteSeleccionarEnUnaPartidaTerminada() {
        partida.rendirse(Color.BLANCO);
        assertEquals(Optional.of(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA),
                controlador.pulsar(pos("e2")));
        assertTrue(controlador.seleccionada().isEmpty());
        assertTrue(controlador.destinosLegales().isEmpty());
    }

    @Test
    void cambioExternoDeTurnoInvalidaLaSeleccionSinMantenerDestinosViejos() {
        controlador.pulsar(pos("e2"));
        partida.mover(mov("g1", "f3"));
        assertTrue(controlador.seleccionada().isEmpty());
        assertTrue(controlador.destinosLegales().isEmpty());
    }

    @Test
    void losDestinosExpuestosNoSonMutables() {
        controlador.pulsar(pos("e2"));
        assertThrows(UnsupportedOperationException.class, () -> controlador.destinosLegales().clear());
        assertEquals(posiciones("e3", "e4"), controlador.destinosLegales());
    }
}
