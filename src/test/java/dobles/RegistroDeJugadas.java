package dobles;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.reglas.CondicionConMemoria;
import Nucleo.reglas.ContextoDeJugada;
import Nucleo.reglas.Recuerdo;
import Nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Hace con una condición con memoria lo mismo que Partida: registra la posición inicial y después
 * cada jugada. Las jugadas se aplican de verdad sobre el tablero (empiezan las blancas).
 */
public final class RegistroDeJugadas {

    /** Recuerdos en orden (el primero es el de la posición inicial) y el contexto de la última posición. */
    public record Resultado(List<Recuerdo> recuerdos, ContextoDeJugada ultimoContexto) {
    }

    private RegistroDeJugadas() {
    }

    public static Resultado registrar(CondicionConMemoria condicion, Tablero tablero, String... jugadas) {
        List<Recuerdo> recuerdos = new ArrayList<>();
        ContextoDeJugada contexto = Contextos.inicial(tablero, Bando.BLANCO);
        recuerdos.add(condicion.registrar(contexto));
        List<Jugada> hechas = new ArrayList<>();
        for (String jugada : jugadas) {
            hechas.addAll(JugadasDePrueba.jugar(tablero, jugada));
            Bando turno = hechas.size() % 2 == 0 ? Bando.BLANCO : Bando.NEGRO;
            contexto = Contextos.conHistorial(tablero, turno, hechas);
            recuerdos.add(condicion.registrar(contexto));
        }
        return new Resultado(recuerdos, contexto);
    }

    /** Registra jugadas armadas a mano, sin aplicarlas (para condiciones que solo miran el historial). */
    public static Resultado registrarSinAplicar(CondicionConMemoria condicion, Tablero tablero, List<Jugada> jugadas) {
        List<Recuerdo> recuerdos = new ArrayList<>();
        ContextoDeJugada contexto = Contextos.inicial(tablero, Bando.BLANCO);
        recuerdos.add(condicion.registrar(contexto));
        for (int i = 1; i <= jugadas.size(); i++) {
            Bando turno = i % 2 == 0 ? Bando.BLANCO : Bando.NEGRO;
            contexto = Contextos.conHistorial(tablero, turno, jugadas.subList(0, i));
            recuerdos.add(condicion.registrar(contexto));
        }
        return new Resultado(recuerdos, contexto);
    }
}
