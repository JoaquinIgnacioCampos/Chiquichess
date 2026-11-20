package core.game;

import core.side.Side;
import core.moves.Move;
import core.moves.Transfer;
import core.rules.GameState;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Algo que pasó en la partida y que a un adaptador le puede interesar (sonido, animaciones, registro).
 * Partida los publica a sus {@link ObservadorDePartida}; el núcleo solo dice qué pasó, nunca cómo se
 * muestra o cómo suena.
 *
 * Es sellado: quien reaccione con un switch sobre los eventos se entera en compilación si aparece uno nuevo.
 */
public sealed interface GameEvent {

    /**
     * Un bando hizo una jugada.
     *
     * @param estado el estado que dejó la jugada (por ejemplo JAQUE para el rival). Si la jugada terminó
     *               la partida, después llega además un {@link PartidaFinalizada}.
     */
    record MoveMade(Side side, Set<Trait> traits, GameState state) implements GameEvent {
        public MoveMade {
            Objects.requireNonNull(side, "side");
            traits = Set.copyOf(Objects.requireNonNull(traits, "traits"));
            Objects.requireNonNull(state, "state");
        }

        public boolean has(Trait trait) {
            return traits.contains(trait);
        }
    }

    /** Se deshizo la última jugada; {@code estado} es el de la posición a la que se volvió. */
    record MoveUndone(GameState state) implements GameEvent {
        public MoveUndone {
            Objects.requireNonNull(state, "state");
        }
    }

    /** Se volvió a hacer una jugada deshecha. Si con eso la partida termina, llega además un {@link PartidaFinalizada}. */
    record MoveRedone(Side side, Set<Trait> traits, GameState state) implements GameEvent {
        public MoveRedone {
            Objects.requireNonNull(side, "side");
            traits = Set.copyOf(Objects.requireNonNull(traits, "traits"));
            Objects.requireNonNull(state, "state");
        }
    }

    /** La partida terminó, por una jugada, por los jugadores (rendición, acuerdo) o por el reloj. */
    record FinishedGame(GameResult result) implements GameEvent {
        public FinishedGame {
            Objects.requireNonNull(result, "result");
        }
    }

    /** Lo que tuvo de particular una jugada, deducido de sus efectos. Una jugada común no tiene ninguno. */
    enum Trait {
        CAPTURE, CASTLING, PROMOTION, PASS, GOAL;

        /**
         * Rasgos de una jugada. El enroque es la única jugada que traslada dos piezas, así que se reconoce
         * por eso y no por el nombre de la regla.
         */
        public static Set<Trait> de(Move move) {
            Objects.requireNonNull(move, "move");
            Set<Trait> traits = EnumSet.noneOf(Trait.class);
            if (move.captured().isPresent()) traits.add(CAPTURE);
            if (move.effects().stream().filter(Transfer.class::isInstance).count() > 1) traits.add(CASTLING);
            if (move.promotion().isPresent()) traits.add(PROMOTION);
            if (move.isPass()) traits.add(PASS);
            if (move.isGoal()) traits.add(GOAL);
            return traits;
        }
    }
}
