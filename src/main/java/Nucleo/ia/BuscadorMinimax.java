package Nucleo.ia;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.CondicionConMemoria;
import Nucleo.reglas.CondicionDeFinalizacion;
import Nucleo.reglas.ContextoDeJugada;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.IDetectorDeJaque;
import Nucleo.reglas.MovimientosLegales;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import Nucleo.tiempo.FuenteDeTiempo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Minimax (en su forma negamax: el puntaje de un bando es el opuesto del otro) con:
 * <ul>
 *   <li><b>Poda alfa-beta:</b> deja de mirar una rama apenas sabe que el rival no la va a permitir.
 *       Elige la misma jugada que Minimax puro, pero mirando muchas menos posiciones.</li>
 *   <li><b>Profundización iterativa:</b> busca a 1 jugada, después a 2, a 3... y se queda con la
 *       última búsqueda que terminó antes de que se acabe el tiempo. Siempre tiene una jugada lista.</li>
 *   <li><b>Quietud</b> (opcional): al llegar al límite, sigue mirando solo capturas y goles, para no
 *       cortar el análisis en medio de un cambio de piezas.</li>
 *   <li><b>Mates y finales:</b> los deciden las mismas condiciones de fin de la partida (las que no
 *       guardan memoria). Un mate vale más cuanto antes llega, así gana lo más rápido posible y, si
 *       pierde, lo demora lo más posible.</li>
 * </ul>
 *
 * Trabaja sobre la copia del tablero de la {@link SituacionDePartida}: aplica y deshace jugadas con
 * sus efectos, sin tocar la partida. Una instancia sirve para una sola búsqueda.
 */
final class BuscadorMinimax {
    static final int MATE = 1_000_000;
    /** Los puntajes por encima de esto son mates (o victorias) a cierta cantidad de jugadas. */
    static final int UMBRAL_DE_MATE = MATE - 10_000;
    private static final int INFINITO = Integer.MAX_VALUE / 2;
    private static final int PROFUNDIDAD_MAXIMA_DE_QUIETUD = 6;

    private final Tablero tablero;
    private final List<Jugada> camino;
    private final MovimientosLegales movimientosLegales;
    private final IDetectorDeJaque detector;
    /** Las condiciones de fin sin memoria: la repetición y los 50 movimientos no se miran al buscar. */
    private final List<CondicionDeFinalizacion> condiciones;
    private final EvaluadorDePosicion evaluador;
    private final FuenteDeTiempo reloj;
    private final boolean conQuietud;
    private long limite;
    private long posiciones;
    private int profundidadAlcanzada;

    /** Se cortó la búsqueda porque se acabó el tiempo. */
    private static final class TiempoAgotado extends RuntimeException {
        TiempoAgotado() {
            super(null, null, false, false);
        }
    }

    /** Una jugada posible desde la posición inicial, con su puntaje para quien juega. */
    record JugadaEvaluada(Jugada jugada, int puntaje) {
    }

    BuscadorMinimax(SituacionDePartida situacion, FuenteDeTiempo reloj, boolean conQuietud) {
        this.tablero = situacion.tablero();
        this.camino = new ArrayList<>(situacion.jugadas());
        this.movimientosLegales = situacion.reglas().movimientosLegales();
        this.detector = situacion.reglas().detector();
        this.condiciones = situacion.reglas().condiciones().stream()
                .filter(condicion -> !(condicion instanceof CondicionConMemoria))
                .toList();
        this.evaluador = new EvaluadorDePosicion(tablero);
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.conQuietud = conQuietud;
    }

    /**
     * Puntúa las jugadas de {@code turno}. Las que pueden quedar a menos de {@code margen} de la mejor
     * tienen su puntaje exacto; las demás, una cota (son peores que la mejor por más que el margen).
     * Vacío si no hay jugadas legales.
     *
     * @param azar mezcla las jugadas antes de ordenarlas, para que entre iguales no elija siempre la misma
     */
    List<JugadaEvaluada> buscar(Bando turno, int profundidadMaxima, Duration tiempo, int margen, Random azar) {
        limite = reloj.ahoraEnNanos() + tiempo.toNanos();
        List<Jugada> jugadas = legales(turno);
        Collections.shuffle(jugadas, azar);
        ordenar(jugadas);
        List<JugadaEvaluada> resultado = List.of();
        for (int profundidad = 1; profundidad <= profundidadMaxima && !jugadas.isEmpty(); profundidad++) {
            try {
                resultado = raiz(jugadas, turno, profundidad, margen);
                profundidadAlcanzada = profundidad;
            } catch (TiempoAgotado agotado) {
                if (resultado.isEmpty()) {
                    // Ni siquiera terminó de mirar a una jugada: termina esa sin límite de tiempo.
                    limite = Long.MAX_VALUE;
                    resultado = raiz(jugadas, turno, 1, margen);
                    profundidadAlcanzada = 1;
                }
                break;
            }
            // La próxima vuelta mira primero las que salieron mejor: así poda más.
            jugadas = new ArrayList<>(resultado.stream().map(JugadaEvaluada::jugada).toList());
            if (resultado.getFirst().puntaje() >= UMBRAL_DE_MATE) break;
        }
        return resultado;
    }

    /** Cuántas posiciones miró en la última búsqueda (para medir). */
    long posiciones() {
        return posiciones;
    }

    /** Hasta qué profundidad terminó de mirar en la última búsqueda (para medir). */
    int profundidadAlcanzada() {
        return profundidadAlcanzada;
    }

    private List<JugadaEvaluada> raiz(List<Jugada> jugadas, Bando turno, int profundidad, int margen) {
        List<JugadaEvaluada> evaluadas = new ArrayList<>();
        int mejor = -INFINITO;
        for (Jugada jugada : jugadas) {
            // Ventana con margen: las que podrían quedar cerca de la mejor se miran con puntaje exacto.
            int alfa = mejor == -INFINITO ? -INFINITO : mejor - margen - 1;
            jugar(jugada);
            int puntaje;
            try {
                puntaje = -negamax(turno.opuesto(), profundidad - 1, -INFINITO, -alfa, 1);
            } finally {
                deshacer(jugada);
            }
            evaluadas.add(new JugadaEvaluada(jugada, puntaje));
            mejor = Math.max(mejor, puntaje);
        }
        evaluadas.sort(Comparator.comparingInt(JugadaEvaluada::puntaje).reversed());
        return List.copyOf(evaluadas);
    }

    private int negamax(Bando turno, int profundidad, int alfa, int beta, int distancia) {
        contarPosicion();
        if (profundidad <= 0) {
            return conQuietud ? quietud(turno, alfa, beta, distancia, 0) : hoja(turno, distancia);
        }
        // Primero los finales que no dependen de tener jugadas (por ejemplo, el tercer gol).
        Optional<Integer> fin = finalDePartida(turno, true, distancia);
        if (fin.isPresent()) return fin.get();
        List<Jugada> jugadas = posibles(turno);
        ordenar(jugadas);
        int mejor = -INFINITO;
        boolean algunaLegal = false;
        for (Jugada jugada : jugadas) {
            // La legalidad se mira recién al probar la jugada: las que la poda descarta no se verifican.
            if (!jugarSiEsLegal(jugada, turno)) continue;
            algunaLegal = true;
            int puntaje;
            try {
                puntaje = -negamax(turno.opuesto(), profundidad - 1, -beta, -alfa, distancia + 1);
            } finally {
                deshacer(jugada);
            }
            if (puntaje > mejor) mejor = puntaje;
            if (puntaje > alfa) alfa = puntaje;
            if (alfa >= beta) break; // El rival nunca dejaría llegar acá: no hace falta seguir mirando.
        }
        // Sin ninguna jugada legal: mate o ahogado, según las reglas.
        return algunaLegal ? mejor : finalDePartida(turno, false, distancia).orElse(0);
    }

    /** Sigue mirando solo capturas y goles hasta que la posición se calma. */
    private int quietud(Bando turno, int alfa, int beta, int distancia, int nivel) {
        int quieto = hoja(turno, distancia);
        if (Math.abs(quieto) >= UMBRAL_DE_MATE || nivel >= PROFUNDIDAD_MAXIMA_DE_QUIETUD) return quieto;
        if (quieto >= beta) return quieto;
        if (quieto > alfa) alfa = quieto;
        List<Jugada> agitadas = new ArrayList<>(posibles(turno).stream()
                .filter(jugada -> jugada.capturada().isPresent() || jugada.esGol())
                .toList());
        ordenar(agitadas);
        int mejor = quieto;
        for (Jugada jugada : agitadas) {
            contarPosicion();
            if (!jugarSiEsLegal(jugada, turno)) continue;
            int puntaje;
            try {
                puntaje = -quietud(turno.opuesto(), -beta, -alfa, distancia + 1, nivel + 1);
            } finally {
                deshacer(jugada);
            }
            if (puntaje > mejor) mejor = puntaje;
            if (puntaje > alfa) alfa = puntaje;
            if (alfa >= beta) break;
        }
        return mejor;
    }

    /**
     * Posición donde se deja de mirar. Antes de evaluarla se fija si la partida terminó: los finales que
     * no dependen de tener jugadas (como el tercer gol) y, si quien juega está en jaque, el mate. Así un
     * mate se ve aunque caiga justo en el límite de la búsqueda.
     */
    private int hoja(Bando turno, int distancia) {
        Optional<Integer> fin = finalDePartida(turno, true, distancia);
        if (fin.isPresent()) return fin.get();
        if (detector.estaEnJaque(tablero, turno) && !tieneAlgunaJugadaLegal(turno)) {
            return finalDePartida(turno, false, distancia).orElse(0);
        }
        return evaluador.evaluar(tablero, turno, camino);
    }

    private boolean tieneAlgunaJugadaLegal(Bando turno) {
        for (Jugada jugada : posibles(turno)) {
            if (jugarSiEsLegal(jugada, turno)) {
                deshacer(jugada);
                return true;
            }
        }
        return false;
    }

    /**
     * Si la posición terminó la partida según las reglas, su puntaje para quien tiene el turno: ganar es
     * {@link #MATE} menos la distancia (antes es mejor), perder lo opuesto y las tablas 0.
     */
    private Optional<Integer> finalDePartida(Bando turno, boolean hayJugadas, int distancia) {
        boolean enJaque = !hayJugadas && detector.estaEnJaque(tablero, turno);
        ContextoDeJugada contexto = new ContextoDeJugada(tablero, turno, camino, enJaque, hayJugadas);
        for (CondicionDeFinalizacion condicion : condiciones) {
            Optional<EstadoPartida> estado = condicion.evaluar(contexto);
            if (estado.isPresent()) {
                Optional<Bando> ganador = condicion.ganador(estado.get(), contexto);
                if (ganador.isEmpty()) return Optional.of(0);
                return Optional.of(ganador.get() == turno ? MATE - distancia : -(MATE - distancia));
            }
        }
        return Optional.empty();
    }

    /** Jugadas legales: las posibles que no dejan al propio rey en jaque. */
    private List<Jugada> legales(Bando turno) {
        List<Jugada> legales = new ArrayList<>();
        for (Jugada jugada : posibles(turno)) {
            if (jugarSiEsLegal(jugada, turno)) {
                deshacer(jugada);
                legales.add(jugada);
            }
        }
        return legales;
    }

    /** Jugadas de las piezas del bando según sus reglas, sin mirar todavía si dejan al rey en jaque. */
    private List<Jugada> posibles(Bando turno) {
        List<Jugada> posibles = new ArrayList<>();
        List<Posicion> propias = new ArrayList<>();
        for (Map.Entry<Posicion, Pieza> entrada : tablero.piezas().entrySet()) {
            if (entrada.getValue().color() == turno) propias.add(entrada.getKey());
        }
        for (Posicion origen : propias) {
            posibles.addAll(movimientosLegales.jugadasPosibles(tablero, origen, camino));
        }
        return posibles;
    }

    /**
     * Juega la jugada si no deja al propio rey en jaque y devuelve true; si lo deja, la deshace y
     * devuelve false. Aplicar y deshacer sobre el mismo tablero evita copiarlo por cada jugada.
     */
    private boolean jugarSiEsLegal(Jugada jugada, Bando turno) {
        jugar(jugada);
        if (detector.estaEnJaque(tablero, turno)) {
            deshacer(jugada);
            return false;
        }
        return true;
    }

    /** Primero las que suelen ser mejores (goles, promociones, capturas de piezas valiosas): así poda más. */
    private static void ordenar(List<Jugada> jugadas) {
        if (jugadas.size() < 2) return;
        // El interés se calcula una vez por jugada, no en cada comparación del ordenamiento.
        Map<Jugada, Integer> interes = new IdentityHashMap<>();
        for (Jugada jugada : jugadas) interes.put(jugada, interes(jugada));
        jugadas.sort((una, otra) -> Integer.compare(interes.get(otra), interes.get(una)));
    }

    private static int interes(Jugada jugada) {
        int interes = 0;
        if (jugada.esGol()) interes += 20_000;
        if (jugada.promocion().isPresent()) interes += 8_000 + EvaluadorDePosicion.valor(jugada.promocion().get());
        if (jugada.capturada().isPresent()) {
            interes += 1_000 + 10 * EvaluadorDePosicion.valor(jugada.capturada().get())
                    - EvaluadorDePosicion.valor(jugada.piezaMovida());
        }
        return interes;
    }

    private void jugar(Jugada jugada) {
        jugada.aplicar(tablero);
        camino.add(jugada);
    }

    private void deshacer(Jugada jugada) {
        camino.removeLast();
        jugada.revertir(tablero);
    }

    private void contarPosicion() {
        posiciones++;
        if ((posiciones & 1023) == 0 && reloj.ahoraEnNanos() > limite) throw new TiempoAgotado();
    }
}
