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
 * de la pieza (o una regla especial, como el enroque) Y no dejan al propio rey en jaque. Se separó
 * de Partida porque también lo usan jaque mate y ahogado ("¿existe alguna jugada legal?").
 *
 * Trabaja con {@link Jugada}: el control de jaque simula la jugada completa (todos sus efectos),
 * así sirve igual para jugadas que mueven más de una pieza o capturan en otra casilla.
 *
 * Las reglas especiales necesitan el historial de la partida (por ejemplo, para saber si el rey ya se
 * movió). Los métodos sin historial equivalen a una partida sin jugadas.
 */
public class MovimientosLegales {

    private final IDetectorDeJaque detector;
    private final List<ReglaEspecial> especiales;

    /** Sin reglas especiales: solo el movimiento propio de cada pieza. */
    public MovimientosLegales(IDetectorDeJaque detector) {
        this(detector, List.of());
    }

    public MovimientosLegales(IDetectorDeJaque detector, List<ReglaEspecial> especiales) {
        this.detector = Objects.requireNonNull(detector, "detector");
        this.especiales = List.copyOf(Objects.requireNonNull(especiales, "especiales"));
    }

    /** Detector con el que se decide si una jugada deja al rey en jaque. */
    public IDetectorDeJaque detector() {
        return detector;
    }

    /**
     * Jugadas de la pieza que está en origen, sin controlar todavía si dejan al propio rey en jaque:
     * las de su movimiento propio más las de las reglas especiales, con las transformaciones de esas
     * reglas aplicadas (por ejemplo, la promoción). Vacío si no hay pieza.
     */
    public List<Jugada> jugadasPosibles(Tablero tablero, Posicion origen, List<Jugada> historial) {
        List<Jugada> jugadas = new ArrayList<>();
        tablero.piezaEn(origen).ifPresent(pieza -> {
            for (Posicion destino : pieza.destinosPosibles(tablero, origen)) {
                jugadas.add(Jugada.normal(tablero, origen, destino));
            }
            for (ReglaEspecial especial : especiales) {
                jugadas.addAll(especial.jugadas(tablero, origen, historial, detector));
            }
        });
        List<Jugada> transformadas = jugadas;
        for (ReglaEspecial especial : especiales) {
            transformadas = transformadas.stream()
                    .flatMap(jugada -> especial.transformar(tablero, jugada).stream())
                    .toList();
        }
        return transformadas;
    }

    /** La jugada que llevaría la pieza de origen a destino; vacío si no puede ir ahí. */
    public Optional<Jugada> jugadaPosible(Tablero tablero, Posicion origen, Posicion destino, List<Jugada> historial) {
        return jugadasPosibles(tablero, origen, historial).stream()
                .filter(jugada -> jugada.destino().equals(destino))
                .findFirst();
    }

    /** Destinos legales de la pieza que está en origen (vacío si no hay pieza). */
    public Set<Posicion> destinos(Tablero tablero, Posicion origen, List<Jugada> historial) {
        Set<Posicion> legales = new HashSet<>();
        for (Jugada jugada : jugadasPosibles(tablero, origen, historial)) {
            if (!dejaAlReyEnJaque(tablero, jugada)) {
                legales.add(jugada.destino());
            }
        }
        return legales;
    }

    /** true si el color tiene al menos una jugada legal. Corta apenas encuentra una. */
    public boolean existeAlguno(Tablero tablero, Bando color, List<Jugada> historial) {
        for (Map.Entry<Posicion, Pieza> entrada : tablero.piezas().entrySet()) {
            if (entrada.getValue().color() != color) {
                continue;
            }
            for (Jugada jugada : jugadasPosibles(tablero, entrada.getKey(), historial)) {
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

    // ---- Variantes sin historial: equivalen a una partida sin jugadas. Son final para que un doble de
    // ---- prueba sobreescriba las de arriba, que son las que usa Partida.

    public final List<Jugada> jugadasPosibles(Tablero tablero, Posicion origen) {
        return jugadasPosibles(tablero, origen, List.of());
    }

    public final Optional<Jugada> jugadaPosible(Tablero tablero, Posicion origen, Posicion destino) {
        return jugadaPosible(tablero, origen, destino, List.of());
    }

    public final Set<Posicion> destinos(Tablero tablero, Posicion origen) {
        return destinos(tablero, origen, List.of());
    }

    public final boolean existeAlguno(Tablero tablero, Bando color) {
        return existeAlguno(tablero, color, List.of());
    }

    /** Variante con la pieza y las casillas sueltas: arma la jugada común y la simula. */
    public final boolean dejaAlReyEnJaque(Tablero tablero, Pieza pieza, Posicion origen, Posicion destino) {
        List<Efecto> efectos = new ArrayList<>();
        tablero.piezaEn(destino).ifPresent(capturada -> efectos.add(new Retiro(capturada, destino)));
        efectos.add(new Traslado(pieza, origen, destino));
        return dejaAlReyEnJaque(tablero, new Jugada(origen, destino, efectos));
    }
}
