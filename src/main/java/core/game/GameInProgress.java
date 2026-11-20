package core.game;

import core.side.Side;

/**
 * Puerto de entrada completo: todo lo de {@link GameQuery} más las órdenes de los jugadores.
 *
 * Es lo que reciben los adaptadores que juegan (la consola, el controlador del tablero, la IA). Así no
 * dependen de la clase concreta {@link Game}: una partida remota o una de prueba podría
 * implementar esta interfaz y los adaptadores no cambiarían. Cada orden la valida el núcleo; si no
 * corresponde, lanza {@link InvalidMoveException} o {@link InvalidActionException}.
 */
public interface GameInProgress extends GameQuery {

    /** Ejecuta el movimiento si es válido; si no, lo rechaza y deja la partida intacta. */
    MoveResult move(MoveRequest movement);

    void undo();

    void redo();

    /** Solo el jugador que tiene el turno puede rendirse. */
    void resign(Side color);

    void offerDraw(Side color);

    void acceptDraw(Side color);

    void rejectDraw(Side color);

    /** Termina la partida si al jugador del turno se le acabó el tiempo. Se puede llamar periódicamente. */
    void verifyTime();
}
