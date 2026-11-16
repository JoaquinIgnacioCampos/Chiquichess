package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.juego.Jugador;
import Nucleo.juego.Partida;
import Nucleo.juego.ResultadoPartida;
import Nucleo.tablero.Tablero;
import Nucleo.tiempo.SinReloj;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * El final por goles, jugado en partidas reales. Se usan límites chicos (1 gol, 2 jugadas) para no
 * tener que jugar partidas largas: la regla es la misma.
 */
class VictoriaPorGolesTest {

    /** La torre blanca de d2 tiene la pelota y puede hacer gol en d8. */
    private static Partida torreEnD2(VictoriaPorGoles victoria, String filaOcho) {
        Tablero tablero = TableroFake.desde(
                filaOcho,
                "........",
                "........",
                "........",
                "........",
                "........",
                "...T....",
                "....R...");
        new Nucleo.reglas.ReglaDeLaPelota().prepararCancha(tablero);
        tablero.colocarPelota(pos("d2"));
        IDetectorDeJaque detector = new DetectorDeJaque();
        ReglasDePartida reglas = new ReglasDePartida(
                new MovimientosLegales(detector, List.of(new Promocion(), new ReglaDeLaPelota())),
                List.of(new JaqueMate(), victoria, new Ahogado()), true);
        return new Partida(tablero, new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                reglas, new SinReloj());
    }

    @Test
    void rechazaLimitesNoPositivos() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new VictoriaPorGoles(0, 90));
        assertThrows(IllegalArgumentException.class, () -> new VictoriaPorGoles(3, 0));
    }

    @Test
    void ganaQuienLlegaALosGoles() {
        // Arrange
        Partida partida = torreEnD2(new VictoriaPorGoles(1, 90), ".......r");

        // Act
        partida.mover(mov("d2", "d8"));

        // Assert
        assertEquals(EstadoPartida.VICTORIA_POR_GOLES, partida.estado());
        assertEquals(Optional.of(new ResultadoPartida(Optional.of(Bando.BLANCO), MotivoFinalizacion.GOLES)),
                partida.resultado());
    }

    @Test
    void alLlegarAlLimiteGanaQuienTieneMasGolesAunqueLeToqueMover() {
        // Arrange
        Partida partida = torreEnD2(new VictoriaPorGoles(3, 2), ".......r");

        // Act: gol blanco y una jugada negra; se llega a 2 jugadas con el turno de las blancas
        partida.mover(mov("d2", "d8"));
        partida.mover(mov("h8", "h7"));

        // Assert: ganan las blancas, que son las que tienen que mover
        assertEquals(Bando.BLANCO, partida.turno());
        assertEquals(EstadoPartida.VICTORIA_POR_GOLES, partida.estado());
        assertEquals(Optional.of(Bando.BLANCO), partida.resultado().orElseThrow().ganador());
    }

    @Test
    void alLlegarAlLimiteConLosGolesIgualesEsEmpate() {
        // Arrange
        Partida partida = torreEnD2(new VictoriaPorGoles(3, 2), ".......r");

        // Act
        partida.mover(mov("d2", "d3"));
        partida.mover(mov("h8", "g8"));

        // Assert
        assertEquals(EstadoPartida.TABLAS_EMPATE_EN_GOLES, partida.estado());
        assertEquals(Optional.of(new ResultadoPartida(Optional.empty(), MotivoFinalizacion.EMPATE_EN_GOLES)),
                partida.resultado());
    }

    @Test
    void elJaqueMateGanaAunqueLaJugadaTambienSeaGol() {
        // Arrange: rey negro en a8 y rey blanco en b6; la torre a d8 es mate y gol a la vez
        Tablero tablero = TableroFake.desde(
                "r.......",
                "........",
                ".R......",
                "........",
                "........",
                "........",
                "...T....",
                "........");
        new Nucleo.reglas.ReglaDeLaPelota().prepararCancha(tablero);
        tablero.colocarPelota(pos("d2"));
        IDetectorDeJaque detector = new DetectorDeJaque();
        ReglasDePartida reglas = new ReglasDePartida(
                new MovimientosLegales(detector, List.of(new Promocion(), new ReglaDeLaPelota())),
                List.of(new JaqueMate(), new VictoriaPorGoles(1, 90), new Ahogado()), true);
        Partida partida = new Partida(tablero, new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                reglas, new SinReloj());

        // Act
        partida.mover(mov("d2", "d8"));

        // Assert
        assertEquals(EstadoPartida.JAQUE_MATE, partida.estado());
        assertEquals(new Marcador(1, 0), partida.marcador());
    }

    @Test
    void deshacerElGolGanadorReabreLaPartida() {
        // Arrange
        Partida partida = torreEnD2(new VictoriaPorGoles(1, 90), ".......r");
        partida.mover(mov("d2", "d8"));

        // Act
        partida.deshacer();

        // Assert
        assertFalse(partida.estado().estaFinalizada());
        assertTrue(partida.resultado().isEmpty());
        assertEquals(new Marcador(0, 0), partida.marcador());
    }

    @Test
    void lasReglasDeFutbolPonenElMateAntesQueLosGoles() {
        // Act
        List<CondicionDeFinalizacion> condiciones = ReglasDePartida.futbol().condiciones();

        // Assert
        assertInstanceOf(JaqueMate.class, condiciones.get(0));
        assertInstanceOf(VictoriaPorGoles.class, condiciones.get(1));
        assertTrue(condiciones.stream().anyMatch(Ahogado.class::isInstance));
        assertTrue(condiciones.stream().anyMatch(TablasPorRepeticion.class::isInstance));
        assertTrue(condiciones.stream().anyMatch(TablasPorReglaDeLos50Movimientos.class::isInstance));
    }

    @Test
    void elLimiteDeJugadasLoEligeQuienArmaLasReglas() {
        // Arrange
        Partida futbol = new Partida(new Nucleo.juego.DisposicionDeFutbol(new Nucleo.juego.DisposicionEstandar(),
                new ReglaDeLaPelota()).crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("Beto", Bando.NEGRO), ReglasDePartida.futbol(30), new SinReloj());
        Partida clasica = new Partida(new Nucleo.juego.DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                ReglasDePartida.clasicas(), new SinReloj());

        // Act + Assert
        assertEquals(Optional.of(30), futbol.limiteDeJugadas());
        assertEquals(Optional.of(90), ReglasDePartida.futbol().condiciones().get(1).limiteDeJugadas());
        assertEquals(Optional.empty(), clasica.limiteDeJugadas());
    }
}
