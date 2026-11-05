package adaptadores.configuracion;

import Nucleo.juego.Partida;
import Nucleo.tiempo.RelojPartida;
import adaptadores.configuracion.ConfiguracionPartida.Equipo;

import java.util.Objects;

import Nucleo.bando.Bando;
/** Resultado del ensamblado: el sorteo y los equipos comparten la misma asignación. */
public record PartidaConfigurada(Partida partida, Equipo equipoBlancas, Equipo equipoNegras,
                                 RelojPartida reloj) implements AutoCloseable {
    public PartidaConfigurada {
        Objects.requireNonNull(partida, "partida");
        Objects.requireNonNull(equipoBlancas, "equipoBlancas");
        Objects.requireNonNull(equipoNegras, "equipoNegras");
        Objects.requireNonNull(reloj, "reloj");
        if (equipoBlancas == equipoNegras) throw new IllegalArgumentException("Los equipos deben ser distintos");
    }

    public Equipo equipo(Bando color) {
        return Objects.requireNonNull(color, "color") == Bando.BLANCO ? equipoBlancas : equipoNegras;
    }

    /** Libera el reloj de una partida descartada sin inventar un resultado deportivo. */
    @Override public void close() {
        reloj.detener();
    }
}
