package core.pieces;

import core.side.Side;
import core.movements.Slide;
import core.movements.CompositeRule;
import core.movements.MovementRule;
import core.movements.pawn.OnlyIf;
import core.movements.pawn.OnlyIfCapture;
import core.movements.pawn.OnlyIfEmpty;
import core.board.IBoard;

public class Pawn extends Piece {

    public Pawn(Side color) {
        super(color, buildRule(color));
    }

    private static MovementRule buildRule(Side color) {
        int forward = color.directionAdvance();

        MovementRule advanceSimple = new OnlyIfEmpty(new Slide(0, forward, 1));
        MovementRule advanceDouble = new OnlyIf(
                (board, origin, c) -> origin.row() == initialRow(board, c),
                new OnlyIfEmpty(new Slide(0, forward, 2)));
        MovementRule captureLeft = new OnlyIfCapture(new Slide(-1, forward, 1));
        MovementRule captureRight = new OnlyIfCapture(new Slide(1, forward, 1));

        return new CompositeRule(advanceSimple, advanceDouble, captureLeft, captureRight);
    }

    private static int initialRow(IBoard board, Side color) {
        return color == Side.WHITE ? 1 : board.height() - 2;
    }

    @Override
    public boolean isPawn() {
        return true;
    }

    @Override
    public String displayName() {
        return "Peon";
    }

    @Override
    public char code() {
        return 'P';
    }

    @Override
    public int value() {
        return 100;
    }
}
