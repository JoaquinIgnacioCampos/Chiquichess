package core.pieces;

import core.side.Side;
import core.movements.BaseRules;

public class Knight extends Piece {

    public Knight(Side color) {
        super(color, BaseRules.knightJumps());
    }

    @Override
    public String displayName() {
        return "Caballo";
    }

    @Override
    public char code() {
        return 'C';
    }

    @Override
    public int value() {
        return 320;
    }
}
