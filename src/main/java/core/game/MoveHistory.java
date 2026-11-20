package core.game;

import core.moves.Move;
import core.rules.Memento;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Jugadas ejecutadas y jugadas deshechas de una partida.
 *
 * Es el lugar donde Partida guarda los Command (cada {@link Jugada}) y, junto a cada uno, los
 * recuerdos de las condiciones con memoria (Memento). Con dos pilas: deshacer pasa la última jugada
 * de "hechas" a "deshechas" y rehacer la devuelve. Una jugada nueva borra las deshechas, porque la
 * partida tomó otro camino.
 *
 * No aplica ni revierte nada: eso lo coordina Partida, que es quien conoce el tablero y las reglas.
 */
final class MoveHistory {

    /** Una jugada ejecutada y los recuerdos que dejaron las condiciones con memoria al registrarla. */
    record Entrada(Move move, List<Memento> mementos) {
        Entrada {
            Objects.requireNonNull(move, "move");
            mementos = List.copyOf(mementos);
        }
    }

    private final List<Entrada> made = new ArrayList<>();
    private final List<Move> undone = new ArrayList<>();

    /** Jugadas ejecutadas, en orden (copia inmutable). */
    List<Move> moves() {
        return made.stream().map(Entrada::move).toList();
    }

    /** Jugadas ejecutadas más una nueva al final, sin modificar el historial (para armar el contexto). */
    List<Move> movesWith(Move newValue) {
        List<Move> moves = new ArrayList<>(moves());
        moves.add(newValue);
        return moves;
    }

    /** Registra una jugada nueva. Borra lo que se podía rehacer. */
    void add(Move move, List<Memento> mementos) {
        made.add(new Entrada(move, mementos));
        undone.clear();
    }

    boolean hasUndo() {
        return !made.isEmpty();
    }

    boolean hasRedo() {
        return !undone.isEmpty();
    }

    /** Saca la última jugada ejecutada y la guarda para poder rehacerla. */
    Entrada undo() {
        if (made.isEmpty()) {
            throw new IllegalStateException("There are no moves to undo");
        }
        Entrada last = made.remove(made.size() - 1);
        undone.add(last.move());
        return last;
    }

    /** Próxima jugada para rehacer; se confirma con {@link #confirmarRehecha}. */
    Move nextToRedo() {
        if (undone.isEmpty()) {
            throw new IllegalStateException("There are no moves to redo");
        }
        return undone.get(undone.size() - 1);
    }

    /** Vuelve a registrar la jugada rehecha, con los recuerdos nuevos, sin borrar el resto de las deshechas. */
    void confirmRedone(List<Memento> mementos) {
        Move move = undone.remove(undone.size() - 1);
        made.add(new Entrada(move, mementos));
    }
}
