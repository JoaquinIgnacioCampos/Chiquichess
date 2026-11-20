package core.game;

import core.side.Side;
import core.moves.Move;
import core.rules.GameRules;
import core.board.Board;

import java.util.List;
import java.util.Objects;

/**
 * Lo que una IA necesita para pensar: una copia propia del tablero, quién juega, las jugadas hechas
 * (las reglas especiales y los goles se calculan con ellas) y las reglas de la partida.
 *
 * El tablero es una copia: la IA lo usa para imaginar jugadas sin tocar la partida real, así no se
 * disparan eventos (ni suena nada, ni se mueve la pantalla) y puede pensar en otro hilo.
 */
public record GameSituation(Board board, Side turn, List<Move> moves, GameRules rules) {

    public GameSituation {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(turn, "turn");
        moves = List.copyOf(Objects.requireNonNull(moves, "moves"));
        Objects.requireNonNull(rules, "rules");
    }
}
