package Nucleo.jugadas;

import Nucleo.bando.Bando;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;

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
public final class HuellaDePosicion {

    private static final long JUEGAN_NEGRAS = mezclar(0x6A09E667F3BCC909L);
    private static final long PELOTA = mezclar(0xBB67AE8584CAA73BL);

    private HuellaDePosicion() {
    }

    /** Huella calculada desde cero recorriendo todas las piezas del tablero. */
    public static long de(ITablero tablero, Bando turno) {
        long huella = turno == Bando.NEGRO ? JUEGAN_NEGRAS : 0L;
        for (Map.Entry<Posicion, Pieza> entrada : tablero.piezas().entrySet()) {
            huella ^= valor(entrada.getValue(), entrada.getKey());
        }
        return huella ^ tablero.pelota().map(HuellaDePosicion::valorDeLaPelota).orElse(0L);
    }

    /** Huella de la posición que resulta de aplicar la jugada; también pasa el turno al otro bando. */
    public static long tras(long huella, Jugada jugada) {
        long nueva = huella ^ JUEGAN_NEGRAS;
        for (Efecto efecto : jugada.efectos()) {
            nueva ^= switch (efecto) {
                case Traslado t -> valor(t.pieza(), t.origen()) ^ valor(t.pieza(), t.destino());
                case Retiro r -> valor(r.pieza(), r.posicion());
                case Reemplazo r -> valor(r.anterior(), r.posicion()) ^ valor(r.nueva(), r.posicion());
                case MoverPelota m -> valorDeLaPelota(m.desde()) ^ valorDeLaPelota(m.hacia());
            };
        }
        return nueva;
    }

    /**
     * Número fijo de una pieza en una casilla. Se deriva de la identidad de la pieza (bando y
     * nombre, igual que antes comparaba la regla de repetición) y de la casilla, así no hace falta
     * una tabla precalculada y sirve para tableros de cualquier tamaño.
     */
    private static long valor(Pieza pieza, Posicion casilla) {
        long tipo = mezclar(((long) pieza.color().ordinal() << 32) ^ pieza.nombre().hashCode());
        long conColumna = mezclar(tipo + 0x9E3779B97F4A7C15L * (casilla.columna() + 1));
        return mezclar(conColumna + 0xC2B2AE3D27D4EB4FL * (casilla.fila() + 1));
    }

    /** Número fijo de la pelota en una casilla, independiente de los de las piezas. */
    private static long valorDeLaPelota(Posicion casilla) {
        long conColumna = mezclar(PELOTA + 0x9E3779B97F4A7C15L * (casilla.columna() + 1));
        return mezclar(conColumna + 0xC2B2AE3D27D4EB4FL * (casilla.fila() + 1));
    }

    /** Mezclador de SplitMix64: convierte un número en otro que parece aleatorio. */
    private static long mezclar(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
