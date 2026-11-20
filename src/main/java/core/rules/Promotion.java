package core.rules;

import core.side.Side;
import core.moves.Move;
import core.moves.Replacement;
import core.pieces.Bishop;
import core.pieces.Knight;
import core.pieces.Piece;
import core.pieces.Queen;
import core.pieces.Rook;
import core.board.IBoard;
import core.board.Position;
import core.board.Board;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Promoción: el peón que llega a la última fila se convierte en otra pieza de su bando.
 *
 * No suma jugadas nuevas: transforma cada jugada de un peón que termina en la última fila (avance
 * o captura) en una variante por cada pieza posible, agregándole un {@link Replacement}. Así el peón ya
 * no puede quedarse como peón en la última fila. La primera opción es la que se usa si el jugador no
 * elige (la dama, en el ajedrez clásico).
 */
public class Promotion implements SpecialRule {

    private final List<Function<Side, Piece>> options;

    /** Ajedrez clásico: dama, torre, alfil o caballo, en ese orden. */
    public Promotion() {
        this(List.of(Queen::new, Rook::new, Bishop::new, Knight::new));
    }

    /** @param options fábricas de las piezas a las que se puede promover; la primera es la elección por defecto */
    public Promotion(List<Function<Side, Piece>> options) {
        this.options = List.copyOf(Objects.requireNonNull(options, "opciones"));
        if (this.options.isEmpty()) {
            throw new IllegalArgumentException("Tiene que haber al menos una pieza para promover");
        }
    }

    @Override
    public List<Move> moves(Board board, Position origin, List<Move> history, ICheckDetector detector) {
        return List.of();
    }

    @Override
    public List<Move> transform(Board board, Move move) {
        Piece pawn = move.pieceMoved();
        // Un peón que pasa la pelota (modo fútbol) no se mueve, así que no promueve.
        if (move.isPass() || !pawn.isPawn() || move.destination().row() != lastRow(board, pawn.color())) {
            return List.of(move);
        }
        return options.stream()
                .map(factory -> move.con(new Replacement(pawn, factory.apply(pawn.color()), move.destination())))
                .toList();
    }

    private static int lastRow(IBoard board, Side side) {
        return side == Side.WHITE ? board.height() - 1 : 0;
    }
}
