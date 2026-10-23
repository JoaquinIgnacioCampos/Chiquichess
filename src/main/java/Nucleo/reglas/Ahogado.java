package Nucleo.reglas;

import java.util.Optional;

/** Ahogado (tablas): el jugador con el turno NO está en jaque pero no tiene ninguna jugada legal. */
public class Ahogado implements CondicionDeFinalizacion {

    @Override
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        return !contexto.enJaque() && !contexto.tieneMovimientosLegales()
                ? Optional.of(EstadoPartida.TABLAS_AHOGADO)
                : Optional.empty();
    }
}
