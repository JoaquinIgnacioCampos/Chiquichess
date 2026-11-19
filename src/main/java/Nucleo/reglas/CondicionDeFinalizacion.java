package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;

import java.util.List;
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

    /**
     * Quién gana cuando esta condición terminó la partida con ese estado; vacío si son tablas.
     * Por defecto gana el bando que acaba de mover (el contrario al que tiene el turno), como en el
     * jaque mate. Una condición donde puede ganar el otro bando (por ejemplo, por goles) lo redefine.
     */
    default Optional<Bando> ganador(EstadoPartida estado, ContextoDeJugada contexto) {
        return estado.esTablas() ? Optional.empty() : Optional.of(contexto.turno().opuesto());
    }

    /**
     * Cantidad de jugadas (de los dos bandos) a la que esta condición termina la partida, si tiene un
     * límite así; vacío si no. Sirve para que la interfaz muestre cuántas jugadas quedan.
     */
    default Optional<Integer> limiteDeJugadas() {
        return Optional.empty();
    }

    /**
     * Marcador de goles tras esas jugadas, si esta condición se juega con goles; vacío si no. Así los
     * adaptadores saben si mostrar un marcador sin preguntar si hay pelota en el tablero.
     */
    default Optional<Marcador> marcador(List<Jugada> jugadas) {
        return Optional.empty();
    }
}
