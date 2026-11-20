package adapters;

import core.ia.Difficulty;
import core.game.GameInProgress;
import adapters.configuration.GameConfiguration;
import adapters.configuration.GameConfiguration.Mode;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Entrada de consola: utiliza el mismo ensamblador que el arranque gráfico. */
public class Main {

    /**
     * Sin argumentos se juega al ajedrez clásico entre dos personas. Argumentos (en cualquier orden):
     * <ul>
     *   <li>"futbol": modo "Posesión y gol"; un número elige el límite de jugadas (por defecto, 90).</li>
     *   <li>"ia": contra la IA, que juega con negras; después puede ir "facil", "medio" o "dificil"
     *       (por defecto, medio).</li>
     * </ul>
     * Ejemplo: {@code futbol 60 ia dificil}.
     */
    public static void main(String[] args) throws IOException {
        List<String> arguments = Arrays.stream(args).map(arg -> arg.toLowerCase(Locale.ROOT)).toList();
        Mode mode = arguments.contains("futbol") ? Mode.FUTBOL : Mode.CLASICO;
        int limit = arguments.stream().filter(arg -> arg.matches("[0-9]+")).map(Integer::parseInt).findFirst()
                .orElse(GameConfiguration.DEFAULT_LIMIT);
        if (limit < GameConfiguration.LIMIT_MINIMUM || limit > GameConfiguration.LIMIT_MAXIMUM) {
            System.out.println("El limite de jugadas debe estar entre " + GameConfiguration.LIMIT_MINIMUM
                    + " y " + GameConfiguration.LIMIT_MAXIMUM + ".");
            return;
        }
        GameAssembler assembler = new GameAssembler();
        // La consola juega sin tiempo, así que permite deshacer y rehacer.
        if (arguments.contains("ia")) {
            Difficulty difficulty = arguments.contains("facil") ? Difficulty.FACIL
                    : arguments.contains("dificil") ? Difficulty.DIFICIL : Difficulty.MEDIO;
            var againstAi = assembler.createWithoutClockAgainstAi(mode, limit, difficulty);
            new ConsoleGame(againstAi.game(), new ConsoleRenderer(), new ConsoleMessages(), System.in,
                    System.out, Optional.of(againstAi.rival())).play();
            return;
        }
        GameInProgress game = assembler.createWithoutClockWithUndo(mode, limit);
        new ConsoleGame(game, new ConsoleRenderer(), new ConsoleMessages(), System.in, System.out).play();
    }
}
