package core.pieces;

import core.side.Side;
import core.board.IBoard;
import core.movements.MovementRule;
import core.board.Position;

import java.util.Objects;
import java.util.Set;

/**
 * Una pieza es un color más una regla de movimiento (composición sobre herencia).
 * Las subclases concretas solo eligen cómo componer su regla; para agregar una pieza
 * nueva se crea una clase nueva sin tocar ninguna existente (Open/Closed).
 * Las piezas son inmutables y no conocen su posición.
 */
public abstract class Piece {

    private final Side color;
    private final MovementRule regla;

    protected Piece(Side color, MovementRule regla) {
        this.color = Objects.requireNonNull(color, "color");
        this.regla = Objects.requireNonNull(regla, "rule");
    }

    public Side color() {
        return color;
    }

    /** Destinos según las reglas propias de la pieza (sin considerar si deja al rey en jaque). */
    public Set<Position> destinationsPossible(IBoard board, Position origin) {
        return regla.destinationsPossible(board, origin, color);
    }

    /** Casillas que la pieza amenaza desde esa posición. */
    public Set<Position> squaresAttacked(IBoard board, Position origin) {
        return regla.squaresAttacked(board, origin, color);
    }

    /** Las piezas que son el rey sobreescriben esto; lo usa el detector de jaque. */
    public boolean isKing() {
        return false;
    }

    /** Los peones sobreescriben esto; lo usa la regla de los 50 movimientos (un movimiento de peón es "progreso"). */
    public boolean isPawn() {
        return false;
    }

    /** Las torres sobreescriben esto; lo usa el enroque para saber con qué pieza puede enrocar el rey. */
    public boolean isRook() {
        return false;
    }

    public abstract String displayName();

    /** Letra que identifica a la pieza (P, T, C, A, D, R). */
    public abstract char code();

    /**
     * Valor material en centésimos de peón (peón 100, dama 900). Es conocimiento del ajedrez, no de la
     * IA: lo declara cada pieza para que una pieza nueva no valga 0 sin que nadie lo note.
     */
    public abstract int value();
}