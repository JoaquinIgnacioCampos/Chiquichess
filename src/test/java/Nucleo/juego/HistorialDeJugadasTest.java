package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.Traslado;
import Nucleo.piezas.Caballo;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.Recuerdo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

class HistorialDeJugadasTest {

    private final Pieza caballo = new Caballo(Bando.BLANCO);
    private final Jugada ida = new Jugada(pos("g1"), pos("f3"), List.of(new Traslado(caballo, pos("g1"), pos("f3"))));
    private final Jugada vuelta = new Jugada(pos("f3"), pos("g1"), List.of(new Traslado(caballo, pos("f3"), pos("g1"))));
    private final Jugada otra = new Jugada(pos("f3"), pos("h4"), List.of(new Traslado(caballo, pos("f3"), pos("h4"))));
    private final Recuerdo recuerdo = new Recuerdo() { };

    @Test
    void empiezaVacio() {
        // Act
        HistorialDeJugadas historial = new HistorialDeJugadas();

        // Assert
        assertTrue(historial.jugadas().isEmpty());
        assertFalse(historial.hayParaDeshacer());
        assertFalse(historial.hayParaRehacer());
    }

    @Test
    void deshacerDevuelveLaUltimaJugadaConSusRecuerdos() {
        // Arrange
        HistorialDeJugadas historial = new HistorialDeJugadas();
        historial.agregar(ida, List.of());
        historial.agregar(vuelta, List.of(recuerdo));

        // Act
        HistorialDeJugadas.Entrada entrada = historial.deshacer();

        // Assert
        assertSame(vuelta, entrada.jugada());
        assertEquals(List.of(recuerdo), entrada.recuerdos());
        assertEquals(List.of(ida), historial.jugadas());
        assertTrue(historial.hayParaRehacer());
    }

    @Test
    void rehacerDevuelveLasJugadasDeshechasEnOrdenInverso() {
        // Arrange
        HistorialDeJugadas historial = new HistorialDeJugadas();
        historial.agregar(ida, List.of());
        historial.agregar(vuelta, List.of());
        historial.deshacer();
        historial.deshacer();

        // Act + Assert: primero vuelve la que se deshizo última
        assertSame(ida, historial.siguienteParaRehacer());
        historial.confirmarRehecha(List.of());
        assertSame(vuelta, historial.siguienteParaRehacer());
        historial.confirmarRehecha(List.of());
        assertEquals(List.of(ida, vuelta), historial.jugadas());
        assertFalse(historial.hayParaRehacer());
    }

    @Test
    void unaJugadaNuevaBorraLoQueSePodiaRehacer() {
        // Arrange
        HistorialDeJugadas historial = new HistorialDeJugadas();
        historial.agregar(ida, List.of());
        historial.agregar(vuelta, List.of());
        historial.deshacer();

        // Act
        historial.agregar(otra, List.of());

        // Assert
        assertFalse(historial.hayParaRehacer());
        assertEquals(List.of(ida, otra), historial.jugadas());
    }

    @Test
    void jugadasConNoModificaElHistorial() {
        // Arrange
        HistorialDeJugadas historial = new HistorialDeJugadas();
        historial.agregar(ida, List.of());

        // Act
        List<Jugada> conLaNueva = historial.jugadasCon(vuelta);

        // Assert
        assertEquals(List.of(ida, vuelta), conLaNueva);
        assertEquals(List.of(ida), historial.jugadas());
    }

    @Test
    void deshacerORehacerSinJugadasEsUnError() {
        // Arrange
        HistorialDeJugadas historial = new HistorialDeJugadas();

        // Act + Assert
        assertThrows(IllegalStateException.class, historial::deshacer);
        assertThrows(IllegalStateException.class, historial::siguienteParaRehacer);
    }
}
