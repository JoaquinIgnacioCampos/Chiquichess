package core.moves;

import core.side.Side;
import core.pieces.Piece;
import core.board.IBoard;
import core.board.Position;

import java.util.Map;

/**
 * Huella numérica de una posición de ajedrez, entendida como la foto completa del tablero
 * (qué pieza hay en cada casilla y a quién le toca mover), técnica conocida como hash de Zobrist.
 *
 * Cada combinación de pieza y casilla tiene asignado un número de 64 bits fijo; la huella es el XOR
 * de los números de todas las piezas, más un número extra si juegan las negras. Como aplicar dos
 * veces el mismo XOR lo cancela, la huella se actualiza con cada jugada sin recorrer el tablero:
 * un traslado "saca" la pieza de su origen y la "pone" en su destino, y un retiro la saca.
 *
 * En el modo fútbol la casilla de la pelota también es parte de la posición: suma su propio número,
 * y mover la pelota lo saca de una casilla y lo pone en otra, igual que con una pieza.
 *
 * Dos posiciones iguales tienen siempre la misma huella. Dos posiciones distintas podrían coincidir
 * por azar, con una probabilidad despreciable (del orden de 1 en 2^64 por par de posiciones).
 */
public final class PositionFingerprint {

    private static final long PLAY_BLACK = mix(0x6A09E667F3BCC909L);
    private static final long BALL = mix(0xBB67AE8584CAA73BL);

    private PositionFingerprint() {
    }

    /** Huella calculada desde cero recorriendo todas las piezas del tablero. */
    public static long de(IBoard board, Side turn) {
        long fingerprint = turn == Side.BLACK ? PLAY_BLACK : 0L;
        for (Map.Entry<Position, Piece> input : board.pieces().entrySet()) {
            fingerprint ^= value(input.getValue(), input.getKey());
        }
        return fingerprint ^ board.ball().map(PositionFingerprint::ballValue).orElse(0L);
    }

    /** Huella de la posición que resulta de aplicar la jugada; también pasa el turno al otro bando. */
    public static long tras(long fingerprint, Move move) {
        long newValue = fingerprint ^ PLAY_BLACK;
        for (Effect effect : move.effects()) {
            newValue ^= switch (effect) {
                case Transfer t -> value(t.piece(), t.origin()) ^ value(t.piece(), t.destination());
                case Removal r -> value(r.piece(), r.position());
                case Replacement r -> value(r.previous(), r.position()) ^ value(r.newValue(), r.position());
                case MoveBall m -> ballValue(m.from()) ^ ballValue(m.toward());
                case Pass pass -> ballValue(pass.from()) ^ ballValue(pass.toward());
            };
        }
        return newValue;
    }

    /**
     * Número fijo de una pieza en una casilla. Se deriva de la identidad de la pieza (bando y
     * nombre, igual que antes comparaba la regla de repetición) y de la casilla, así no hace falta
     * una tabla precalculada y sirve para tableros de cualquier tamaño.
     */
    private static long value(Piece piece, Position square) {
        long type = mix(((long) piece.color().ordinal() << 32) ^ piece.displayName().hashCode());
        long withColumn = mix(type + 0x9E3779B97F4A7C15L * (square.column() + 1));
        return mix(withColumn + 0xC2B2AE3D27D4EB4FL * (square.row() + 1));
    }

    /** Número fijo de la pelota en una casilla, independiente de los de las piezas. */
    private static long ballValue(Position square) {
        long withColumn = mix(BALL + 0x9E3779B97F4A7C15L * (square.column() + 1));
        return mix(withColumn + 0xC2B2AE3D27D4EB4FL * (square.row() + 1));
    }

    /** Mezclador de SplitMix64: convierte un número en otro que parece aleatorio. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
