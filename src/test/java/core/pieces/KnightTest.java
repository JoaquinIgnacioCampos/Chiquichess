package core.pieces;

import core.side.Side;
import core.game.StandardLayout;
import core.board.Position;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static doubles.Squares.pos;
import static doubles.Squares.positions;
import static org.junit.jupiter.api.Assertions.*;

class KnightTest {

    @Test
    void inCenterHas8Jumps() {
        // Arrange
        Knight knight = new Knight(Side.WHITE);
        Board board = BoardFake.withAPiece("d4", knight);

        // Act
        Set<Position> destinations = knight.destinationsPossible(board, pos("d4"));

        // Assert
        assertEquals(8, destinations.size());
    }

    @Test
    void inCornerHas2Jumps() {
        // Arrange
        Knight knight = new Knight(Side.WHITE);
        Board board = BoardFake.withAPiece("a1", knight);

        // Act
        Set<Position> destinations = knight.destinationsPossible(board, pos("a1"));

        // Assert
        assertEquals(positions("b3", "c2"), destinations);
    }

    @Test
    void inInitialPositionJumpsByAboveOfPawns() {
        // Arrange
        Board board = new StandardLayout().createBoard();
        Piece knight = board.pieceIn(pos("b1")).orElseThrow();

        // Act
        Set<Position> destinations = knight.destinationsPossible(board, pos("b1"));

        // Assert
        assertEquals(positions("a3", "c3"), destinations);
    }

    @Test
    void identifiesAsKnight() {
        // Arrange
        Knight knight = new Knight(Side.WHITE);

        // Assert
        assertEquals('C', knight.code());
        assertEquals("Caballo", knight.displayName());
        assertFalse(knight.isKing());
    }
}
