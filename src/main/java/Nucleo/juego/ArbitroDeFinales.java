package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.reglas.CondicionConMemoria;
import Nucleo.reglas.CondicionDeFinalizacion;
import Nucleo.reglas.ContextoDeJugada;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.Marcador;
import Nucleo.reglas.Recuerdo;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Decide, después de cada jugada, si la partida sigue (con o sin jaque) o terminó, y quién ganó.
 * Consulta las condiciones de fin de las reglas en orden; la primera que se cumple decide.
 *
 * También coordina la memoria de las condiciones que la tienen (Memento): en cada jugada todas
 * registran la posición y devuelven un {@link Recuerdo}; al deshacer, cada una recupera el suyo.
 * Partida guarda esos recuerdos en su historial (es el cuidador) sin saber qué contienen.
 *
 * No guarda el estado de la partida: recibe la posición y devuelve un veredicto.
 */
final class ArbitroDeFinales {

    /** Estado que decidieron las condiciones de fin y, si alguna terminó la partida con victoria, quién ganó. */
    record Veredicto(EstadoPartida estado, Optional<Bando> ganador) {
        Veredicto {
            Objects.requireNonNull(estado, "estado");
            Objects.requireNonNull(ganador, "ganador");
        }
    }

    /** Lo que deja registrar una jugada: los recuerdos para poder deshacerla y el veredicto. */
    record Registro(List<Recuerdo> recuerdos, Veredicto veredicto) {
        Registro {
            recuerdos = List.copyOf(recuerdos);
            Objects.requireNonNull(veredicto, "veredicto");
        }
    }

    private final ReglasDePartida reglas;
    /** Condiciones de las reglas que guardan estado, en el orden en que registran. */
    private final List<CondicionConMemoria> condicionesConMemoria;

    ArbitroDeFinales(ReglasDePartida reglas) {
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.condicionesConMemoria = reglas.condiciones().stream()
                .filter(CondicionConMemoria.class::isInstance)
                .map(CondicionConMemoria.class::cast)
                .toList();
    }

    /**
     * Hace que todas las condiciones con memoria registren la posición y recién después decide el
     * estado. Registrar primero garantiza que todas anoten cada jugada, aunque una condición anterior
     * termine la partida. Los recuerdos vienen en el orden en que se registraron.
     */
    Registro registrarYEvaluar(Tablero tablero, Bando turno, List<Jugada> jugadas) {
        ContextoDeJugada contexto = contexto(tablero, turno, jugadas);
        List<Recuerdo> recuerdos = new ArrayList<>();
        for (CondicionConMemoria condicion : condicionesConMemoria) {
            recuerdos.add(condicion.registrar(contexto));
        }
        return new Registro(recuerdos, evaluar(contexto));
    }

    /** Veredicto de la posición sin registrarla (después de deshacer, ya estaba registrada). */
    Veredicto evaluar(Tablero tablero, Bando turno, List<Jugada> jugadas) {
        return evaluar(contexto(tablero, turno, jugadas));
    }

    /** Cada condición con memoria vuelve a como estaba antes de registrar esa jugada, en orden inverso. */
    void restaurar(List<Recuerdo> recuerdos) {
        if (recuerdos.size() != condicionesConMemoria.size()) {
            throw new IllegalArgumentException("Los recuerdos no corresponden a las condiciones de estas reglas");
        }
        for (int i = condicionesConMemoria.size() - 1; i >= 0; i--) {
            condicionesConMemoria.get(i).restaurar(recuerdos.get(i));
        }
    }

    /** El menor límite de jugadas que impone alguna condición; vacío si ninguna tiene límite. */
    Optional<Integer> limiteDeJugadas() {
        return reglas.condiciones().stream()
                .map(CondicionDeFinalizacion::limiteDeJugadas)
                .flatMap(Optional::stream)
                .min(Integer::compare);
    }

    /** Marcador de goles tras esas jugadas, si alguna condición se juega con goles; vacío si no. */
    Optional<Marcador> marcador(List<Jugada> jugadas) {
        return reglas.condiciones().stream()
                .map(condicion -> condicion.marcador(jugadas))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /** Foto de la posición que reciben las condiciones de fin. */
    private ContextoDeJugada contexto(Tablero tablero, Bando turno, List<Jugada> jugadas) {
        boolean enJaque = reglas.detector().estaEnJaque(tablero, turno);
        boolean hayJugadasLegales = reglas.movimientosLegales().existeAlguno(tablero, turno, jugadas);
        return new ContextoDeJugada(tablero, turno, jugadas, enJaque, hayJugadasLegales);
    }

    /**
     * Consulta las condiciones de fin en orden; si ninguna se cumple, la partida sigue (con o sin jaque).
     * La condición que termina la partida es la que dice quién ganó.
     */
    private Veredicto evaluar(ContextoDeJugada contexto) {
        for (CondicionDeFinalizacion condicion : reglas.condiciones()) {
            Optional<EstadoPartida> estado = condicion.evaluar(contexto);
            if (estado.isPresent()) {
                return new Veredicto(estado.get(), condicion.ganador(estado.get(), contexto));
            }
        }
        return new Veredicto(contexto.enJaque() ? EstadoPartida.JAQUE : EstadoPartida.EN_CURSO, Optional.empty());
    }
}
