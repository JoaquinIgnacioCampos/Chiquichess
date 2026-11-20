package core.rules;

import core.side.Side;
import core.moves.Effect;
import core.moves.Move;
import core.moves.MoveBall;
import core.moves.Pass;
import core.moves.Replacement;
import core.moves.Removal;
import core.moves.Transfer;
import core.pieces.Pawn;
import core.pieces.Piece;
import core.board.Position;
import core.board.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Modo fútbol "Posesión y gol": la pelota se lleva y se mete en el arco rival.
 *
 * La pelota y los arcos son marcas del tablero, no piezas: la pelota no bloquea a nadie y la tiene
 * la pieza que está parada sobre ella. {@link #prepararCancha} pone esas marcas al empezar; después
 * la regla las lee del tablero, así el dibujo y la regla usan los mismos arcos.
 *
 * A cada jugada que toca la pelota le suma un {@link MoverPelota}, igual que la promoción le suma un
 * reemplazo. Además agrega los pases.
 * <ul>
 *   <li>Terminar el movimiento en la casilla de la pelota la toma; pasar por encima no.</li>
 *   <li>Quien lleva la pelota la arrastra en cada movimiento.</li>
 *   <li>Capturar a quien la lleva la gana (también en la captura al paso, donde el que captura
 *       termina en otra casilla).</li>
 *   <li>Llevarla a una casilla del arco rival es gol; el rey la puede llevar pero no hace gol. Un
 *       peón que promueve en el arco hace gol con la pieza nueva.</li>
 *   <li>Después del gol la pelota va al saque del bando que lo recibió, sin mover las piezas. Si
 *       hay una pieza parada en el saque, esa pieza se queda con la pelota.</li>
 *   <li>Pase: quien tiene la pelota se la puede dar a un compañero parado en una casilla que podría
 *       capturar si fuera rival. Las piezas que se deslizan necesitan el camino libre; el caballo
 *       la pasa por arriba; el rey, a una casilla de al lado; el peón, solo en diagonal hacia
 *       adelante. El pase gasta el turno. Si el que recibe está en el arco rival y no es el rey,
 *       es gol.</li>
 * </ul>
 *
 * Tiene que ir después de la promoción en la lista de reglas especiales, para ver la pieza nueva.
 * Si el tablero no tiene pelota, no cambia ninguna jugada.
 */
public class BallRule implements SpecialRule {

    private final Set<Integer> netColumns;
    private final Position kickoffInitial;
    private final Position kickoffAfterGoalOfWhite;
    private final Position kickoffAfterGoalOfBlack;

    /**
     * Tablero de 8x8: el arco son las columnas d y e de la fila de fondo de cada bando; la pelota
     * empieza en d5, vuelve a d5 (campo negro) tras un gol blanco y a d4 (campo blanco) tras un gol negro.
     */
    public BallRule() {
        this(Set.of(3, 4), new Position(3, 4), new Position(3, 4), new Position(3, 3));
    }

    /**
     * @param columnasDelArco        columnas que forman el arco, en la fila de fondo de cada bando
     * @param saqueInicial           dónde empieza la pelota
     * @param saqueTrasGolDeBlancas  dónde vuelve la pelota cuando hacen gol las blancas
     * @param saqueTrasGolDeNegras   dónde vuelve la pelota cuando hacen gol las negras
     */
    public BallRule(Set<Integer> netColumns, Position kickoffInitial,
                           Position kickoffAfterGoalOfWhite, Position kickoffAfterGoalOfBlack) {
        this.netColumns = Set.copyOf(Objects.requireNonNull(netColumns, "netColumns"));
        if (this.netColumns.isEmpty()) {
            throw new IllegalArgumentException("The goal must have at least one column");
        }
        this.kickoffInitial = Objects.requireNonNull(kickoffInitial, "kickoffInitial");
        this.kickoffAfterGoalOfWhite = Objects.requireNonNull(kickoffAfterGoalOfWhite, "kickoffAfterGoalOfWhite");
        this.kickoffAfterGoalOfBlack = Objects.requireNonNull(kickoffAfterGoalOfBlack, "kickoffAfterGoalOfBlack");
    }

    /**
     * Marca la cancha en el tablero: el arco de cada bando en su fila de fondo y la pelota en el saque
     * inicial. Las piezas no se tocan.
     */
    public void preparePitch(Board board) {
        for (int column : netColumns) {
            board.markNet(new Position(column, 0), Side.WHITE);
            board.markNet(new Position(column, board.height() - 1), Side.BLACK);
        }
        board.placeBall(kickoffInitial);
    }

    /** Los pases de la pieza que está en origen, si es la que tiene la pelota. */
    @Override
    public List<Move> moves(Board board, Position origin, List<Move> history, ICheckDetector detector) {
        Optional<Piece> passer = board.pieceIn(origin);
        if (passer.isEmpty() || !board.ball().filter(origin::equals).isPresent()) {
            return List.of();
        }
        List<Move> passes = new ArrayList<>();
        for (Map.Entry<Position, Piece> input : board.pieces().entrySet()) {
            Position toward = input.getKey();
            Piece receiver = input.getValue();
            if (!toward.equals(origin) && receiver.color() == passer.get().color()
                    && reaches(board, passer.get(), origin, toward)) {
                passes.add(pass(board, passer.get(), origin, receiver, toward));
            }
        }
        return passes;
    }

    /**
     * true si la pieza podría capturar en esa casilla si ahí hubiera un rival. Así cada pieza pasa
     * con su propio movimiento de captura: camino libre para las que se deslizan, salto para el
     * caballo, diagonal hacia adelante para el peón.
     */
    private static boolean reaches(Board board, Piece passer, Position origin, Position toward) {
        Board test = board.copy();
        test.place(toward, new Pawn(passer.color().opposite()));
        return passer.destinationsPossible(test, origin).contains(toward);
    }

    /** Jugada de pase; si el que recibe está en el arco rival y no es el rey, la pelota va al saque. */
    private Move pass(Board board, Piece passer, Position origin, Piece receiver, Position toward) {
        Move pass = new Move(origin, toward, List.of(new Pass(passer, origin, receiver, toward)));
        boolean goal = !receiver.isKing()
                && board.netIn(toward).filter(defender -> defender != receiver.color()).isPresent();
        return goal ? pass.con(new MoveBall(toward, kickoffAfterGoalOf(receiver.color()), true)) : pass;
    }

    private Position kickoffAfterGoalOf(Side scorer) {
        return scorer == Side.WHITE ? kickoffAfterGoalOfWhite : kickoffAfterGoalOfBlack;
    }

    @Override
    public List<Move> transform(Board board, Move move) {
        Optional<Position> ball = board.ball();
        if (ball.isEmpty()) {
            return List.of(move);
        }
        Position from = ball.get();
        Position destination = goesTo(move, from);
        Optional<Piece> carrier = carrierIn(move, destination);
        boolean goal = carrier
                .filter(piece -> !piece.isKing())
                .filter(piece -> board.netIn(destination).filter(defender -> defender != piece.color()).isPresent())
                .isPresent();
        if (goal) {
            return List.of(move.con(new MoveBall(from, kickoffAfterGoalOf(carrier.get().color()), true)));
        }
        if (destination.equals(from)) {
            return List.of(move);
        }
        return List.of(move.con(new MoveBall(from, destination, false)));
    }

    /**
     * Casilla donde queda la pelota después de la jugada, antes de ver si es gol: la sigue a quien la
     * llevaba; si a ese lo capturaron, va a la casilla de quien capturó; si no, se queda donde está.
     */
    private static Position goesTo(Move move, Position ball) {
        for (Effect effect : move.effects()) {
            if (effect instanceof Transfer transfer && transfer.origin().equals(ball)) {
                return transfer.destination();
            }
        }
        for (Effect effect : move.effects()) {
            if (effect instanceof Removal withdrawal && withdrawal.position().equals(ball)) {
                return move.destination();
            }
        }
        return ball;
    }

    /**
     * Pieza de esta jugada que termina en la casilla (ya promovida, si promovió); vacío si ninguna
     * pieza de la jugada termina ahí, es decir, si la pelota quedó con una pieza que no se movió.
     */
    private static Optional<Piece> carrierIn(Move move, Position square) {
        Optional<Piece> carrier = Optional.empty();
        for (Effect effect : move.effects()) {
            if (effect instanceof Transfer transfer && transfer.destination().equals(square)) {
                carrier = Optional.of(transfer.piece());
            } else if (effect instanceof Replacement replacement && replacement.position().equals(square)) {
                carrier = Optional.of(replacement.newValue());
            }
        }
        return carrier;
    }
}
