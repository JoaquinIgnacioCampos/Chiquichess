package core.rules;

import core.side.Side;
import core.moves.Move;
import core.pieces.Piece;
import core.board.IBoard;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Foto de la partida que reciben las condiciones de finalización después de cada jugada
 * (y una vez al crear la partida, cuando todavía no hay jugadas).
 *
 * Incluye todas las jugadas ejecutadas para que las condiciones calculen su resultado a partir del
 * historial en lugar de llevar contadores propios: así siguen siendo correctas si se deshace una jugada.
 *
 * @param tablero                 tablero ya actualizado (solo lectura)
 * @param turno                   bando que debe jugar a continuación
 * @param jugadas                 jugadas ejecutadas en la partida, en orden; vacía en la evaluación inicial
 * @param enJaque                 true si el rey de quien tiene el turno está en jaque
 * @param tieneMovimientosLegales true si quien tiene el turno puede hacer al menos una jugada legal
 */
public record MoveContext(
        IBoard board,
        Side turn,
        List<Move> moves,
        boolean inCheck,
        boolean hasMovementsLegal) {

    public MoveContext {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(turn, "turn");
        moves = List.copyOf(Objects.requireNonNull(moves, "moves"));
    }

    /** La última jugada ejecutada; vacío en la evaluación inicial. */
    public Optional<Move> lastMove() {
        return moves.isEmpty() ? Optional.empty() : Optional.of(moves.get(moves.size() - 1));
    }

    /** Pieza que acaba de moverse; vacío en la evaluación inicial. */
    public Optional<Piece> pieceMoved() {
        return lastMove().map(Move::pieceMoved);
    }

    /** true si la última jugada capturó una pieza. */
    public boolean wasCapture() {
        return lastMove().flatMap(Move::captured).isPresent();
    }
}
