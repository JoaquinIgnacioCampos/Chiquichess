package Nucleo.reglas;

import Nucleo.juego.Posicion;
import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Tablero;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Responsabilidad única: decidir qué jugadas son legales, es decir, las que permiten las reglas
 * de la pieza Y no dejan al propio rey en jaque. Se separó de Partida porque ahora también lo usan
 * jaque mate y ahogado ("¿existe alguna jugada legal?").
 */
public class MovimientosLegales {

    private final IDetectorDeJaque detector;

    public MovimientosLegales(IDetectorDeJaque detector) {
        this.detector = Objects.requireNonNull(detector, "detector");
    }

    /** Destinos legales de la pieza que está en origen (vacío si no hay pieza). */
    public Set<Posicion> destinos(Tablero tablero, Posicion origen) {
        Set<Posicion> legales = new HashSet<>();
        tablero.piezaEn(origen).ifPresent(pieza -> {
            for (Posicion destino : pieza.destinosPosibles(tablero, origen)) {
                if (!dejaAlReyEnJaque(tablero, pieza, origen, destino)) {
                    legales.add(destino);
                }
            }
        });
        return legales;
    }

    /** true si el color tiene al menos una jugada legal. Corta apenas encuentra una. */
    public boolean existeAlguno(Tablero tablero, Color color) {
        for (Map.Entry<Posicion, Pieza> entrada : tablero.piezas().entrySet()) {
            Pieza pieza = entrada.getValue();
            if (pieza.color() != color) {
                continue;
            }
            for (Posicion destino : pieza.destinosPosibles(tablero, entrada.getKey())) {
                if (!dejaAlReyEnJaque(tablero, pieza, entrada.getKey(), destino)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Simula la jugada en una copia del tablero y consulta si el rey propio queda atacado. */
    public boolean dejaAlReyEnJaque(Tablero tablero, Pieza pieza, Posicion origen, Posicion destino) {
        Tablero simulacion = tablero.copiar();
        simulacion.quitar(origen);
        simulacion.quitar(destino);
        simulacion.colocar(destino, pieza);
        return detector.estaEnJaque(simulacion, pieza.color());
    }
}
