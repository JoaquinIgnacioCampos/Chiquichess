package core.rules;

import core.side.Side;
import core.moves.Move;
import core.moves.Removal;
import core.moves.Transfer;
import core.pieces.Knight;
import core.pieces.Pawn;
import core.pieces.Piece;
import core.board.Board;
import doubles.Contexts;
import doubles.RecordOfMoves;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * La condición solo mira la última jugada de cada registro, así que alcanza con jugadas armadas a
 * mano: no hace falta que correspondan a un tablero real.
 */
class FiftyMoveRuleDrawTest {

    private final Board board = BoardFake.empty();
    private final Piece knight = new Knight(Side.WHITE);
    private final Piece pawn = new Pawn(Side.WHITE);

    /** Jugada de una pieza que no es peón y sin captura: no hay "progreso". */
    private Move releases() {
        return new Move(pos("b1"), pos("c3"), List.of(new Transfer(knight, pos("b1"), pos("c3"))));
    }

    private Move ofPawn() {
        return new Move(pos("e2"), pos("e4"), List.of(new Transfer(pawn, pos("e2"), pos("e4"))));
    }

    private Move capture() {
        return new Move(pos("b1"), pos("c3"), List.of(
                new Removal(new Knight(Side.BLACK), pos("c3")), new Transfer(knight, pos("b1"), pos("c3"))));
    }

    /** Registra la posición inicial y las jugadas, y evalúa al final. */
    private Optional<GameState> tras(FiftyMoveRuleDraw regla, Move... moves) {
        var recordValue = RecordOfMoves.registerWithoutApply(regla, board, List.of(moves));
        return regla.evaluate(recordValue.lastContext());
    }

    @Test
    void rejectsALimitSmallerToOne() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new FiftyMoveRuleDraw(0));
    }

    @Test
    void inInitialPositionNotHasDraw() {
        // Arrange: con límite 1 bastarían 2 jugadas sueltas
        FiftyMoveRuleDraw regla = new FiftyMoveRuleDraw(1);

        // Act
        Optional<GameState> result = tras(regla);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void withLimitOneAreDrawAfterOfAMoveReleasesOfEachPlayer() {
        // Act
        Optional<GameState> afterA = tras(new FiftyMoveRuleDraw(1), releases());
        Optional<GameState> afterTwo = tras(new FiftyMoveRuleDraw(1), releases(), releases());

        // Assert
        assertTrue(afterA.isEmpty());
        assertEquals(Optional.of(GameState.DRAW_RULE_50_MOVEMENTS), afterTwo);
    }

    @Test
    void aMovementOfPawnRestartsAccount() {
        // Act: después del peón hay una sola jugada suelta
        Optional<GameState> result = tras(new FiftyMoveRuleDraw(1), releases(), ofPawn(), releases());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void aCaptureRestartsAccount() {
        // Act
        Optional<GameState> result = tras(new FiftyMoveRuleDraw(1), releases(), capture(), releases());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void byDefaultAreDrawJustInMove100WithoutProgress() {
        // Act
        Optional<GameState> in99 = tras(new FiftyMoveRuleDraw(),
                Collections.nCopies(99, releases()).toArray(Move[]::new));
        Optional<GameState> in100 = tras(new FiftyMoveRuleDraw(),
                Collections.nCopies(100, releases()).toArray(Move[]::new));

        // Assert
        assertTrue(in99.isEmpty());
        assertEquals(Optional.of(GameState.DRAW_RULE_50_MOVEMENTS), in100);
    }

    // ---------------- Memento ----------------

    @Test
    void restoreLastRecordLeavesConditionAsBeforeOfRegisterIt() {
        // Arrange: con límite 1, dos jugadas sueltas son tablas
        FiftyMoveRuleDraw regla = new FiftyMoveRuleDraw(1);
        var recordValue = RecordOfMoves.registerWithoutApply(regla, board, List.of(releases(), releases()));
        assertEquals(Optional.of(GameState.DRAW_RULE_50_MOVEMENTS), regla.evaluate(recordValue.lastContext()));

        // Act: deshacer la segunda jugada
        regla.restore(recordValue.mementos().get(2));

        // Assert
        assertTrue(regla.evaluate(recordValue.lastContext()).isEmpty());
    }

    @Test
    void restoreRecoversCounterAlthoughMoveItHasReturnedToZero() {
        // Arrange: límite 1. Suelta (contador 1), peón (contador 0)
        FiftyMoveRuleDraw regla = new FiftyMoveRuleDraw(1);
        var recordValue = RecordOfMoves.registerWithoutApply(regla, board, List.of(releases(), ofPawn()));

        // Act: deshacer el peón y jugar otra suelta en su lugar
        regla.restore(recordValue.mementos().get(2));
        regla.register(Contexts.withHistory(board, Side.WHITE, List.of(releases(), releases())));

        // Assert: el contador volvió a 1 y con la nueva suelta llegó a 2
        assertEquals(Optional.of(GameState.DRAW_RULE_50_MOVEMENTS), regla.evaluate(recordValue.lastContext()));
    }

    @Test
    void rejectsMementosOfOtherCondition() {
        // Arrange
        FiftyMoveRuleDraw a = new FiftyMoveRuleDraw();
        FiftyMoveRuleDraw other = new FiftyMoveRuleDraw();
        Memento ofOther = other.register(Contexts.initial(board, Side.WHITE));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> a.restore(ofOther));
        assertThrows(IllegalArgumentException.class, () -> a.restore(new Memento() { }));
    }

    @Test
    void aPassOfAPawnNotAccountAsAdvanceOfPawn() {
        // Arrange: con límite 1 (una jugada de cada uno), dos jugadas sin progreso alcanzan
        Move passOfPawn = new Move(pos("e2"), pos("d3"), List.of(
                new core.moves.Pass(pawn, pos("e2"), knight, pos("d3"))));

        // Act + Assert: igual que dos jugadas sueltas, y no como un avance de peón
        assertEquals(Optional.of(GameState.DRAW_RULE_50_MOVEMENTS),
                tras(new FiftyMoveRuleDraw(1), releases(), passOfPawn));
        assertEquals(Optional.empty(), tras(new FiftyMoveRuleDraw(1), releases(), ofPawn()));
    }
}
