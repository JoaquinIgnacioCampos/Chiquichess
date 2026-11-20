package core.game;

import core.side.Side;
import core.rules.*;
import core.rules.FinishReason;
import core.time.ChessClock;
import core.time.NoClock;
import doubles.FontOfTimeFalse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static doubles.Squares.move;
import static org.junit.jupiter.api.Assertions.*;

class HistoryTest {
    private Game game() {
        return new Game(new StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                new GameRules(new CheckDetector(), List.of(new Checkmate(), new Stalemate())), new NoClock());
    }

    @Test
    void historyStartsEmptyAndKeepsOrderIncludingCaptures() {
        Game game = game();
        assertTrue(game.history().isEmpty());
        MoveRequest first = move("e2", "e4");
        MoveRequest second = move("d7", "d5");
        MoveRequest capture = move("e4", "d5");
        game.move(first);
        game.move(second);
        assertTrue(game.move(capture).capturedPiece().isPresent());
        assertEquals(List.of(first, second, capture), game.history());
    }

    @Test
    void attemptsRejectedNotRecord() {
        Game game = game();
        assertThrows(InvalidMoveException.class, () -> game.move(move("e2", "e5")));
        game.move(move("e2", "e4"));
        assertThrows(InvalidMoveException.class, () -> game.move(move("d2", "d4")));
        game.resign(Side.BLACK);
        assertThrows(InvalidMoveException.class, () -> game.move(move("e7", "e5")));
        assertEquals(List.of(move("e2", "e4")), game.history());
    }

    @Test
    void queriesReturnCopiasImmutableIndependent() {
        Game game = game();
        game.move(move("e2", "e4"));
        List<MoveRequest> copy = game.history();
        assertThrows(UnsupportedOperationException.class, () -> copy.add(move("e7", "e5")));
        assertThrows(UnsupportedOperationException.class, copy::clear);
        game.move(move("e7", "e5"));
        assertEquals(List.of(move("e2", "e4")), copy);
        assertEquals(List.of(move("e2", "e4"), move("e7", "e5")), game.history());
    }

    @Test
    void moveThatGivesMateAlsoRecords() {
        Game game = game();
        List<MoveRequest> movements = List.of(move("f2", "f3"), move("e7", "e5"),
                move("g2", "g4"), move("d8", "h4"));
        movements.forEach(game::move);
        assertEquals(GameState.CHECK_MATE, game.state());
        assertEquals(movements, game.history());
    }

    @Test
    void offersAndAgreementNotConfuseWithMovements() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        game.rejectDraw(Side.BLACK);
        game.offerDraw(Side.WHITE);
        game.acceptDraw(Side.BLACK);
        assertTrue(game.history().isEmpty());
    }

    @Test
    void rejectedMovementByTimeNotRecords() {
        FontOfTimeFalse font = new FontOfTimeFalse();
        Game game = new Game(new StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                new GameRules(new CheckDetector(), List.of()),
                new ChessClock(Duration.ofSeconds(1), Duration.ofSeconds(1), font));
        font.advance(Duration.ofSeconds(1));
        assertThrows(InvalidMoveException.class, () -> game.move(move("e2", "e4")));
        assertTrue(game.history().isEmpty());
        assertEquals(FinishReason.TIME_EXHAUSTED, game.result().orElseThrow().reason());
    }
}
