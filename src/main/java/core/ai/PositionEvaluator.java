package core.ai;

import core.side.Side;
import core.moves.Move;
import core.pieces.Piece;
import core.rules.Scoreboard;
import core.board.IBoard;
import core.board.Position;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Qué tan buena es una posición, en centésimos de peón, desde el punto de vista de un bando. Es lo que
 * Minimax usa cuando deja de mirar hacia adelante. No decide finales (mate, goles para ganar): eso lo
 * dicen las condiciones de fin de las reglas.
 *
 * <ul>
 *   <li><b>Material:</b> el {@link Pieza#valor()} que declara cada pieza (peón 100 … dama 900).</li>
 *   <li><b>Posición:</b> las piezas menores (las que valen hasta {@link #PIEZA_MENOR}, como caballos y
 *       alfiles) y los peones valen un poco más cerca del centro; los peones, también a medida que avanzan.</li>
 *   <li><b>Fútbol</b> (si hay pelota): cada gol de diferencia vale {@link #VALOR_GOL}, más que una
 *       dama; tener la pelota suma, y más cuanto más cerca está del arco rival.</li>
 * </ul>
 */
public final class PositionEvaluator {
    static final int VALUE_GOAL = 1200;
    /** Hasta este valor una pieza conviene centralizarla; las más valiosas (torre, dama) no. */
    static final int PIECE_SMALLER = 400;
    static final int POSSESSION = 40;
    static final int BY_NEARNESS_TO_NET = 12;

    /** Casillas de arco que defiende cada bando: ahí hace gol el rival. */
    private final Map<Side, List<Position>> nets = new EnumMap<>(Side.class);

    /** Los arcos se leen una sola vez del tablero de la partida: no cambian durante el juego. */
    public PositionEvaluator(IBoard board) {
        for (Side side : Side.values()) nets.put(side, new ArrayList<>());
        for (int column = 0; column < board.width(); column++) {
            for (int row = 0; row < board.height(); row++) {
                Position square = new Position(column, row);
                board.netIn(square).ifPresent(defender -> nets.get(defender).add(square));
            }
        }
    }

    /** Puntaje de la posición para {@code bando}: positivo si le conviene, negativo si no. */
    public int evaluate(IBoard board, Side side, List<Move> moves) {
        int white = 0;
        for (Map.Entry<Position, Piece> input : board.pieces().entrySet()) {
            Piece piece = input.getValue();
            int value = piece.value() + positional(piece, input.getKey(), board);
            white += piece.color() == Side.WHITE ? value : -value;
        }
        if (board.ball().isPresent()) white += football(board, moves);
        return side == Side.WHITE ? white : -white;
    }

    private static int positional(Piece piece, Position square, IBoard board) {
        boolean smaller = !piece.isKing() && piece.value() <= PIECE_SMALLER;
        if (!piece.isPawn() && !smaller) return 0;
        int towardCenterColumns = Math.min(square.column(), board.width() - 1 - square.column());
        int towardCenterRows = Math.min(square.row(), board.height() - 1 - square.row());
        int center = 5 * (Math.min(towardCenterColumns, 3) + Math.min(towardCenterRows, 3));
        if (!piece.isPawn()) return center;
        int advance = piece.color() == Side.WHITE ? square.row() - 1 : board.height() - 2 - square.row();
        return center / 2 + 8 * Math.max(0, advance);
    }

    /** Goles y pelota, desde el punto de vista de las blancas. */
    private int football(IBoard board, List<Move> moves) {
        Scoreboard scoreboard = Scoreboard.de(moves);
        int score = VALUE_GOAL * (scoreboard.white() - scoreboard.black());
        Position ball = board.ball().orElseThrow();
        Optional<Piece> withBall = board.pieceIn(ball);
        if (withBall.isPresent()) {
            Side owner = withBall.get().color();
            // Ataca el arco que defiende el rival.
            int distance = distanceToNet(ball, nets.get(owner.opposite()));
            int nearness = Math.max(0, board.height() - distance);
            int advantage = POSSESSION + BY_NEARNESS_TO_NET * nearness;
            score += owner == Side.WHITE ? advantage : -advantage;
        }
        return score;
    }

    private static int distanceToNet(Position ball, List<Position> net) {
        int distance = Integer.MAX_VALUE;
        for (Position square : net) {
            distance = Math.min(distance, Math.max(Math.abs(square.column() - ball.column()),
                    Math.abs(square.row() - ball.row())));
        }
        return net.isEmpty() ? 0 : distance;
    }
}
