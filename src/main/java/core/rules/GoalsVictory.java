package core.rules;

import core.side.Side;
import core.moves.Move;

import java.util.List;
import java.util.Optional;

/**
 * Final del modo fútbol: gana quien llega a 3 goles. Si se llega al límite de jugadas (90 por
 * defecto, contando las de los dos bandos; lo elige el usuario) sin que nadie llegue, gana quien
 * tenga más goles, y con los goles iguales es empate.
 *
 * No guarda estado: cuenta los goles en las jugadas del contexto (ver {@link Scoreboard}), así deshacer
 * un gol funciona solo. Como al llegar al límite de jugadas puede ganar el bando que tiene el turno, esta
 * condición dice quién gana en lugar de dejar que se asuma que ganó el que acaba de mover.
 *
 * Conviene ponerla después del jaque mate, que gana siempre.
 */
public class GoalsVictory implements EndCondition {

    private final int goalsToWin;
    private final int movesMaximum;

    /** 3 goles o 90 jugadas en total. */
    public GoalsVictory() {
        this(3, 90);
    }

    public GoalsVictory(int goalsToWin, int movesMaximum) {
        if (goalsToWin < 1 || movesMaximum < 1) {
            throw new IllegalArgumentException("Los goles para ganar y las jugadas máximas deben ser positivos");
        }
        this.goalsToWin = goalsToWin;
        this.movesMaximum = movesMaximum;
    }

    @Override
    public Optional<GameState> evaluate(MoveContext context) {
        Scoreboard scoreboard = Scoreboard.de(context.moves());
        if (scoreboard.white() >= goalsToWin || scoreboard.black() >= goalsToWin) {
            return Optional.of(GameState.VICTORY_BY_GOALS);
        }
        if (context.moves().size() >= movesMaximum) {
            return Optional.of(scoreboard.leader().isPresent()
                    ? GameState.VICTORY_BY_GOALS
                    : GameState.DRAW_TIED_ON_GOALS);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Integer> moveLimit() {
        return Optional.of(movesMaximum);
    }

    @Override
    public Optional<Scoreboard> scoreboard(List<Move> moves) {
        return Optional.of(Scoreboard.de(moves));
    }

    /** Gana quien tiene más goles, haya movido o no. */
    @Override
    public Optional<Side> winner(GameState state, MoveContext context) {
        return state.isDraw() ? Optional.empty() : Scoreboard.de(context.moves()).leader();
    }
}
