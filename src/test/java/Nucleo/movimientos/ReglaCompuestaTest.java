package Nucleo.movimientos;

import Nucleo.juego.Posicion;
import Nucleo.piezas.Color;
import dobles.StubReglaMovimiento;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

/** Se usan stubs para probar solo la "unión" de ReglaCompuesta, sin depender de reglas reales. */
class ReglaCompuestaTest {

    @Test
    void unaLosDestinosDeTodasLasReglas() {
        // Arrange
        ReglaCompuesta compuesta = new ReglaCompuesta(
                new StubReglaMovimiento(posiciones("a1", "b2")),
                new StubReglaMovimiento(posiciones("c3")));

        // Act
        Set<Posicion> destinos = compuesta.destinosPosibles(TableroFake.vacio(), pos("d4"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("a1", "b2", "c3"), destinos);
    }

    @Test
    void unaLasCasillasAtacadasDeTodasLasReglas() {
        // Arrange
        ReglaCompuesta compuesta = new ReglaCompuesta(
                new StubReglaMovimiento(Set.of(), posiciones("e5")),
                new StubReglaMovimiento(Set.of(), posiciones("f6")));

        // Act
        Set<Posicion> atacadas = compuesta.casillasAtacadas(TableroFake.vacio(), pos("d4"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("e5", "f6"), atacadas);
    }

    @Test
    void noRepiteCasillasQueDevuelvenVariasReglas() {
        // Arrange
        ReglaCompuesta compuesta = new ReglaCompuesta(
                new StubReglaMovimiento(posiciones("a1", "b2")),
                new StubReglaMovimiento(posiciones("b2", "c3")));

        // Act
        Set<Posicion> destinos = compuesta.destinosPosibles(TableroFake.vacio(), pos("d4"), Color.BLANCO);

        // Assert
        assertEquals(3, destinos.size());
    }

    @Test
    void sinReglasNoTieneDestinos() {
        // Arrange
        ReglaCompuesta vacia = new ReglaCompuesta();

        // Act
        Set<Posicion> destinos = vacia.destinosPosibles(TableroFake.vacio(), pos("d4"), Color.BLANCO);

        // Assert
        assertTrue(destinos.isEmpty());
    }
}
