package core.game;

import core.side.Side;
import core.moves.Move;
import core.rules.*;
import core.rules.FinishReason;
import core.time.ChessClock;
import core.time.GameClock;
import core.board.Board;
import core.time.NoClock;
import doubles.FontOfTimeFalse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static doubles.Squares.move;
import static org.junit.jupiter.api.Assertions.*;

class GameWithClockTest {
    private final FontOfTimeFalse font = new FontOfTimeFalse();

    private Game game(EndCondition... conditions) {
        return withClock(new ChessClock(Duration.ofMinutes(5), Duration.ofMinutes(5), font), conditions);
    }

    private Game withClock(GameClock clock, EndCondition... conditions) {
        return new Game(new StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                new GameRules(new CheckDetector(), List.of(conditions)), clock);
    }

    private void advance(long seconds) {
        font.advance(Duration.ofSeconds(seconds));
    }

    private void assertTime(Game game, Side color, long seconds) {
        assertEquals(Optional.of(Duration.ofSeconds(seconds)), game.timeRemaining(color));
    }

    @Test
    void startsWithWhiteAndAfterMoveRunsBlack() {
        Game game = game();
        advance(30);
        assertTime(game, Side.WHITE, 270);
        assertTime(game, Side.BLACK, 300);
        game.move(move("e2", "e4"));
        advance(10);
        assertTime(game, Side.WHITE, 270);
        assertTime(game, Side.BLACK, 290);
    }

    @Test
    void movementInvalidNotChangesNorRestartsClock() {
        Game game = game();
        advance(30);
        assertThrows(InvalidMoveException.class, () -> game.move(move("e2", "e5")));
        advance(10);
        assertEquals(Side.WHITE, game.turn());
        assertTime(game, Side.WHITE, 260);
        assertTime(game, Side.BLACK, 300);
    }

    @Test
    void exactZeroVerifiedEndsWithVictoryOfRival() {
        Game game = game();
        advance(300);
        // Las lecturas son puras; finalizar es una orden explícita.
        assertTime(game, Side.WHITE, 0);
        assertTrue(game.result().isEmpty());
        game.verifyTime();
        assertEquals(GameState.TIME_EXHAUSTED, game.state());
        assertEquals(new GameResult(Optional.of(Side.BLACK), FinishReason.TIME_EXHAUSTED),
                game.result().orElseThrow());
        assertEquals(Side.BLACK, game.winner().orElseThrow().color());
        advance(60);
        game.verifyTime();
        assertTime(game, Side.WHITE, 0);
        assertTime(game, Side.BLACK, 300);
    }

    @Test
    void blackAlsoLoseByTimeDuringItsTurn() {
        Game game = game();
        game.move(move("e2", "e4"));
        advance(301);
        game.verifyTime();
        assertEquals(Side.WHITE, game.winner().orElseThrow().color());
        assertTime(game, Side.BLACK, 0);
    }

    @Test
    void moveWithTimeExpiredFinishesAndNotExecutesMove() {
        Game game = game();
        var boardBefore = new HashMap<>(game.board().pieces());
        advance(300);
        var error = assertThrows(InvalidMoveException.class,
                () -> game.move(move("e2", "e4")));
        assertEquals(InvalidMoveException.Reason.PARTIDA_FINALIZADA, error.reason());
        assertEquals(boardBefore, game.board().pieces());
        assertEquals(Side.WHITE, game.turn());
        assertEquals(FinishReason.TIME_EXHAUSTED, game.result().orElseThrow().reason());
    }

    @Test
    void ifValidationExhaustsTimeRejectsBeforeOfExecute() {
        ICheckDetector detector = new CheckDetector();
        LegalMoves validation = new LegalMoves(detector) {
            @Override
            public boolean existsSome(Board board, Side color, List<Move> history) {
                return true;
            }

            @Override
            public boolean leavesKingInCheck(Board board, Move move) {
                font.advance(Duration.ofMinutes(5));
                return false;
            }
        };
        Game game = new Game(new StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                new GameRules(validation, List.of()),
                new ChessClock(Duration.ofMinutes(5), Duration.ofMinutes(5), font));
        game.offerDraw(Side.WHITE);
        var boardBefore = new HashMap<>(game.board().pieces());
        var error = assertThrows(InvalidMoveException.class,
                () -> game.move(move("e2", "e4")));
        assertEquals(InvalidMoveException.Reason.PARTIDA_FINALIZADA, error.reason());
        assertEquals(boardBefore, game.board().pieces());
        assertEquals(Side.WHITE, game.turn());
        assertTrue(game.history().isEmpty());
        assertTrue(game.offerDrawPending().isEmpty());
        assertEquals(new GameResult(Optional.of(Side.BLACK), FinishReason.TIME_EXHAUSTED),
                game.result().orElseThrow());
        assertTime(game, Side.WHITE, 0);
        assertTime(game, Side.BLACK, 300);
    }

    @Test
    void verifyWithTimeOfLeftNotChangesGameOfferNorClock() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        advance(10);
        game.verifyTime();
        game.verifyTime();
        assertEquals(GameState.IN_PROGRESS, game.state());
        assertEquals(Optional.of(Side.WHITE), game.offerDrawPending());
        advance(10);
        assertTime(game, Side.WHITE, 280);
        assertTime(game, Side.BLACK, 300);
    }

    @Test
    void resignationStopsBothClocksAndNotReplacesByTime() {
        Game game = game();
        advance(30);
        game.resign(Side.WHITE);
        advance(1000);
        game.verifyTime();
        assertTime(game, Side.WHITE, 270);
        assertTime(game, Side.BLACK, 300);
        assertEquals(FinishReason.RESIGNATION, game.result().orElseThrow().reason());
        assertEquals(Side.BLACK, game.winner().orElseThrow().color());
    }

    @Test
    void agreementStopsClockWhileThatOfferAndRejectionNotItStop() {
        Game game = game();
        advance(10);
        game.offerDraw(Side.WHITE);
        advance(10);
        game.rejectDraw(Side.BLACK);
        advance(10);
        game.offerDraw(Side.WHITE);
        game.acceptDraw(Side.BLACK);
        advance(100);
        assertTime(game, Side.WHITE, 270);
        assertTime(game, Side.BLACK, 300);
        assertEquals(FinishReason.AGREEMENT, game.result().orElseThrow().reason());
    }

    @Test
    void eachFinalAutomaticStopsClockAndDeletesOffer() {
        for (GameState finalAutomatic : List.of(GameState.CHECK_MATE,
                GameState.DRAW_STALEMATE, GameState.DRAW_REPETITION,
                GameState.DRAW_RULE_50_MOVEMENTS)) {
            Game game = game(c -> c.pieceMoved().isPresent()
                    ? Optional.of(finalAutomatic) : Optional.empty());
            game.offerDraw(Side.WHITE);
            advance(15);
            game.move(move("e2", "e4"));
            advance(100);
            assertEquals(finalAutomatic, game.state());
            assertTrue(game.offerDrawPending().isEmpty());
            assertTime(game, Side.WHITE, 285);
            assertTime(game, Side.BLACK, 300);
        }
    }

    @Test
    void aInitialBoardTerminalNotLeavesClockRunning() {
        Game game = game(c -> Optional.of(GameState.CHECK_MATE));
        advance(300);
        assertTime(game, Side.WHITE, 300);
        assertTime(game, Side.BLACK, 300);
        game.verifyTime();
        assertEquals(FinishReason.CHECK_MATE, game.result().orElseThrow().reason());
    }

    @Test
    void expiryDeletesOfferAndNotAllowsAcceptNorResignAfter() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        advance(300);
        assertThrows(InvalidActionException.class, () -> game.acceptDraw(Side.BLACK));
        assertTrue(game.offerDrawPending().isEmpty());
        assertEquals(FinishReason.TIME_EXHAUSTED, game.result().orElseThrow().reason());
        assertThrows(InvalidActionException.class, () -> game.resign(Side.BLACK));
    }

    @Test
    void constructorPreviousFollowsWithoutLimitOfTime() {
        Game game = new Game(new StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                new GameRules(new CheckDetector(), List.of()), new NoClock());
        advance(100000);
        game.verifyTime();
        assertTrue(game.timeRemaining(Side.WHITE).isEmpty());
        assertTrue(game.timeRemaining(Side.BLACK).isEmpty());
        game.move(move("e2", "e4"));
        assertEquals(Side.BLACK, game.turn());
        assertTrue(game.result().isEmpty());
    }

    @Test
    void clockNullRejectsToBuild() {
        assertThrows(NullPointerException.class, () -> withClock(null));
    }
}
