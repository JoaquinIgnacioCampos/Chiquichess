package Nucleo.piezas;

/** Bando de una pieza o jugador. La dirección de avance de los peones depende del color. */
public enum Color {
    BLANCO(1),
    NEGRO(-1);

    private final int direccionAvance;

    Color(int direccionAvance) {
        this.direccionAvance = direccionAvance;
    }

    /** +1 si el color avanza hacia filas mayores, -1 si avanza hacia filas menores. */
    public int direccionAvance() {
        return direccionAvance;
    }

    public Color opuesto() {
        return this == BLANCO ? NEGRO : BLANCO;
    }
}
