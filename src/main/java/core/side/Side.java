package core.side;

/**
 * Bando (blancas o negras) al que pertenece una pieza o un jugador.
 * Tiene paquete propio porque lo usan casi todos los paquetes del núcleo (piezas, reglas,
 * tiempo, juego) y no es un concepto exclusivo de las piezas.
 * La dirección de avance de los peones depende del bando.
 */
public enum Side {
    WHITE(1),
    BLACK(-1);

    private final int directionAdvance;

    Side(int directionAdvance) {
        this.directionAdvance = directionAdvance;
    }

    /** +1 si el bando avanza hacia filas mayores, -1 si avanza hacia filas menores. */
    public int directionAdvance() {
        return directionAdvance;
    }

    public Side opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
