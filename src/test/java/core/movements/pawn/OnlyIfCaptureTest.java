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

class OnlyIfCaptureTest {

    @Test
    void onlyLeavesDestinationsWhereHasAPiece() {
        // Arrange
        Board board = BoardFake.empty();
        board.place(pos("d5"), new Rook(Side.BLACK));
        OnlyIfCapture regla = new OnlyIfCapture(new StubRuleMovement(positions("d5", "f5")));

        // Act
        Set<Position> destinations = regla.destinationsPossible(board, pos("e4"), Side.WHITE);

        // Assert
        assertEquals(positions("d5"), destinations);
    }

    @Test
    void withoutPiecesInDestinationsNotCanMoveSelf() {
        // Arrange
        OnlyIfCapture regla = new OnlyIfCapture(new StubRuleMovement(positions("d5", "f5")));

        // Act
        Set<Position> destinations = regla.destinationsPossible(BoardFake.empty(), pos("e4"), Side.WHITE);

        // Assert
        assertTrue(destinations.isEmpty());
    }

    @Test
    void attacksSquaresOfRuleInternalAlthoughAreEmpty() {
        // Arrange
        OnlyIfCapture regla = new OnlyIfCapture(new StubRuleMovement(positions("d5", "f5")));

        // Act
        Set<Position> attacked = regla.squaresAttacked(BoardFake.empty(), pos("e4"), Side.WHITE);

        // Assert
        assertEquals(positions("d5", "f5"), attacked);
    }
}
