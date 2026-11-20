package adapters;

import core.side.Side;
import core.game.*;
import core.rules.GameState;
import core.rules.Scoreboard;
import core.board.Position;

import java.io.*;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Adaptador de entrada/salida por consola: lee jugadas como "e2 e4" y las envía a la Partida. */
public class ConsoleGame {

    private final GameInProgress game;
    private final ConsoleRenderer renderer;
    private final ConsoleMessages messages;
    private final BufferedReader input;
    private final PrintStream output;
    /** La IA, si se juega contra ella: cuando le toca, juega sola y se muestra qué hizo. */
    private final Optional<ArtificialRival> rival;
    /** Cómo espera mientras la IA "piensa" como una persona; en los tests, no espera. */
    private final Consumer<Duration> wait;

    public ConsoleGame(GameInProgress game, ConsoleRenderer renderer, InputStream input, PrintStream output) {
        this(game, renderer, new ConsoleMessages(), input, output);
    }

    public ConsoleGame(GameInProgress game, ConsoleRenderer renderer, ConsoleMessages messages,
                        InputStream input, PrintStream output) {
        this(game, renderer, messages, input, output, Optional.empty());
    }

    public ConsoleGame(GameInProgress game, ConsoleRenderer renderer, ConsoleMessages messages,
                        InputStream input, PrintStream output, Optional<ArtificialRival> rival) {
        this(game, renderer, messages, input, output, rival, ConsoleGame::sleep);
    }

    public ConsoleGame(GameInProgress game, ConsoleRenderer renderer, ConsoleMessages messages,
                        InputStream input, PrintStream output, Optional<ArtificialRival> rival,
                        Consumer<Duration> wait) {
        this.rival = Objects.requireNonNull(rival, "rival");
        this.wait = Objects.requireNonNull(wait, "wait");
        this.game = game;
        this.renderer = renderer;
        this.messages = messages;
        this.input = new BufferedReader(new InputStreamReader(input));
        this.output = output;
    }

    public void play() throws IOException {
        output.println(game.allowsUndo()
                ? "Type the move as 'origin destination' (e.g. e2 e4), 'undo', 'redo' or 'exit'."
                : "Type the move as 'origin destination' (e.g. e2 e4) or 'exit'.");
        output.println("To promote a pawn, add the piece: Q (queen), R (rook), B (bishop) or N (knight). E.g. e7 e8 N");
        if (withBall()) {
            output.println("Football mode: carry the ball (o; a * marks whoever has it) to the rival goal (#).");
            output.println("To pass it, move whoever has it toward a teammate it could capture (e.g. d4 d7).");
            output.println("Whoever reaches 3 goals or has more goals at move "
                    + game.moveLimit().map(String::valueOf).orElse("final") + ". Checkmate always wins.");
        }
        while (true) {
            output.println();
            output.println(renderer.draw(game.board()));
            if (withBall()) {
                output.println(scoreboard());
            }
            if (game.state().isFinished()) {
                output.println(finalMessage());
                // Un mate o unas tablas automáticas se pueden deshacer: se ofrece volver atrás.
                if (!game.canUndo()) {
                    break;
                }
                output.print("Type 'undo' to go back or 'exit': ");
            } else {
                if (game.state() == GameState.CHECK) {
                    output.println("Check!");
                }
                if (rival.filter(ArtificialRival::himPlays).isPresent()) {
                    playAi(rival.get());
                    continue;
                }
                output.print("Turn of " + game.currentPlayer().displayName()
                        + " (" + game.turn() + "): ");
            }
            output.flush();

            String line = input.readLine();
            if (line == null || line.trim().equalsIgnoreCase("exit")) {
                break;
            }
            process(line.trim());
        }
    }

    private void process(String line) {
        if (line.equalsIgnoreCase("undo") || line.equalsIgnoreCase("redo")) {
            try {
                // Contra la IA se deshace de a dos jugadas, para que vuelva a tocarle a la persona.
                if (line.equalsIgnoreCase("undo")) {
                    if (rival.isPresent()) rival.get().undo(); else game.undo();
                } else {
                    if (rival.isPresent()) rival.get().redo(); else game.redo();
                }
            } catch (InvalidActionException e) {
                output.println(messages.actionRejection(e.reason()));
            }
            return;
        }
        String[] parts = line.split("\\s+");
        // Un tercer dato, de una letra, elige la pieza al promover: "e7 e8 C". Sin él, se promueve a dama.
        boolean withPromotion = parts.length == 3 && parts[2].length() == 1;
        if (parts.length != 2 && !withPromotion) {
            output.println("Invalid format. Example: e2 e4 (to promote: e7 e8 N)");
            return;
        }
        try {
            Position origin = AlgebraicNotation.parse(parts[0]);
            Position destination = AlgebraicNotation.parse(parts[1]);
            MoveRequest movement = withPromotion
                    ? MoveRequest.withPromotion(origin, destination, Character.toUpperCase(parts[2].charAt(0)))
                    : new MoveRequest(origin, destination);
            Optional<Scoreboard> before = game.scoreboard();
            boolean pass = game.board().pieceIn(destination)
                    .filter(piece -> piece.color() == game.turn()).isPresent();
            MoveResult result = game.move(movement);
            if (pass) {
                output.println("Pass!");
            }
            result.capturedPiece().ifPresent(p -> output.println("Capture: " + p.displayName()));
            if (!game.scoreboard().equals(before)) {
                output.println("GOAL!");
            }
        } catch (IllegalArgumentException e) {
            output.println(e.getMessage());
        } catch (InvalidMoveException e) {
            output.println(messages.moveRejection(e.reason()));
        }
    }

    /** La IA piensa (la consola espera) y juega; se muestra la jugada como la escribiría una persona. */
    private void playAi(ArtificialRival ia) {
        output.println(game.currentPlayer().displayName() + " is thinking...");
        Optional<Scoreboard> before = game.scoreboard();
        long start = System.nanoTime();
        ArtificialRival.Response response = ia.think();
        // Mueve recién cuando pasó lo que tardaría una persona de su nivel.
        Duration missing = response.delay().minusNanos(System.nanoTime() - start);
        if (!missing.isNegative() && !missing.isZero()) wait.accept(missing);
        MoveRequest move = response.movement();
        game.move(move);
        output.println(game.player(ia.side()).displayName() + " plays " + AlgebraicNotation.format(move.origin())
                + " " + AlgebraicNotation.format(move.destination())
                + move.promotion().map(code -> " " + code).orElse(""));
        if (!game.scoreboard().equals(before)) {
            output.println("GOAL!");
        }
    }

    private static void sleep(Duration time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean withBall() {
        return game.scoreboard().isPresent();
    }

    private String scoreboard() {
        Scoreboard scoreboard = game.scoreboard().orElseThrow();
        return "Score: " + game.player(Side.WHITE).displayName() + " " + scoreboard.white()
                + " - " + scoreboard.black() + " " + game.player(Side.BLACK).displayName()
                + "   (move " + game.history().size()
                + game.moveLimit().map(limit -> " of " + limit).orElse("") + ")";
    }

    private String finalMessage() {
        return game.result()
                .map(result -> messages.gameEnd(result.reason(),
                        game.winner().map(Player::displayName)))
                .orElse("");
    }
}
