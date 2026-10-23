package Nucleo.reglas;

import java.util.Optional;

/** Jaque mate: el jugador con el turno está en jaque y no tiene ninguna jugada legal. */
public class JaqueMate implements CondicionDeFinalizacion {

    @Override
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        return contexto.enJaque() && !contexto.tieneMovimientosLegales()
                ? Optional.of(EstadoPartida.JAQUE_MATE)
                : Optional.empty();
    }
}
