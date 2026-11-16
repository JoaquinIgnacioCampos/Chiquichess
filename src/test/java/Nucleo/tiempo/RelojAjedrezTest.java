package Nucleo.tiempo;

import Nucleo.bando.Bando;

import dobles.FuenteDeTiempoFalsa;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RelojAjedrezTest {
    private final FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();

    private RelojAjedrez reloj() {
        return new RelojAjedrez(Duration.ofMinutes(5), Duration.ofMinutes(3), fuente);
    }

    @Test
    void treintaSegundosSoloSeDescuentanAlColorActivo() {
        RelojAjedrez reloj = reloj();
        reloj.iniciar(Bando.BLANCO);
        fuente.avanzar(Duration.ofSeconds(30));
        assertEquals(Optional.of(Duration.ofSeconds(270)), reloj.tiempoRestante(Bando.BLANCO));
        assertEquals(Optional.of(Duration.ofSeconds(180)), reloj.tiempoRestante(Bando.NEGRO));
        // Consultar no acumula descuentos ni cambia el turno.
        assertEquals(Optional.of(Duration.ofSeconds(270)), reloj.tiempoRestante(Bando.BLANCO));
    }

    @Test
    void cambiarTurnoConsolidaElTiempoYActivaAlOtroColor() {
        RelojAjedrez reloj = reloj();
        reloj.iniciar(Bando.BLANCO);
        fuente.avanzar(Duration.ofSeconds(30));
        reloj.cambiarTurno();
        fuente.avanzar(Duration.ofSeconds(12));
        assertEquals(Optional.of(Duration.ofSeconds(270)), reloj.tiempoRestante(Bando.BLANCO));
        assertEquals(Optional.of(Duration.ofSeconds(168)), reloj.tiempoRestante(Bando.NEGRO));
        reloj.cambiarTurno();
        fuente.avanzar(Duration.ofSeconds(5));
        assertEquals(Optional.of(Duration.ofSeconds(265)), reloj.tiempoRestante(Bando.BLANCO));
        assertEquals(Optional.of(Duration.ofSeconds(168)), reloj.tiempoRestante(Bando.NEGRO));
    }

    @Test
    void detenerCongelaAmbosTiemposYEsIdempotente() {
        RelojAjedrez reloj = reloj();
        reloj.iniciar(Bando.BLANCO);
        fuente.avanzar(Duration.ofSeconds(20));
        reloj.detener();
        fuente.avanzar(Duration.ofHours(1));
        reloj.detener();
        assertEquals(Optional.of(Duration.ofSeconds(280)), reloj.tiempoRestante(Bando.BLANCO));
        assertEquals(Optional.of(Duration.ofSeconds(180)), reloj.tiempoRestante(Bando.NEGRO));
        assertThrows(IllegalStateException.class, reloj::cambiarTurno);
    }

    @Test
    void exactoCeroAgotaElTiempoYNuncaSeMuestranNegativos() {
        RelojAjedrez reloj = reloj();
        reloj.iniciar(Bando.NEGRO);
        fuente.avanzar(Duration.ofSeconds(179));
        assertFalse(reloj.tiempoAgotado(Bando.NEGRO));
        fuente.avanzar(Duration.ofSeconds(1));
        assertTrue(reloj.tiempoAgotado(Bando.NEGRO));
        fuente.avanzar(Duration.ofMinutes(10));
        assertEquals(Optional.of(Duration.ZERO), reloj.tiempoRestante(Bando.NEGRO));
        assertFalse(reloj.tiempoAgotado(Bando.BLANCO));
        reloj.detener();
        assertEquals(Optional.of(Duration.ZERO), reloj.tiempoRestante(Bando.NEGRO));
    }

    @Test
    void noDescuentaAntesDeIniciarYNoSePuedeReiniciar() {
        RelojAjedrez reloj = reloj();
        fuente.avanzar(Duration.ofHours(1));
        assertEquals(Optional.of(Duration.ofMinutes(5)), reloj.tiempoRestante(Bando.BLANCO));
        assertThrows(IllegalStateException.class, reloj::cambiarTurno);
        reloj.iniciar(Bando.BLANCO);
        assertThrows(IllegalStateException.class, () -> reloj.iniciar(Bando.NEGRO));
        reloj.detener();
        assertThrows(IllegalStateException.class, () -> reloj.iniciar(Bando.BLANCO));
    }

    @Test
    void exigeTiemposInicialesPositivosYDependenciasNoNulas() {
        assertThrows(IllegalArgumentException.class,
                () -> new RelojAjedrez(Duration.ZERO, Duration.ofMinutes(1), fuente));
        assertThrows(IllegalArgumentException.class,
                () -> new RelojAjedrez(Duration.ofMinutes(1), Duration.ofSeconds(-1), fuente));
        assertThrows(NullPointerException.class,
                () -> new RelojAjedrez(null, Duration.ofMinutes(1), fuente));
        assertThrows(NullPointerException.class,
                () -> new RelojAjedrez(Duration.ofMinutes(1), Duration.ofMinutes(1), null));
    }

    @Test
    void relojSinLimiteNuncaSeAgotaNiPublicaUnaDuracionFicticia() {
        RelojPartida reloj = new SinReloj();
        reloj.iniciar(Bando.BLANCO);
        reloj.cambiarTurno();
        reloj.detener();
        for (Bando color : Bando.values()) {
            assertTrue(reloj.tiempoRestante(color).isEmpty());
            assertFalse(reloj.tiempoAgotado(color));
        }
    }

    @Test
    void colorNuloNoIniciaNiModificaElReloj() {
        RelojAjedrez reloj = reloj();
        assertThrows(NullPointerException.class, () -> reloj.iniciar(null));
        reloj.iniciar(Bando.BLANCO);
        assertThrows(NullPointerException.class, () -> reloj.tiempoRestante(null));
        assertThrows(NullPointerException.class, () -> reloj.tiempoAgotado(null));
        assertEquals(Optional.of(Duration.ofMinutes(5)), reloj.tiempoRestante(Bando.BLANCO));
    }
}
