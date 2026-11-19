package Nucleo.ia;

import Nucleo.bando.Bando;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Jugador;
import Nucleo.juego.Movimiento;
import Nucleo.juego.Partida;
import Nucleo.juego.SituacionDePartida;
import Nucleo.jugadas.Jugada;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.ReglaDeLaPelota;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tablero.Tablero;
import Nucleo.tiempo.FuenteDeTiempo;
import Nucleo.tiempo.SinReloj;
import dobles.FuenteDeTiempoFalsa;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * La IA resolviendo problemas conocidos. El tiempo es falso y no avanza, así cada dificultad mira
 * exactamente hasta su profundidad máxima y el resultado no depende de la velocidad de la máquina.
 */
class JugadorArtificialTest {

    /** Tiempo que no corre: la búsqueda nunca se corta por tiempo. */
    private static final FuenteDeTiempo QUIETO = new FuenteDeTiempoFalsa();

    /** Sin distracciones: para probar lo que ve cada dificultad cuando presta atención. */
    private static JugadorArtificial ia(Dificultad dificultad) {
        return new JugadorArtificial(dificultad, new AzarSinDescuidos(7), QUIETO);
    }

    /** El azar de siempre, pero nunca "se distrae" (la probabilidad de descuido nunca se cumple). */
    private static final class AzarSinDescuidos extends Random {
        AzarSinDescuidos(long semilla) {
            super(semilla);
        }

        @Override
        public double nextDouble() {
            return 0.99;
        }
    }

    /** Se distrae siempre. */
    private static final class AzarDistraido extends Random {
        AzarDistraido(long semilla) {
            super(semilla);
        }

        @Override
        public double nextDouble() {
            return 0.0;
        }
    }

    private static Partida partida(ReglasDePartida reglas, String... filas) {
        return partida(TableroFake.desde(filas), reglas);
    }

    private static Partida partida(Tablero tablero, ReglasDePartida reglas) {
        return new Partida(tablero, new Jugador("Ana", Bando.BLANCO), new Jugador("IA", Bando.NEGRO),
                reglas, new SinReloj());
    }

    private static void jugarLaIa(Partida partida, JugadorArtificial ia) {
        partida.mover(Movimiento.de(ia.elegirJugada(partida.situacion())));
    }

    @Test
    void todasLasDificultadesVenElMateEnUno() {
        for (Dificultad dificultad : Dificultad.values()) {
            // Arrange: mate del pasillo con la torre en a8
            Partida partida = partida(ReglasDePartida.clasicas(),
                    "......r.",
                    ".....ppp",
                    "........",
                    "........",
                    "........",
                    "........",
                    "........",
                    "T.....R.");
            JugadorArtificial ia = dificultad == Dificultad.DIFICIL
                    ? new JugadorArtificial(dificultad, new AzarSinDescuidos(7), new FuenteQueAvanza())
                    : ia(dificultad);

            // Act
            jugarLaIa(partida, ia);

            // Assert
            assertEquals(EstadoPartida.JAQUE_MATE, partida.estado(), dificultad.name());
        }
    }

    @Test
    void elMedioDaMateEnDosJugadasAunqueElRivalSeDefiendaLoMejorPosible() {
        // Arrange: escalera de torres contra el rey solo
        Partida partida = partida(ReglasDePartida.clasicas(),
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "TT..R...");
        JugadorArtificial blancas = ia(Dificultad.MEDIO);
        JugadorArtificial negras = ia(Dificultad.MEDIO);

        // Act
        jugarLaIa(partida, blancas);
        jugarLaIa(partida, negras);
        jugarLaIa(partida, blancas);

        // Assert
        assertEquals(EstadoPartida.JAQUE_MATE, partida.estado());
    }

    @Test
    void hastaElFacilSeComeUnaDamaQueQuedoSinDefensa() {
        // Arrange: el caballo blanco de f3 puede comer la dama negra de e5
        Partida partida = partida(ReglasDePartida.clasicas(),
                "....r...",
                "........",
                "........",
                "....d...",
                "........",
                ".....C..",
                "........",
                "....R...");

        // Act
        Jugada jugada = ia(Dificultad.FACIL).elegirJugada(partida.situacion());

        // Assert
        assertEquals(pos("e5"), jugada.destino());
        assertTrue(jugada.capturada().isPresent());
    }

    @Test
    void elFacilMiraUnaSolaJugadaYPierdeLaDamaPorUnaTorreDefendidaElMedioNo() {
        // Arrange: la torre negra de d6 está defendida por el peón de e7
        Partida partida = partida(ReglasDePartida.clasicas(),
                ".......r",
                "....p...",
                "...t....",
                "........",
                "........",
                "........",
                "........",
                "R..D....");

        // Act
        Jugada facil = ia(Dificultad.FACIL).elegirJugada(partida.situacion());
        Jugada medio = ia(Dificultad.MEDIO).elegirJugada(partida.situacion());

        // Assert: el fácil ve la torre pero no que después le comen la dama
        assertEquals(pos("d6"), facil.destino());
        assertNotEquals(pos("d6"), medio.destino());
    }

    @Test
    void distraidoJuegaCualquierCosaMenosUnaJugadaQueLeDaMateEnUno() {
        for (long semilla = 0; semilla < 25; semilla++) {
            // Arrange: si la torre blanca deja la primera fila, Ta1 es mate
            Partida partida = partida(ReglasDePartida.clasicas(),
                    "t......r",
                    "........",
                    "........",
                    "........",
                    "........",
                    "........",
                    ".....PPP",
                    "....T.R.");
            JugadorArtificial distraido = new JugadorArtificial(Dificultad.FACIL, new AzarDistraido(semilla), QUIETO);

            // Act: juega el distraído y responde un rival que no se pierde un mate en 1
            jugarLaIa(partida, distraido);
            jugarLaIa(partida, ia(Dificultad.MEDIO));

            // Assert
            assertNotEquals(EstadoPartida.JAQUE_MATE, partida.estado(), "semilla " + semilla);
        }
    }

    @Test
    void elMedioNoDejaSuDamaDondeSeLaComen() {
        // Arrange: un peón negro en e5 ataca a la dama blanca de d4
        Partida partida = partida(ReglasDePartida.clasicas(),
                "r.......",
                "........",
                "........",
                "....p...",
                "...D....",
                "........",
                "........",
                ".......R");

        // Act
        Jugada jugada = ia(Dificultad.MEDIO).elegirJugada(partida.situacion());

        // Assert: la dama se va de d4 (o se come el peón)
        assertEquals(pos("d4"), jugada.origen());
    }

    @Test
    void enElFutbolMeteElGolCuandoPuede() {
        // Arrange: torre blanca con la pelota en d2 y el arco rival libre en d8
        Tablero tablero = TableroFake.desde(
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...T....",
                "....R...");
        new ReglaDeLaPelota().prepararCancha(tablero);
        tablero.colocarPelota(pos("d2"));
        Partida partida = partida(tablero, ReglasDePartida.futbol());

        // Act
        jugarLaIa(partida, ia(Dificultad.FACIL));

        // Assert
        assertEquals(1, partida.marcador().orElseThrow().blancas());
    }

    @Test
    void conLaMismaSemillaEligeLaMismaJugada() {
        // Arrange
        Partida partida = new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("IA", Bando.NEGRO), ReglasDePartida.clasicas(), new SinReloj());

        // Act
        Jugada primera = new JugadorArtificial(Dificultad.FACIL, new Random(42), QUIETO).elegirJugada(partida.situacion());
        Jugada segunda = new JugadorArtificial(Dificultad.FACIL, new Random(42), QUIETO).elegirJugada(partida.situacion());

        // Assert
        assertEquals(Movimiento.de(primera), Movimiento.de(segunda));
    }

    @Test
    void elDificilCortaCuandoSeLeAcabaElTiempoYJuegaAlgoLegal() {
        // Arrange: el tiempo avanza en cada consulta, así se agota enseguida
        Partida partida = new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("IA", Bando.NEGRO), ReglasDePartida.clasicas(), new SinReloj());
        JugadorArtificial ia = new JugadorArtificial(Dificultad.DIFICIL, new Random(1), new FuenteQueAvanza());

        // Act
        Jugada jugada = ia.elegirJugada(partida.situacion(), Duration.ofMillis(5));

        // Assert
        assertTrue(partida.movimientosLegales(jugada.origen()).contains(jugada.destino()));
    }

    @Test
    void pensarNoTocaLaPartidaNiDisparaEventos() {
        // Arrange
        Partida partida = new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("IA", Bando.NEGRO), ReglasDePartida.clasicas(), new SinReloj());
        List<Object> eventos = new ArrayList<>();
        partida.agregarObservador(eventos::add);
        var antes = partida.tablero().piezas();

        // Act
        ia(Dificultad.MEDIO).elegirJugada(partida.situacion());

        // Assert
        assertTrue(eventos.isEmpty());
        assertEquals(antes, partida.tablero().piezas());
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void aceptaTablasSoloSiVaClaramentePerdiendo() {
        // Arrange: las blancas ofrecen; en una partida tienen dama de más, en la otra están iguales
        Partida conDamaDeMas = partida(ReglasDePartida.clasicas(),
                "....r...", "........", "........", "........", "........", "........", "...D....", "....R...");
        Partida pareja = new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("IA", Bando.NEGRO), ReglasDePartida.clasicas(), new SinReloj());

        // Act + Assert
        assertTrue(ia(Dificultad.MEDIO).aceptaTablas(conDamaDeMas.situacion()));
        assertFalse(ia(Dificultad.MEDIO).aceptaTablas(pareja.situacion()));
    }

    @Test
    void sinJugadasLegalesNoPuedeElegir() {
        // Arrange: el rey negro está ahogado y le toca a las negras
        Tablero tablero = TableroFake.desde(
                "r.......",
                "..D.....",
                ".R......",
                "........",
                "........",
                "........",
                "........",
                "........");
        SituacionDePartida situacion = new SituacionDePartida(tablero, Bando.NEGRO, List.of(), ReglasDePartida.clasicas());

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> ia(Dificultad.FACIL).elegirJugada(situacion));
    }

    @Test
    void elMovimientoDeUnaPromocionLlevaLaPiezaElegida() {
        // Arrange
        Partida partida = partida(ReglasDePartida.clasicas(),
                "r.......",
                "......P.",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");

        // Act
        Movimiento movimiento = Movimiento.de(ia(Dificultad.FACIL).elegirJugada(partida.situacion()));

        // Assert: corona en g8 y dice a qué pieza
        assertEquals(pos("g8"), movimiento.destino());
        assertEquals(Optional.of('D'), movimiento.promocion());
    }

    @Test
    void conUnaSolaJugadaPosibleRespondeEnUnSegundo() {
        // Arrange: el rey blanco solo puede ir a a2 (la torre negra controla la columna b)
        Partida partida = partida(ReglasDePartida.clasicas(),
                ".t.....r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R.......");

        // Act
        DecisionDeJuego decision = ia(Dificultad.DIFICIL).decidir(partida.situacion(), Optional.empty());

        // Assert
        assertEquals(pos("a2"), decision.jugada().destino());
        assertTrue(decision.tiempoDeRespuesta().toMillis() <= 1200, decision.tiempoDeRespuesta().toString());
    }

    @Test
    void elDificilTardaComoUnaPersonaDeSuNivelYPiensaEseTiempo() {
        // Arrange: cada consulta del tiempo avanza un segundo, así la búsqueda se corta enseguida
        Partida partida = new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("IA", Bando.NEGRO), ReglasDePartida.clasicas(), new SinReloj());
        JugadorArtificial ia = new JugadorArtificial(Dificultad.DIFICIL, new Random(3), new FuenteQueAvanzaUnSegundo());

        // Act
        DecisionDeJuego decision = ia.decidir(partida.situacion(), Optional.empty());

        // Assert: en la apertura, la mitad de su rango de 3 a 7 s
        long ms = decision.tiempoDeRespuesta().toMillis();
        assertTrue(ms >= 1500 && ms <= 3500, decision.tiempoDeRespuesta().toString());
        assertTrue(partida.movimientosLegales(decision.jugada().origen()).contains(decision.jugada().destino()));
    }

    /** Cada consulta del tiempo avanza un segundo. */
    private static final class FuenteQueAvanzaUnSegundo implements FuenteDeTiempo {
        private long nanos;

        @Override
        public long ahoraEnNanos() {
            nanos += 1_000_000_000L;
            return nanos;
        }
    }

    /** Cada consulta del tiempo avanza un milisegundo: la búsqueda se corta sola enseguida. */
    private static final class FuenteQueAvanza implements FuenteDeTiempo {
        private long nanos;

        @Override
        public long ahoraEnNanos() {
            nanos += 1_000_000;
            return nanos;
        }
    }
}
