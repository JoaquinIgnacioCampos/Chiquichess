package doubles;

import core.ia.GameStrategy;
import core.game.GameSituation;
import core.moves.Move;
import core.board.Position;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * IA de prueba: juega la primera jugada legal (recorriendo las casillas en orden) y acepta o rechaza
 * tablas según se le diga. Anota cuánto tiempo le dieron para pensar.
 */
public class StrategyFalse implements GameStrategy {
    private final boolean acceptsDraw;
    public final List<Duration> timesReceived = new ArrayList<>();

    public StrategyFalse(boolean acceptsDraw) {
        this.acceptsDraw = acceptsDraw;
    }

    @Override
    public Move chooseMove(GameSituation situation, Duration timeMaximum) {
        timesReceived.add(timeMaximum);
        var legal = situation.rules().movementsLegal();
        List<Position> own = situation.board().pieces().entrySet().stream()
                .filter(input -> input.getValue().color() == situation.turn())
                .map(java.util.Map.Entry::getKey)
                .sorted(Comparator.comparingInt(Position::row).thenComparingInt(Position::column))
                .toList();
        for (Position origin : own) {
            for (Move move : legal.movesPossible(situation.board(), origin, situation.moves())) {
                if (!legal.leavesKingInCheck(situation.board(), move)) return move;
            }
        }
        throw new IllegalStateException("No hay jugadas legales");
    }

    @Override
    public boolean acceptsDraw(GameSituation situation) {
        return acceptsDraw;
    }
}
