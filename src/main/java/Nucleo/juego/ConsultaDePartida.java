package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.Marcador;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Puerto de entrada de solo lectura: lo que un adaptador puede saber de una partida sin cambiarla,
 * y cómo enterarse de lo que pasa (Observer).
 *
 * Lo reciben los adaptadores que solo muestran o hacen sonar la partida: por tipo, no pueden mover
 * piezas ni terminarla. Los que además dan órdenes reciben {@link PartidaEnJuego}.
 */
public interface ConsultaDePartida {

    ITablero tablero();

    Bando turno();

    Jugador jugadorActual();

    /** Identidad inmutable de un bando, independiente del turno actual. */
    Jugador jugador(Bando color);

    EstadoPartida estado();

    /** Quién ganó; vacío si la partida sigue o terminó en tablas. */
    Optional<Jugador> ganador();

    Optional<ResultadoPartida> resultado();

    /** Jugadas a las que termina la partida, si alguna condición de fin tiene ese límite. */
    Optional<Integer> limiteDeJugadas();

    /** Goles de cada bando si las reglas se juegan con goles (el modo fútbol); vacío si no. */
    Optional<Marcador> marcador();

    /** Jugadas ejecutadas, en orden. */
    List<Movimiento> historial();

    /** Copia de la situación para que una IA la analice sin tocar la partida. */
    SituacionDePartida situacion();

    /** Vacío si la partida no tiene límite de tiempo. */
    Optional<Duration> tiempoRestante(Bando color);

    Optional<Bando> ofertaTablasPendiente();

    /** Destinos permitidos para la pieza de esa casilla (vacío si no puede moverse ahora). */
    Set<Posicion> movimientosLegales(Posicion origen);

    /** Piezas a las que puede promover ese movimiento; vacío si no es una promoción permitida. */
    List<Pieza> opcionesDePromocion(Posicion origen, Posicion destino);

    /** true si esta partida admite deshacer en algún momento. */
    boolean permiteDeshacer();

    /** true si en este momento deshacer se aceptaría. */
    boolean puedeDeshacer();

    /** true si en este momento rehacer se aceptaría. */
    boolean puedeRehacer();

    /** Suscribe un adaptador a los eventos de esta partida (ver {@link EventoDePartida}). */
    void agregarObservador(ObservadorDePartida observador);

    void quitarObservador(ObservadorDePartida observador);
}
