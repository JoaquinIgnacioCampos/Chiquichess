package adaptadores;

import Nucleo.bando.Bando;
import Nucleo.juego.Jugador;
import Nucleo.juego.Partida;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tiempo.SinReloj;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/** La consola completa con una entrada simulada: muestra los rechazos y el final con los textos del catálogo. */
class JuegoConsolaTest {

    private String jugar(Partida partida, String entrada) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        new JuegoConsola(partida, new RenderizadorConsola(), new MensajesDeConsola(),
                new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(salida, true, StandardCharsets.UTF_8)).jugar();
        return salida.toString(StandardCharsets.UTF_8);
    }

    private String jugar(String entrada) throws IOException {
        return jugar(new EnsambladorPartida().crearSinReloj(), entrada);
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

    // ---------------- Promoción ----------------

    /** Peón blanco en a7 a punto de promover. */
    private Partida conPeonEnA7() {
        return new Partida(dobles.TableroFake.desde(
                "....r...",
                "P.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R..."), new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                ReglasDePartida.clasicas(), new SinReloj());
    }

    @Test
    void laTerceraLetraEligeLaPiezaDeLaPromocion() throws IOException {
        // Arrange
        Partida partida = conPeonEnA7();

        // Act
        jugar(partida, "a7 a8 c\nsalir\n");

        // Assert
        assertEquals('C', partida.tablero().piezaEn(pos("a8")).orElseThrow().codigo());
    }

    @Test
    void sinTerceraLetraPromueveADama() throws IOException {
        // Arrange
        Partida partida = conPeonEnA7();

        // Act
        jugar(partida, "a7 a8\nsalir\n");

        // Assert
        assertEquals('D', partida.tablero().piezaEn(pos("a8")).orElseThrow().codigo());
    }

    @Test
    void unaPiezaDePromocionInvalidaSeRechaza() throws IOException {
        // Arrange
        Partida partida = conPeonEnA7();

        // Act
        String salida = jugar(partida, "a7 a8 R\na7 a8 dama\nsalir\n");

        // Assert
        assertTrue(salida.contains("Esa pieza no puede moverse ahi."));
        assertTrue(salida.contains("Formato invalido. Ejemplo: e2 e4 (para promover: e7 e8 C)"));
        assertTrue(partida.historial().isEmpty());
    }

    // ---------------- Deshacer y rehacer ----------------

    @Test
    void conDeshacerHabilitadoLaAyudaMencionaLosComandos() throws IOException {
        // Act
        String salida = jugar(new EnsambladorPartida().crearSinRelojConDeshacer(), "salir\n");

        // Assert
        assertTrue(salida.startsWith("Escriba la jugada como 'origen destino' (ej: e2 e4), 'deshacer', 'rehacer' o 'salir'."));
    }

    @Test
    void deshacerYRehacerDesdeLaConsola() throws IOException {
        // Arrange
        Partida partida = new EnsambladorPartida().crearSinRelojConDeshacer();

        // Act: jugar, deshacer, rehacer y deshacer otra vez
        String salida = jugar(partida, "e2 e4\ndeshacer\nrehacer\ndeshacer\nsalir\n");

        // Assert
        assertTrue(partida.historial().isEmpty());
        assertFalse(salida.contains("No hay jugadas"));
    }

    @Test
    void avisaCuandoNoHayNadaParaDeshacerORehacer() throws IOException {
        // Act
        String salida = jugar(new EnsambladorPartida().crearSinRelojConDeshacer(), "deshacer\nrehacer\nsalir\n");

        // Assert
        assertTrue(salida.contains("No hay jugadas para deshacer."));
        assertTrue(salida.contains("No hay jugadas para rehacer."));
    }

    @Test
    void sinDeshacerHabilitadoLoRechaza() throws IOException {
        // Act
        String salida = jugar("e2 e4\ndeshacer\nsalir\n");

        // Assert
        assertTrue(salida.contains("Esta partida no permite deshacer jugadas."));
    }

    @Test
    void despuesDelMateOfreceDeshacerYLaPartidaSigue() throws IOException {
        // Arrange
        Partida partida = new EnsambladorPartida().crearSinRelojConDeshacer();

        // Act: mate del loco, deshacer el mate y jugar otra cosa
        String salida = jugar(partida, "f2 f3\ne7 e5\ng2 g4\nd8 h4\ndeshacer\nd8 g5\nsalir\n");

        // Assert
        assertTrue(salida.contains("Jaque mate! Gana Jugador 2."));
        assertTrue(salida.contains("Escriba 'deshacer' para volver atras o 'salir': "));
        assertFalse(partida.estado().estaFinalizada());
        assertEquals(4, partida.historial().size());
    }
}
