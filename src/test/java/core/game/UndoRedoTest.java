package core.game;

import core.side.Side;
import core.game.InvalidActionException.Reason;
import core.pieces.Piece;
import core.rules.*;
import core.board.Position;
import core.time.ChessClock;
import core.time.GameClock;
import core.time.NoClock;
import doubles.FontOfTimeFalse;
import doubles.MockConditionWithMemory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Deshacer y rehacer en Game. Se usa el criterio del Command: la foto de la partida después de
 * deshacer es idéntica a la de antes de jugar, y después de rehacer, idéntica a la de después de jugar.
 */
class UndoRedoTest {

    /** Todo lo observable de una partida en un momento. */
    private record Photo(Map<Position, Piece> pieces, Side turn, GameState state,
                        Optional<GameResult> result, List<MoveRequest> history) {
    }

    private static Photo photo(Game game) {
        return new Photo(new HashMap<>(game.board().pieces()), game.turn(), game.state(),
                game.result(), game.history());
    }

    private Game game(GameRules rules, GameClock clock) {
        return new Game(new StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK), rules, clock);
    }

    /** Ajedrez clásico con deshacer habilitado y sin límite de tiempo. */
    private Game withUndo() {
        return game(GameRules.classic().withUndo(), new NoClock());
    }

    private void play(Game game, String... moves) {
        for (String move : moves) {
            game.move(move(move.substring(0, 2), move.substring(2, 4)));
        }
    }

    private InvalidActionException.Reason rejection(Runnable action) {
        return assertThrows(InvalidActionException.class, action::run).reason();
    }

    // ---------------- Deshacer y rehacer ----------------

    @Test
    void undoAMoveLeavesGameExactlyAsBefore() {
        // Arrange
        Game game = withUndo();
        play(game, "e2e4");
        Photo before = photo(game);
        play(game, "e7e5");

        // Act
        game.undo();

        // Assert
        assertEquals(before, photo(game));
    }

    @Test
    void undoACaptureReturnsPieceToItsSquare() {
        // Arrange
        Game game = withUndo();
        play(game, "e2e4", "d7d5");
        Piece blackPawn = game.board().pieceIn(pos("d5")).orElseThrow();
        play(game, "e4d5");

        // Act
        game.undo();

        // Assert
        assertSame(blackPawn, game.board().pieceIn(pos("d5")).orElseThrow());
        assertEquals(32, game.board().pieces().size());
        assertEquals(Side.WHITE, game.turn());
    }

    @Test
    void canUndoUntilStartAndNotMore() {
        // Arrange
        Game game = withUndo();
        Photo initial = photo(game);
        play(game, "e2e4", "e7e5", "g1f3");

        // Act
        game.undo();
        game.undo();
        game.undo();

        // Assert
        assertEquals(initial, photo(game));
        assertFalse(game.canUndo());
        assertEquals(Reason.NADA_PARA_DESHACER, rejection(game::undo));
    }

    @Test
    void anyoneOfTwoPlayersCanUndo() {
        // Arrange: juegan las blancas; ahora le toca a las negras, que deshacen la jugada rival
        Game game = withUndo();
        play(game, "e2e4");

        // Act
        game.undo();

        // Assert
        assertEquals(Side.WHITE, game.turn());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void redoReturnsToExecuteMoveUndone() {
        // Arrange
        Game game = withUndo();
        play(game, "e2e4", "d7d5", "e4d5");
        Photo after = photo(game);
        game.undo();

        // Act
        game.redo();

        // Assert
        assertEquals(after, photo(game));
        assertFalse(game.canRedo());
        assertEquals(Reason.NADA_PARA_REHACER, rejection(game::redo));
    }

    @Test
    void canRedoSeveralMovesInOrder() {
        // Arrange
        Game game = withUndo();
        play(game, "e2e4", "e7e5", "g1f3");
        Photo after = photo(game);
        game.undo();
        game.undo();
        game.undo();

        // Act
        game.redo();
        game.redo();
        game.redo();

        // Assert
        assertEquals(after, photo(game));
    }

    @Test
    void aNewMoveDeletesItThatCouldRedo() {
        // Arrange
        Game game = withUndo();
        play(game, "e2e4", "e7e5");
        game.undo();
        assertTrue(game.canRedo());

        // Act: las negras eligen otra jugada
        play(game, "c7c5");

        // Assert
        assertFalse(game.canRedo());
        assertEquals(Reason.NADA_PARA_REHACER, rejection(game::redo));
        assertEquals(List.of(move("e2", "e4"), move("c7", "c5")), game.history());
    }

    // ---------------- Finales ----------------

    @Test
    void undoCheckMateResumesGameAndRedoItReturnsToEndIt() {
        // Arrange: mate del loco
        Game game = withUndo();
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals(GameState.CHECK_MATE, game.state());
        assertTrue(game.canUndo());

        // Act
        game.undo();

        // Assert
        assertEquals(GameState.IN_PROGRESS, game.state());
        assertTrue(game.result().isEmpty());
        assertTrue(game.winner().isEmpty());
        assertEquals(Side.BLACK, game.turn());

        // Act + Assert: rehacer
        game.redo();
        assertEquals(GameState.CHECK_MATE, game.state());
        assertEquals("Beto", game.winner().orElseThrow().displayName());
    }

    @Test
    void afterOfUndoMateCanPlayOtherThing() {
        // Arrange
        Game game = withUndo();
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");
        game.undo();

        // Act
        play(game, "d8g5");

        // Assert
        assertEquals(GameState.IN_PROGRESS, game.state());
        assertFalse(game.canRedo());
    }

    @Test
    void notUndoesAResignation() {
        // Arrange
        Game game = withUndo();
        play(game, "e2e4");
        game.resign(Side.BLACK);

        // Act + Assert
        assertFalse(game.canUndo());
        assertEquals(Reason.PARTIDA_FINALIZADA, rejection(game::undo));
        assertEquals(GameState.RESIGNATION, game.state());
    }

    @Test
    void notUndoesAAgreementOfDraw() {
        // Arrange
        Game game = withUndo();
        play(game, "e2e4");
        game.offerDraw(Side.BLACK);
        game.acceptDraw(Side.WHITE);

        // Act + Assert
        assertEquals(Reason.PARTIDA_FINALIZADA, rejection(game::undo));
    }

    // ---------------- Cuándo está permitido ----------------

    @Test
    void withoutEnableItInRulesNotCanUndo() {
        // Arrange
        Game game = game(GameRules.classic(), new NoClock());
        play(game, "e2e4");

        // Act + Assert
        assertFalse(game.canUndo());
        assertEquals(Reason.DESHACER_NO_PERMITIDO, rejection(game::undo));
        assertEquals(Reason.DESHACER_NO_PERMITIDO, rejection(game::redo));
    }

    @Test
    void withClockNotCanUndoAlthoughRulesItAllow() {
        // Arrange
        GameClock clock = new ChessClock(Duration.ofMinutes(5), Duration.ofMinutes(5), new FontOfTimeFalse());
        Game game = game(GameRules.classic().withUndo(), clock);
        play(game, "e2e4");

        // Act + Assert
        assertFalse(game.canUndo());
        assertEquals(Reason.DESHACER_NO_PERMITIDO, rejection(game::undo));
    }

    @Test
    void undoAndRedoDeleteOfferOfDrawPending() {
        // Arrange
        Game game = withUndo();
        play(game, "e2e4");
        game.offerDraw(Side.BLACK);

        // Act + Assert: deshacer
        game.undo();
        assertTrue(game.offerDrawPending().isEmpty());

        // Act + Assert: rehacer
        game.offerDraw(Side.WHITE);
        game.redo();
        assertTrue(game.offerDrawPending().isEmpty());
    }

    // ---------------- Condiciones con memoria ----------------

    @Test
    void undoAndRedoNotInventARepetition() {
        // Arrange: la posición inicial aparece por segunda vez
        Game game = withUndo();
        play(game, "g1f3", "g8f6", "f3g1", "f6g8");

        // Act: ir y volver con deshacer y rehacer no la cuenta de nuevo
        game.undo();
        game.redo();

        // Assert
        assertEquals(GameState.IN_PROGRESS, game.state());

        // Act + Assert: una vuelta real más sí es la tercera
        play(game, "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(GameState.DRAW_REPETITION, game.state());
    }

    @Test
    void undoDrawByRepetitionResumesGame() {
        // Arrange
        Game game = withUndo();
        play(game, "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(GameState.DRAW_REPETITION, game.state());

        // Act
        game.undo();

        // Assert
        assertEquals(GameState.IN_PROGRESS, game.state());
    }

    @Test
    void ruleOf50MovementsRecoversItsAccountToUndo() {
        // Arrange: límite reducido a 1 movimiento por jugador (2 jugadas sueltas)
        Game game = game(new GameRules(new CheckDetector(), List.of(
                new Checkmate(), new Stalemate(), new FiftyMoveRuleDraw(1))).withUndo(), new NoClock());
        play(game, "g1f3", "e7e6");    // el peón reinicia la cuenta

        // Act: deshacer el peón devuelve la cuenta a 1; otra jugada suelta llega a 2
        game.undo();
        play(game, "g8f6");

        // Assert
        assertEquals(GameState.DRAW_RULE_50_MOVEMENTS, game.state());
    }

    @Test
    void eachConditionRecoversItsOwnMementoInOrderInverse() {
        // Arrange: dos condiciones con memoria que anotan en qué orden se las restaura
        List<String> order = new ArrayList<>();
        MockConditionWithMemory first = new MockConditionWithMemory() {
            @Override
            public void restore(Memento memento) {
                order.add("primera");
                super.restore(memento);
            }
        };
        MockConditionWithMemory second = new MockConditionWithMemory() {
            @Override
            public void restore(Memento memento) {
                order.add("segunda");
                super.restore(memento);
            }
        };
        Game game = game(new GameRules(new CheckDetector(), List.of(first, second)).withUndo(),
                new NoClock());
        play(game, "e2e4");

        // Act
        game.undo();

        // Assert: cada una recibe el recuerdo que entregó al registrar e2e4 (el segundo registro)
        assertEquals(List.of(first.delivered().get(1)), first.restored());
        assertEquals(List.of(second.delivered().get(1)), second.restored());
        assertEquals(List.of("segunda", "primera"), order);
    }
}
