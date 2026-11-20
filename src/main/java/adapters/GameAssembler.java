package adapters;

import core.side.Side;
import core.ia.Difficulty;
import core.ia.ArtificialPlayer;
import core.game.FootballLayout;
import core.game.StandardLayout;
import core.game.Player;
import core.game.Game;
import core.game.GameInProgress;
import core.rules.BallRule;
import core.rules.GameRules;
import core.board.InitialLayout;
import core.time.TimeSource;
import core.time.ChessClock;
import core.time.GameClock;
import core.time.NoClock;
import adapters.configuration.GameConfiguration;
import adapters.configuration.GameConfiguration.Mode;
import adapters.configuration.ConfiguredGame;
import adapters.configuration.ColorLottery;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Único lugar que construye el grafo del juego para los adaptadores de entrada.
 * Recibe por constructor lo que depende del mundo exterior (el azar del sorteo y el tiempo),
 * así todo el ensamblado real se puede probar con dobles.
 */
public final class GameAssembler {
    private final ColorLottery draw;
    private final TimeSource font;

    /** Ensamblado real: sorteo al azar y tiempo del sistema. */
    public GameAssembler() {
        this(new RandomColorLottery(), new SystemTimeSource());
    }

    public GameAssembler(ColorLottery draw, TimeSource font) {
        this.draw = Objects.requireNonNull(draw, "draw");
        this.font = Objects.requireNonNull(font, "font");
    }

    public GameInProgress createWithClock(Duration timePerPlayer) {
        return createWithClock(timePerPlayer, font);
    }

    /** Permite sustituir únicamente la fuente externa, conservando el ensamblado real. */
    public GameInProgress createWithClock(Duration timePerPlayer, TimeSource font) {
        return create(new ChessClock(timePerPlayer, timePerPlayer, font));
    }

    public GameInProgress createWithoutClock() {
        return create(new NoClock());
    }

    /** Partida sin tiempo en la que se puede deshacer y rehacer (la usa la consola). */
    public GameInProgress createWithoutClockWithUndo() {
        return createWithoutClockWithUndo(Mode.CLASSIC);
    }

    /** Partida sin tiempo, con deshacer, del modo elegido y el límite de jugadas por defecto. */
    public GameInProgress createWithoutClockWithUndo(Mode mode) {
        return createWithoutClockWithUndo(mode, GameConfiguration.DEFAULT_LIMIT);
    }

    /** Partida sin tiempo, con deshacer, del modo elegido (la consola puede jugar al fútbol). */
    public GameInProgress createWithoutClockWithUndo(Mode mode, int moveLimit) {
        return create(new NoClock(), new Player("Player 1", Side.WHITE), new Player("Player 2", Side.BLACK),
                true, mode, moveLimit);
    }

    /**
     * Partida de consola contra la IA: sin tiempo y con deshacer. La IA juega con negras.
     */
    public GameAgainstAi createWithoutClockAgainstAi(Mode mode, int moveLimit, Difficulty difficulty) {
        Game game = create(new NoClock(), new Player("Player 1", Side.WHITE),
                new Player(aiName(difficulty), Side.BLACK), true, mode, moveLimit);
        return new GameAgainstAi(game, createRival(game, Side.BLACK, difficulty));
    }

    /** Una partida y la IA que juega en ella. */
    public record GameAgainstAi(GameInProgress game, ArtificialRival rival) {
    }

    /** La IA de esa dificultad, con azar real y el tiempo del sistema. */
    public ArtificialRival createRival(GameInProgress game, Side side, Difficulty difficulty) {
        return new ArtificialRival(game, side, new ArtificialPlayer(difficulty, new Random(), font));
    }

    private static String aiName(Difficulty difficulty) {
        return "AI (" + switch (difficulty) {
            case EASY -> "easy";
            case MIDDLE -> "middle";
            case HARD -> "hard";
        } + ")";
    }

    /** Resuelve el sorteo una sola vez y construye el juego elegido en el menú. */
    public GameInProgress createConfigured(GameConfiguration configuration) {
        return prepareConfigured(configuration).game();
    }

    public ConfiguredGame prepareConfigured(GameConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        boolean player1White = switch (configuration.colors()) {
            case PLAYER_1_WHITE -> true;
            case PLAYER_2_WHITE -> false;
            case RANDOM -> draw.player1PlaysWithWhite();
        };
        GameClock clock = configuration.time().duration()
                .<GameClock>map(duration -> new ChessClock(duration, duration, font))
                .orElseGet(NoClock::new);
        // La IA es el Jugador 2: juega con su equipo y con el color que le tocó.
        String player2 = configuration.difficulty().map(GameAssembler::aiName).orElse("Player 2");
        Game game = create(clock, new Player(player1White ? "Player 1" : player2, Side.WHITE),
                new Player(player1White ? player2 : "Player 1", Side.BLACK),
                configuration.allowUndo(), configuration.mode(), configuration.moveLimit());
        Side player2Side = player1White ? Side.BLACK : Side.WHITE;
        Optional<ArtificialRival> rival = configuration.difficulty()
                .map(difficulty -> createRival(game, player2Side, difficulty));
        return new ConfiguredGame(game,
                player1White ? configuration.teamPlayer1() : configuration.teamPlayer2(),
                player1White ? configuration.teamPlayer2() : configuration.teamPlayer1(), clock, rival);
    }

    private Game create(GameClock clock) {
        return create(clock, new Player("Player 1", Side.WHITE), new Player("Player 2", Side.BLACK), false,
                Mode.CLASSIC, GameConfiguration.DEFAULT_LIMIT);
    }

    /**
     * Cada partida recibe reglas nuevas: las condiciones de repetición y 50 movimientos guardan memoria.
     * El modo elige las reglas y la disposición: el fútbol marca la cancha (arcos y pelota) y termina
     * en el límite de jugadas elegido.
     */
    private Game create(GameClock clock, Player white, Player black, boolean allowUndo, Mode mode,
                          int moveLimit) {
        GameRules rules = switch (mode) {
            case CLASSIC -> GameRules.classic();
            case FOOTBALL -> GameRules.football(moveLimit);
        };
        InitialLayout layout = switch (mode) {
            case CLASSIC -> new StandardLayout();
            case FOOTBALL -> new FootballLayout(new StandardLayout(), new BallRule());
        };
        return new Game(layout.createBoard(), white, black,
                allowUndo ? rules.withUndo() : rules, clock);
    }
}
