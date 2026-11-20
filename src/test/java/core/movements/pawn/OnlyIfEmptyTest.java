package core.movements.pawn;

import core.side.Side;
import core.pieces.Rook;
import core.board.Position;
import core.board.Board;
import doubles.StubRuleMovement;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static doubles.Squares.pos;
import static doubles.Squares.positions;
import static org.junit.jupiter.api.Assertions.*;

class OnlyIfEmptyTest {

    @Test
    void discardsDestinationsOccupiedByAnyPiece() {
        // Arrange
        Board board = BoardFake.empty();
        board.place(pos("e5"), new Rook(Side.BLACK));
        board.place(pos("e6"), new Rook(Side.WHITE));
        OnlyIfEmpty regla = new OnlyIfEmpty(new StubRuleMovement(positions("e4", "e5", "e6")));

        // Act
        Set<Position> destinations = regla.destinationsPossible(board, pos("e3"), Side.WHITE);

        // Assert
        assertEquals(positions("e4"), destinations);
    }

    @Test
    void withAllSquaresFreeLeavesPassAll() {
        // Arrange
        OnlyIfEmpty regla = new OnlyIfEmpty(new StubRuleMovement(positions("e4", "e5")));

        // Act
        Set<Position> destinations = regla.destinationsPossible(BoardFake.empty(), pos("e3"), Side.WHITE);

        // Assert
        assertEquals(positions("e4", "e5"), destinations);
    }

    @Test
    void notAttacksNoSquareAlthoughRuleInternalIf() {
        // Arrange
        OnlyIfEmpty regla = new OnlyIfEmpty(new StubRuleMovement(positions("e4"), positions("e4", "d4")));

        // Act
        Set<Position> attacked = regla.squaresAttacked(BoardFake.empty(), pos("e3"), Side.WHITE);

        // Assert
        assertTrue(attacked.isEmpty());
    }
}
