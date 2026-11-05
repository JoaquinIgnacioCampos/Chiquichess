package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Efecto;
import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.Traslado;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enroque: el rey se mueve dos casillas hacia una torre y la torre pasa al otro lado del rey,
 * en una sola jugada de dos traslados. Se juega moviendo el rey a su casilla de destino
 * (por ejemplo, e1 → g1 para el enroque corto y e1 → c1 para el largo).
 *
 * Condiciones:
 * - el rey y la torre están en la fila base de su bando y no se movieron en la partida;
 * - las casillas entre el rey y la torre están vacías;
 * - el rey no está en jaque y no pasa ni cae en una casilla atacada.
 *
 * Que la pieza "no se movió" se deduce del historial (por instancia de pieza, no por casilla), así no
 * hace falta guardar derechos de enroque: al deshacer una jugada, el derecho vuelve solo. En un tablero
 * armado a mano, sin historial, el rey y las torres se consideran sin mover.
 */
public class Enroque implements ReglaEspecial {

    /** El rey se mueve dos casillas: hace falta al menos una casilla más hasta la torre. */
    private static final int DISTANCIA_MINIMA_A_LA_TORRE = 3;

    @Override
    public List<Jugada> jugadas(Tablero tablero, Posicion origen, List<Jugada> historial, IDetectorDeJaque detector) {
        Optional<Pieza> candidata = tablero.piezaEn(origen);
        if (candidata.isEmpty() || !candidata.get().esRey()) {
            return List.of();
        }
        Pieza rey = candidata.get();
        if (origen.fila() != filaBase(tablero, rey.color()) || seMovio(rey, historial)
                || detector.estaEnJaque(tablero, rey.color())) {
            return List.of();
        }
        List<Jugada> jugadas = new ArrayList<>();
        for (int direccion : new int[]{1, -1}) {
            enroqueHacia(direccion, tablero, origen, rey, historial, detector).ifPresent(jugadas::add);
        }
        return jugadas;
    }

    private Optional<Jugada> enroqueHacia(int direccion, Tablero tablero, Posicion origen, Pieza rey,
                                          List<Jugada> historial, IDetectorDeJaque detector) {
        // La primera pieza en esa dirección tiene que ser una torre propia que no se movió:
        // así también se verifica que las casillas del medio estén vacías.
        Posicion casilla = origen.desplazada(direccion, 0);
        while (tablero.contiene(casilla) && tablero.piezaEn(casilla).isEmpty()) {
            casilla = casilla.desplazada(direccion, 0);
        }
        Optional<Pieza> encontrada = tablero.piezaEn(casilla);
        if (encontrada.isEmpty()) {
            return Optional.empty();
        }
        Pieza torre = encontrada.get();
        if (!torre.esTorre() || torre.color() != rey.color() || seMovio(torre, historial)
                || Math.abs(casilla.columna() - origen.columna()) < DISTANCIA_MINIMA_A_LA_TORRE) {
            return Optional.empty();
        }
        Posicion paso = origen.desplazada(direccion, 0);
        Posicion destino = origen.desplazada(2 * direccion, 0);
        if (quedaAtacado(tablero, rey, origen, paso, detector) || quedaAtacado(tablero, rey, origen, destino, detector)) {
            return Optional.empty();
        }
        List<Efecto> efectos = List.of(new Traslado(rey, origen, destino), new Traslado(torre, casilla, paso));
        return Optional.of(new Jugada(origen, destino, efectos));
    }

    /** true si el rey, llevado a esa casilla, quedaría en jaque. */
    private static boolean quedaAtacado(Tablero tablero, Pieza rey, Posicion origen, Posicion casilla,
                                        IDetectorDeJaque detector) {
        Tablero simulacion = tablero.copiar();
        simulacion.quitar(origen);
        simulacion.colocar(casilla, rey);
        return detector.estaEnJaque(simulacion, rey.color());
    }

    /** Una pieza se movió si algún traslado del historial la llevó a alguna parte. */
    private static boolean seMovio(Pieza pieza, List<Jugada> historial) {
        return historial.stream()
                .flatMap(jugada -> jugada.efectos().stream())
                .anyMatch(efecto -> efecto instanceof Traslado t && t.pieza() == pieza);
    }

    private static int filaBase(ITablero tablero, Bando bando) {
        return bando == Bando.BLANCO ? 0 : tablero.alto() - 1;
    }
}
