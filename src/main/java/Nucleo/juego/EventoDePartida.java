package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.Traslado;
import Nucleo.reglas.EstadoPartida;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Algo que pasó en la partida y que a un adaptador le puede interesar (sonido, animaciones, registro).
 * Partida los publica a sus {@link ObservadorDePartida}; el núcleo solo dice qué pasó, nunca cómo se
 * muestra o cómo suena.
 *
 * Es sellado: quien reaccione con un switch sobre los eventos se entera en compilación si aparece uno nuevo.
 */
public sealed interface EventoDePartida {

    /**
     * Un bando hizo una jugada.
     *
     * @param estado el estado que dejó la jugada (por ejemplo JAQUE para el rival). Si la jugada terminó
     *               la partida, después llega además un {@link PartidaFinalizada}.
     */
    record JugadaHecha(Bando bando, Set<Rasgo> rasgos, EstadoPartida estado) implements EventoDePartida {
        public JugadaHecha {
            Objects.requireNonNull(bando, "bando");
            rasgos = Set.copyOf(Objects.requireNonNull(rasgos, "rasgos"));
            Objects.requireNonNull(estado, "estado");
        }

        public boolean tiene(Rasgo rasgo) {
            return rasgos.contains(rasgo);
        }
    }

    /** Se deshizo la última jugada; {@code estado} es el de la posición a la que se volvió. */
    record JugadaDeshecha(EstadoPartida estado) implements EventoDePartida {
        public JugadaDeshecha {
            Objects.requireNonNull(estado, "estado");
        }
    }

    /** Se volvió a hacer una jugada deshecha. Si con eso la partida termina, llega además un {@link PartidaFinalizada}. */
    record JugadaRehecha(Bando bando, Set<Rasgo> rasgos, EstadoPartida estado) implements EventoDePartida {
        public JugadaRehecha {
            Objects.requireNonNull(bando, "bando");
            rasgos = Set.copyOf(Objects.requireNonNull(rasgos, "rasgos"));
            Objects.requireNonNull(estado, "estado");
        }
    }

    /** La partida terminó, por una jugada, por los jugadores (rendición, acuerdo) o por el reloj. */
    record PartidaFinalizada(ResultadoPartida resultado) implements EventoDePartida {
        public PartidaFinalizada {
            Objects.requireNonNull(resultado, "resultado");
        }
    }

    /** Lo que tuvo de particular una jugada, deducido de sus efectos. Una jugada común no tiene ninguno. */
    enum Rasgo {
        CAPTURA, ENROQUE, PROMOCION, PASE, GOL;

        /**
         * Rasgos de una jugada. El enroque es la única jugada que traslada dos piezas, así que se reconoce
         * por eso y no por el nombre de la regla.
         */
        public static Set<Rasgo> de(Jugada jugada) {
            Objects.requireNonNull(jugada, "jugada");
            Set<Rasgo> rasgos = EnumSet.noneOf(Rasgo.class);
            if (jugada.capturada().isPresent()) rasgos.add(CAPTURA);
            if (jugada.efectos().stream().filter(Traslado.class::isInstance).count() > 1) rasgos.add(ENROQUE);
            if (jugada.promocion().isPresent()) rasgos.add(PROMOCION);
            if (jugada.esPase()) rasgos.add(PASE);
            if (jugada.esGol()) rasgos.add(GOL);
            return rasgos;
        }
    }
}
