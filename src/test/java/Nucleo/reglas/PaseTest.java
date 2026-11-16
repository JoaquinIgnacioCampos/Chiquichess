package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.juego.Jugador;
import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.juego.Partida;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import Nucleo.tiempo.SinReloj;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/** Los pases del modo fútbol, jugados en partidas reales. */
class PaseTest {

    private static Partida partida(String pelota, String... filas) {
        Tablero tablero = TableroFake.desde(filas);
        new ReglaDeLaPelota().prepararCancha(tablero);
        tablero.colocarPelota(pos(pelota));
        return new Partida(tablero, new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                ReglasDePartida.futbol().conDeshacer(), new SinReloj());
    }

    private static Optional<Posicion> pelota(Partida partida) {
        return partida.tablero().pelota();
    }

    private static char codigoEn(Partida partida, String casilla) {
        return partida.tablero().piezaEn(pos(casilla)).orElseThrow().codigo();
    }

    @Test
    void elReySeLibraDeLaPelotaPasandolaAUnCompanieroDeAlLado() {
        // Arrange: el rey blanco de e2 tiene la pelota y el caballo está en d3
        Partida partida = partida("e2",
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "...C....",
                "....R...",
                "........");

        // Act
        partida.mover(mov("e2", "d3"));

        // Assert: ninguna pieza se movió, la pelota la tiene el caballo y el turno pasó
        assertEquals(Optional.of(pos("d3")), pelota(partida));
        assertEquals('R', codigoEn(partida, "e2"));
        assertEquals('C', codigoEn(partida, "d3"));
        assertEquals(Bando.NEGRO, partida.turno());
    }

    @Test
    void laTorrePasaSoloConElCaminoLibre() {
        // Arrange
        String[] libre = {
                "....r...",
                "........",
                "........",
                "C.......",
                "........",
                "........",
                "........",
                "T...R..."};
        String[] tapado = libre.clone();
        tapado[5] = "p.......";

        // Act + Assert
        assertTrue(partida("a1", libre).movimientosLegales(pos("a1")).contains(pos("a5")));
        Partida conRivalEnElMedio = partida("a1", tapado);
        assertFalse(conRivalEnElMedio.movimientosLegales(pos("a1")).contains(pos("a5")));
        assertThrows(MovimientoInvalidoException.class, () -> conRivalEnElMedio.mover(mov("a1", "a5")));
    }

    @Test
    void elCaballoPasaPorArriba() {
        // Arrange: el caballo de b1 está rodeado de peones propios
        Partida partida = partida("b1",
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "PPPP....",
                ".C..R...");

        // Act
        partida.mover(mov("b1", "d2"));

        // Assert
        assertEquals(Optional.of(pos("d2")), pelota(partida));
        assertEquals('C', codigoEn(partida, "b1"));
    }

    @Test
    void elPeonPasaSoloEnDiagonalHaciaAdelante() {
        // Arrange: compañeros en e5 (diagonal adelante), c3 (diagonal atrás) y d5 (adelante derecho)
        Partida partida = partida("d4",
                "....r...",
                "........",
                "........",
                "...TC...",
                "...P....",
                "..C.....",
                "........",
                "....R...");

        // Act
        Set<Posicion> destinos = partida.movimientosLegales(pos("d4"));

        // Assert
        assertTrue(destinos.contains(pos("e5")));
        assertFalse(destinos.contains(pos("c3")));
        assertFalse(destinos.contains(pos("d5")));
    }

    @Test
    void pasarleAUnCompanieroQueEstaEnElArcoRivalEsGol() {
        // Arrange
        Partida partida = partida("e2",
                "....C..r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....T...",
                "R.......");

        // Act
        partida.mover(mov("e2", "e8"));

        // Assert: gol y la pelota al saque; la torre no se movió
        assertEquals(new Marcador(1, 0), partida.marcador());
        assertEquals(Optional.of(pos("d5")), pelota(partida));
        assertEquals('T', codigoEn(partida, "e2"));
    }

    @Test
    void pasarleAlReyEnElArcoNoEsGol() {
        // Arrange
        Partida partida = partida("e2",
                "....R..r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....T...",
                "........");

        // Act
        partida.mover(mov("e2", "e8"));

        // Assert
        assertEquals(new Marcador(0, 0), partida.marcador());
        assertEquals(Optional.of(pos("e8")), pelota(partida));
    }

    @Test
    void enJaqueNoSePuedePasar() {
        // Arrange: la torre negra de e8 le da jaque al rey blanco de e1
        Partida partida = partida("a1",
                "....t..r",
                "........",
                "........",
                "C.......",
                "........",
                "........",
                "........",
                "T...R...");

        // Act + Assert
        assertFalse(partida.movimientosLegales(pos("a1")).contains(pos("a5")));
    }

    @Test
    void unPeonQuePasaDesdeLaSeptimaNoPromueveYPuedeHacerGol() {
        // Arrange: peón blanco en c7 con la pelota, caballo blanco en d8 (arco negro)
        Partida partida = partida("c7",
                "...C...r",
                "..P.....",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R.......");

        // Act
        assertTrue(partida.opcionesDePromocion(pos("c7"), pos("d8")).isEmpty());
        partida.mover(mov("c7", "d8"));

        // Assert
        assertEquals('P', codigoEn(partida, "c7"));
        assertEquals('C', codigoEn(partida, "d8"));
        assertEquals(new Marcador(1, 0), partida.marcador());
    }

    @Test
    void deshacerUnPaseDevuelveLaPelota() {
        // Arrange
        Partida partida = partida("e2",
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "...C....",
                "....R...",
                "........");
        partida.mover(mov("e2", "d3"));

        // Act
        partida.deshacer();

        // Assert
        assertEquals(Optional.of(pos("e2")), pelota(partida));
        assertEquals(Bando.BLANCO, partida.turno());
    }

    @Test
    void soloPuedePasarQuienTieneLaPelota() {
        // Arrange: la pelota la tiene el caballo de d3, no el rey
        Partida partida = partida("d3",
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "...C....",
                "....R...",
                "........");

        // Act + Assert
        assertFalse(partida.movimientosLegales(pos("e2")).contains(pos("d3")));
    }
}
