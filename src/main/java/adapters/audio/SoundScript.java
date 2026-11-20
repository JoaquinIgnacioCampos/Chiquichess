package adapters.audio;

import core.side.Side;
import core.game.GameEvent;
import core.game.GameEvent.MoveUndone;
import core.game.GameEvent.MoveMade;
import core.game.GameEvent.MoveRedone;
import core.game.GameEvent.FinishedGame;
import core.game.GameEvent.Trait;
import core.rules.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Qué suena con cada evento de la partida. No sabe de archivos ni de parlantes: solo decide, así se
 * prueba entero. Una jugada suena una sola vez, con su rasgo más importante (el gol tapa al pase, el
 * jaque a la captura).
 */
public final class SoundScript {

    /** Un sonido que hay que tocar; {@code bando} dice de qué hinchada es, para los sonidos propios de cada club. */
    public record Cue(Sound sound, Optional<Side> side) {
        public Cue {
            Objects.requireNonNull(sound, "sonido");
            Objects.requireNonNull(side, "bando");
        }

        static Cue common(Sound sound) {
            return new Cue(sound, Optional.empty());
        }

        static Cue de(Sound sound, Side side) {
            return new Cue(sound, Optional.of(side));
        }
    }

    private final boolean football;

    /** @param futbol si la partida es del modo fútbol (silbato final y hinchada) */
    public SoundScript(boolean football) {
        this.football = football;
    }

    /** Lo que suena cuando arranca la partida (al tocar "Jugar" o la revancha): el silbato, en todos los modos. */
    public List<Cue> onStart() {
        return List.of(Cue.common(Sound.WHISTLE_INITIAL));
    }

    public List<Cue> para(GameEvent event) {
        return switch (Objects.requireNonNull(event, "evento")) {
            case MoveMade made -> List.of(forMove(made));
            case MoveRedone redone -> List.of(Cue.common(Sound.REWIND));
            case MoveUndone undone -> List.of(Cue.common(Sound.REWIND));
            case FinishedGame fin -> forEnding(fin);
        };
    }

    private Cue forMove(MoveMade move) {
        if (move.has(Trait.GOAL)) return Cue.de(Sound.GOAL, move.side());
        if (move.state() == GameState.CHECK) return Cue.common(Sound.CHECK);
        if (move.has(Trait.PROMOTION)) return Cue.common(Sound.PROMOTION);
        if (move.has(Trait.CASTLING)) return Cue.common(Sound.CASTLING);
        if (move.has(Trait.CAPTURE)) return Cue.common(Sound.CAPTURE);
        if (move.has(Trait.PASS)) return Cue.common(Sound.PASS);
        return Cue.common(Sound.MOVEMENT);
    }

    private List<Cue> forEnding(FinishedGame fin) {
        List<Cue> cues = new ArrayList<>();
        if (football) cues.add(Cue.common(Sound.WHISTLE_FINAL));
        cues.add(fin.result().winner()
                .map(winner -> Cue.de(Sound.VICTORIA, winner))
                .orElse(Cue.common(Sound.DRAW)));
        return List.copyOf(cues);
    }
}
