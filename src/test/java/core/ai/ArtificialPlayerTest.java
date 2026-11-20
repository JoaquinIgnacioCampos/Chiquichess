package core.ia;

import core.side.Side;
import core.game.StandardLayout;
import core.game.Player;
import core.game.MoveRequest;
import core.game.Game;
import core.game.GameSituation;
import core.moves.Move;
import core.rules.GameState;
import core.rules.BallRule;
import core.rules.GameRules;
import core.board.Board;
import core.time.TimeSource;
import core.time.NoClock;
import doubles.FontOfTimeFalse;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * La IA resolviendo problemas conocidos. El tiempo es falso y no avanza, así cada dificultad mira
 * exactamente hasta su profundidad máxima y el resultado no depende de la velocidad de la máquina.
 */
class ArtificialPlayerTest {

    /** Tiempo que no corre: la búsqueda nunca se corta por tiempo. */
    private static final TimeSource STILL = new FontOfTimeFalse();

    /** Sin distracciones: para probar lo que ve cada dificultad cuando presta atención. */
    private static ArtificialPlayer ia(Difficulty difficulty) {
        return new ArtificialPlayer(difficulty, new ChanceWithoutOversights(7), STILL);
    }

    /** El azar de siempre, pero nunca "se distrae" (la probabilidad de descuido nunca se cumple). */
    private static final class ChanceWithoutOversights extends Random {
        ChanceWithoutOversights(long seed) {
            super(seed);
        }

        @Override
        public double nextDouble() {
            return 0.99;
        }
    }

    /** Se distrae siempre. */
    private static final class ChanceDistracted extends Random {
        ChanceDistracted(long seed) {
            super(seed);
        }

        @Override
        public double nextDouble() {
            return 0.0;
        }
    }

    private static Game game(GameRules rules, String... rows) {
        return game(BoardFake.from(rows), rules);
    }

    private static Game game(Board board, GameRules rules) {
        return new Game(board, new Player("Ana", Side.WHITE), new Player("IA", Side.BLACK),
                rules, new NoClock());
    }

    private static void playAi(Game game, ArtificialPlayer ia) {
        game.move(MoveRequest.de(ia.chooseMove(game.situation())));
    }

    @Test
    void allDifficultiesVenMateInOne() {
        for (Difficulty difficulty : Difficulty.values()) {
            // Arrange: mate del pasillo con la torre en a8
            Game game = game(GameRules.classic(),
                    "......r.",
                    ".....ppp",
                    "........",
                    "........",
                    "........",
                    "........",
                    "........",
                    "T.....R.");
            ArtificialPlayer ia = difficulty == Difficulty.DIFICIL
                    ? new ArtificialPlayer(difficulty, new ChanceWithoutOversights(7), new FontThatAdvances())
                    : ia(difficulty);

            // Act
            playAi(game, ia);

            // Assert
            assertEquals(GameState.CHECK_MATE, game.state(), difficulty.name());
        }
    }

    @Test
    void middleGivesMateInTwoMovesAlthoughRivalDefendsItBestPossible() {
        // Arrange: escalera de torres contra el rey solo
        Game game = game(GameRules.classic(),
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "TT..R...");
        ArtificialPlayer white = ia(Difficulty.MEDIO);
        ArtificialPlayer black = ia(Difficulty.MEDIO);

        // Act
        playAi(game, white);
        playAi(game, black);
        playAi(game, white);

        // Assert
        assertEquals(GameState.CHECK_MATE, game.state());
    }

    @Test
    void untilEasyEatsAQueenThatStayedWithoutDefense() {
        // Arrange: el caballo blanco de f3 puede comer la dama negra de e5
        Game game = game(GameRules.classic(),
                "....r...",
                "........",
                "........",
                "....d...",
                "........",
                ".....C..",
                "........",
                "....R...");

        // Act
        Move move = ia(Difficulty.FACIL).chooseMove(game.situation());

        // Assert
        assertEquals(pos("e5"), move.destination());
        assertTrue(move.captured().isPresent());
    }

    @Test
    void easyLooksAAloneMoveAndLosesQueenByARookDefendedMiddleNot() {
        // Arrange: la torre negra de d6 está defendida por el peón de e7
        Game game = game(GameRules.classic(),
                ".......r",
                "....p...",
                "...t....",
                "........",
                "........",
                "........",
                "........",
                "R..D....");

        // Act
        Move easy = ia(Difficulty.FACIL).chooseMove(game.situation());
        Move middle = ia(Difficulty.MEDIO).chooseMove(game.situation());

        // Assert: el fácil ve la torre pero no que después le comen la dama
        assertEquals(pos("d6"), easy.destination());
        assertNotEquals(pos("d6"), middle.destination());
    }

    @Test
    void distractedPlaysAnyThingLessAMoveThatHimGivesMateInOne() {
        for (long seed = 0; seed < 25; seed++) {
            // Arrange: si la torre blanca deja la primera fila, Ta1 es mate
            Game game = game(GameRules.classic(),
                    "t......r",
                    "........",
                    "........",
                    "........",
                    "........",
                    "........",
                    ".....PPP",
                    "....T.R.");
            ArtificialPlayer distracted = new ArtificialPlayer(Difficulty.FACIL, new ChanceDistracted(seed), STILL);

            // Act: juega el distraído y responde un rival que no se pierde un mate en 1
            playAi(game, distracted);
            playAi(game, ia(Difficulty.MEDIO));

            // Assert
            assertNotEquals(GameState.CHECK_MATE, game.state(), "semilla " + seed);
        }
    }

    @Test
    void middleNotLeavesItsQueenWhereEat() {
        // Arrange: un peón negro en e5 ataca a la dama blanca de d4
        Game game = game(GameRules.classic(),
                "r.......",
                "........",
                "........",
                "....p...",
                "...D....",
                "........",
                "........",
                ".......R");

        // Act
        Move move = ia(Difficulty.MEDIO).chooseMove(game.situation());

        // Assert: la dama se va de d4 (o se come el peón)
        assertEquals(pos("d4"), move.origin());
    }

    @Test
    void inFootballPutsGoalWhenCan() {
        // Arrange: torre blanca con la pelota en d2 y el arco rival libre en d8
        Board board = BoardFake.from(
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...T....",
                "....R...");
        new BallRule().preparePitch(board);
        board.placeBall(pos("d2"));
        Game game = game(board, GameRules.football());

        // Act
        playAi(game, ia(Difficulty.FACIL));

        // Assert
        assertEquals(1, game.scoreboard().orElseThrow().white());
    }

    @Test
    void withSameSeedChoosesSameMove() {
        // Arrange
        Game game = new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("IA", Side.BLACK), GameRules.classic(), new NoClock());

        // Act
        Move first = new ArtificialPlayer(Difficulty.FACIL, new Random(42), STILL).chooseMove(game.situation());
        Move second = new ArtificialPlayer(Difficulty.FACIL, new Random(42), STILL).chooseMove(game.situation());

        // Assert
        assertEquals(MoveRequest.de(first), MoveRequest.de(second));
    }

    @Test
    void hardShortWhenHimEndsTimeAndPlaysSomethingLegal() {
        // Arrange: el tiempo avanza en cada consulta, así se agota enseguida
        Game game = new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("IA", Side.BLACK), GameRules.classic(), new NoClock());
        ArtificialPlayer ia = new ArtificialPlayer(Difficulty.DIFICIL, new Random(1), new FontThatAdvances());

        // Act
        Move move = ia.chooseMove(game.situation(), Duration.ofMillis(5));

        // Assert
        assertTrue(game.movementsLegal(move.origin()).contains(move.destination()));
    }

    @Test
    void thinkNotPlaysGameNorFiresEvents() {
        // Arrange
        Game game = new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("IA", Side.BLACK), GameRules.classic(), new NoClock());
        List<Object> events = new ArrayList<>();
        game.addObserver(events::add);
        var before = game.board().pieces();

        // Act
        ia(Difficulty.MEDIO).chooseMove(game.situation());

        // Assert
        assertTrue(events.isEmpty());
        assertEquals(before, game.board().pieces());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void acceptsDrawOnlyIfGoesClearlyLosing() {
        // Arrange: las blancas ofrecen; en una partida tienen dama de más, en la otra están iguales
        Game withQueenOfMore = game(GameRules.classic(),
                "....r...", "........", "........", "........", "........", "........", "...D....", "....R...");
        Game even = new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("IA", Side.BLACK), GameRules.classic(), new NoClock());

        // Act + Assert
        assertTrue(ia(Difficulty.MEDIO).acceptsDraw(withQueenOfMore.situation()));
        assertFalse(ia(Difficulty.MEDIO).acceptsDraw(even.situation()));
    }

    @Test
    void withoutMovesLegalNotCanChoose() {
        // Arrange: el rey negro está ahogado y le toca a las negras
        Board board = BoardFake.from(
                "r.......",
                "..D.....",
                ".R......",
                "........",
                "........",
                "........",
                "........",
                "........");
        GameSituation situation = new GameSituation(board, Side.BLACK, List.of(), GameRules.classic());

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> ia(Difficulty.FACIL).chooseMove(situation));
    }

    @Test
    void movementOfAPromotionCarriesChosenPiece() {
        // Arrange
        Game game = game(GameRules.classic(),
                "r.......",
                "......P.",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");

        // Act
        MoveRequest movement = MoveRequest.de(ia(Difficulty.FACIL).chooseMove(game.situation()));

        // Assert: corona en g8 y dice a qué pieza
        assertEquals(pos("g8"), movement.destination());
        assertEquals(Optional.of('D'), movement.promotion());
    }

    @Test
    void withAAloneMovePossibleRespondsInASecond() {
        // Arrange: el rey blanco solo puede ir a a2 (la torre negra controla la columna b)
        Game game = game(GameRules.classic(),
                ".t.....r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R.......");

        // Act
        GameDecision decision = ia(Difficulty.DIFICIL).decide(game.situation(), Optional.empty());

        // Assert
        assertEquals(pos("a2"), decision.move().destination());
        assertTrue(decision.responseTime().toMillis() <= 1200, decision.responseTime().toString());
    }

    @Test
    void hardTakesAsAPersonOfItsLevelAndThinksThatTime() {
        // Arrange: cada consulta del tiempo avanza un segundo, así la búsqueda se corta enseguida
        Game game = new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("IA", Side.BLACK), GameRules.classic(), new NoClock());
        ArtificialPlayer ia = new ArtificialPlayer(Difficulty.DIFICIL, new Random(3), new FontThatAdvancesASecond());

        // Act
        GameDecision decision = ia.decide(game.situation(), Optional.empty());

        // Assert: en la apertura, la mitad de su rango de 3 a 7 s
        long ms = decision.responseTime().toMillis();
        assertTrue(ms >= 1500 && ms <= 3500, decision.responseTime().toString());
        assertTrue(game.movementsLegal(decision.move().origin()).contains(decision.move().destination()));
    }

    /** Cada consulta del tiempo avanza un segundo. */
    private static final class FontThatAdvancesASecond implements TimeSource {
        private long nanos;

        @Override
        public long nowInNanos() {
            nanos += 1_000_000_000L;
            return nanos;
        }
    }

    /** Cada consulta del tiempo avanza un milisegundo: la búsqueda se corta sola enseguida. */
    private static final class FontThatAdvances implements TimeSource {
        private long nanos;

        @Override
        public long nowInNanos() {
            nanos += 1_000_000;
            return nanos;
        }
    }
}
