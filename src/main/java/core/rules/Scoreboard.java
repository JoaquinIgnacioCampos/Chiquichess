package core.rules;

import core.side.Side;
import core.moves.Move;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Goles de cada bando en el modo fútbol.
 *
 * No se lleva un contador: se cuenta en las jugadas ejecutadas, que ya guardan si fueron gol (ver
 * {@link Nucleo.jugadas.MoverPelota}). Así el marcador siempre coincide con el historial, también
 * después de deshacer o rehacer un gol, sin necesidad de un Memento propio.
 */
public record Scoreboard(int white, int black) {

    public Scoreboard {
        if (white < 0 || black < 0) {
            throw new IllegalArgumentException("Goals cannot be negative");
        }
    }

    /** Marcador que resulta de esas jugadas. En una partida sin pelota es 0 a 0. */
    public static Scoreboard de(List<Move> moves) {
        int white = 0;
        int black = 0;
        for (Move move : Objects.requireNonNull(moves, "moves")) {
            if (move.isGoal()) {
                if (move.pieceMoved().color() == Side.WHITE) {
                    white++;
                } else {
                    black++;
                }
            }
        }
        return new Scoreboard(white, black);
    }

    public int goals(Side side) {
        return Objects.requireNonNull(side, "side") == Side.WHITE ? white : black;
    }

    /** El bando que va ganando; vacío si están empatados. */
    public Optional<Side> leader() {
        if (white == black) {
            return Optional.empty();
        }
        return Optional.of(white > black ? Side.WHITE : Side.BLACK);
    }
}
