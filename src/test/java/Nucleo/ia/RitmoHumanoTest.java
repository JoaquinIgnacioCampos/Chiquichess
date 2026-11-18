package Nucleo.ia;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Cuánto tarda la IA en responder, como una persona de su nivel. El azar se pasa como números fijos. */
class RitmoHumanoTest {

    private static final Optional<Duration> SIN_RELOJ = Optional.empty();
    /** Una jugada del medio de la partida, con muchas opciones y sin jaque. */
    private static final int MEDIO_JUEGO = 30;

    private static Duration comun(Dificultad dificultad, double azar) {
        return RitmoHumano.tiempo(dificultad, 30, false, MEDIO_JUEGO, SIN_RELOJ, azar, azar);
    }

    @Test
    void cadaDificultadTardaDentroDeSuRango() {
        for (Dificultad dificultad : Dificultad.values()) {
            assertEquals(dificultad.respuestaMinima(), comun(dificultad, 0));
            assertEquals(dificultad.respuestaMaxima(), comun(dificultad, 1));
        }
        assertEquals(Duration.ofSeconds(2), comun(Dificultad.FACIL, 0.5));
    }

    @Test
    void elPrincipianteJuegaMasRapidoQueElFuerte() {
        assertTrue(comun(Dificultad.FACIL, 0.5).compareTo(comun(Dificultad.MEDIO, 0.5)) < 0);
        assertTrue(comun(Dificultad.MEDIO, 0.5).compareTo(comun(Dificultad.DIFICIL, 0.5)) < 0);
    }

    @Test
    void conUnaSolaJugadaPosibleRespondeEnUnSegundo() {
        Duration forzada = RitmoHumano.tiempo(Dificultad.DIFICIL, 1, true, MEDIO_JUEGO, SIN_RELOJ, 1, 1);
        assertTrue(forzada.toMillis() >= 900 && forzada.toMillis() <= 1200, forzada.toString());
    }

    @Test
    void enJaqueConPocasSalidasTardaSuMinimo() {
        assertEquals(Dificultad.MEDIO.respuestaMinima(),
                RitmoHumano.tiempo(Dificultad.MEDIO, 3, true, MEDIO_JUEGO, SIN_RELOJ, 1, 1));
    }

    @Test
    void enLaAperturaJuegaMasRapido() {
        Duration apertura = RitmoHumano.tiempo(Dificultad.MEDIO, 20, false, 2, SIN_RELOJ, 0.5, 0.5);
        assertEquals(comun(Dificultad.MEDIO, 0.5).dividedBy(2), apertura);
    }

    @Test
    void conRelojNuncaTardaMasQueUnaFraccionDeLoQueLeQueda() {
        Optional<Duration> pocoTiempo = Optional.of(Duration.ofSeconds(80));
        Duration tiempo = RitmoHumano.tiempo(Dificultad.DIFICIL, 30, false, MEDIO_JUEGO, pocoTiempo, 1, 1);
        assertEquals(Duration.ofSeconds(80).dividedBy(RitmoHumano.FRACCION_DEL_TIEMPO_RESTANTE), tiempo);
    }

    @Test
    void unaPosicionComplicadaLaHaceDudarSalvoEnLaApertura() {
        Duration rapido = Dificultad.MEDIO.respuestaMinima();
        Duration poco = RitmoHumano.alargarPorComplejidad(rapido, Dificultad.MEDIO, MEDIO_JUEGO, SIN_RELOJ, 0);
        Duration mucho = RitmoHumano.alargarPorComplejidad(rapido, Dificultad.MEDIO, MEDIO_JUEGO, SIN_RELOJ, 1);
        assertTrue(poco.compareTo(rapido) > 0);
        assertTrue(mucho.compareTo(poco) > 0);
        assertEquals(Dificultad.MEDIO.respuestaMaxima(), mucho);
        assertEquals(rapido, RitmoHumano.alargarPorComplejidad(rapido, Dificultad.MEDIO, 2, SIN_RELOJ, 1));
    }
}
