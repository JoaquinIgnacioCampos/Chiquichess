package core.moves;

import core.pieces.Piece;
import core.board.IBoard;
import core.board.Position;
import core.board.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Command: una jugada ya resuelta, con todo lo necesario para ejecutarla y deshacerla.
 * Origen y destino son la intención del jugador (la casilla que tocó y a la que fue); los efectos
 * dicen qué cambia en el tablero. Una jugada normal es [Removal?, Transfer]; las jugadas especiales
 * usan la misma clase con otros efectos, sin modificar Move ni Game.
 *
 * Toda jugada mueve la pieza de origen ({@link Transfer}) o, en el modo fútbol, es un {@link Pass}
 * desde el origen: la pieza no se mueve y la pelota va a un compañero que está en el destino.
 *
 * Guarda posiciones y no un MoveRequest porque MoveRequest pertenece al paquete juego, que depende
 * de este: usarlo acá crearía un ciclo entre paquetes.
 */
public record Move(Position origin, Position destination, List<Effect> effects) {

    public Move {
        Objects.requireNonNull(origin, "origen");
        Objects.requireNonNull(destination, "destino");
        effects = List.copyOf(Objects.requireNonNull(effects, "efectos"));
        if (!movesFrom(effects, origin)) {
            throw new IllegalArgumentException("La jugada debe trasladar la pieza que está en " + origin
                    + " o ser un pase desde ahí");
        }
    }

    /** true si algún efecto traslada la pieza de origen o es un pase desde ahí. */
    private static boolean movesFrom(List<Effect> effects, Position origin) {
        for (Effect effect : effects) {
            if (effect instanceof Transfer t && t.origin().equals(origin)
                    || effect instanceof Pass pass && pass.from().equals(origin)) {
                return true;
            }
        }
        return false;
    }

    // Las consultas de abajo usan ciclos y no streams: la IA las llama millones de veces al pensar.

    /** Jugada común: la pieza de origen va a destino y, si había una pieza rival ahí, la captura. */
    public static Move normal(IBoard board, Position origin, Position destination) {
        Piece piece = board.pieceIn(origin)
                .orElseThrow(() -> new IllegalArgumentException("No hay ninguna pieza en " + origin));
        List<Effect> effects = new ArrayList<>();
        board.pieceIn(destination).ifPresent(captured -> effects.add(new Removal(captured, destination)));
        effects.add(new Transfer(piece, origin, destination));
        return new Move(origin, destination, effects);
    }

    /** Pieza que el jugador jugó: la que se traslada desde el origen o, en un pase, la que pasa. */
    public Piece pieceMoved() {
        for (Effect effect : effects) {
            if (effect instanceof Transfer t && t.origin().equals(origin)) {
                return t.piece();
            }
        }
        for (Effect effect : effects) {
            if (effect instanceof Pass pass) {
                return pass.passer();
            }
        }
        throw new IllegalStateException("La jugada no mueve ninguna pieza");
    }

    /** true si la jugada es un pase (modo fútbol): ninguna pieza se mueve, solo la pelota. */
    public boolean isPass() {
        for (Effect effect : effects) {
            if (effect instanceof Transfer t && t.origin().equals(origin)) {
                return false;
            }
        }
        return true;
    }

    /** Pieza capturada, si la hubo. */
    public Optional<Piece> captured() {
        for (Effect effect : effects) {
            if (effect instanceof Removal withdrawal) {
                return Optional.of(withdrawal.piece());
            }
        }
        return Optional.empty();
    }

    /** Pieza en la que se convirtió la pieza movida (promoción), si hubo un reemplazo. */
    public Optional<Piece> promotion() {
        for (Effect effect : effects) {
            if (effect instanceof Replacement replacement) {
                return Optional.of(replacement.newValue());
            }
        }
        return Optional.empty();
    }

    /** true si la jugada metió un gol (modo fútbol). */
    public boolean isGoal() {
        for (Effect effect : effects) {
            if (effect instanceof MoveBall m && m.goal()) {
                return true;
            }
        }
        return false;
    }

    /** La misma jugada con un efecto más al final. */
    public Move con(Effect effect) {
        List<Effect> enlarged = new ArrayList<>(effects);
        enlarged.add(Objects.requireNonNull(effect, "efecto"));
        return new Move(origin, destination, enlarged);
    }

    /** Aplica los efectos en orden. Si uno falla, revierte los anteriores y deja el tablero como estaba. */
    public void apply(Board board) {
        List<Effect> applied = new ArrayList<>();
        try {
            for (Effect effect : effects) {
                effect.apply(board);
                applied.add(effect);
            }
        } catch (RuntimeException failure) {
            for (int i = applied.size() - 1; i >= 0; i--) {
                applied.get(i).revert(board);
            }
            throw failure;
        }
    }

    /** Deshace los efectos en orden inverso. Si uno falla, vuelve a aplicar los ya revertidos. */
    public void revert(Board board) {
        List<Effect> reverted = new ArrayList<>();
        try {
            for (int i = effects.size() - 1; i >= 0; i--) {
                effects.get(i).revert(board);
                reverted.add(effects.get(i));
            }
        } catch (RuntimeException failure) {
            for (int i = reverted.size() - 1; i >= 0; i--) {
                reverted.get(i).apply(board);
            }
            throw failure;
        }
    }
}
