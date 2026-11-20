package core.rules;

import core.side.Side;
import core.moves.Effect;
import core.moves.Move;
import core.moves.Removal;
import core.moves.Transfer;
import core.pieces.Piece;
import core.board.Position;
import core.board.Board;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Responsabilidad única: decidir qué jugadas son legales, es decir, las que permiten las reglas
 * de la pieza (o una regla especial, como el enroque) Y no dejan al propio rey en jaque. Se separó
 * de Partida porque también lo usan jaque mate y ahogado ("¿existe alguna jugada legal?").
 *
 * Trabaja con {@link Jugada}: el control de jaque simula la jugada completa (todos sus efectos),
 * así sirve igual para jugadas que mueven más de una pieza o capturan en otra casilla.
 *
 * Las reglas especiales necesitan el historial de la partida (por ejemplo, para saber si el rey ya se
 * movió). Los métodos sin historial equivalen a una partida sin jugadas.
 */
public class LegalMoves {

    private final ICheckDetector detector;
    private final List<SpecialRule> special;

    /** Sin reglas especiales: solo el movimiento propio de cada pieza. */
    public LegalMoves(ICheckDetector detector) {
        this(detector, List.of());
    }

    public LegalMoves(ICheckDetector detector, List<SpecialRule> special) {
        this.detector = Objects.requireNonNull(detector, "detector");
        this.special = List.copyOf(Objects.requireNonNull(special, "especiales"));
    }

    /** Detector con el que se decide si una jugada deja al rey en jaque. */
    public ICheckDetector detector() {
        return detector;
    }

    /**
     * Jugadas de la pieza que está en origen, sin controlar todavía si dejan al propio rey en jaque:
     * las de su movimiento propio más las de las reglas especiales, con las transformaciones de esas
     * reglas aplicadas (por ejemplo, la promoción). Vacío si no hay pieza.
     */
    public List<Move> movesPossible(Board board, Position origin, List<Move> history) {
        List<Move> moves = new ArrayList<>();
        board.pieceIn(origin).ifPresent(piece -> {
            for (Position destination : piece.destinationsPossible(board, origin)) {
                moves.add(Move.normal(board, origin, destination));
            }
            for (SpecialRule special : special) {
                moves.addAll(special.moves(board, origin, history, detector));
            }
        });
        List<Move> transformed = moves;
        for (SpecialRule special : special) {
            transformed = transformed.stream()
                    .flatMap(move -> special.transform(board, move).stream())
                    .toList();
        }
        return transformed;
    }

    /** La jugada que llevaría la pieza de origen a destino; vacío si no puede ir ahí. */
    public Optional<Move> movePossible(Board board, Position origin, Position destination, List<Move> history) {
        return movesPossible(board, origin, history).stream()
                .filter(move -> move.destination().equals(destination))
                .findFirst();
    }

    /** Destinos legales de la pieza que está en origen (vacío si no hay pieza). */
    public Set<Position> destinations(Board board, Position origin, List<Move> history) {
        Set<Position> legal = new HashSet<>();
        for (Move move : movesPossible(board, origin, history)) {
            if (!leavesKingInCheck(board, move)) {
                legal.add(move.destination());
            }
        }
        return legal;
    }

    /** true si el color tiene al menos una jugada legal. Corta apenas encuentra una. */
    public boolean existsSome(Board board, Side color, List<Move> history) {
        for (Map.Entry<Position, Piece> input : board.pieces().entrySet()) {
            if (input.getValue().color() != color) {
                continue;
            }
            for (Move move : movesPossible(board, input.getKey(), history)) {
                if (!leavesKingInCheck(board, move)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Simula la jugada en una copia del tablero y consulta si el rey del que movió queda atacado. */
    public boolean leavesKingInCheck(Board board, Move move) {
        Board simulation = board.copy();
        move.apply(simulation);
        return detector.isInCheck(simulation, move.pieceMoved().color());
    }

    // ---- Variantes sin historial: equivalen a una partida sin jugadas. Son final para que un doble de
    // ---- prueba sobreescriba las de arriba, que son las que usa Partida.

    public final List<Move> movesPossible(Board board, Position origin) {
        return movesPossible(board, origin, List.of());
    }

    public final Optional<Move> movePossible(Board board, Position origin, Position destination) {
        return movePossible(board, origin, destination, List.of());
    }

    public final Set<Position> destinations(Board board, Position origin) {
        return destinations(board, origin, List.of());
    }

    public final boolean existsSome(Board board, Side color) {
        return existsSome(board, color, List.of());
    }

    /** Variante con la pieza y las casillas sueltas: arma la jugada común y la simula. */
    public final boolean leavesKingInCheck(Board board, Piece piece, Position origin, Position destination) {
        List<Effect> effects = new ArrayList<>();
        board.pieceIn(destination).ifPresent(captured -> effects.add(new Removal(captured, destination)));
        effects.add(new Transfer(piece, origin, destination));
        return leavesKingInCheck(board, new Move(origin, destination, effects));
    }
}
