package Nucleo.ia;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tablero.Tablero;

import java.util.List;
import java.util.Objects;

/**
 * Lo que una IA necesita para pensar: una copia propia del tablero, quién juega, las jugadas hechas
 * (las reglas especiales y los goles se calculan con ellas) y las reglas de la partida.
 *
 * El tablero es una copia: la IA lo usa para imaginar jugadas sin tocar la partida real, así no se
 * disparan eventos (ni suena nada, ni se mueve la pantalla) y puede pensar en otro hilo.
 */
public record SituacionDePartida(Tablero tablero, Bando turno, List<Jugada> jugadas, ReglasDePartida reglas) {

    public SituacionDePartida {
        Objects.requireNonNull(tablero, "tablero");
        Objects.requireNonNull(turno, "turno");
        jugadas = List.copyOf(Objects.requireNonNull(jugadas, "jugadas"));
        Objects.requireNonNull(reglas, "reglas");
    }
}
