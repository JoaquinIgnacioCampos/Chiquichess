package integration;

import core.side.Side;
import core.game.*;
import core.game.InvalidMoveException.Reason;
import core.rules.*;
import core.board.Board;
import core.time.NoClock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración: todas las clases reales trabajando juntas, armadas igual que en Main.
 * No hay dobles; se juegan partidas cortas y se verifica el resultado de punta a punta.
 */
class GameCompleteTest {

    private final Player white = new Player("Jugador 1", Side.WHITE);
    private final Player black = new Player("Jugador 2", Side.BLACK);

    private Game gameStandard() {
        return newGame(new StandardLayout().createBoard(), conditionsStandard());
    }

    private Game newGame(Board board, List<EndCondition> conditions) {
        return new Game(board, white, black,
                new GameRules(new CheckDetector(), conditions), new NoClock());
    }

    private List<EndCondition> conditionsStandard() {
        return List.of(new Checkmate(), new Stalemate(),
                new RepetitionDraw(), new FiftyMoveRuleDraw());
    }

    /** Juega una secuencia de jugadas escritas como "e2e4". Devuelve el resultado de la última. */
    private MoveResult play(Game game, String... moves) {
        MoveResult last = null;
        for (String move : moves) {
            last = game.move(move(move.substring(0, 2), move.substring(2, 4)));
        }
        return last;
    }

    @Test
    void mateOfFoolWinBlack() {
        // Arrange
        Game game = gameStandard();

        // Act
        MoveResult result = play(game, "f2f3", "e7e5", "g2g4", "d8h4");

        // Assert
        assertEquals(GameState.CHECK_MATE, result.state());
        assertEquals(Optional.of(black), game.winner());
    }

    @Test
    void mateOfScholarWinWhiteCapturingInF7() {
        // Arrange
        Game game = gameStandard();

        // Act
        MoveResult result = play(game,
                "e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7");

        // Assert
        assertTrue(result.capturedPiece().orElseThrow().isPawn());
        assertEquals(GameState.CHECK_MATE, game.state());
        assertEquals(Optional.of(white), game.winner());
    }

    @Test
    void afterOfMateNotCanFollowPlaying() {
        // Arrange
        Game game = gameStandard();
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");

        // Act
        InvalidMoveException e = assertThrows(InvalidMoveException.class,
                () -> play(game, "a2a3"));

        // Assert
        assertEquals(Reason.PARTIDA_FINALIZADA, e.reason());
    }

    @Test
    void aCaptureTakesPieceAndPassesTurn() {
        // Arrange
        Game game = gameStandard();
        play(game, "e2e4", "d7d5");

        // Act
        MoveResult result = play(game, "e4d5");

        // Assert
        assertTrue(result.capturedPiece().orElseThrow().isPawn());
        assertEquals(Side.BLACK, result.capturedPiece().get().color());
        assertEquals(31, game.board().pieces().size());
        assertEquals(Side.BLACK, game.turn());
        assertEquals(GameState.IN_PROGRESS, result.state());
    }

    @Test
    void inCheckOnlyAcceptMovesThatItResolve() {
        // Arrange: la dama blanca en h5 da jaque por la diagonal abierta e8-h5
        Game game = gameStandard();
        play(game, "e2e4", "f7f6", "d1h5");
        assertEquals(GameState.CHECK, game.state());

        // Act: una jugada que ignora el jaque
        InvalidMoveException e = assertThrows(InvalidMoveException.class,
                () -> play(game, "a7a6"));

        // Assert
        assertEquals(Reason.DEJA_AL_REY_EN_JAQUE, e.reason());
        assertEquals(Side.BLACK, game.turn());

        // Act: tapar el jaque con el peón
        MoveResult result = play(game, "g7g6");

        // Assert
        assertEquals(GameState.IN_PROGRESS, result.state());
    }

    @Test
    void piecesRespectTheirRulesInsideOfAGameReal() {
        // Arrange
        Game game = gameStandard();

        // Act + Assert: el alfil no puede saltar sus propios peones
        InvalidMoveException e = assertThrows(InvalidMoveException.class,
                () -> play(game, "c1e3"));
        assertEquals(Reason.MOVIMIENTO_NO_PERMITIDO, e.reason());

        // Act + Assert: el caballo sí puede saltar
        assertDoesNotThrow(() -> play(game, "g1f3"));
    }

    @Test
    void repeatThreeTimesSamePositionAreDraw() {
        // Arrange: los caballos van y vuelven; la posición inicial se repite tras cada 4 jugadas
        Game game = gameStandard();
        play(game, "g1f3", "g8f6", "f3g1", "f6g8");   // posición inicial por 2ª vez
        play(game, "g1f3", "g8f6", "f3g1");
        assertEquals(GameState.IN_PROGRESS, game.state());

        // Act
        MoveResult result = play(game, "f6g8"); // posición inicial por 3ª vez

        // Assert
        assertEquals(GameState.DRAW_REPETITION, result.state());
        assertTrue(game.winner().isEmpty());
    }

    @Test
    void withoutCapturesNorMovementsOfPawnArrivesToDrawBy50Movements() {
        // Arrange: límite reducido a 2 movimientos por jugador para no jugar 100 jugadas
        Game game = newGame(new StandardLayout().createBoard(),
                List.of(new Checkmate(), new Stalemate(), new FiftyMoveRuleDraw(2)));
        play(game, "g1f3", "g8f6", "f3g1");
        assertEquals(GameState.IN_PROGRESS, game.state());

        // Act
        MoveResult result = play(game, "f6g8");

        // Assert
        assertEquals(GameState.DRAW_RULE_50_MOVEMENTS, result.state());
    }

    @Test
    void aMovementOfPawnRestartsAccountOf50Movements() {
        // Arrange
        Game game = newGame(new StandardLayout().createBoard(),
                List.of(new Checkmate(), new Stalemate(), new FiftyMoveRuleDraw(2)));
        play(game, "g1f3", "g8f6", "f3g1");

        // Act: el peón reinicia la cuenta, y después hay solo 3 jugadas sueltas
        play(game, "e7e6", "g1f3", "f6g8", "f3g1");

        // Assert
        assertEquals(GameState.IN_PROGRESS, game.state());
    }

    @Test
    void mateOfCorridorInAPositionBuilt() {
        // Arrange
        Board board = doubles.BoardFake.from(
                ".......r",
                "........",
                "......R.",
                "........",
                "........",
                "........",
                "........",
                "T.......");
        Game game = newGame(board, conditionsStandard());

        // Act
        MoveResult result = play(game, "a1a8");

        // Assert
        assertEquals(GameState.CHECK_MATE, result.state());
        assertEquals(Optional.of(white), game.winner());
    }

    @Test
    void stalemateInAPositionBuilt() {
        // Arrange: rey negro en h8; la dama blanca va a f7 y lo deja sin jugadas pero sin jaque
        Board board = doubles.BoardFake.from(
                ".......r",
                "....D...",
                "......R.",
                "........",
                "........",
                "........",
                "........",
                "........");
        Game game = newGame(board, conditionsStandard());

        // Act
        MoveResult result = play(game, "e7f7");

        // Assert
        assertEquals(GameState.DRAW_STALEMATE, result.state());
        assertTrue(game.winner().isEmpty());
    }

    @Test
    void kingNotCanCaptureAPieceDefended() {
        // Arrange: la torre negra en e2 está defendida por la torre negra en e8
        Board board = doubles.BoardFake.from(
                "....t..r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....t...",
                "....R...");
        Game game = newGame(board, conditionsStandard());
        assertEquals(GameState.CHECK, game.state());

        // Act
        InvalidMoveException e = assertThrows(InvalidMoveException.class,
                () -> play(game, "e1e2"));

        // Assert
        assertEquals(Reason.DEJA_AL_REY_EN_JAQUE, e.reason());
        assertFalse(game.movementsLegal(pos("e1")).contains(pos("e2")));
    }
}
