package core.rules;

import core.moves.PositionFingerprint;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tablas cuando la misma posición (mismas piezas en las mismas casillas y mismo turno)
 * aparece N veces, por defecto 3. No considera derechos de enroque ni de captura al paso.
 *
 * Cada posición se identifica por su {@link HuellaDePosicion}, que se actualiza con los efectos de
 * cada jugada sin recorrer el tablero. La condición cuenta cuántas veces apareció cada huella; cada
 * registro devuelve como recuerdo qué huella sumó y cuál era la anterior, así deshacer una jugada
 * resta esa aparición y vuelve a la huella previa.
 */
public class RepetitionDraw implements StatefulCondition {

    /** Memento: la huella previa al registro y la que se sumó en él. */
    private record FingerprintPrevious(RepetitionDraw owner, boolean wasPosition, long previous, long added)
            implements Memento {
    }

    private final int repetitionsForDraw;
    private final Map<Long, Integer> appearances = new HashMap<>();
    private boolean hasPosition = false;
    private long fingerprintCurrent;

    public RepetitionDraw() {
        this(3);
    }

    public RepetitionDraw(int repetitionsForDraw) {
        if (repetitionsForDraw < 2) {
            throw new IllegalArgumentException("At least 2 repetitions are needed");
        }
        this.repetitionsForDraw = repetitionsForDraw;
    }

    @Override
    public Memento register(MoveContext context) {
        // Si ya hay una posición registrada alcanza con aplicar la última jugada a su huella;
        // si no (posición inicial), se calcula desde el tablero.
        long newValue = hasPosition && context.lastMove().isPresent()
                ? PositionFingerprint.tras(fingerprintCurrent, context.lastMove().get())
                : PositionFingerprint.de(context.board(), context.turn());
        Memento previous = new FingerprintPrevious(this, hasPosition, fingerprintCurrent, newValue);
        appearances.merge(newValue, 1, Integer::sum);
        fingerprintCurrent = newValue;
        hasPosition = true;
        return previous;
    }

    @Override
    public void restore(Memento memento) {
        if (!(memento instanceof FingerprintPrevious previous) || previous.owner() != this) {
            throw new IllegalArgumentException("The memento does not belong to this condition");
        }
        appearances.computeIfPresent(previous.added(), (fingerprint, times) -> times > 1 ? times - 1 : null);
        fingerprintCurrent = previous.previous();
        hasPosition = previous.wasPosition();
    }

    @Override
    public Optional<GameState> evaluate(MoveContext context) {
        return hasPosition && appearances.getOrDefault(fingerprintCurrent, 0) >= repetitionsForDraw
                ? Optional.of(GameState.DRAW_REPETITION)
                : Optional.empty();
    }
}
