package core.rules;

import core.moves.Move;
import core.moves.Removal;
import core.moves.Transfer;
import core.pieces.Piece;
import core.board.Position;
import core.board.Board;

import java.util.List;
import java.util.Optional;

/**
 * Captura al paso: si en la jugada anterior un peón rival avanzó dos casillas y quedó al lado de un
 * peón propio, ese peón puede capturarlo moviéndose en diagonal a la casilla que el rival "saltó".
 * Solo vale en la jugada inmediata siguiente.
 *
 * La pieza capturada no está en la casilla de destino, sino al lado del peón que captura: la jugada
 * tiene un Retiro en otra casilla, cosa que una Jugada admite sin cambios. Que no deje al propio rey en
 * jaque (por ejemplo, si al sacar los dos peones de la fila el rey queda expuesto) lo controla
 * MovimientosLegales simulando la jugada completa.
 */
public class EnPassant implements SpecialRule {

    @Override
    public List<Move> moves(Board board, Position origin, List<Move> history, ICheckDetector detector) {
        Optional<Piece> candidate = board.pieceIn(origin);
        if (candidate.isEmpty() || !candidate.get().isPawn() || history.isEmpty()) {
            return List.of();
        }
        Piece pawn = candidate.get();
        Optional<Transfer> advanceDouble = rivalPawnDoubleAdvance(history.get(history.size() - 1), pawn);
        if (advanceDouble.isEmpty()) {
            return List.of();
        }
        Position rival = advanceDouble.get().destination();
        if (rival.row() != origin.row() || Math.abs(rival.column() - origin.column()) != 1) {
            return List.of();
        }
        Position destination = new Position(rival.column(), origin.row() + pawn.color().directionAdvance());
        if (!board.contains(destination) || board.pieceIn(destination).isPresent()) {
            return List.of();
        }
        return List.of(new Move(origin, destination, List.of(
                new Removal(advanceDouble.get().piece(), rival),
                new Transfer(pawn, origin, destination))));
    }

    /** El traslado de la última jugada, si fue un peón del otro bando que avanzó dos casillas. */
    private static Optional<Transfer> rivalPawnDoubleAdvance(Move last, Piece pawn) {
        Piece moved = last.pieceMoved();
        if (!moved.isPawn() || moved.color() == pawn.color()) {
            return Optional.empty();
        }
        return last.effects().stream()
                .filter(effect -> effect instanceof Transfer t && t.piece() == moved)
                .map(effect -> (Transfer) effect)
                .filter(t -> t.origin().column() == t.destination().column()
                        && Math.abs(t.destination().row() - t.origin().row()) == 2)
                .findFirst();
    }
}
