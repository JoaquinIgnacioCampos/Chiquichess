package adaptadores.configuracion;

import Nucleo.bando.Bando;
import Nucleo.juego.Partida;
import adaptadores.RivalArtificial;
import Nucleo.tiempo.RelojPartida;
import adaptadores.configuracion.ConfiguracionPartida.Equipo;

import java.util.Objects;
import java.util.Optional;

/**
 * Resultado del ensamblado: el sorteo y los equipos comparten la misma asignación.
 *
 * @param rival la IA, si se eligió jugar contra ella (juega con el equipo del Jugador 2)
 */
public record PartidaConfigurada(Partida partida, Equipo equipoBlancas, Equipo equipoNegras,
                                 RelojPartida reloj, Optional<RivalArtificial> rival) implements AutoCloseable {
    public PartidaConfigurada {
        Objects.requireNonNull(partida, "partida");
        Objects.requireNonNull(rival, "rival");
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
