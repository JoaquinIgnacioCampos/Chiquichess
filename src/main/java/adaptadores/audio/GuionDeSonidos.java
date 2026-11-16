package adaptadores.audio;

import Nucleo.bando.Bando;
import Nucleo.juego.EventoDePartida;
import Nucleo.juego.EventoDePartida.JugadaDeshecha;
import Nucleo.juego.EventoDePartida.JugadaHecha;
import Nucleo.juego.EventoDePartida.JugadaRehecha;
import Nucleo.juego.EventoDePartida.PartidaFinalizada;
import Nucleo.juego.EventoDePartida.Rasgo;
import Nucleo.reglas.EstadoPartida;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Qué suena con cada evento de la partida. No sabe de archivos ni de parlantes: solo decide, así se
 * prueba entero. Una jugada suena una sola vez, con su rasgo más importante (el gol tapa al pase, el
 * jaque a la captura).
 */
public final class GuionDeSonidos {

    /** Un sonido que hay que tocar; {@code bando} dice de qué hinchada es, para los sonidos propios de cada club. */
    public record Toque(Sonido sonido, Optional<Bando> bando) {
        public Toque {
            Objects.requireNonNull(sonido, "sonido");
            Objects.requireNonNull(bando, "bando");
        }

        static Toque comun(Sonido sonido) {
            return new Toque(sonido, Optional.empty());
        }

        static Toque de(Sonido sonido, Bando bando) {
            return new Toque(sonido, Optional.of(bando));
        }
    }

    private final boolean futbol;

    /** @param futbol si la partida es del modo fútbol (silbatos y hinchada) */
    public GuionDeSonidos(boolean futbol) {
        this.futbol = futbol;
    }

    /** Lo que suena cuando arranca la partida. */
    public List<Toque> alEmpezar() {
        return futbol ? List.of(Toque.comun(Sonido.SILBATO_INICIAL)) : List.of();
    }

    public List<Toque> para(EventoDePartida evento) {
        return switch (Objects.requireNonNull(evento, "evento")) {
            case JugadaHecha hecha -> List.of(paraLaJugada(hecha));
            case JugadaRehecha rehecha -> List.of(Toque.comun(Sonido.REBOBINAR));
            case JugadaDeshecha deshecha -> List.of(Toque.comun(Sonido.REBOBINAR));
            case PartidaFinalizada fin -> paraElFinal(fin);
        };
    }

    private Toque paraLaJugada(JugadaHecha jugada) {
        if (jugada.tiene(Rasgo.GOL)) return Toque.de(Sonido.GOL, jugada.bando());
        if (jugada.estado() == EstadoPartida.JAQUE) return Toque.comun(Sonido.JAQUE);
        if (jugada.tiene(Rasgo.PROMOCION)) return Toque.comun(Sonido.PROMOCION);
        if (jugada.tiene(Rasgo.ENROQUE)) return Toque.comun(Sonido.ENROQUE);
        if (jugada.tiene(Rasgo.CAPTURA)) return Toque.comun(Sonido.CAPTURA);
        if (jugada.tiene(Rasgo.PASE)) return Toque.comun(Sonido.PASE);
        return Toque.comun(Sonido.MOVIMIENTO);
    }

    private List<Toque> paraElFinal(PartidaFinalizada fin) {
        List<Toque> toques = new ArrayList<>();
        if (futbol) toques.add(Toque.comun(Sonido.SILBATO_FINAL));
        toques.add(fin.resultado().ganador()
                .map(ganador -> Toque.de(Sonido.VICTORIA, ganador))
                .orElse(Toque.comun(Sonido.TABLAS)));
        return List.copyOf(toques);
    }
}
