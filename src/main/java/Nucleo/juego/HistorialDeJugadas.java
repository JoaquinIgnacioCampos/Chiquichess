package Nucleo.juego;

import Nucleo.jugadas.Jugada;
import Nucleo.reglas.Recuerdo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Jugadas ejecutadas y jugadas deshechas de una partida.
 *
 * Es el lugar donde Partida guarda los Command (cada {@link Jugada}) y, junto a cada uno, los
 * recuerdos de las condiciones con memoria (Memento). Con dos pilas: deshacer pasa la última jugada
 * de "hechas" a "deshechas" y rehacer la devuelve. Una jugada nueva borra las deshechas, porque la
 * partida tomó otro camino.
 *
 * No aplica ni revierte nada: eso lo coordina Partida, que es quien conoce el tablero y las reglas.
 */
final class HistorialDeJugadas {

    /** Una jugada ejecutada y los recuerdos que dejaron las condiciones con memoria al registrarla. */
    record Entrada(Jugada jugada, List<Recuerdo> recuerdos) {
        Entrada {
            Objects.requireNonNull(jugada, "jugada");
            recuerdos = List.copyOf(recuerdos);
        }
    }

    private final List<Entrada> hechas = new ArrayList<>();
    private final List<Jugada> deshechas = new ArrayList<>();

    /** Jugadas ejecutadas, en orden (copia inmutable). */
    List<Jugada> jugadas() {
        return hechas.stream().map(Entrada::jugada).toList();
    }

    /** Jugadas ejecutadas más una nueva al final, sin modificar el historial (para armar el contexto). */
    List<Jugada> jugadasCon(Jugada nueva) {
        List<Jugada> jugadas = new ArrayList<>(jugadas());
        jugadas.add(nueva);
        return jugadas;
    }

    /** Registra una jugada nueva. Borra lo que se podía rehacer. */
    void agregar(Jugada jugada, List<Recuerdo> recuerdos) {
        hechas.add(new Entrada(jugada, recuerdos));
        deshechas.clear();
    }

    boolean hayParaDeshacer() {
        return !hechas.isEmpty();
    }

    boolean hayParaRehacer() {
        return !deshechas.isEmpty();
    }

    /** Saca la última jugada ejecutada y la guarda para poder rehacerla. */
    Entrada deshacer() {
        if (hechas.isEmpty()) {
            throw new IllegalStateException("No hay jugadas para deshacer");
        }
        Entrada ultima = hechas.remove(hechas.size() - 1);
        deshechas.add(ultima.jugada());
        return ultima;
    }

    /** Próxima jugada para rehacer; se confirma con {@link #confirmarRehecha}. */
    Jugada siguienteParaRehacer() {
        if (deshechas.isEmpty()) {
            throw new IllegalStateException("No hay jugadas para rehacer");
        }
        return deshechas.get(deshechas.size() - 1);
    }

    /** Vuelve a registrar la jugada rehecha, con los recuerdos nuevos, sin borrar el resto de las deshechas. */
    void confirmarRehecha(List<Recuerdo> recuerdos) {
        Jugada jugada = deshechas.remove(deshechas.size() - 1);
        hechas.add(new Entrada(jugada, recuerdos));
    }
}
