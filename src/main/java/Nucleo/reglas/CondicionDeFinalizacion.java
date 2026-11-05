package Nucleo.reglas;

import java.util.Optional;

/**
 * Strategy: una regla que puede terminar la partida (jaque mate, ahogado, repetición, 50 movimientos...).
 * Partida las consulta en orden después de cada jugada y se queda con la primera que responda.
 * Agregar una condición nueva es crear una clase nueva, sin tocar Partida (Open/Closed).
 *
 * Se llama una vez al crear la partida y una vez por cada jugada ejecutada. El resultado debe
 * depender solo del contexto recibido (que incluye el historial de jugadas), sin guardar estado
 * propio: así las condiciones siguen siendo correctas si se deshace una jugada, y una misma
 * instancia se puede consultar varias veces con el mismo contexto.
 */
public interface CondicionDeFinalizacion {

    /** Estado final si la condición se cumple; vacío si la partida debe continuar. */
    Optional<EstadoPartida> evaluar(ContextoDeJugada contexto);
}
