package dobles;

import adaptadores.configuracion.Sorteo;

/** Stub con registro de uso: siempre da el mismo resultado y cuenta cuántas veces se lo consultó. */
public class SorteoFijo implements Sorteo {

    private final boolean jugador1Blancas;
    private int consultas = 0;

    private SorteoFijo(boolean jugador1Blancas) {
        this.jugador1Blancas = jugador1Blancas;
    }

    public static SorteoFijo jugador1ConBlancas() {
        return new SorteoFijo(true);
    }

    public static SorteoFijo jugador1ConNegras() {
        return new SorteoFijo(false);
    }

    @Override
    public boolean jugador1JuegaConBlancas() {
        consultas++;
        return jugador1Blancas;
    }

    public int consultas() {
        return consultas;
    }
}
