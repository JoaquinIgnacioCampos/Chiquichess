package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Efecto;
import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.Retiro;
import Nucleo.jugadas.Traslado;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Responsabilidad única: decidir qué jugadas son legales, es decir, las que permiten las reglas
 * de la pieza Y no dejan al propio rey en jaque. Se separó de Partida porque ahora también lo usan
 * jaque mate y ahogado ("¿existe alguna jugada legal?").
 *
 * Trabaja con {@link Jugada}: el control de jaque simula la jugada completa (todos sus efectos),
 * así sirve igual para jugadas que mueven más de una pieza o capturan en otra casilla.
 */
public class MovimientosLegales {

    private final IDetectorDeJaque detector;

    public MovimientosLegales(IDetectorDeJaque detector) {
        this.detector = Objects.requireNonNull(detector, "detector");
    }

    /** Detector con el que se decide si una jugada deja al rey en jaque. */
    public IDetectorDeJaque detector() {
        return detector;
    }

    /**
     * Jugadas que permiten las reglas de la pieza que está en origen, sin controlar todavía si
     * dejan al propio rey en jaque. Vacío si no hay pieza.
     */
    public List<Jugada> jugadasPosibles(Tablero tablero, Posicion origen) {
        List<Jugada> jugadas = new ArrayList<>();
        tablero.piezaEn(origen).ifPresent(pieza -> {
            for (Posicion destino : pieza.destinosPosibles(tablero, origen)) {
                jugadas.add(Jugada.normal(tablero, origen, destino));
            }
        });
        return jugadas;
    }

    /** La jugada que llevaría la pieza de origen a destino según sus reglas; vacío si no puede ir ahí. */
    public Optional<Jugada> jugadaPosible(Tablero tablero, Posicion origen, Posicion destino) {
        return jugadasPosibles(tablero, origen).stream()
                .filter(jugada -> jugada.destino().equals(destino))
                .findFirst();
    }

    /** Destinos legales de la pieza que está en origen (vacío si no hay pieza). */
    public Set<Posicion> destinos(Tablero tablero, Posicion origen) {
        Set<Posicion> legales = new HashSet<>();
        for (Jugada jugada : jugadasPosibles(tablero, origen)) {
            if (!dejaAlReyEnJaque(tablero, jugada)) {
                legales.add(jugada.destino());
            }
        }
        return legales;
    }

    /** true si el color tiene al menos una jugada legal. Corta apenas encuentra una. */
    public boolean existeAlguno(Tablero tablero, Bando color) {
        for (Map.Entry<Posicion, Pieza> entrada : tablero.piezas().entrySet()) {
            if (entrada.getValue().color() != color) {
                continue;
            }
            for (Jugada jugada : jugadasPosibles(tablero, entrada.getKey())) {
                if (!dejaAlReyEnJaque(tablero, jugada)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Simula la jugada en una copia del tablero y consulta si el rey del que movió queda atacado. */
    public boolean dejaAlReyEnJaque(Tablero tablero, Jugada jugada) {
        Tablero simulacion = tablero.copiar();
        jugada.aplicar(simulacion);
        return detector.estaEnJaque(simulacion, jugada.piezaMovida().color());
    }

    /** Variante con la pieza y las casillas sueltas: arma la jugada común y la simula. */
    public boolean dejaAlReyEnJaque(Tablero tablero, Pieza pieza, Posicion origen, Posicion destino) {
        List<Efecto> efectos = new ArrayList<>();
        tablero.piezaEn(destino).ifPresent(capturada -> efectos.add(new Retiro(capturada, destino)));
        efectos.add(new Traslado(pieza, origen, destino));
        return dejaAlReyEnJaque(tablero, new Jugada(origen, destino, efectos));
    }
}
