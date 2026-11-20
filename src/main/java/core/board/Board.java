package core.board;

import core.side.Side;
import core.pieces.Piece;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class Board implements IBoard {

    private final int width;
    private final int height;
    private final Map<Position, Piece> pieces = new HashMap<>();
    /** Casilla de la pelota del modo fútbol; null si no se juega con pelota. */
    private Position ball;
    /** Casillas de los arcos del modo fútbol y el bando que defiende cada una. No cambian en la partida. */
    private final Map<Position, Side> nets = new HashMap<>();

    public Board(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("The board dimensions must be positive");
        }
        this.width = width;
        this.height = height;
    }

    private Board(Board other) {
        this(other.width, other.height);
        this.pieces.putAll(other.pieces); // las piezas son inmutables: se pueden compartir
        this.ball = other.ball;
        this.nets.putAll(other.nets);
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public boolean contains(Position position) {
        return position.column() >= 0 && position.column() < width
                && position.row() >= 0 && position.row() < height;
    }

    @Override
    public Optional<Piece> pieceIn(Position position) {
        return Optional.ofNullable(pieces.get(position));
    }

    @Override
    public Map<Position, Piece> pieces() {
        return Collections.unmodifiableMap(pieces);
    }

    @Override
    public Optional<Position> ball() {
        return Optional.ofNullable(ball);
    }

    /** Pone la pelota en esa casilla (haya o no una pieza ahí); si ya estaba en otra, la mueve. */
    public void placeBall(Position position) {
        if (!contains(position)) {
            throw new IllegalArgumentException("Position outside the board: " + position);
        }
        ball = position;
    }

    @Override
    public Optional<Side> netIn(Position position) {
        return Optional.ofNullable(nets.get(position));
    }

    /** Marca la casilla como parte del arco que defiende ese bando. */
    public void markNet(Position position, Side defender) {
        if (!contains(position)) {
            throw new IllegalArgumentException("Position outside the board: " + position);
        }
        nets.put(position, java.util.Objects.requireNonNull(defender, "defender"));
    }

    /** Coloca la pieza; si había otra en esa casilla, la reemplaza. */
    public void place(Position position, Piece piece) {
        if (!contains(position)) {
            throw new IllegalArgumentException("Position outside the board: " + position);
        }
        pieces.put(position, piece);
    }

    /** Retira y devuelve la pieza de esa casilla, si había alguna. */
    public Optional<Piece> remove(Position position) {
        return Optional.ofNullable(pieces.remove(position));
    }

    /** Copia independiente, útil para simular jugadas sin alterar el tablero real. */
    public Board copy() {
        return new Board(this);
    }
}
