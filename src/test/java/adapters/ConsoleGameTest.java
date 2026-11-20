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
        String output = play("e4 e5\ne7 e5\na1 a3\nsalir\n");

        // Assert
        assertTrue(output.contains("No hay ninguna pieza en la casilla de origen."));
        assertTrue(output.contains("Esa pieza no es suya: es el turno del otro jugador."));
        assertTrue(output.contains("Esa pieza no puede moverse ahi."));
    }

    @Test
    void announcesCheckMateWithNameOfWinner() throws IOException {
        // Act
        String output = play("f2 f3\ne7 e5\ng2 g4\nd8 h4\n");

        // Assert
        assertTrue(output.strip().endsWith("Jaque mate! Gana Jugador 2."));
    }

    // ---------------- Promoción ----------------

    /** Peón blanco en a7 a punto de promover. */
    private Game withPawnInTo7() {
        return new Game(doubles.BoardFake.from(
                "....r...",
                "P.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R..."), new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.classic(), new NoClock());
    }

    @Test
    void thirdLetterChoosesPieceOfPromotion() throws IOException {
        // Arrange
        GameInProgress game = withPawnInTo7();

        // Act
        play(game, "a7 a8 c\nsalir\n");

        // Assert
        assertEquals('C', game.board().pieceIn(pos("a8")).orElseThrow().code());
    }

    @Test
    void withoutThirdLetterPromotesToQueen() throws IOException {
        // Arrange
        GameInProgress game = withPawnInTo7();

        // Act
        play(game, "a7 a8\nsalir\n");

        // Assert
        assertEquals('D', game.board().pieceIn(pos("a8")).orElseThrow().code());
    }

    @Test
    void aPieceOfPromotionInvalidRejects() throws IOException {
        // Arrange
        GameInProgress game = withPawnInTo7();

        // Act
        String output = play(game, "a7 a8 R\na7 a8 dama\nsalir\n");

        // Assert
        assertTrue(output.contains("Esa pieza no puede moverse ahi."));
        assertTrue(output.contains("Formato invalido. Ejemplo: e2 e4 (para promover: e7 e8 C)"));
        assertTrue(game.history().isEmpty());
    }

    // ---------------- Deshacer y rehacer ----------------

    @Test
    void withUndoEnabledHelpMentionsCommands() throws IOException {
        // Act
        String output = play(new GameAssembler().createWithoutClockWithUndo(), "salir\n");

        // Assert
        assertTrue(output.startsWith("Escriba la jugada como 'origen destino' (ej: e2 e4), 'deshacer', 'rehacer' o 'salir'."));
    }

    @Test
    void undoAndRedoFromConsole() throws IOException {
        // Arrange
        GameInProgress game = new GameAssembler().createWithoutClockWithUndo();

        // Act: jugar, deshacer, rehacer y deshacer otra vez
        String output = play(game, "e2 e4\ndeshacer\nrehacer\ndeshacer\nsalir\n");

        // Assert
        assertTrue(game.history().isEmpty());
        assertFalse(output.contains("No hay jugadas"));
    }

    @Test
    void notifiesWhenNotHasNothingForUndoOrRedo() throws IOException {
        // Act
        String output = play(new GameAssembler().createWithoutClockWithUndo(), "deshacer\nrehacer\nsalir\n");

        // Assert
        assertTrue(output.contains("No hay jugadas para deshacer."));
        assertTrue(output.contains("No hay jugadas para rehacer."));
    }

    @Test
    void withoutUndoEnabledItRejects() throws IOException {
        // Act
        String output = play("e2 e4\ndeshacer\nsalir\n");

        // Assert
        assertTrue(output.contains("Esta partida no permite deshacer jugadas."));
    }

    @Test
    void afterOfMateOffersUndoAndGameFollows() throws IOException {
        // Arrange
        GameInProgress game = new GameAssembler().createWithoutClockWithUndo();

        // Act: mate del loco, deshacer el mate y jugar otra cosa
        String output = play(game, "f2 f3\ne7 e5\ng2 g4\nd8 h4\ndeshacer\nd8 g5\nsalir\n");

        // Assert
        assertTrue(output.contains("Jaque mate! Gana Jugador 2."));
        assertTrue(output.contains("Escriba 'deshacer' para volver atras o 'salir': "));
        assertFalse(game.state().isFinished());
        assertEquals(4, game.history().size());
    }

    // ---------------- Modo fútbol ----------------

    @Test
    void inModeFootballShowsBallScoreboardAndGoal() throws IOException {
        // Arrange: la torre blanca de d2 tiene la pelota
        core.board.Board board = doubles.BoardFake.from(
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...T....",
                "....R...");
        new core.rules.BallRule().preparePitch(board);
        board.placeBall(pos("d2"));
        GameInProgress game = new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.football(), new NoClock());

        // Act
        String output = play(game, "d2 d8\nsalir\n");

        // Assert
        assertTrue(output.contains("Modo futbol"));
        assertTrue(output.contains(" 2 . . . T*. . . ."), output);
        assertTrue(output.contains("Marcador: Ana 0 - 0 Beto   (jugada 0 de 90)"));
        assertTrue(output.contains("GOL!"));
        assertTrue(output.contains(" 5 . . . o . . . ."), output);
        assertTrue(output.contains(" 1 . . . # R . . ."), output);
        assertTrue(output.contains("Marcador: Ana 1 - 0 Beto   (jugada 1 de 90)"));
    }

    @Test
    void chessClassicNotShowsScoreboard() throws IOException {
        // Act
        String output = play("salir\n");

        // Assert
        assertFalse(output.contains("Marcador"));
        assertFalse(output.contains("Modo futbol"));
    }

    @Test
    void consoleShowsLimitChosen() throws IOException {
        // Act
        String output = play(new GameAssembler().createWithoutClockWithUndo(
                adapters.configuration.GameConfiguration.Mode.FUTBOL, 40), "salir\n");

        // Assert
        assertTrue(output.contains("mas goles en la jugada 40."), output);
        assertTrue(output.contains("(jugada 0 de 40)"), output);
        assertTrue(output.contains(" 5 . . . o . . . ."), output);
    }

    @Test
    void inConsolePassesAsAMovementTowardPartner() throws IOException {
        // Arrange: el rey blanco de e2 tiene la pelota y el caballo está en d3
        core.board.Board board = doubles.BoardFake.from(
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "...C....",
                "....R...",
                "........");
        new core.rules.BallRule().preparePitch(board);
        board.placeBall(pos("e2"));
        GameInProgress game = new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.football(), new NoClock());

        // Act
        String output = play(game, "e2 d3\nsalir\n");

        // Assert
        assertTrue(output.contains("Pase!"), output);
        assertTrue(output.contains(" 3 . . . C*. . . ."), output);
    }

    // ---------------- Contra la IA ----------------

    /** Lo que la consola esperó a la IA; en los tests no se espera de verdad. */
    private final java.util.List<java.time.Duration> expected = new java.util.ArrayList<>();

    @Test
    void againstAiIsPlaysAloneAndShowsItsMove() throws IOException {
        var againstAi = new GameAssembler().createWithoutClockAgainstAi(
                adapters.configuration.GameConfiguration.Mode.CLASICO, 90, core.ia.Difficulty.FACIL);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new ConsoleGame(againstAi.game(), new ConsoleRenderer(), new ConsoleMessages(),
                new ByteArrayInputStream("e2 e4\nsalir\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8), java.util.Optional.of(againstAi.rival()), expected::add).play();

        String texto = output.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("IA (fácil) juega "), texto);
        assertEquals(2, againstAi.game().history().size());
        // Esperó como una persona: entre medio segundo (apertura) y los 3 s del fácil
        assertEquals(1, expected.size());
        assertTrue(expected.getFirst().toMillis() > 300 && expected.getFirst().toMillis() <= 3000, expected.toString());
    }

    @Test
    void againstAiUndoReturnsToTurnOfPerson() throws IOException {
        var againstAi = new GameAssembler().createWithoutClockAgainstAi(
                adapters.configuration.GameConfiguration.Mode.CLASICO, 90, core.ia.Difficulty.FACIL);

        new ConsoleGame(againstAi.game(), new ConsoleRenderer(), new ConsoleMessages(),
                new ByteArrayInputStream("e2 e4\ndeshacer\nsalir\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                java.util.Optional.of(againstAi.rival()), expected::add).play();

        assertTrue(againstAi.game().history().isEmpty());
    }
}
