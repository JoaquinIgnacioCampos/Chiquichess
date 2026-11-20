package adapters.ui;

import core.side.Side;
import core.game.*;
import core.rules.CheckDetector;
import core.rules.FinishReason;
import core.rules.GameRules;
import core.time.ChessClock;
import core.time.NoClock;
import adapters.GameAssembler;
import doubles.FontOfTimeFalse;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static doubles.Squares.*;
import static org.junit.jupiter.api.Assertions.*;

class BoardControllerTest {
    private final GameInProgress game = new GameAssembler().createWithoutClock();
    private final BoardController controller = new BoardController(game);

    @Test
    void selectQueryDestinationsRealWithoutMove() {
        assertTrue(controller.press(pos("e2")).isEmpty());
        assertEquals(Optional.of(pos("e2")), controller.selected());
        assertEquals(positions("e3", "e4"), controller.destinationsLegal());
        assertTrue(game.history().isEmpty());
        assertEquals(Side.WHITE, game.turn());
    }

    @Test
    void otherOwnPieceReplacesSelectionAndDestinations() {
        controller.press(pos("e2"));
        controller.press(pos("g1"));
        assertEquals(Optional.of(pos("g1")), controller.selected());
        assertEquals(positions("f3", "h3"), controller.destinationsLegal());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void emptySquareNotAllowedRemovesSelection() {
        controller.press(pos("e2"));
        assertTrue(controller.press(pos("e6")).isEmpty());
        assertTrue(controller.selected().isEmpty());
        assertTrue(controller.destinationsLegal().isEmpty());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void pieceEnemyNotCapturableRemovesSelection() {
        controller.press(pos("e2"));
        controller.press(pos("a7"));
        assertTrue(controller.selected().isEmpty());
        assertTrue(controller.destinationsLegal().isEmpty());
        assertEquals(32, game.board().pieces().size());
    }

    @Test
    void notSelectsPiecesOfRival() {
        controller.press(pos("e7"));
        assertTrue(controller.selected().isEmpty());
        assertTrue(controller.destinationsLegal().isEmpty());
    }

    @Test
    void aDestinationAllowedExecutesByCoreAndPassesTurn() {
        controller.press(pos("e2"));
        assertTrue(controller.press(pos("e4")).isEmpty());
        assertTrue(game.board().pieceIn(pos("e2")).isEmpty());
        assertEquals(Side.WHITE, game.board().pieceIn(pos("e4")).orElseThrow().color());
        assertEquals(Side.BLACK, game.turn());
        assertEquals(List.of(move("e2", "e4")), game.history());
        assertTrue(controller.selected().isEmpty());
        assertTrue(controller.destinationsLegal().isEmpty());
        controller.press(pos("e7"));
        assertEquals(positions("e5", "e6"), controller.destinationsLegal());
    }

    @Test
    void aEnemyCapturableIsADestinationAndCaptureResolvesGame() {
        game.move(move("e2", "e4"));
        game.move(move("d7", "d5"));
        controller.press(pos("e4"));
        assertEquals(positions("e5", "d5"), controller.destinationsLegal());
        controller.press(pos("d5"));
        assertTrue(game.board().pieceIn(pos("e4")).isEmpty());
        assertEquals(Side.WHITE, game.board().pieceIn(pos("d5")).orElseThrow().color());
        assertEquals(31, game.board().pieces().size());
        assertEquals(move("e4", "d5"), game.history().getLast());
        assertEquals(Side.BLACK, game.turn());
        assertTrue(controller.selected().isEmpty());
    }

    @Test
    void destinationsExcludeMovesThatLeaveToKingInCheck() {
        GameInProgress pinned = new Game(BoardFake.from(
                "r...t...", "........", "........", "........",
                "........", "........", "....T...", "....R..."),
                new Player("B", Side.WHITE), new Player("N", Side.BLACK),
                new GameRules(new CheckDetector(), List.of()), new NoClock());
        BoardController interaction = new BoardController(pinned);
        interaction.press(pos("e2"));
        assertTrue(interaction.destinationsLegal().contains(pos("e3")));
        assertFalse(interaction.destinationsLegal().contains(pos("f2")));
        interaction.press(pos("f2"));
        assertTrue(interaction.selected().isEmpty());
        assertTrue(pinned.board().pieceIn(pos("e2")).isPresent());
        assertTrue(pinned.history().isEmpty());
    }

    @Test
    void coreCanRejectAHighlightedMoveByTimeExhausted() {
        FontOfTimeFalse font = new FontOfTimeFalse();
        GameInProgress timed = new Game(new StandardLayout().createBoard(),
                new Player("B", Side.WHITE), new Player("N", Side.BLACK),
                new GameRules(new CheckDetector(), List.of()),
                new ChessClock(Duration.ofSeconds(10), Duration.ofSeconds(10), font));
        BoardController interaction = new BoardController(timed);
        interaction.press(pos("e2"));
        var boardBefore = new HashMap<>(timed.board().pieces());
        font.advance(Duration.ofSeconds(10));
        assertEquals(Optional.of(InvalidMoveException.Reason.PARTIDA_FINALIZADA),
                interaction.press(pos("e4")));
        assertEquals(FinishReason.TIME_EXHAUSTED, timed.result().orElseThrow().reason());
        assertEquals(boardBefore, timed.board().pieces());
        assertTrue(timed.history().isEmpty());
        assertTrue(interaction.selected().isEmpty());
    }

    @Test
    void notAllowsSelectInAEndedGame() {
        game.resign(Side.WHITE);
        assertEquals(Optional.of(InvalidMoveException.Reason.PARTIDA_FINALIZADA),
                controller.press(pos("e2")));
        assertTrue(controller.selected().isEmpty());
        assertTrue(controller.destinationsLegal().isEmpty());
    }

    @Test
    void changeExternalOfTurnInvalidSelectionWithoutKeepDestinationsOld() {
        controller.press(pos("e2"));
        game.move(move("g1", "f3"));
        assertTrue(controller.selected().isEmpty());
        assertTrue(controller.destinationsLegal().isEmpty());
    }

    @Test
    void destinationsExposedNotAreMutable() {
        controller.press(pos("e2"));
        assertThrows(UnsupportedOperationException.class, () -> controller.destinationsLegal().clear());
        assertEquals(positions("e3", "e4"), controller.destinationsLegal());
    }
}
