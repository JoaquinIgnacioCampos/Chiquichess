package dobles;

import Nucleo.juego.Movimiento;
import Nucleo.juego.Posicion;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ayuda para escribir casillas como "e4" en los tests del núcleo, sin depender del adaptador
 * NotacionAlgebraica (el núcleo se prueba sin adaptadores). a1 = columna 0, fila 0.
 */
public final class Casillas {

    private Casillas() {
    }

    public static Posicion pos(String casilla) {
        return new Posicion(casilla.charAt(0) - 'a', Integer.parseInt(casilla.substring(1)) - 1);
    }

    public static Set<Posicion> posiciones(String... casillas) {
        return Arrays.stream(casillas).map(Casillas::pos).collect(Collectors.toSet());
    }

    public static Movimiento mov(String origen, String destino) {
        return new Movimiento(pos(origen), pos(destino));
    }
}
