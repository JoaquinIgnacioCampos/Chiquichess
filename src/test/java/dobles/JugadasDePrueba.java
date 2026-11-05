package dobles;

import Nucleo.jugadas.Jugada;
import Nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

import static dobles.Casillas.pos;

/** Aplica una secuencia de jugadas comunes ("e2e4") sobre un tablero y devuelve las jugadas ejecutadas. */
public final class JugadasDePrueba {

    private JugadasDePrueba() {
    }

    public static List<Jugada> jugar(Tablero tablero, String... jugadas) {
        List<Jugada> ejecutadas = new ArrayList<>();
        for (String texto : jugadas) {
            Jugada jugada = Jugada.normal(tablero, pos(texto.substring(0, 2)), pos(texto.substring(2, 4)));
            jugada.aplicar(tablero);
            ejecutadas.add(jugada);
        }
        return ejecutadas;
    }
}
