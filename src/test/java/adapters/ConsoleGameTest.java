package adapters;

import core.side.Side;
import core.game.Player;
import core.game.Game;
import core.game.GameInProgress;
import core.rules.GameRules;
import core.time.NoClock;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/** La consola completa con una entrada simulada: muestra los rechazos y el final con los textos del catálogo. */
class ConsoleGameTest {

    private String play(GameInProgress game, String input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new ConsoleGame(game, new ConsoleRenderer(), new ConsoleMessages(),
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8)).play();
        return output.toString(StandardCharsets.UTF_8);
    }

    private String play(String input) throws IOException {
        return play(new GameAssembler().createWithoutClock(), input);
    }

    @Test
    void showsReasonOfEachRejectedMove() throws IOException {
        // Act
        String output = play("e4 e5\ne7 e5\na1 a3\nexit\n");

        // Assert
        assertTrue(output.contains("There is no piece on the origin square."));
        assertTrue(output.contains("That piece is not yours: it is the other player's turn."));
        assertTrue(output.contains("That piece cannot move there."));
    }

    @Test
    void announcesCheckMateWithNameOfWinner() throws IOException {
        // Act
        String output = play("f2 f3\ne7 e5\ng2 g4\nd8 h4\n");

        // Assert
        assertTrue(output.strip().endsWith("Checkmate! Player 2 wins."));
    }

    // ---------------- Promoción ----------------

    /** Peón blanco en a7 a punto de promover. */
    private Game withPawnInTo7() {
        return new Game(doubles.BoardFake.from(
                "....k...",
                "P.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....K..."), new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.classic(), new NoClock());
    }

    @Test
    void thirdLetterChoosesPieceOfPromotion() throws IOException {
        // Arrange
        GameInProgress game = withPawnInTo7();

        // Act
        play(game, "a7 a8 n\nexit\n");

        // Assert
        assertEquals('N', game.board().pieceIn(pos("a8")).orElseThrow().code());
    }

    @Test
    void withoutThirdLetterPromotesToQueen() throws IOException {
        // Arrange
        GameInProgress game = withPawnInTo7();

        // Act
        play(game, "a7 a8\nexit\n");

        // Assert
        assertEquals('Q', game.board().pieceIn(pos("a8")).orElseThrow().code());
    }

    @Test
    void aPieceOfPromotionInvalidRejects() throws IOException {
        // Arrange
        GameInProgress game = withPawnInTo7();

        // Act
        String output = play(game, "a7 a8 K\na7 a8 queen\nexit\n");

        // Assert
        assertTrue(output.contains("That piece cannot move there."));
        assertTrue(output.contains("Invalid format. Example: e2 e4 (to promote: e7 e8 N)"));
        assertTrue(game.history().isEmpty());
    }

    // ---------------- Deshacer y rehacer ----------------

    @Test
    void withUndoEnabledHelpMentionsCommands() throws IOException {
        // Act
        String output = play(new GameAssembler().createWithoutClockWithUndo(), "exit\n");

        // Assert
        assertTrue(output.startsWith("Type the move as 'origin destination' (e.g. e2 e4), 'undo', 'redo' or 'exit'."));
    }

    @Test
    void undoAndRedoFromConsole() throws IOException {
        // Arrange
        GameInProgress game = new GameAssembler().createWithoutClockWithUndo();

        // Act: jugar, deshacer, rehacer y deshacer otra vez
        String output = play(game, "e2 e4\nundo\nredo\nundo\nexit\n");

        // Assert
        assertTrue(game.history().isEmpty());
        assertFalse(output.contains("There are no moves"));
    }

    @Test
    void notifiesWhenNotHasNothingForUndoOrRedo() throws IOException {
        // Act
        String output = play(new GameAssembler().createWithoutClockWithUndo(), "undo\nredo\nexit\n");

        // Assert
        assertTrue(output.contains("There are no moves to undo."));
        assertTrue(output.contains("There are no moves to redo."));
    }

    @Test
    void withoutUndoEnabledItRejects() throws IOException {
        // Act
        String output = play("e2 e4\nundo\nexit\n");

        // Assert
        assertTrue(output.contains("This game does not allow undoing moves."));
    }

    @Test
    void afterOfMateOffersUndoAndGameFollows() throws IOException {
        // Arrange
        GameInProgress game = new GameAssembler().createWithoutClockWithUndo();

        // Act: mate del loco, deshacer el mate y jugar otra cosa
        String output = play(game, "f2 f3\ne7 e5\ng2 g4\nd8 h4\nundo\nd8 g5\nexit\n");

        // Assert
        assertTrue(output.contains("Checkmate! Player 2 wins."));
        assertTrue(output.contains("Type 'undo' to go back or 'exit': "));
        assertFalse(game.state().isFinished());
        assertEquals(4, game.history().size());
    }

    // ---------------- Modo fútbol ----------------

    @Test
    void inModeFootballShowsBallScoreboardAndGoal() throws IOException {
        // Arrange: la torre blanca de d2 tiene la pelota
        core.board.Board board = doubles.BoardFake.from(
                ".......k",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...R....",
                "....K...");
        new core.rules.BallRule().preparePitch(board);
        board.placeBall(pos("d2"));
        GameInProgress game = new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.football(), new NoClock());

        // Act
        String output = play(game, "d2 d8\nexit\n");

        // Assert
        assertTrue(output.contains("Football mode"));
        assertTrue(output.contains(" 2 . . . R*. . . ."), output);
        assertTrue(output.contains("Score: Ana 0 - 0 Beto   (move 0 of 90)"));
        assertTrue(output.contains("GOAL!"));
        assertTrue(output.contains(" 5 . . . o . . . ."), output);
        assertTrue(output.contains(" 1 . . . # K . . ."), output);
        assertTrue(output.contains("Score: Ana 1 - 0 Beto   (move 1 of 90)"));
    }

    @Test
    void chessClassicNotShowsScoreboard() throws IOException {
        // Act
        String output = play("exit\n");

        // Assert
        assertFalse(output.contains("Scoreboard"));
        assertFalse(output.contains("Football mode"));
    }

    @Test
    void consoleShowsLimitChosen() throws IOException {
        // Act
        String output = play(new GameAssembler().createWithoutClockWithUndo(
                adapters.configuration.GameConfiguration.Mode.FOOTBALL, 40), "exit\n");

        // Assert
        assertTrue(output.contains("more goals at move 40."), output);
        assertTrue(output.contains("(move 0 of 40)"), output);
        assertTrue(output.contains(" 5 . . . o . . . ."), output);
    }

    @Test
    void inConsolePassesAsAMovementTowardPartner() throws IOException {
        // Arrange: el rey blanco de e2 tiene la pelota y el caballo está en d3
        core.board.Board board = doubles.BoardFake.from(
                ".......k",
                "........",
                "........",
                "........",
                "........",
                "...N....",
                "....K...",
                "........");
        new core.rules.BallRule().preparePitch(board);
        board.placeBall(pos("e2"));
        GameInProgress game = new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.football(), new NoClock());

        // Act
        String output = play(game, "e2 d3\nexit\n");

        // Assert
        assertTrue(output.contains("Pass!"), output);
        assertTrue(output.contains(" 3 . . . N*. . . ."), output);
    }

    // ---------------- Contra la IA ----------------

    /** Lo que la consola esperó a la IA; en los tests no se espera de verdad. */
    private final java.util.List<java.time.Duration> expected = new java.util.ArrayList<>();

    @Test
    void againstAiIsPlaysAloneAndShowsItsMove() throws IOException {
        var againstAi = new GameAssembler().createWithoutClockAgainstAi(
                adapters.configuration.GameConfiguration.Mode.CLASSIC, 90, core.ia.Difficulty.EASY);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new ConsoleGame(againstAi.game(), new ConsoleRenderer(), new ConsoleMessages(),
                new ByteArrayInputStream("e2 e4\nexit\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8), java.util.Optional.of(againstAi.rival()), expected::add).play();

        String texto = output.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("AI (easy) plays "), texto);
        assertEquals(2, againstAi.game().history().size());
        // Esperó como una persona: entre medio segundo (apertura) y los 3 s del fácil
        assertEquals(1, expected.size());
        assertTrue(expected.getFirst().toMillis() > 300 && expected.getFirst().toMillis() <= 3000, expected.toString());
    }

    @Test
    void againstAiUndoReturnsToTurnOfPerson() throws IOException {
        var againstAi = new GameAssembler().createWithoutClockAgainstAi(
                adapters.configuration.GameConfiguration.Mode.CLASSIC, 90, core.ia.Difficulty.EASY);

        new ConsoleGame(againstAi.game(), new ConsoleRenderer(), new ConsoleMessages(),
                new ByteArrayInputStream("e2 e4\nundo\nexit\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                java.util.Optional.of(againstAi.rival()), expected::add).play();

        assertTrue(againstAi.game().history().isEmpty());
    }
}
