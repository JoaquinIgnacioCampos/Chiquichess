package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.juego.Jugador;
import Nucleo.juego.Partida;
import Nucleo.jugadas.Jugada;
import Nucleo.tablero.Tablero;
import Nucleo.tiempo.SinReloj;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/** Cómo se mueve la pelota, jugado en partidas reales con las reglas del modo fútbol. */
class ReglaDeLaPelotaTest {

    private static Partida partida(String pelota, String... filas) {
        Tablero tablero = TableroFake.desde(filas);
        tablero.colocarPelota(pos(pelota));
        return new Partida(tablero, new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                ReglasDePartida.futbol().conDeshacer(), new SinReloj());
    }

    private static Optional<?> pelota(Partida partida) {
        return partida.tablero().pelota();
    }

    @Test
    void sinPelotaNoCambiaNingunaJugada() {
        // Arrange
        Tablero tablero = new Nucleo.juego.DisposicionEstandar().crearTablero();
        Jugada jugada = Jugada.normal(tablero, pos("e2"), pos("e4"));

        // Act + Assert
        assertEquals(List.of(jugada), new ReglaDeLaPelota().transformar(tablero, jugada));
    }

    @Test
    void lasJugadasQueNoTocanLaPelotaQuedanIguales() {
        // Arrange
        Tablero tablero = new Nucleo.juego.DisposicionEstandar().crearTablero();
        tablero.colocarPelota(pos("d5"));
        Jugada jugada = Jugada.normal(tablero, pos("e2"), pos("e4"));

        // Act + Assert
        assertEquals(List.of(jugada), new ReglaDeLaPelota().transformar(tablero, jugada));
    }

    @Test
    void terminarEnLaCasillaDeLaPelotaLaTomaYDespuesLaLleva() {
        // Arrange
        Partida partida = partida("d5",
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "..C.....",
                "........",
                "....R...");

        // Act
        partida.mover(mov("c3", "d5"));
        partida.mover(mov("e8", "f8"));
        partida.mover(mov("d5", "f6"));

        // Assert
        assertEquals(Optional.of(pos("f6")), pelota(partida));
    }

    @Test
    void pasarPorEncimaDeLaPelotaNoLaToma() {
        // Arrange
        Partida partida = partida("d4",
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...TR...");

        // Act
        partida.mover(mov("d1", "d7"));

        // Assert
        assertEquals(Optional.of(pos("d4")), pelota(partida));
    }

    @Test
    void capturarAQuienLlevaLaPelotaSeLaQuita() {
        // Arrange: el caballo negro de d5 tiene la pelota
        Partida partida = partida("d5",
                "....r...",
                "........",
                "........",
                "...c....",
                "....P...",
                "........",
                "........",
                "....R...");

        // Act
        partida.mover(mov("e4", "d5"));
        partida.mover(mov("e8", "f8"));
        partida.mover(mov("d5", "d6"));

        // Assert: el peón que capturó se la llevó
        assertEquals(Optional.of(pos("d6")), pelota(partida));
    }

    @Test
    void enLaCapturaAlPasoLaPelotaVaConQuienCaptura() {
        // Arrange: el peón negro de d7 tiene la pelota y avanza dos casillas al lado del peón blanco
        Partida partida = partida("d7",
                "....r...",
                "...p....",
                "........",
                "....P...",
                "........",
                "........",
                "........",
                "....R...");
        partida.mover(mov("e1", "f1"));
        partida.mover(mov("d7", "d5"));

        // Act
        partida.mover(mov("e5", "d6"));

        // Assert
        assertTrue(partida.tablero().piezaEn(pos("d5")).isEmpty());
        assertEquals(Optional.of(pos("d6")), pelota(partida));
    }

    @Test
    void llevarLaPelotaAlArcoRivalEsGolYLaPelotaVuelveAlSaque() {
        // Arrange
        Partida partida = partida("d2",
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...T....",
                "....R...");

        // Act
        partida.mover(mov("d2", "d8"));

        // Assert: la torre queda en el arco y la pelota va a d5, el campo del que recibió el gol
        assertTrue(partida.tablero().piezaEn(pos("d8")).orElseThrow().esTorre());
        assertEquals(Optional.of(pos("d5")), pelota(partida));
        assertEquals(new Marcador(1, 0), partida.marcador());
        assertFalse(partida.estado().estaFinalizada());
    }

    @Test
    void elGolDeLasNegrasVuelveAlSaqueDelCampoBlanco() {
        // Arrange
        Partida partida = partida("d7",
                "r.......",
                "...t....",
                "........",
                "........",
                "........",
                "........",
                "........",
                ".......R");
        partida.mover(mov("h1", "h2"));

        // Act
        partida.mover(mov("d7", "d1"));

        // Assert
        assertEquals(Optional.of(pos("d4")), pelota(partida));
        assertEquals(new Marcador(0, 1), partida.marcador());
    }

    @Test
    void elReyPuedeLlevarLaPelotaPeroNoHaceGol() {
        // Arrange
        Partida partida = partida("d7",
                ".......r",
                "...R....",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........");

        // Act
        partida.mover(mov("d7", "d8"));

        // Assert
        assertEquals(Optional.of(pos("d8")), pelota(partida));
        assertEquals(new Marcador(0, 0), partida.marcador());
    }

    @Test
    void unPeonPuedePromoverYHacerGolEnLaMismaJugada() {
        // Arrange
        Partida partida = partida("e7",
                ".......r",
                "....P...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R.......");

        // Act
        partida.mover(mov("e7", "e8"));

        // Assert
        assertTrue(partida.tablero().piezaEn(pos("e8")).orElseThrow().codigo() == 'D');
        assertEquals(Optional.of(pos("d5")), pelota(partida));
        assertEquals(new Marcador(1, 0), partida.marcador());
    }

    @Test
    void siHayUnaPiezaEnElSaqueSeQuedaConLaPelota() {
        // Arrange: el caballo negro está parado en d5, el saque tras un gol blanco
        Partida partida = partida("e2",
                "........",
                ".......r",
                "........",
                "...c....",
                "........",
                "........",
                "....T...",
                "R.......");
        partida.mover(mov("e2", "e8"));

        // Act
        partida.mover(mov("d5", "f4"));

        // Assert
        assertEquals(Optional.of(pos("f4")), pelota(partida));
    }

    @Test
    void deshacerUnGolDevuelveLaPelotaYElMarcador() {
        // Arrange
        Partida partida = partida("d2",
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...T....",
                "....R...");
        partida.mover(mov("d2", "d8"));

        // Act
        partida.deshacer();

        // Assert
        assertEquals(Optional.of(pos("d2")), pelota(partida));
        assertEquals(new Marcador(0, 0), partida.marcador());

        // Act: rehacer vuelve a meter el gol
        partida.rehacer();

        // Assert
        assertEquals(Optional.of(pos("d5")), pelota(partida));
        assertEquals(new Marcador(1, 0), partida.marcador());
    }
}
