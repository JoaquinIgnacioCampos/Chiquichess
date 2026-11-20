package core.pieces;

import core.side.Side;
import core.movements.Slide;
import core.movements.BaseRules;

public class Rook extends Piece {

    public Rook(Side color) {
        super(color, BaseRules.orthogonal(Slide.WITHOUT_LIMIT));
    }

    @Override
    public boolean isRook() {
        return true;
    }

    @Override
    public String displayName() {
        return "Rook";
    }

    @Override
    public char code() {
        return 'R';
    }

    @Override
    public int value() {
        return 500;
    }
}
