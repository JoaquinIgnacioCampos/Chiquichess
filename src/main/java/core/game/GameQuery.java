package core.game;

import core.side.Side;
import core.pieces.Piece;
import core.rules.GameState;
import core.rules.Scoreboard;
import core.board.IBoard;
import core.board.Position;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Puerto de entrada de solo lectura: lo que un adaptador puede saber de una partida sin cambiarla,
 * y cómo enterarse de lo que pasa (Observer).
 *
 * Lo reciben los adaptadores que solo muestran o hacen sonar la partida: por tipo, no pueden mover
 * piezas ni terminarla. Los que además dan órdenes reciben {@link GameInProgress}.
 */
public interface GameQuery {

    IBoard board();

    Side turn();

    Player currentPlayer();

    /** Identidad inmutable de un bando, independiente del turno actual. */
    Player player(Side color);

    GameState state();

    /** Quién ganó; vacío si la partida sigue o terminó en tablas. */
    Optional<Player> winner();

    Optional<GameResult> result();

    /** Jugadas a las que termina la partida, si alguna condición de fin tiene ese límite. */
    Optional<Integer> moveLimit();

    /** Goles de cada bando si las reglas se juegan con goles (el modo fútbol); vacío si no. */
    Optional<Scoreboard> scoreboard();

    /** Jugadas ejecutadas, en orden. */
    List<MoveRequest> history();

    /** Copia de la situación para que una IA la analice sin tocar la partida. */
    GameSituation situation();

    /** Vacío si la partida no tiene límite de tiempo. */
    Optional<Duration> timeRemaining(Side color);

    Optional<Side> offerDrawPending();

    /** Destinos permitidos para la pieza de esa casilla (vacío si no puede moverse ahora). */
    Set<Position> movementsLegal(Position origin);

    /** Piezas a las que puede promover ese movimiento; vacío si no es una promoción permitida. */
    List<Piece> promotionOptions(Position origin, Position destination);

    /** true si esta partida admite deshacer en algún momento. */
    boolean allowsUndo();

    /** true si en este momento deshacer se aceptaría. */
    boolean canUndo();

    /** true si en este momento rehacer se aceptaría. */
    boolean canRedo();

    /** Suscribe un adaptador a los eventos de esta partida (ver {@link GameEvent}). */
    void addObserver(GameObserver observer);

    void removeObserver(GameObserver observer);
}
