package adaptadores;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** La consola completa con una entrada simulada: muestra los rechazos y el final con los textos del catálogo. */
class JuegoConsolaTest {

    private String jugar(String entrada) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        new JuegoConsola(new EnsambladorPartida().crearSinReloj(), new RenderizadorConsola(), new MensajesDeConsola(),
                new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(salida, true, StandardCharsets.UTF_8)).jugar();
        return salida.toString(StandardCharsets.UTF_8);
    }

    @Test
    void muestraElMotivoDeCadaJugadaRechazada() throws IOException {
        // Act
        String salida = jugar("e4 e5\ne7 e5\na1 a3\nsalir\n");

        // Assert
        assertTrue(salida.contains("No hay ninguna pieza en la casilla de origen."));
        assertTrue(salida.contains("Esa pieza no es suya: es el turno del otro jugador."));
        assertTrue(salida.contains("Esa pieza no puede moverse ahi."));
    }

    @Test
    void anunciaElJaqueMateConElNombreDelGanador() throws IOException {
        // Act
        String salida = jugar("f2 f3\ne7 e5\ng2 g4\nd8 h4\n");

        // Assert
        assertTrue(salida.strip().endsWith("Jaque mate! Gana Jugador 2."));
    }
}
