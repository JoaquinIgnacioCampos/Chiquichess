package core.game;

import core.side.Side;
import core.game.GameEvent.MoveUndone;
import core.game.GameEvent.MoveMade;
import core.game.GameEvent.MoveRedone;
import core.game.GameEvent.FinishedGame;
import core.game.GameEvent.Trait;
import core.rules.GameState;
import core.rules.FinishReason;
import core.rules.BallRule;
import core.rules.GameRules;
import core.board.Board;
import core.time.ChessClock;
import core.time.NoClock;
import doubles.FontOfTimeFalse;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/** Los avisos que Partida publica a sus observadores (Observer). */
class EventsOfGameTest {

    private final List<GameEvent> received = new ArrayList<>();

    private Game observed(Game game) {
        game.addObserver(received::add);
        return game;
    }

    private Game classic() {
        return observed(new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("Beto", Side.BLACK), GameRules.classic().withUndo(), new NoClock()));
    }

    private Game built(GameRules rules, String... rows) {
        return observed(new Game(BoardFake.from(rows), new Player("Ana", Side.WHITE),
                new Player("Beto", Side.BLACK), rules, new NoClock()));
    }

    private void play(Game game, String... moves) {
        for (String move : moves) {
            game.move(move(move.substring(0, 2), move.substring(2, 4)));
        }
    }

    private MoveMade lastMove() {
        for (int i = received.size() - 1; i >= 0; i--) {
            if (received.get(i) instanceof MoveMade made) return made;
        }
        return fail("No move was notified");
    }

    @Test
    void aCommonMoveNotifiesWhoPlayedWithoutTraits() {
        // Arrange
        Game game = classic();

        // Act
        play(game, "e2e4");

        // Assert
        assertEquals(List.of(new MoveMade(Side.WHITE, Set.of(), GameState.IN_PROGRESS)), received);
    }

    @Test
    void aRejectedMoveNotNotifiesNothing() {
        // Arrange
        Game game = classic();

        // Act
        assertThrows(InvalidMoveException.class, () -> play(game, "e2e5"));

        // Assert
        assertTrue(received.isEmpty());
    }

    @Test
    void captureAndCheckStayInNotice() {
        // Arrange: la dama blanca puede tomar el peón de f7 dando jaque
        Game game = built(GameRules.classic(),
                "....k...",
                ".....p..",
                "........",
                "........",
                "........",
                "........",
                "........",
                "K....Q..");

        // Act
        play(game, "f1f7");

        // Assert
        assertEquals(new MoveMade(Side.WHITE, Set.of(Trait.CAPTURE), GameState.CHECK), lastMove());
    }

    @Test
    void castlingRecognizesByThatMovesTwoPieces() {
        // Arrange
        Game game = built(GameRules.classic(),
                "....k...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....K..R");

        // Act
        play(game, "e1g1");

        // Assert
        assertEquals(Set.of(Trait.CASTLING), lastMove().traits());
    }

    @Test
    void promotionStaysInNotice() {
        // Arrange
        Game game = built(GameRules.classic(),
                "k.......",
                "......P.",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....K...");

        // Act
        play(game, "g7g8");

        // Assert
        assertTrue(lastMove().has(Trait.PROMOTION));
    }

    @Test
    void mateNotifiesFirstMoveAndAfterFinal() {
        // Arrange
        Game game = classic();

        // Act: mate del loco
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");

        // Assert
        int amount = received.size();
        assertEquals(new MoveMade(Side.BLACK, Set.of(), GameState.CHECK_MATE), received.get(amount - 2));
        assertEquals(new FinishedGame(new GameResult(Optional.of(Side.BLACK), FinishReason.CHECK_MATE)),
                received.get(amount - 1));
    }

    @Test
    void resignationAndAgreementOfDrawNotifyFinal() {
        // Arrange
        Game resigned = classic();
        Game agreed = classic();

        // Act
        resigned.resign(Side.WHITE);
        agreed.offerDraw(Side.WHITE);
        agreed.acceptDraw(Side.BLACK);

        // Assert
        assertEquals(List.of(
                new FinishedGame(new GameResult(Optional.of(Side.BLACK), FinishReason.RESIGNATION)),
                new FinishedGame(new GameResult(Optional.empty(), FinishReason.AGREEMENT))), received);
    }

    @Test
    void timeExhaustedNotifiesAAloneTime() {
        // Arrange
        FontOfTimeFalse font = new FontOfTimeFalse();
        Game game = observed(new Game(new StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK), GameRules.classic(),
                new ChessClock(Duration.ofMinutes(1), Duration.ofMinutes(1), font)));

        // Act
        font.advance(Duration.ofMinutes(2));
        game.verifyTime();
        game.verifyTime();

        // Assert
        assertEquals(List.of(new FinishedGame(
                new GameResult(Optional.of(Side.BLACK), FinishReason.TIME_EXHAUSTED))), received);
    }

    @Test
    void undoAndRedoNotifyWithStateThatStays() {
        // Arrange
        Game game = classic();
        play(game, "e2e4");
        received.clear();

        // Act
        game.undo();
        game.redo();

        // Assert
        assertEquals(List.of(new MoveUndone(GameState.IN_PROGRESS),
                new MoveRedone(Side.WHITE, Set.of(), GameState.IN_PROGRESS)), received);
    }

    @Test
    void passThatEndsInNetRivalNotifiesPassAndGoal() {
        // Arrange: el caballo blanco espera en el arco rival y la torre tiene la pelota
        Board board = BoardFake.from(
                "....N..k",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...",
                "K.......");
        new BallRule().preparePitch(board);
        board.placeBall(pos("e2"));
        Game game = observed(new Game(board, new Player("Ana", Side.WHITE),
                new Player("Beto", Side.BLACK), GameRules.football(), new NoClock()));

        // Act
        play(game, "e2e8");

        // Assert
        assertEquals(new MoveMade(Side.WHITE, Set.of(Trait.PASS, Trait.GOAL), GameState.IN_PROGRESS),
                lastMove());
    }

    @Test
    void aObserverRemovedNotReceivesMoreNotices() {
        // Arrange
        Game game = new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("Beto", Side.BLACK), GameRules.classic(), new NoClock());
        GameObserver observer = received::add;
        game.addObserver(observer);

        // Act
        game.removeObserver(observer);
        play(game, "e2e4");

        // Assert
        assertTrue(received.isEmpty());
    }
}
