package core.rules;

import core.side.Side;
import core.board.Board;
import doubles.Contexts;
import doubles.RecordOfMoves;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Cada test registra en la condición una secuencia real jugada sobre un tablero del fake, como hace Game. */
class RepetitionDrawTest {

    private static final String[] LAP_OF_KNIGHTS = {"g1f3", "g8f6", "f3g1", "f6g8"};

    /** Reyes en e1/e8 y caballos en g1/g8: los caballos pueden ir y volver sin cambiar nada más. */
    private Board withKnights() {
        return BoardFake.from(
                "....r.c.",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R.C.");
    }

    private Optional<GameState> tras(RepetitionDraw regla, Board board, String... moves) {
        var recordValue = RecordOfMoves.register(regla, board, moves);
        return regla.evaluate(recordValue.lastContext());
    }

    @Test
    void rejectsLessOfTwoRepetitions() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new RepetitionDraw(1));
    }

    @Test
    void initialPositionAloneNotIsDraw() {
        // Act
        Optional<GameState> result = tras(new RepetitionDraw(2), withKnights());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void thirdTimeThatAppearsAPositionIsDraw() {
        // Act: cada vuelta de los caballos repite la posición inicial
        Optional<GameState> secondTime = tras(new RepetitionDraw(), withKnights(), LAP_OF_KNIGHTS);
        Optional<GameState> thirdTime = tras(new RepetitionDraw(), withKnights(),
                "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");

        // Assert
        assertTrue(secondTime.isEmpty());
        assertEquals(Optional.of(GameState.DRAW_REPETITION), thirdTime);
    }

    @Test
    void withTurnDifferentNotIsSamePosition() {
        // Arrange: el rey blanco hace un triángulo (3 jugadas) y el negro va y vuelve (2 jugadas).
        // Las piezas quedan como al principio, pero ahora juegan las negras.

        // Act
        Optional<GameState> result = tras(new RepetitionDraw(2), withKnights(),
                "e1d1", "e8d8", "d1d2", "d8e8", "d2e1");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void detectsRepetitionAfterOfACapture() {
        // Arrange: la torre blanca captura el peón de a7 y después los caballos van y vuelven.
        // La posición posterior a la captura (juegan las negras) aparece dos veces.
        Board board = BoardFake.from(
                "....r.c.",
                "p.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T...R.C.");

        // Act
        Optional<GameState> withTwo = tras(new RepetitionDraw(2), board.copy(),
                "a1a7", "g8f6", "g1f3", "f6g8", "f3g1");
        Optional<GameState> withThree = tras(new RepetitionDraw(3), board.copy(),
                "a1a7", "g8f6", "g1f3", "f6g8", "f3g1");

        // Assert
        assertEquals(Optional.of(GameState.DRAW_REPETITION), withTwo);
        assertTrue(withThree.isEmpty());
    }

    // ---------------- Memento ----------------

    @Test
    void restoreLastRecordRemovesThatAppearance() {
        // Arrange: tercera aparición de la posición inicial
        RepetitionDraw regla = new RepetitionDraw();
        var recordValue = RecordOfMoves.register(regla, withKnights(),
                "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");
        List<Memento> mementos = recordValue.mementos();
        assertEquals(Optional.of(GameState.DRAW_REPETITION), regla.evaluate(recordValue.lastContext()));

        // Act: deshacer la última jugada
        regla.restore(mementos.get(mementos.size() - 1));

        // Assert
        assertTrue(regla.evaluate(recordValue.lastContext()).isEmpty());
    }

    @Test
    void redoAfterOfRestoreReturnsToCountAppearance() {
        // Arrange
        RepetitionDraw regla = new RepetitionDraw();
        var recordValue = RecordOfMoves.register(regla, withKnights(),
                "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");
        List<Memento> mementos = recordValue.mementos();
        regla.restore(mementos.get(mementos.size() - 1));

        // Act: volver a registrar la misma jugada (lo que hará Game al rehacer)
        regla.register(recordValue.lastContext());

        // Assert
        assertEquals(Optional.of(GameState.DRAW_REPETITION), regla.evaluate(recordValue.lastContext()));
    }

    @Test
    void restoreAllInOrderInverseLeavesEmptyCondition() {
        // Arrange
        RepetitionDraw regla = new RepetitionDraw(2);
        Board board = withKnights();
        List<Memento> mementos = RecordOfMoves.register(regla, board, LAP_OF_KNIGHTS).mementos();

        // Act
        for (int i = mementos.size() - 1; i >= 0; i--) {
            regla.restore(mementos.get(i));
        }

        // Assert: sin registros no hay tablas, y empezar de nuevo funciona como la primera vez
        assertTrue(regla.evaluate(Contexts.initial(board, Side.WHITE)).isEmpty());
        regla.register(Contexts.initial(board, Side.WHITE));
        assertTrue(regla.evaluate(Contexts.initial(board, Side.WHITE)).isEmpty());
    }

    @Test
    void rejectsMementosOfOtherCondition() {
        // Arrange
        RepetitionDraw a = new RepetitionDraw();
        Memento ofOther = new RepetitionDraw().register(Contexts.initial(withKnights(), Side.WHITE));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> a.restore(ofOther));
    }
}
