package core.pieces;

import core.side.Side;
import core.movements.Slide;
import core.movements.BaseRules;

public class Bishop extends Piece {

    public Bishop(Side color) {
        super(color, BaseRules.diagonals(Slide.WITHOUT_LIMIT));
    }

    @Override
    public String displayName() {
        return "Bishop";
    }

    @Override
    public char code() {
        return 'B';
    }

    @Override
    public int value() {
        return 330;
    }
}
