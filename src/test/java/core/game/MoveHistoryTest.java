package core.game;

import core.side.Side;
import core.moves.Move;
import core.moves.Transfer;
import core.pieces.Knight;
import core.pieces.Piece;
import core.rules.Memento;
import org.junit.jupiter.api.Test;

import java.util.List;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

class MoveHistoryTest {

    private final Piece knight = new Knight(Side.WHITE);
    private final Move outbound = new Move(pos("g1"), pos("f3"), List.of(new Transfer(knight, pos("g1"), pos("f3"))));
    private final Move lap = new Move(pos("f3"), pos("g1"), List.of(new Transfer(knight, pos("f3"), pos("g1"))));
    private final Move other = new Move(pos("f3"), pos("h4"), List.of(new Transfer(knight, pos("f3"), pos("h4"))));
    private final Memento memento = new Memento() { };

    @Test
    void startsEmpty() {
        // Act
        MoveHistory history = new MoveHistory();

        // Assert
        assertTrue(history.moves().isEmpty());
        assertFalse(history.hasUndo());
        assertFalse(history.hasRedo());
    }

    @Test
    void undoReturnsLastMoveWithTheirMementos() {
        // Arrange
        MoveHistory history = new MoveHistory();
        history.add(outbound, List.of());
        history.add(lap, List.of(memento));

        // Act
        MoveHistory.Entrada input = history.undo();

        // Assert
        assertSame(lap, input.move());
        assertEquals(List.of(memento), input.mementos());
        assertEquals(List.of(outbound), history.moves());
        assertTrue(history.hasRedo());
    }

    @Test
    void redoReturnsMovesUndoneInOrderInverse() {
        // Arrange
        MoveHistory history = new MoveHistory();
        history.add(outbound, List.of());
        history.add(lap, List.of());
        history.undo();
        history.undo();

        // Act + Assert: primero vuelve la que se deshizo última
        assertSame(outbound, history.nextToRedo());
        history.confirmRedone(List.of());
        assertSame(lap, history.nextToRedo());
        history.confirmRedone(List.of());
        assertEquals(List.of(outbound, lap), history.moves());
        assertFalse(history.hasRedo());
    }

    @Test
    void aNewMoveDeletesItThatCouldRedo() {
        // Arrange
        MoveHistory history = new MoveHistory();
        history.add(outbound, List.of());
        history.add(lap, List.of());
        history.undo();

        // Act
        history.add(other, List.of());

        // Assert
        assertFalse(history.hasRedo());
        assertEquals(List.of(outbound, other), history.moves());
    }

    @Test
    void movesWithNotModifiesHistory() {
        // Arrange
        MoveHistory history = new MoveHistory();
        history.add(outbound, List.of());

        // Act
        List<Move> withNew = history.movesWith(lap);

        // Assert
        assertEquals(List.of(outbound, lap), withNew);
        assertEquals(List.of(outbound), history.moves());
    }

    @Test
    void undoOrRedoWithoutMovesIsAError() {
        // Arrange
        MoveHistory history = new MoveHistory();

        // Act + Assert
        assertThrows(IllegalStateException.class, history::undo);
        assertThrows(IllegalStateException.class, history::nextToRedo);
    }
}
