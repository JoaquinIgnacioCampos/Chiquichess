package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tablas cuando la misma posición (mismas piezas en las mismas casillas y mismo turno)
 * aparece N veces, por defecto 3. No considera derechos de enroque ni de captura al paso.
 *
 * No guarda memoria: reconstruye las posiciones anteriores revirtiendo, sobre una copia del
 * tablero, las jugadas del historial (cada Jugada sabe deshacerse). Así el resultado depende solo
 * del contexto y sigue siendo correcto aunque se deshagan jugadas.
 */
public class TablasPorRepeticion implements CondicionDeFinalizacion {

    /** Identidad de una posición. Al ser record, equals/hashCode comparan el contenido. */
    private record Instantanea(Map<Posicion, String> casillas, Bando turno) {
    }

    private final int repeticionesParaTablas;

    public TablasPorRepeticion() {
        this(3);
    }

    public TablasPorRepeticion(int repeticionesParaTablas) {
        if (repeticionesParaTablas < 2) {
            throw new IllegalArgumentException("Se necesitan al menos 2 repeticiones");
        }
        this.repeticionesParaTablas = repeticionesParaTablas;
    }

    @Override
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        return vecesQueAparece(contexto) >= repeticionesParaTablas
                ? Optional.of(EstadoPartida.TABLAS_REPETICION)
                : Optional.empty();
    }

    /**
     * Cuenta la posición actual y cada posición anterior igual. Solo hace falta retroceder hasta la
     * última captura o movimiento de peón: las piezas capturadas no vuelven y los peones no
     * retroceden, así que ninguna posición anterior a esa jugada puede repetirse después.
     */
    private int vecesQueAparece(ContextoDeJugada contexto) {
        Tablero anterior = copia(contexto.tablero());
        Bando turno = contexto.turno();
        Instantanea actual = instantanea(anterior, turno);
        int veces = 1;
        List<Jugada> jugadas = contexto.jugadas();
        for (int i = jugadas.size() - 1; i >= 0; i--) {
            Jugada jugada = jugadas.get(i);
            if (esIrreversible(jugada)) {
                break;
            }
            jugada.revertir(anterior);
            turno = turno.opuesto();
            if (instantanea(anterior, turno).equals(actual)) {
                veces++;
            }
        }
        return veces;
    }

    private static boolean esIrreversible(Jugada jugada) {
        return jugada.capturada().isPresent() || jugada.piezaMovida().esPeon();
    }

    private static Tablero copia(ITablero tablero) {
        Tablero copia = new Tablero(tablero.ancho(), tablero.alto());
        tablero.piezas().forEach(copia::colocar);
        return copia;
    }

    private static Instantanea instantanea(ITablero tablero, Bando turno) {
        Map<Posicion, String> casillas = new HashMap<>();
        for (Map.Entry<Posicion, Pieza> entrada : tablero.piezas().entrySet()) {
            Pieza pieza = entrada.getValue();
            casillas.put(entrada.getKey(), pieza.color() + ":" + pieza.nombre());
        }
        return new Instantanea(Map.copyOf(casillas), turno);
    }
}
