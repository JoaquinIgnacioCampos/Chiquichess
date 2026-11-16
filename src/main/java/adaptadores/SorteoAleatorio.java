package adaptadores;

import adaptadores.configuracion.Sorteo;

import java.util.concurrent.ThreadLocalRandom;

/** Sorteo real: cada color tiene la misma probabilidad. */
public class SorteoAleatorio implements Sorteo {
    @Override
    public boolean jugador1JuegaConBlancas() {
        return ThreadLocalRandom.current().nextBoolean();
    }
}
