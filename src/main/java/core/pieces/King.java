package core.pieces;

import core.side.Side;
import core.movements.CompositeRule;
import core.movements.BaseRules;

/**
 * Rey = torre + alfil limitados a 1 paso. Que no pueda moverse a una casilla atacada
 * no se resuelve acá sino en la validación de la Partida (no se permite dejar al propio rey en jaque).
 */
public class King extends Piece {

    public King(Side color) {
        super(color, new CompositeRule(
                BaseRules.orthogonal(1),
                BaseRules.diagonals(1)));
    }

    @Override
    public boolean isKing() {
        return true;
    }

    @Override
    public String displayName() {
        return "King";
    }

    @Override
    public char code() {
        return 'K';
    }

    /** No se cuenta: nunca sale del tablero (el jaque mate termina la partida antes). */
    @Override
    public int value() {
        return 0;
    }
}
