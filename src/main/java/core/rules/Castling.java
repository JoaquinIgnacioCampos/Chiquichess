package core.rules;

import core.side.Side;
import core.moves.Effect;
import core.moves.Move;
import core.moves.Transfer;
import core.pieces.Piece;
import core.board.IBoard;
import core.board.Position;
import core.board.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enroque: el rey se mueve dos casillas hacia una torre y la torre pasa al otro lado del rey,
 * en una sola jugada de dos traslados. Se juega moviendo el rey a su casilla de destino
 * (por ejemplo, e1 → g1 para el enroque corto y e1 → c1 para el largo).
 *
 * Condiciones:
 * - el rey y la torre están en la fila base de su bando y no se movieron en la partida;
 * - las casillas entre el rey y la torre están vacías;
 * - el rey no está en jaque y no pasa ni cae en una casilla atacada.
 *
 * Que la pieza "no se movió" se deduce del historial (por instancia de pieza, no por casilla), así no
 * hace falta guardar derechos de enroque: al deshacer una jugada, el derecho vuelve solo. En un tablero
 * armado a mano, sin historial, el rey y las torres se consideran sin mover.
 */
public class Castling implements SpecialRule {

    /** El rey se mueve dos casillas: hace falta al menos una casilla más hasta la torre. */
    private static final int MIN_DISTANCE_TO_ROOK = 3;

    @Override
    public List<Move> moves(Board board, Position origin, List<Move> history, ICheckDetector detector) {
        Optional<Piece> candidate = board.pieceIn(origin);
        if (candidate.isEmpty() || !candidate.get().isKing()) {
            return List.of();
        }
        Piece king = candidate.get();
        if (origin.row() != rowBase(board, king.color()) || moved(king, history)
                || detector.isInCheck(board, king.color())) {
            return List.of();
        }
        List<Move> moves = new ArrayList<>();
        for (int direction : new int[]{1, -1}) {
            castlingToward(direction, board, origin, king, history, detector).ifPresent(moves::add);
        }
        return moves;
    }

    private Optional<Move> castlingToward(int direction, Board board, Position origin, Piece king,
                                          List<Move> history, ICheckDetector detector) {
        // La primera pieza en esa dirección tiene que ser una torre propia que no se movió:
        // así también se verifica que las casillas del medio estén vacías.
        Position square = origin.shifted(direction, 0);
        while (board.contains(square) && board.pieceIn(square).isEmpty()) {
            square = square.shifted(direction, 0);
        }
        Optional<Piece> found = board.pieceIn(square);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Piece rook = found.get();
        if (!rook.isRook() || rook.color() != king.color() || moved(rook, history)
                || Math.abs(square.column() - origin.column()) < MIN_DISTANCE_TO_ROOK) {
            return Optional.empty();
        }
        Position step = origin.shifted(direction, 0);
        Position destination = origin.shifted(2 * direction, 0);
        if (staysAttacked(board, king, origin, step, detector) || staysAttacked(board, king, origin, destination, detector)) {
            return Optional.empty();
        }
        List<Effect> effects = List.of(new Transfer(king, origin, destination), new Transfer(rook, square, step));
        return Optional.of(new Move(origin, destination, effects));
    }

    /** true si el rey, llevado a esa casilla, quedaría en jaque. */
    private static boolean staysAttacked(Board board, Piece king, Position origin, Position square,
                                        ICheckDetector detector) {
        Board simulation = board.copy();
        simulation.remove(origin);
        simulation.place(square, king);
        return detector.isInCheck(simulation, king.color());
    }

    /** Una pieza se movió si algún traslado del historial la llevó a alguna parte. */
    private static boolean moved(Piece piece, List<Move> history) {
        return history.stream()
                .flatMap(move -> move.effects().stream())
                .anyMatch(effect -> effect instanceof Transfer t && t.piece() == piece);
    }

    private static int rowBase(IBoard board, Side side) {
        return side == Side.WHITE ? 0 : board.height() - 1;
    }
}
