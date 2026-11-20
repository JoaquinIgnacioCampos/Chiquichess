package core.rules;

import core.moves.Move;
import core.moves.Removal;
import core.moves.Transfer;
import core.pieces.Piece;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.List;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cada test arma con el fake la posición ya con el último avance hecho, y le pasa a la regla un
 * historial cuya última jugada es ese avance.
 */
class EnPassantTest {

    private final EnPassant onStep = new EnPassant();
    private final CheckDetector detector = new CheckDetector();

    /** Peón blanco en e5 y peón negro en d5 (que acaba de llegar), con los dos reyes. */
    private Board whiteInE5BlackInD5() {
        return BoardFake.from(
                "....k...",
                "........",
                "........",
                "...pP...",
                "........",
                "........",
                "........",
                "....K...");
    }

    private Move advance(Piece pawn, String from, String until) {
        return new Move(pos(from), pos(until), List.of(new Transfer(pawn, pos(from), pos(until))));
    }

    private List<Move> moves(Board board, String origin, List<Move> history) {
        return onStep.moves(board, pos(origin), history, detector);
    }

    @Test
    void afterAAdvanceDoubleOfRivalToSideCanCaptureToStep() {
        // Arrange
        Board board = whiteInE5BlackInD5();
        Piece white = board.pieceIn(pos("e5")).orElseThrow();
        Piece black = board.pieceIn(pos("d5")).orElseThrow();

        // Act
        List<Move> result = moves(board, "e5", List.of(advance(black, "d7", "d5")));

        // Assert: el peón va a d6 y el negro sale de d5, no de la casilla de destino
        assertEquals(1, result.size());
        Move capture = result.get(0);
        assertEquals(pos("d6"), capture.destination());
        assertEquals(List.of(new Removal(black, pos("d5")), new Transfer(white, pos("e5"), pos("d6"))),
                capture.effects());
        assertSame(black, capture.captured().orElseThrow());
    }

    @Test
    void worksTowardTwoSidesAndForBlack() {
        // Arrange: peón negro en d4; el blanco acaba de avanzar e2-e4 y queda a su derecha
        Board board = BoardFake.from(
                "....k...",
                "........",
                "........",
                "........",
                "...pP...",
                "........",
                "........",
                "....K...");
        Piece white = board.pieceIn(pos("e4")).orElseThrow();

        // Act
        List<Move> result = moves(board, "d4", List.of(advance(white, "e2", "e4")));

        // Assert: el peón negro avanza hacia abajo, a e3
        assertEquals(1, result.size());
        assertEquals(pos("e3"), result.get(0).destination());
    }

    @Test
    void onlyIsInMoveImmediateNext() {
        // Arrange: después del avance doble hubo otra jugada
        Board board = whiteInE5BlackInD5();
        Piece black = board.pieceIn(pos("d5")).orElseThrow();
        Piece whiteKing = board.pieceIn(pos("e1")).orElseThrow();
        List<Move> history = List.of(advance(black, "d7", "d5"),
                new Move(pos("e2"), pos("e1"), List.of(new Transfer(whiteKing, pos("e2"), pos("e1")))));

        // Act + Assert
        assertTrue(moves(board, "e5", history).isEmpty());
    }

    @Test
    void aAdvanceOfAAloneSquareNotEnables() {
        // Arrange
        Board board = whiteInE5BlackInD5();
        Piece black = board.pieceIn(pos("d5")).orElseThrow();

        // Act + Assert
        assertTrue(moves(board, "e5", List.of(advance(black, "d6", "d5"))).isEmpty());
    }

    @Test
    void ifLastMoveWasOfOtherPieceNotHasCapture() {
        // Arrange: un rey negro que se movió, no un peón
        Board board = whiteInE5BlackInD5();
        Piece blackKing = board.pieceIn(pos("e8")).orElseThrow();

        // Act + Assert
        assertTrue(moves(board, "e5", List.of(advance(blackKing, "e6", "e8"))).isEmpty());
    }

    @Test
    void pawnRivalHasThatStayToSide() {
        // Arrange: el peón negro llegó a b5, lejos del blanco en e5
        Board board = BoardFake.from(
                "....k...",
                "........",
                "........",
                ".p..P...",
                "........",
                "........",
                "........",
                "....K...");
        Piece black = board.pieceIn(pos("b5")).orElseThrow();

        // Act + Assert
        assertTrue(moves(board, "e5", List.of(advance(black, "b7", "b5"))).isEmpty());
    }

    @Test
    void notCaptureToStepAOwnPawn() {
        // Arrange: el que avanzó doble es blanco, igual que el que pregunta
        Board board = BoardFake.from(
                "....k...",
                "........",
                "........",
                "........",
                "...PP...",
                "........",
                "........",
                "....K...");
        Piece otherWhite = board.pieceIn(pos("e4")).orElseThrow();

        // Act + Assert
        assertTrue(moves(board, "d4", List.of(advance(otherWhite, "e2", "e4"))).isEmpty());
    }

    @Test
    void onlyPawnsCaptureToStep() {
        // Arrange: un caballo blanco en e5 al lado del peón que avanzó
        Board board = BoardFake.from(
                "....k...",
                "........",
                "........",
                "...pN...",
                "........",
                "........",
                "........",
                "....K...");
        Piece black = board.pieceIn(pos("d5")).orElseThrow();

        // Act + Assert
        assertTrue(moves(board, "e5", List.of(advance(black, "d7", "d5"))).isEmpty());
        assertTrue(moves(board, "e5", List.of()).isEmpty());
    }
}
