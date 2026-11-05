package Nucleo.reglas;

import java.util.Optional;

/**
 * Strategy: una regla que puede terminar la partida (jaque mate, ahogado, repetición, 50 movimientos...).
 * Partida las consulta en orden después de cada jugada y se queda con la primera que responda.
 * Agregar una condición nueva es crear una clase nueva, sin tocar Partida (Open/Closed).
 *
 * Se llama una vez al crear la partida y una vez por cada jugada ejecutada. evaluar es una consulta:
 * no debe modificar estado, porque Partida deja de consultar en cuanto una condición responde.
 * Las condiciones que necesitan recordar algo entre jugadas implementan {@link CondicionConMemoria},
 * que separa el registro de cada jugada (siempre para todas) de la evaluación.
 */
public interface CondicionDeFinalizacion {

    /** Estado final si la condición se cumple; vacío si la partida debe continuar. */
    Optional<EstadoPartida> evaluar(ContextoDeJugada contexto);
}
