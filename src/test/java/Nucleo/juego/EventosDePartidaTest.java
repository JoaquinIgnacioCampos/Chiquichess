package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.juego.EventoDePartida.JugadaDeshecha;
import Nucleo.juego.EventoDePartida.JugadaHecha;
import Nucleo.juego.EventoDePartida.JugadaRehecha;
import Nucleo.juego.EventoDePartida.PartidaFinalizada;
import Nucleo.juego.EventoDePartida.Rasgo;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.MotivoFinalizacion;
import Nucleo.reglas.ReglaDeLaPelota;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tablero.Tablero;
import Nucleo.tiempo.RelojAjedrez;
import Nucleo.tiempo.SinReloj;
import dobles.FuenteDeTiempoFalsa;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/** Los avisos que Partida publica a sus observadores (Observer). */
class EventosDePartidaTest {

    private final List<EventoDePartida> recibidos = new ArrayList<>();

    private Partida observada(Partida partida) {
        partida.agregarObservador(recibidos::add);
        return partida;
    }

    private Partida clasica() {
        return observada(new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("Beto", Bando.NEGRO), ReglasDePartida.clasicas().conDeshacer(), new SinReloj()));
    }

    private Partida armada(ReglasDePartida reglas, String... filas) {
        return observada(new Partida(TableroFake.desde(filas), new Jugador("Ana", Bando.BLANCO),
                new Jugador("Beto", Bando.NEGRO), reglas, new SinReloj()));
    }

    private void jugar(Partida partida, String... jugadas) {
        for (String jugada : jugadas) {
            partida.mover(mov(jugada.substring(0, 2), jugada.substring(2, 4)));
        }
    }

    private JugadaHecha ultimaJugada() {
        for (int i = recibidos.size() - 1; i >= 0; i--) {
            if (recibidos.get(i) instanceof JugadaHecha hecha) return hecha;
        }
        return fail("No se avisó ninguna jugada");
    }

    @Test
    void unaJugadaComunAvisaQuienJugoSinRasgos() {
        // Arrange
        Partida partida = clasica();

        // Act
        jugar(partida, "e2e4");

        // Assert
        assertEquals(List.of(new JugadaHecha(Bando.BLANCO, Set.of(), EstadoPartida.EN_CURSO)), recibidos);
    }

    @Test
    void unaJugadaRechazadaNoAvisaNada() {
        // Arrange
        Partida partida = clasica();

        // Act
        assertThrows(MovimientoInvalidoException.class, () -> jugar(partida, "e2e5"));

        // Assert
        assertTrue(recibidos.isEmpty());
    }

    @Test
    void laCapturaYElJaqueQuedanEnElAviso() {
        // Arrange: la dama blanca puede tomar el peón de f7 dando jaque
        Partida partida = armada(ReglasDePartida.clasicas(),
                "....r...",
                ".....p..",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R....D..");

        // Act
        jugar(partida, "f1f7");

        // Assert
        assertEquals(new JugadaHecha(Bando.BLANCO, Set.of(Rasgo.CAPTURA), EstadoPartida.JAQUE), ultimaJugada());
    }

    @Test
    void elEnroqueSeReconocePorQueMueveDosPiezas() {
        // Arrange
        Partida partida = armada(ReglasDePartida.clasicas(),
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R..T");

        // Act
        jugar(partida, "e1g1");

        // Assert
        assertEquals(Set.of(Rasgo.ENROQUE), ultimaJugada().rasgos());
    }

    @Test
    void laPromocionQuedaEnElAviso() {
        // Arrange
        Partida partida = armada(ReglasDePartida.clasicas(),
                "r.......",
                "......P.",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");

        // Act
        jugar(partida, "g7g8");

        // Assert
        assertTrue(ultimaJugada().tiene(Rasgo.PROMOCION));
    }

    @Test
    void elMateAvisaPrimeroLaJugadaYDespuesElFinal() {
        // Arrange
        Partida partida = clasica();

        // Act: mate del loco
        jugar(partida, "f2f3", "e7e5", "g2g4", "d8h4");

        // Assert
        int cantidad = recibidos.size();
        assertEquals(new JugadaHecha(Bando.NEGRO, Set.of(), EstadoPartida.JAQUE_MATE), recibidos.get(cantidad - 2));
        assertEquals(new PartidaFinalizada(new ResultadoPartida(Optional.of(Bando.NEGRO), MotivoFinalizacion.JAQUE_MATE)),
                recibidos.get(cantidad - 1));
    }

    @Test
    void laRendicionYElAcuerdoDeTablasAvisanElFinal() {
        // Arrange
        Partida rendida = clasica();
        Partida acordada = clasica();

        // Act
        rendida.rendirse(Bando.BLANCO);
        acordada.ofrecerTablas(Bando.BLANCO);
        acordada.aceptarTablas(Bando.NEGRO);

        // Assert
        assertEquals(List.of(
                new PartidaFinalizada(new ResultadoPartida(Optional.of(Bando.NEGRO), MotivoFinalizacion.RENDICION)),
                new PartidaFinalizada(new ResultadoPartida(Optional.empty(), MotivoFinalizacion.ACUERDO))), recibidos);
    }

    @Test
    void elTiempoAgotadoSeAvisaUnaSolaVez() {
        // Arrange
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        Partida partida = observada(new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO), ReglasDePartida.clasicas(),
                new RelojAjedrez(Duration.ofMinutes(1), Duration.ofMinutes(1), fuente)));

        // Act
        fuente.avanzar(Duration.ofMinutes(2));
        partida.verificarTiempo();
        partida.verificarTiempo();

        // Assert
        assertEquals(List.of(new PartidaFinalizada(
                new ResultadoPartida(Optional.of(Bando.NEGRO), MotivoFinalizacion.TIEMPO_AGOTADO))), recibidos);
    }

    @Test
    void deshacerYRehacerAvisanConElEstadoQueQueda() {
        // Arrange
        Partida partida = clasica();
        jugar(partida, "e2e4");
        recibidos.clear();

        // Act
        partida.deshacer();
        partida.rehacer();

        // Assert
        assertEquals(List.of(new JugadaDeshecha(EstadoPartida.EN_CURSO),
                new JugadaRehecha(Bando.BLANCO, Set.of(), EstadoPartida.EN_CURSO)), recibidos);
    }

    @Test
    void elPaseQueTerminaEnElArcoRivalAvisaPaseYGol() {
        // Arrange: el caballo blanco espera en el arco rival y la torre tiene la pelota
        Tablero tablero = TableroFake.desde(
                "....C..r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....T...",
                "R.......");
        new ReglaDeLaPelota().prepararCancha(tablero);
        tablero.colocarPelota(pos("e2"));
        Partida partida = observada(new Partida(tablero, new Jugador("Ana", Bando.BLANCO),
                new Jugador("Beto", Bando.NEGRO), ReglasDePartida.futbol(), new SinReloj()));

        // Act
        jugar(partida, "e2e8");

        // Assert
        assertEquals(new JugadaHecha(Bando.BLANCO, Set.of(Rasgo.PASE, Rasgo.GOL), EstadoPartida.EN_CURSO),
                ultimaJugada());
    }

    @Test
    void unObservadorQuitadoNoRecibeMasAvisos() {
        // Arrange
        Partida partida = new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("Beto", Bando.NEGRO), ReglasDePartida.clasicas(), new SinReloj());
        ObservadorDePartida observador = recibidos::add;
        partida.agregarObservador(observador);

        // Act
        partida.quitarObservador(observador);
        jugar(partida, "e2e4");

        // Assert
        assertTrue(recibidos.isEmpty());
    }
}
