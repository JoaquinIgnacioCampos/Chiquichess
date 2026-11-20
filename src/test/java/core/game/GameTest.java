package core.game;

import core.side.Side;
import core.game.InvalidMoveException.Reason;
import core.pieces.Piece;
import core.pieces.Rook;
import core.rules.*;
import core.board.Position;
import core.board.Board;
import core.time.NoClock;
import doubles.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static doubles.Squares.positions;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de Partida: sus colaboradores (detector de jaque, condiciones de fin,
 * movimientos legales) se reemplazan por dobles para probar solo la lógica de Partida.
 */
class GameTest {

    private final Player white = new Player("Ana", Side.WHITE);
    private final Player black = new Player("Beto", Side.BLACK);

    private Game game(Board board, ICheckDetector detector, EndCondition... conditions) {
        return new Game(board, white, black, new GameRules(detector, List.of(conditions)), new NoClock());
    }

    /** Partida desde la posición inicial, sin jaques ni condiciones de fin. */
    private Game gameSimple(EndCondition... conditions) {
        return game(new StandardLayout().createBoard(), StubDetectorOfCheck.neverInCheck(), conditions);
    }

    // ---------------- Construcción ----------------

    @Test
    void rejectsPlayersWithColorsChanged() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Game(
                new StandardLayout().createBoard(), black, white,
                new GameRules(StubDetectorOfCheck.neverInCheck(), List.of()), new NoClock()));
    }

    @Test
    void rejectsDependenciesNull() {
        // Arrange
        GameRules rules = new GameRules(StubDetectorOfCheck.neverInCheck(), List.of());

        // Act + Assert
        assertThrows(NullPointerException.class, () -> new Game(
                null, white, black, rules, new NoClock()));
        assertThrows(NullPointerException.class, () -> new Game(
                new StandardLayout().createBoard(), white, black, null, new NoClock()));
        assertThrows(NullPointerException.class, () -> new Game(
                new StandardLayout().createBoard(), white, black, rules, null));
    }

    @Test
    void withoutCheckNorConditionsGameStartsInProgress() {
        // Act
        Game game = gameSimple();

        // Assert
        assertEquals(GameState.IN_PROGRESS, game.state());
    }

    // ---------------- Turnos ----------------

    @Test
    void startWhite() {
        // Act
        Game game = gameSimple();

        // Assert
        assertEquals(Side.WHITE, game.turn());
        assertEquals(white, game.currentPlayer());
    }

    @Test
    void afterOfAMovementValidHimPlaysToOtherPlayer() {
        // Arrange
        Game game = gameSimple();

        // Act
        game.move(move("e2", "e4"));

        // Assert
        assertEquals(Side.BLACK, game.turn());
        assertEquals(black, game.currentPlayer());
    }

    @Test
    void turnsAlternateBetweenBothPlayers() {
        // Arrange
        Game game = gameSimple();

        // Act
        game.move(move("e2", "e4"));
        game.move(move("e7", "e5"));

        // Assert
        assertEquals(Side.WHITE, game.turn());
    }

    // ---------------- Ejecución del movimiento ----------------

    @Test
    void moveCarriesPieceOfOriginToDestination() {
        // Arrange
        Game game = gameSimple();
        Piece pawn = game.board().pieceIn(pos("e2")).orElseThrow();

        // Act
        MoveResult result = game.move(move("e2", "e4"));

        // Assert
        assertTrue(game.board().pieceIn(pos("e2")).isEmpty());
        assertSame(pawn, game.board().pieceIn(pos("e4")).orElseThrow());
        assertTrue(result.capturedPiece().isEmpty());
        assertEquals(GameState.IN_PROGRESS, result.state());
    }

    @Test
    void onCaptureReturnsCapturedPieceAndTakesOfBoard() {
        // Arrange
        Board board = BoardFake.from(
                "t.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T.......");
        Piece blackRook = board.pieceIn(pos("a8")).orElseThrow();
        Game game = game(board, StubDetectorOfCheck.neverInCheck());

        // Act
        MoveResult result = game.move(move("a1", "a8"));

        // Assert
        assertEquals(Optional.of(blackRook), result.capturedPiece());
        assertEquals(Side.WHITE, game.board().pieceIn(pos("a8")).orElseThrow().color());
        assertEquals(1, game.board().pieces().size());
    }

    // ---------------- Validaciones (cada Motivo de error) ----------------

    @Test
    void withoutPieceInOriginThrowsWithoutPieceInOrigin() {
        // Arrange
        Game game = gameSimple();

        // Act
        InvalidMoveException e = assertThrows(InvalidMoveException.class,
                () -> game.move(move("e4", "e5")));

        // Assert
        assertEquals(Reason.SIN_PIEZA_EN_ORIGEN, e.reason());
    }

    @Test
    void moveAPieceOfRivalThrowsNotIsItsTurn() {
        // Arrange
        Game game = gameSimple();

        // Act
        InvalidMoveException e = assertThrows(InvalidMoveException.class,
                () -> game.move(move("e7", "e5")));

        // Assert
        assertEquals(Reason.NO_ES_SU_TURNO, e.reason());
    }

    @Test
    void aDestinationThatPieceNotReachesThrowsMovementNotAllowed() {
        // Arrange
        Game game = gameSimple();

        // Act
        InvalidMoveException e = assertThrows(InvalidMoveException.class,
                () -> game.move(move("a1", "a3")));

        // Assert
        assertEquals(Reason.MOVIMIENTO_NO_PERMITIDO, e.reason());
    }

    @Test
    void ifMoveLeavesToKingInCheckThrowsLeavesToKingInCheck() {
        // Arrange: el stub dice que el rey queda en jaque haga lo que haga
        Game game = game(new StandardLayout().createBoard(), StubDetectorOfCheck.alwaysInCheck());

        // Act
        InvalidMoveException e = assertThrows(InvalidMoveException.class,
                () -> game.move(move("e2", "e4")));

        // Assert
        assertEquals(Reason.DEJA_AL_REY_EN_JAQUE, e.reason());
    }

    @Test
    void withEndedGameThrowsFinishedGame() {
        // Arrange: una condición que termina la partida apenas se crea
        Game game = gameSimple(StubCondition.always(GameState.DRAW_STALEMATE));

        // Act
        InvalidMoveException e = assertThrows(InvalidMoveException.class,
                () -> game.move(move("e2", "e4")));

        // Assert
        assertEquals(Reason.PARTIDA_FINALIZADA, e.reason());
    }

    @Test
    void aMovementInvalidNotModifiesNorBoardNorTurn() {
        // Arrange
        Game game = gameSimple();
        Map<Position, Piece> before = new HashMap<>(game.board().pieces());

        // Act
        assertThrows(InvalidMoveException.class, () -> game.move(move("a1", "a5")));

        // Assert
        assertEquals(before, game.board().pieces());
        assertEquals(Side.WHITE, game.turn());
    }

    // ---------------- Estado y condiciones de fin (stubs y mocks) ----------------

    @Test
    void ifKingIsInCheckAndNoConditionEndsStateIsCheck() {
        // Act
        Game game = game(new StandardLayout().createBoard(), StubDetectorOfCheck.alwaysInCheck());

        // Assert
        assertEquals(GameState.CHECK, game.state());
    }

    @Test
    void stateIsThatReturnsConditionThatMeets() {
        // Act
        Game game = gameSimple(StubCondition.always(GameState.DRAW_REPETITION));

        // Assert
        assertEquals(GameState.DRAW_REPETITION, game.state());
        assertTrue(game.state().isFinished());
    }

    @Test
    void conditionsQueryATimeToCreateGameWithoutPieceMoved() {
        // Arrange
        MockCondition mock = new MockCondition();

        // Act
        gameSimple(mock);

        // Assert
        assertEquals(1, mock.amountOfCalls());
        assertTrue(mock.lastContext().pieceMoved().isEmpty());
        assertEquals(Side.WHITE, mock.lastContext().turn());
    }

    @Test
    void afterOfEachMoveConditionReceivesContextCorrect() {
        // Arrange
        MockCondition mock = new MockCondition();
        Game game = gameSimple(mock);

        // Act
        game.move(move("e2", "e4"));

        // Assert
        assertEquals(2, mock.amountOfCalls());
        MoveContext context = mock.lastContext();
        assertEquals(Side.BLACK, context.turn(), "El contexto es desde el punto de vista de quien juega ahora");
        assertTrue(context.pieceMoved().orElseThrow().isPawn());
        assertFalse(context.wasCapture());
        assertFalse(context.inCheck());
        assertTrue(context.hasMovementsLegal());
    }

    @Test
    void conditionWholeOfCaptures() {
        // Arrange
        Board board = BoardFake.from(
                "t.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T.......");
        MockCondition mock = new MockCondition();
        Game game = game(board, StubDetectorOfCheck.neverInCheck(), mock);

        // Act
        game.move(move("a1", "a8"));

        // Assert
        assertTrue(mock.lastContext().wasCapture());
    }

    @Test
    void aMoveInvalidNotQueryConditions() {
        // Arrange
        MockCondition mock = new MockCondition();
        Game game = gameSimple(mock);

        // Act
        assertThrows(InvalidMoveException.class, () -> game.move(move("e2", "e5")));

        // Assert
        assertEquals(1, mock.amountOfCalls());
    }

    @Test
    void firstConditionThatMeetsWinsAndNextNotQuery() {
        // Arrange
        MockCondition next = new MockCondition();

        // Act
        Game game = gameSimple(StubCondition.always(GameState.DRAW_STALEMATE), next);

        // Assert
        assertEquals(GameState.DRAW_STALEMATE, game.state());
        assertEquals(0, next.amountOfCalls());
    }

    @Test
    void ifNoConditionMeetsQueryAll() {
        // Arrange
        MockCondition first = new MockCondition();
        MockCondition second = new MockCondition();

        // Act
        gameSimple(first, StubCondition.never(), second);

        // Assert
        assertEquals(1, first.amountOfCalls());
        assertEquals(1, second.amountOfCalls());
    }

    // ---------------- Condiciones con memoria (Memento) ----------------

    @Test
    void conditionsWithMemoryRecordAlthoughOtherEndsGameBefore() {
        // Arrange: la primera condición termina la partida; la que tiene memoria está después
        MockConditionWithMemory withMemory = new MockConditionWithMemory();

        // Act
        gameSimple(StubCondition.always(GameState.DRAW_STALEMATE), withMemory);

        // Assert: registró la posición inicial aunque no se la llegó a evaluar
        assertEquals(1, withMemory.records());
        assertEquals(0, withMemory.evaluations());
    }

    @Test
    void conditionsWithMemoryRecordEachMoveBeforeOfEvaluate() {
        // Arrange
        MockConditionWithMemory withMemory = new MockConditionWithMemory();
        Game game = gameSimple(withMemory);

        // Act
        game.move(move("e2", "e4"));

        // Assert
        assertEquals(2, withMemory.records());
        assertEquals(2, withMemory.evaluations());
        assertEquals(1, withMemory.lastRegistered().moves().size());
        assertEquals(Side.BLACK, withMemory.lastRegistered().turn());
    }

    @Test
    void aMoveInvalidNotRecordsNothing() {
        // Arrange
        MockConditionWithMemory withMemory = new MockConditionWithMemory();
        Game game = gameSimple(withMemory);

        // Act
        assertThrows(InvalidMoveException.class, () -> game.move(move("e2", "e5")));

        // Assert
        assertEquals(1, withMemory.records());
    }

    // ---------------- MovimientosLegales inyectado (stub) ----------------

    @Test
    void withoutMovesLegalNorCheckGameEndsInStalemate() {
        // Arrange
        Board board = new StandardLayout().createBoard();
        ICheckDetector detector = StubDetectorOfCheck.neverInCheck();

        // Act
        Game game = new Game(board, white, black,
                new GameRules(new StubMovementsLegal(false, detector), List.of(new Checkmate(), new Stalemate())),
                new NoClock());

        // Assert
        assertEquals(GameState.DRAW_STALEMATE, game.state());
    }

    @Test
    void inCheckAndWithoutMovesLegalGameEndsInCheckMate() {
        // Arrange
        Board board = new StandardLayout().createBoard();
        ICheckDetector detector = StubDetectorOfCheck.alwaysInCheck();

        // Act
        Game game = new Game(board, white, black,
                new GameRules(new StubMovementsLegal(false, detector), List.of(new Checkmate(), new Stalemate())),
                new NoClock());

        // Assert
        assertEquals(GameState.CHECK_MATE, game.state());
    }

    @Test
    void withRulesOfGameDetectorIsOfMovementsLegal() {
        // Arrange: no hay forma de pasar un segundo detector
        ICheckDetector detector = StubDetectorOfCheck.alwaysInCheck();
        GameRules rules = new GameRules(new StubMovementsLegal(false, detector),
                List.of(new Checkmate(), new Stalemate()));

        // Act
        Game game = new Game(new StandardLayout().createBoard(), white, black,
                rules, new NoClock());

        // Assert
        assertEquals(GameState.CHECK_MATE, game.state());
    }

    // ---------------- Ganador ----------------

    @Test
    void inCheckMateWinsPlayerThatNotHasTurn() {
        // Arrange: el mate se declara con el turno de las blancas, así que pierden ellas
        Game game = gameSimple(StubCondition.always(GameState.CHECK_MATE));

        // Act
        Optional<Player> winner = game.winner();

        // Assert
        assertEquals(Optional.of(black), winner);
    }

    @Test
    void inDrawNotHasWinner() {
        // Arrange
        Game game = gameSimple(StubCondition.always(GameState.DRAW_REPETITION));

        // Act + Assert
        assertTrue(game.winner().isEmpty());
    }

    @Test
    void withGameInProgressNotHasWinner() {
        // Arrange
        Game game = gameSimple();

        // Act + Assert
        assertTrue(game.winner().isEmpty());
    }

    // ---------------- Consulta de movimientos legales ----------------

    @Test
    void returnsDestinationsLegalOfAOwnPiece() {
        // Arrange
        Game game = gameSimple();

        // Act
        Set<Position> destinations = game.movementsLegal(pos("b1"));

        // Assert
        assertEquals(positions("a3", "c3"), destinations);
    }

    @Test
    void notReturnsDestinationsForPiecesOfRivalNorSquaresEmpty() {
        // Arrange
        Game game = gameSimple();

        // Act + Assert
        assertTrue(game.movementsLegal(pos("b8")).isEmpty());
        assertTrue(game.movementsLegal(pos("e4")).isEmpty());
    }

    @Test
    void withEndedGameNotHasMovementsLegal() {
        // Arrange
        Game game = gameSimple(StubCondition.always(GameState.DRAW_STALEMATE));

        // Act
        Set<Position> destinations = game.movementsLegal(pos("b1"));

        // Assert
        assertTrue(destinations.isEmpty());
    }

    @Test
    void boardThatExposesIsOfOnlyReading() {
        // Arrange
        Game game = gameSimple();

        // Act + Assert
        assertThrows(UnsupportedOperationException.class,
                () -> game.board().pieces().put(pos("e4"), new Rook(Side.WHITE)));
    }
}
