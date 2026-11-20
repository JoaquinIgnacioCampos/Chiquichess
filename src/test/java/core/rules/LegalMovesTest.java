package core.rules;

import core.side.Side;
import core.game.StandardLayout;
import core.moves.Move;
import core.pieces.Piece;
import core.board.Position;
import core.board.Board;
import doubles.StubDetectorOfCheck;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static doubles.Squares.pos;
import static doubles.Squares.positions;
import static org.junit.jupiter.api.Assertions.*;

class LegalMovesTest {

    // ---------- Con stub del detector: se prueba solo el filtrado, sin posiciones reales ----------

    @Test
    void ifDetectorNeverVeCheckAllDestinationsOfPieceAreLegal() {
        // Arrange
        Board board = new StandardLayout().createBoard();
        LegalMoves legal = new LegalMoves(StubDetectorOfCheck.neverInCheck());

        // Act
        Set<Position> destinations = legal.destinations(board, pos("g1"));

        // Assert
        assertEquals(positions("f3", "h3"), destinations);
    }

    @Test
    void ifDetectorAlwaysVeCheckNoMoveIsLegal() {
        // Arrange
        Board board = new StandardLayout().createBoard();
        LegalMoves legal = new LegalMoves(StubDetectorOfCheck.alwaysInCheck());

        // Act + Assert
        assertTrue(legal.destinations(board, pos("g1")).isEmpty());
        assertFalse(legal.existsSome(board, Side.WHITE));
    }

    @Test
    void withoutPieceInOriginNotHasDestinations() {
        // Arrange
        LegalMoves legal = new LegalMoves(StubDetectorOfCheck.neverInCheck());

        // Act
        Set<Position> destinations = legal.destinations(BoardFake.empty(), pos("e4"));

        // Assert
        assertTrue(destinations.isEmpty());
    }

    @Test
    void rejectsADetectorNull() {
        // Act + Assert
        assertThrows(NullPointerException.class, () -> new LegalMoves(null));
    }

    // ---------- Con detector real y tableros armados con el fake ----------

    private final LegalMoves legal = new LegalMoves(new CheckDetector());

    @Test
    void aPiecePinnedNotCanMoveSelfIfLeavesToKingInCheck() {
        // Arrange: el alfil blanco en e2 tapa el jaque de la torre negra en e8
        Board board = BoardFake.from(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....B...",
                "....K...");

        // Act
        Set<Position> destinations = legal.destinations(board, pos("e2"));

        // Assert
        assertTrue(destinations.isEmpty());
    }

    @Test
    void kingNotCanEnterInASquareAttacked() {
        // Arrange: la torre negra en a2 controla toda la fila 2
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "r.......",
                "....K...");

        // Act
        Set<Position> destinations = legal.destinations(board, pos("e1"));

        // Assert
        assertEquals(positions("d1", "f1"), destinations);
    }

    @Test
    void inCheckOnlyAreLegalMovesThatItResolve() {
        // Arrange: torre negra en e8 da jaque; la torre blanca en a8 puede capturarla
        Board board = BoardFake.from(
                "R...r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....K...");

        // Act
        Set<Position> destinationsRook = legal.destinations(board, pos("a8"));

        // Assert
        assertEquals(positions("e8"), destinationsRook);
    }

    @Test
    void inInitialPositionExistMovesLegal() {
        // Arrange
        Board board = new StandardLayout().createBoard();

        // Act + Assert
        assertTrue(legal.existsSome(board, Side.WHITE));
        assertTrue(legal.existsSome(board, Side.BLACK));
    }

    @Test
    void inCheckMateNotExistsNoLegalMove() {
        // Arrange: mate del pasillo, rey negro en h8
        Board board = BoardFake.from(
                "R......k",
                "........",
                "......K.",
                "........",
                "........",
                "........",
                "........",
                "........");

        // Act
        boolean hasMove = legal.existsSome(board, Side.BLACK);

        // Assert
        assertFalse(hasMove);
    }

    @Test
    void leavesToKingInCheckSimulatesWithoutModifyBoardReal() {
        // Arrange
        Board board = BoardFake.from(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....B...",
                "....K...");
        Map<Position, Piece> before = new HashMap<>(board.pieces());
        Piece bishop = board.pieceIn(pos("e2")).orElseThrow();

        // Act
        boolean leaves = legal.leavesKingInCheck(board, bishop, pos("e2"), pos("d3"));

        // Assert
        assertTrue(leaves);
        assertEquals(before, board.pieces());
    }

    // ---------- Jugadas: la unidad que se valida y se ejecuta ----------

    @Test
    void movesPossibleOfInitialKnightAreTwo() {
        // Arrange
        Board board = new StandardLayout().createBoard();

        // Act
        List<Move> moves = legal.movesPossible(board, pos("g1"));

        // Assert
        assertEquals(Set.of(pos("f3"), pos("h3")),
                moves.stream().map(Move::destination).collect(Collectors.toSet()));
        assertTrue(moves.stream().allMatch(j -> j.origin().equals(pos("g1"))));
    }

    @Test
    void movePossibleOfACaptureIncludesWithdrawalOfPieceRival() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "...p....",
                "....P...",
                "........",
                "........",
                "........");

        // Act
        Move move = legal.movePossible(board, pos("e4"), pos("d5")).orElseThrow();

        // Assert
        assertSame(board.pieceIn(pos("d5")).orElseThrow(), move.captured().orElseThrow());
    }

    @Test
    void notHasMovePossibleTowardADestinationThatPieceNotReaches() {
        // Arrange
        Board board = new StandardLayout().createBoard();

        // Act + Assert
        assertTrue(legal.movePossible(board, pos("a1"), pos("a3")).isEmpty());
        assertTrue(legal.movePossible(board, pos("e4"), pos("e5")).isEmpty());
    }

    @Test
    void leavesToKingInCheckWithAMoveSimulatesAllTheirEffectsWithoutPlayBoard() {
        // Arrange: el alfil clavado en e2 no puede salir de la columna
        Board board = BoardFake.from(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....B...",
                "....K...");
        Map<Position, Piece> before = new HashMap<>(board.pieces());
        Move exitOfPinned = Move.normal(board, pos("e2"), pos("d3"));

        // Act
        boolean leaves = legal.leavesKingInCheck(board, exitOfPinned);

        // Assert
        assertTrue(leaves);
        assertEquals(before, board.pieces());
    }

    // ---------- Reglas especiales ----------

    private Board readyForCastle() {
        return BoardFake.from(
                "k.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R...K..R");
    }

    @Test
    void withoutRulesSpecialKingNotCastles() {
        // Act
        Set<Position> destinations = legal.destinations(readyForCastle(), pos("e1"), List.of());

        // Assert
        assertFalse(destinations.contains(pos("g1")));
        assertFalse(destinations.contains(pos("c1")));
    }

    @Test
    void withCastlingInjectedKingAddsTheirTwoCastlings() {
        // Arrange
        LegalMoves withCastling = new LegalMoves(new CheckDetector(), List.of(new Castling()));

        // Act
        Set<Position> destinations = withCastling.destinations(readyForCastle(), pos("e1"), List.of());

        // Assert: los cinco movimientos comunes del rey más los dos enroques
        assertEquals(positions("d1", "d2", "e2", "f2", "f1", "g1", "c1"), destinations);
    }
}
