package core.game;

import core.side.Side;
import core.rules.*;
import core.rules.FinishReason;
import core.time.NoClock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static doubles.Squares.move;
import static org.junit.jupiter.api.Assertions.*;

class OffersDrawTest {
    private Game game() {
        return new Game(new StandardLayout().createBoard(),
                new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                new GameRules(new CheckDetector(), List.of(new Checkmate(), new Stalemate())), new NoClock());
    }

    @Test
    void offerAndAcceptEndsByAgreementWithoutWinner() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        assertEquals(Optional.of(Side.WHITE), game.offerDrawPending());
        game.acceptDraw(Side.BLACK);
        assertEquals(GameState.DRAW_AGREEMENT, game.state());
        assertTrue(game.state().isFinished());
        assertTrue(game.state().isDraw());
        assertEquals(new GameResult(Optional.empty(), FinishReason.AGREEMENT),
                game.result().orElseThrow());
        assertTrue(game.winner().isEmpty());
        assertTrue(game.offerDrawPending().isEmpty());
        assertThrows(InvalidMoveException.class, () -> game.move(move("e2", "e4")));
    }

    @Test
    void blackAlsoCanOfferDuringItsTurn() {
        Game game = game();
        game.move(move("e2", "e4"));
        game.offerDraw(Side.BLACK);
        game.acceptDraw(Side.WHITE);
        assertEquals(FinishReason.AGREEMENT, game.result().orElseThrow().reason());
    }

    @Test
    void rejectDeletesOfferAndAllowsFollowPlaying() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        game.rejectDraw(Side.BLACK);
        assertTrue(game.offerDrawPending().isEmpty());
        assertEquals(GameState.IN_PROGRESS, game.state());
        game.offerDraw(Side.WHITE);
        assertEquals(Optional.of(Side.WHITE), game.offerDrawPending());
    }

    @Test
    void acceptWithoutOfferRejects() {
        var error = assertThrows(InvalidActionException.class,
                () -> game().acceptDraw(Side.BLACK));
        assertEquals(InvalidActionException.Reason.SIN_OFERTA, error.reason());
    }

    @Test
    void rejectWithoutOfferRejects() {
        var error = assertThrows(InvalidActionException.class,
                () -> game().rejectDraw(Side.BLACK));
        assertEquals(InvalidActionException.Reason.SIN_OFERTA, error.reason());
    }

    @Test
    void nobodyAcceptsNorRejectsItsOwnOffer() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        var error = assertThrows(InvalidActionException.class,
                () -> game.acceptDraw(Side.WHITE));
        assertEquals(InvalidActionException.Reason.ES_SU_PROPIA_OFERTA, error.reason());
        assertThrows(InvalidActionException.class, () -> game.rejectDraw(Side.WHITE));
        assertEquals(Optional.of(Side.WHITE), game.offerDrawPending());
        assertTrue(game.result().isEmpty());
    }

    @Test
    void offerTwoTimesNotReplacesOffer() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        var error = assertThrows(InvalidActionException.class,
                () -> game.offerDraw(Side.WHITE));
        assertEquals(InvalidActionException.Reason.OFERTA_PENDIENTE, error.reason());
        assertEquals(Optional.of(Side.WHITE), game.offerDrawPending());
    }

    @Test
    void offerOutsideOfTurnRejects() {
        Game game = game();
        var error = assertThrows(InvalidActionException.class,
                () -> game.offerDraw(Side.BLACK));
        assertEquals(InvalidActionException.Reason.NO_ES_SU_TURNO, error.reason());
        assertTrue(game.offerDrawPending().isEmpty());
    }

    @Test
    void moveOffererKeepsOfferButMoveRivalDeletes() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        game.move(move("e2", "e4"));
        assertEquals(Optional.of(Side.WHITE), game.offerDrawPending());
        game.move(move("e7", "e5"));
        assertTrue(game.offerDrawPending().isEmpty());
        assertEquals(GameState.IN_PROGRESS, game.state());
    }

    @Test
    void movementInvalidOfRivalKeepsOffer() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        game.move(move("e2", "e4"));
        assertThrows(InvalidMoveException.class, () -> game.move(move("e7", "e4")));
        assertEquals(Optional.of(Side.WHITE), game.offerDrawPending());
        game.acceptDraw(Side.BLACK);
        assertEquals(GameState.DRAW_AGREEMENT, game.state());
    }

    @Test
    void offerAndRespondAfterFinishRejects() {
        Game game = game();
        game.offerDraw(Side.WHITE);
        game.resign(Side.WHITE);
        assertTrue(game.offerDrawPending().isEmpty());
        var error = assertThrows(InvalidActionException.class,
                () -> game.offerDraw(Side.WHITE));
        assertEquals(InvalidActionException.Reason.PARTIDA_FINALIZADA, error.reason());
        assertThrows(InvalidActionException.class, () -> game.acceptDraw(Side.BLACK));
        assertThrows(InvalidActionException.class, () -> game.rejectDraw(Side.BLACK));
    }

    @Test
    void canOfferAndResignBeingInCheck() {
        Game game = game();
        game.move(move("e2", "e4"));
        game.move(move("f7", "f6"));
        game.move(move("d1", "h5"));
        assertEquals(GameState.CHECK, game.state());
        assertTrue(game.result().isEmpty());
        game.offerDraw(Side.BLACK);
        game.resign(Side.BLACK);
        assertEquals(Side.WHITE, game.winner().orElseThrow().color());
        assertTrue(game.offerDrawPending().isEmpty());
    }

    @Test
    void colorsNullNotModifyOffer() {
        Game game = game();
        assertThrows(NullPointerException.class, () -> game.offerDraw(null));
        game.offerDraw(Side.WHITE);
        assertThrows(NullPointerException.class, () -> game.acceptDraw(null));
        assertThrows(NullPointerException.class, () -> game.rejectDraw(null));
        assertEquals(Optional.of(Side.WHITE), game.offerDrawPending());
    }
}
