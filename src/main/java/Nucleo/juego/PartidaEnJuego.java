package Nucleo.juego;

import Nucleo.bando.Bando;

/**
 * Puerto de entrada completo: todo lo de {@link ConsultaDePartida} más las órdenes de los jugadores.
 *
 * Es lo que reciben los adaptadores que juegan (la consola, el controlador del tablero, la IA). Así no
 * dependen de la clase concreta {@link Partida}: una partida remota o una de prueba podría
 * implementar esta interfaz y los adaptadores no cambiarían. Cada orden la valida el núcleo; si no
 * corresponde, lanza {@link MovimientoInvalidoException} o {@link AccionInvalidaException}.
 */
public interface PartidaEnJuego extends ConsultaDePartida {

    /** Ejecuta el movimiento si es válido; si no, lo rechaza y deja la partida intacta. */
    ResultadoMovimiento mover(Movimiento movimiento);

    void deshacer();

    void rehacer();

    /** Solo el jugador que tiene el turno puede rendirse. */
    void rendirse(Bando color);

    void ofrecerTablas(Bando color);

    void aceptarTablas(Bando color);

    void rechazarTablas(Bando color);

    /** Termina la partida si al jugador del turno se le acabó el tiempo. Se puede llamar periódicamente. */
    void verificarTiempo();
}
