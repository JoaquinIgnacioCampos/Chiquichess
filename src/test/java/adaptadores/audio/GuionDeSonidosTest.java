package adaptadores.audio;

import Nucleo.bando.Bando;
import Nucleo.juego.EventoDePartida;
import Nucleo.juego.EventoDePartida.JugadaDeshecha;
import Nucleo.juego.EventoDePartida.JugadaHecha;
import Nucleo.juego.EventoDePartida.JugadaRehecha;
import Nucleo.juego.EventoDePartida.PartidaFinalizada;
import Nucleo.juego.EventoDePartida.Rasgo;
import Nucleo.juego.ResultadoPartida;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.MotivoFinalizacion;
import adaptadores.audio.GuionDeSonidos.Toque;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GuionDeSonidosTest {

    private final GuionDeSonidos clasico = new GuionDeSonidos(false);
    private final GuionDeSonidos futbol = new GuionDeSonidos(true);

    private static JugadaHecha jugada(EstadoPartida estado, Rasgo... rasgos) {
        return new JugadaHecha(Bando.BLANCO, rasgos.length == 0 ? Set.of() : EnumSet.of(rasgos[0], rasgos), estado);
    }

    private static List<Sonido> sonidos(List<Toque> toques) {
        return toques.stream().map(Toque::sonido).toList();
    }

    @Test
    void cadaJugadaSuenaConSuRasgo() {
        assertEquals(List.of(Sonido.MOVIMIENTO), sonidos(clasico.para(jugada(EstadoPartida.EN_CURSO))));
        assertEquals(List.of(Sonido.CAPTURA), sonidos(clasico.para(jugada(EstadoPartida.EN_CURSO, Rasgo.CAPTURA))));
        assertEquals(List.of(Sonido.ENROQUE), sonidos(clasico.para(jugada(EstadoPartida.EN_CURSO, Rasgo.ENROQUE))));
        assertEquals(List.of(Sonido.PASE), sonidos(futbol.para(jugada(EstadoPartida.EN_CURSO, Rasgo.PASE))));
    }

    @Test
    void unaJugadaSuenaUnaSolaVezConSuRasgoMasImportante() {
        // El jaque tapa a la captura y a la promoción; el gol tapa a todo
        assertEquals(List.of(Sonido.JAQUE),
                sonidos(clasico.para(jugada(EstadoPartida.JAQUE, Rasgo.CAPTURA, Rasgo.PROMOCION))));
        assertEquals(List.of(Sonido.PROMOCION),
                sonidos(clasico.para(jugada(EstadoPartida.EN_CURSO, Rasgo.CAPTURA, Rasgo.PROMOCION))));
        assertEquals(List.of(Sonido.GOL),
                sonidos(futbol.para(jugada(EstadoPartida.JAQUE, Rasgo.PASE, Rasgo.GOL))));
    }

    @Test
    void elGolEsDeLaHinchadaDelBandoQueLoHizo() {
        // Act
        List<Toque> toques = futbol.para(new JugadaHecha(Bando.NEGRO, Set.of(Rasgo.GOL), EstadoPartida.EN_CURSO));

        // Assert
        assertEquals(List.of(new Toque(Sonido.GOL, Optional.of(Bando.NEGRO))), toques);
    }

    @Test
    void laVictoriaEsDelGanadorYEnElFutbolLaPrecedeElSilbato() {
        // Arrange
        PartidaFinalizada mate = new PartidaFinalizada(
                new ResultadoPartida(Optional.of(Bando.NEGRO), MotivoFinalizacion.JAQUE_MATE));

        // Act + Assert
        assertEquals(List.of(new Toque(Sonido.VICTORIA, Optional.of(Bando.NEGRO))), clasico.para(mate));
        assertEquals(List.of(new Toque(Sonido.SILBATO_FINAL, Optional.empty()),
                new Toque(Sonido.VICTORIA, Optional.of(Bando.NEGRO))), futbol.para(mate));
    }

    @Test
    void lasTablasNoSonDeNingunEquipo() {
        // Arrange
        PartidaFinalizada tablas = new PartidaFinalizada(new ResultadoPartida(Optional.empty(), MotivoFinalizacion.ACUERDO));

        // Act + Assert
        assertEquals(List.of(new Toque(Sonido.TABLAS, Optional.empty())), clasico.para(tablas));
    }

    @Test
    void deshacerYRehacerRebobinan() {
        assertEquals(List.of(Sonido.REBOBINAR), sonidos(clasico.para(new JugadaDeshecha(EstadoPartida.EN_CURSO))));
        assertEquals(List.of(Sonido.REBOBINAR),
                sonidos(clasico.para(new JugadaRehecha(Bando.BLANCO, Set.of(), EstadoPartida.EN_CURSO))));
    }

    @Test
    void soloElFutbolArrancaConSilbato() {
        assertEquals(List.of(), clasico.alEmpezar());
        assertEquals(List.of(Sonido.SILBATO_INICIAL), sonidos(futbol.alEmpezar()));
    }

    @Test
    void todosLosSonidosSalenDeAlgunEvento() {
        // Arrange: un evento de cada tipo y rasgo; la hinchada no sale del guion porque suena en bucle
        List<EventoDePartida> eventos = Stream.concat(
                Stream.of(Rasgo.values()).map(rasgo -> (EventoDePartida) jugada(EstadoPartida.EN_CURSO, rasgo)),
                Stream.of(jugada(EstadoPartida.EN_CURSO), jugada(EstadoPartida.JAQUE),
                        new JugadaDeshecha(EstadoPartida.EN_CURSO),
                        new PartidaFinalizada(new ResultadoPartida(Optional.empty(), MotivoFinalizacion.ACUERDO)),
                        new PartidaFinalizada(new ResultadoPartida(Optional.of(Bando.BLANCO), MotivoFinalizacion.GOLES))))
                .toList();

        // Act
        Set<Sonido> usados = EnumSet.noneOf(Sonido.class);
        eventos.forEach(evento -> usados.addAll(sonidos(futbol.para(evento))));
        usados.addAll(sonidos(futbol.alEmpezar()));

        // Assert
        // La hinchada suena en bucle, la selección la avisa la vista, y la risa y el ambiente son del menú.
        assertEquals(EnumSet.complementOf(EnumSet.of(Sonido.HINCHADA, Sonido.SELECCION, Sonido.RISA,
                Sonido.AMBIENTE_MENU)), usados);
    }
}
