package core.moves;

import core.board.Position;
import core.board.Board;

import java.util.Objects;

/**
 * Lleva la pelota del modo fútbol de una casilla a otra. Al revertir, la devuelve.
 *
 * Si la jugada es un gol, la pelota no queda en el arco: va directo a la casilla del saque, y el
 * efecto lo marca con {@code goal}. Así el marcador se puede contar mirando las jugadas, y deshacer
 * un gol es deshacer este efecto. En un gol el saque puede coincidir con la casilla de donde salió
 * la pelota, por eso origen y destino pueden ser iguales.
 */
public record MoveBall(Position from, Position toward, boolean goal) implements Effect {

    public MoveBall {
        Objects.requireNonNull(from, "desde");
        Objects.requireNonNull(toward, "hacia");
    }

    @Override
    public void apply(Board board) {
        move(board, from, toward);
    }

    @Override
    public void revert(Board board) {
        move(board, toward, from);
    }

    private static void move(Board board, Position de, Position a) {
        if (!board.ball().filter(de::equals).isPresent()) {
            throw new IllegalStateException("La pelota no está en " + de);
        }
        board.placeBall(a);
    }
}
