package core.rules;

import core.side.Side;
import core.game.Player;
import core.game.InvalidMoveException;
import core.game.Game;
import core.board.Position;
import core.board.Board;
import core.time.NoClock;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/** Los pases del modo fútbol, jugados en partidas reales. */
class PassTest {

    private static Game game(String ball, String... rows) {
        Board board = BoardFake.from(rows);
        new BallRule().preparePitch(board);
        board.placeBall(pos(ball));
        return new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.football().withUndo(), new NoClock());
    }

    private static Optional<Position> ball(Game game) {
        return game.board().ball();
    }

    private static char codeIn(Game game, String square) {
        return game.board().pieceIn(pos(square)).orElseThrow().code();
    }

    @Test
    void kingFreesOfBallPassingItToAPartnerOfToSide() {
        // Arrange: el rey blanco de e2 tiene la pelota y el caballo está en d3
        Game game = game("e2",
                ".......k",
                "........",
                "........",
                "........",
                "........",
                "...N....",
                "....K...",
                "........");

        // Act
        game.move(move("e2", "d3"));

        // Assert: ninguna pieza se movió, la pelota la tiene el caballo y el turno pasó
        assertEquals(Optional.of(pos("d3")), ball(game));
        assertEquals('K', codeIn(game, "e2"));
        assertEquals('N', codeIn(game, "d3"));
        assertEquals(Side.BLACK, game.turn());
    }

    @Test
    void rookPassesOnlyWithPathFree() {
        // Arrange
        String[] free = {
                "....k...",
                "........",
                "........",
                "N.......",
                "........",
                "........",
                "........",
                "R...K..."};
        String[] covered = free.clone();
        covered[5] = "p.......";

        // Act + Assert
        assertTrue(game("a1", free).movementsLegal(pos("a1")).contains(pos("a5")));
        Game withRivalInMiddle = game("a1", covered);
        assertFalse(withRivalInMiddle.movementsLegal(pos("a1")).contains(pos("a5")));
        assertThrows(InvalidMoveException.class, () -> withRivalInMiddle.move(move("a1", "a5")));
    }

    @Test
    void knightPassesByUp() {
        // Arrange: el caballo de b1 está rodeado de peones propios
        Game game = game("b1",
                "....k...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "PPPP....",
                ".N..K...");

        // Act
        game.move(move("b1", "d2"));

        // Assert
        assertEquals(Optional.of(pos("d2")), ball(game));
        assertEquals('N', codeIn(game, "b1"));
    }

    @Test
    void pawnPassesOnlyInDiagonalTowardForward() {
        // Arrange: compañeros en e5 (diagonal adelante), c3 (diagonal atrás) y d5 (adelante derecho)
        Game game = game("d4",
                "....k...",
                "........",
                "........",
                "...RN...",
                "...P....",
                "..N.....",
                "........",
                "....K...");

        // Act
        Set<Position> destinations = game.movementsLegal(pos("d4"));

        // Assert
        assertTrue(destinations.contains(pos("e5")));
        assertFalse(destinations.contains(pos("c3")));
        assertFalse(destinations.contains(pos("d5")));
    }

    @Test
    void passToToAPartnerThatIsInNetRivalIsGoal() {
        // Arrange
        Game game = game("e2",
                "....N..k",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...",
                "K.......");

        // Act
        game.move(move("e2", "e8"));

        // Assert: gol y la pelota al saque; la torre no se movió
        assertEquals(new Scoreboard(1, 0), game.scoreboard().orElseThrow());
        assertEquals(Optional.of(pos("d5")), ball(game));
        assertEquals('R', codeIn(game, "e2"));
    }

    @Test
    void passToToKingInNetNotIsGoal() {
        // Arrange
        Game game = game("e2",
                "....K..k",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...",
                "........");

        // Act
        game.move(move("e2", "e8"));

        // Assert
        assertEquals(new Scoreboard(0, 0), game.scoreboard().orElseThrow());
        assertEquals(Optional.of(pos("e8")), ball(game));
    }

    @Test
    void inCheckNotCanPass() {
        // Arrange: la torre negra de e8 le da jaque al rey blanco de e1
        Game game = game("a1",
                "....r..k",
                "........",
                "........",
                "N.......",
                "........",
                "........",
                "........",
                "R...K...");

        // Act + Assert
        assertFalse(game.movementsLegal(pos("a1")).contains(pos("a5")));
    }

    @Test
    void aPawnThatPassesFromSeventhNotPromotesAndCanDoGoal() {
        // Arrange: peón blanco en c7 con la pelota, caballo blanco en d8 (arco negro)
        Game game = game("c7",
                "...N...k",
                "..P.....",
                "........",
                "........",
                "........",
                "........",
                "........",
                "K.......");

        // Act
        assertTrue(game.promotionOptions(pos("c7"), pos("d8")).isEmpty());
        game.move(move("c7", "d8"));

        // Assert
        assertEquals('P', codeIn(game, "c7"));
        assertEquals('N', codeIn(game, "d8"));
        assertEquals(new Scoreboard(1, 0), game.scoreboard().orElseThrow());
    }

    @Test
    void undoAPassReturnsBall() {
        // Arrange
        Game game = game("e2",
                ".......k",
                "........",
                "........",
                "........",
                "........",
                "...N....",
                "....K...",
                "........");
        game.move(move("e2", "d3"));

        // Act
        game.undo();

        // Assert
        assertEquals(Optional.of(pos("e2")), ball(game));
        assertEquals(Side.WHITE, game.turn());
    }

    @Test
    void onlyCanPassWhoHasBall() {
        // Arrange: la pelota la tiene el caballo de d3, no el rey
        Game game = game("d3",
                ".......k",
                "........",
                "........",
                "........",
                "........",
                "...N....",
                "....K...",
                "........");

        // Act + Assert
        assertFalse(game.movementsLegal(pos("e2")).contains(pos("d3")));
    }
}
