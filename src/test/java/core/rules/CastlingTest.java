package core.rules;

import core.moves.Move;
import core.moves.Transfer;
import core.pieces.Piece;
import core.board.Position;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static doubles.Squares.pos;
import static doubles.Squares.positions;
import static org.junit.jupiter.api.Assertions.*;

/** Cada test arma con el fake una posición que habilita o impide un enroque, con el detector real. */
class CastlingTest {

    private final Castling castling = new Castling();
    private final CheckDetector detector = new CheckDetector();

    /** Rey negro en a8 para que haya rey rival; rey y torres blancas en su fila base. */
    private Board readyForCastle(String rowEight) {
        return BoardFake.from(
                rowEight,
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T...R..T");
    }

    private Set<Position> destinations(Board board, String origin, List<Move> history) {
        return castling.moves(board, pos(origin), history, detector).stream()
                .map(Move::destination)
                .collect(Collectors.toSet());
    }

    private Set<Position> destinations(Board board, String origin) {
        return destinations(board, origin, List.of());
    }

    /** Una pieza que fue y volvió: queda en su casilla, pero ya se movió. */
    private List<Move> outboundAndLap(Piece piece, String from, String until) {
        return List.of(
                new Move(pos(from), pos(until), List.of(new Transfer(piece, pos(from), pos(until)))),
                new Move(pos(until), pos(from), List.of(new Transfer(piece, pos(until), pos(from)))));
    }

    @Test
    void withAllFreeKingCanCastleShortAndLength() {
        // Act
        Set<Position> result = destinations(readyForCastle("r......."), "e1");

        // Assert
        assertEquals(positions("g1", "c1"), result);
    }

    @Test
    void castlingShortMovesKingAg1AndRookAf1() {
        // Arrange
        Board board = readyForCastle("r.......");
        Piece king = board.pieceIn(pos("e1")).orElseThrow();
        Piece rook = board.pieceIn(pos("h1")).orElseThrow();

        // Act
        Move shortValue = castling.moves(board, pos("e1"), List.of(), detector).stream()
                .filter(j -> j.destination().equals(pos("g1"))).findFirst().orElseThrow();

        // Assert
        assertEquals(List.of(new Transfer(king, pos("e1"), pos("g1")), new Transfer(rook, pos("h1"), pos("f1"))),
                shortValue.effects());
        assertSame(king, shortValue.pieceMoved());
        assertTrue(shortValue.captured().isEmpty());
    }

    @Test
    void castlingLengthMovesKingAc1AndRookAd1() {
        // Arrange
        Board board = readyForCastle("r.......");
        Piece king = board.pieceIn(pos("e1")).orElseThrow();
        Piece rook = board.pieceIn(pos("a1")).orElseThrow();

        // Act
        Move length = castling.moves(board, pos("e1"), List.of(), detector).stream()
                .filter(j -> j.destination().equals(pos("c1"))).findFirst().orElseThrow();

        // Assert
        assertEquals(List.of(new Transfer(king, pos("e1"), pos("c1")), new Transfer(rook, pos("a1"), pos("d1"))),
                length.effects());
    }

    @Test
    void blackCastleInRowEight() {
        // Arrange
        Board board = BoardFake.from(
                "t...r..t",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");

        // Act + Assert
        assertEquals(positions("g8", "c8"), destinations(board, "e8"));
    }

    @Test
    void ifKingAlreadyMovedNotCanCastle() {
        // Arrange: el rey fue a e2 y volvió
        Board board = readyForCastle("r.......");
        Piece king = board.pieceIn(pos("e1")).orElseThrow();

        // Act + Assert
        assertTrue(destinations(board, "e1", outboundAndLap(king, "e1", "e2")).isEmpty());
    }

    @Test
    void ifARookAlreadyMovedOnlyCanCastleOfOtherSide() {
        // Arrange: la torre de h1 fue a h2 y volvió
        Board board = readyForCastle("r.......");
        Piece rook = board.pieceIn(pos("h1")).orElseThrow();

        // Act + Assert
        assertEquals(positions("c1"), destinations(board, "e1", outboundAndLap(rook, "h1", "h2")));
    }

    @Test
    void aPieceInMiddlePreventsThatCastling() {
        // Arrange
        Board withKnightInB1 = BoardFake.from("r.......", "........", "........", "........",
                "........", "........", "........", "TC..R..T");
        Board withKnightInG1 = BoardFake.from("r.......", "........", "........", "........",
                "........", "........", "........", "T...R.CT");

        // Act + Assert
        assertEquals(positions("g1"), destinations(withKnightInB1, "e1"));
        assertEquals(positions("c1"), destinations(withKnightInG1, "e1"));
    }

    @Test
    void inCheckNotCanCastle() {
        // Act + Assert: la torre negra de e8 da jaque
        assertTrue(destinations(readyForCastle("r...t..."), "e1").isEmpty());
    }

    @Test
    void kingNotCanPassByASquareAttacked() {
        // Act + Assert: la torre negra de f8 controla f1, por donde pasa el enroque corto
        assertEquals(positions("c1"), destinations(readyForCastle("r....t.."), "e1"));
    }

    @Test
    void kingNotCanEndInASquareAttacked() {
        // Act + Assert: la torre negra de g8 controla g1
        assertEquals(positions("c1"), destinations(readyForCastle("r.....t."), "e1"));
    }

    @Test
    void thatB1ThisAttackedNotPreventsCastlingLength() {
        // Act + Assert: el rey no pasa por b1; solo tiene que estar vacía
        assertEquals(positions("g1", "c1"), destinations(readyForCastle("rt......"), "e1"));
    }

    @Test
    void onlyCastlesWithAOwnRook() {
        // Arrange: en h1 hay un alfil propio
        Board board = BoardFake.from("r.......", "........", "........", "........",
                "........", "........", "........", "T...R..A");

        // Act + Assert
        assertEquals(positions("c1"), destinations(board, "e1"));
    }

    @Test
    void withRookTooMuchNearNotHasCastling() {
        // Arrange: la torre en g1 deja una sola casilla libre entre ambos
        Board board = BoardFake.from("r.......", "........", "........", "........",
                "........", "........", "........", "T...R.T.");

        // Act + Assert
        assertEquals(positions("c1"), destinations(board, "e1"));
    }

    @Test
    void outsideOfRowBaseNotHasCastling() {
        // Arrange: rey y torre en la fila 4
        Board board = BoardFake.from("r.......", "........", "........", "........",
                "....R..T", "........", "........", "........");

        // Act + Assert
        assertTrue(destinations(board, "e4").isEmpty());
    }

    @Test
    void onlyKingCastles() {
        // Act + Assert: pedir enroques desde la torre o desde una casilla vacía no devuelve nada
        Board board = readyForCastle("r.......");
        assertTrue(destinations(board, "h1").isEmpty());
        assertTrue(destinations(board, "d4").isEmpty());
    }
}
