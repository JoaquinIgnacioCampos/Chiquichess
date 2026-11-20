package core.pieces;

import core.side.Side;
import core.movements.Slide;
import core.movements.CompositeRule;
import core.movements.BaseRules;

/** Reina = torre + alfil, ambos sin límite de pasos. */
public class Queen extends Piece {

    public Queen(Side color) {
        super(color, new CompositeRule(
                BaseRules.orthogonal(Slide.WITHOUT_LIMIT),
                BaseRules.diagonals(Slide.WITHOUT_LIMIT)));
    }

    @Override
    public String displayName() {
        return "Queen";
    }

    @Override
    public char code() {
        return 'Q';
    }

    @Override
    public int value() {
        return 900;
    }
}
