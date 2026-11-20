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
        return "Torre";
    }

    @Override
    public char code() {
        return 'T';
    }

    @Override
    public int value() {
        return 500;
    }
}
