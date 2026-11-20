package core.game;

import core.rules.BallRule;
import core.board.InitialLayout;
import core.board.Board;

import java.util.Objects;

/**
 * Decorator: la misma disposición inicial, con la cancha del modo fútbol marcada (los arcos y la
 * pelota en el saque inicial). Las piezas quedan como las pone la disposición de base.
 */
public class FootballLayout implements InitialLayout {

    private final InitialLayout base;
    private final BallRule ball;

    public FootballLayout(InitialLayout base, BallRule ball) {
        this.base = Objects.requireNonNull(base, "base");
        this.ball = Objects.requireNonNull(ball, "pelota");
    }

    @Override
    public Board createBoard() {
        Board board = base.createBoard();
        ball.preparePitch(board);
        return board;
    }
}
