package Nucleo.bando;

/**
 * Bando (blancas o negras) al que pertenece una pieza o un jugador.
 * Tiene paquete propio porque lo usan casi todos los paquetes del núcleo (piezas, reglas,
 * tiempo, juego) y no es un concepto exclusivo de las piezas.
 * La dirección de avance de los peones depende del bando.
 */
public enum Bando {
    BLANCO(1),
    NEGRO(-1);

    private final int direccionAvance;

    Bando(int direccionAvance) {
        this.direccionAvance = direccionAvance;
    }

    /** +1 si el bando avanza hacia filas mayores, -1 si avanza hacia filas menores. */
    public int direccionAvance() {
        return direccionAvance;
    }

    public Bando opuesto() {
        return this == BLANCO ? NEGRO : BLANCO;
    }
}
