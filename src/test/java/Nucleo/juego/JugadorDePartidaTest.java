package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.reglas.DetectorDeJaque;
import org.junit.jupiter.api.Test;
import java.util.List;
import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

class JugadorDePartidaTest {
    private final Jugador blancas = new Jugador("Lucía", Bando.BLANCO);
    private final Jugador negras = new Jugador("Tomás", Bando.NEGRO);
    private final Partida partida = new Partida(new DisposicionEstandar().crearTablero(),
            blancas, negras, new DetectorDeJaque(), List.of());

    @Test
    void consultaAmbosBandosSinDependerDelTurno() {
        assertEquals(blancas, partida.jugador(Bando.BLANCO));
        assertEquals(negras, partida.jugador(Bando.NEGRO));
        partida.mover(mov("e2", "e4"));
        assertEquals(blancas, partida.jugador(Bando.BLANCO));
        assertEquals(negras, partida.jugador(Bando.NEGRO));
        assertEquals(partida.jugador(Bando.NEGRO), partida.jugadorActual());
    }

    @Test
    void consultarNoModificaPartidaYRechazaColorNulo() {
        assertThrows(NullPointerException.class, () -> partida.jugador(null));
        assertEquals(Bando.BLANCO, partida.turno());
        assertTrue(partida.historial().isEmpty());
        assertTrue(partida.resultado().isEmpty());
    }
}
