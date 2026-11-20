package adapters;

import core.side.Side;
import core.game.GameInProgress;
import core.rules.GameState;
import core.rules.FinishReason;
import adapters.configuration.GameConfiguration;
import adapters.configuration.GameConfiguration.Colors;
import adapters.configuration.GameConfiguration.Team;
import adapters.configuration.GameConfiguration.Mode;
import adapters.configuration.GameConfiguration.Rival;
import adapters.configuration.GameConfiguration.Time;
import adapters.configuration.ConfiguredGame;
import doubles.FontOfTimeFalse;
import doubles.FixedColorLottery;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

class GameAssemblerTest {
    private final GameAssembler assembler = new GameAssembler();

    @Test
    void assemblesPositionStandardAndClockWithFontInjected() {
        FontOfTimeFalse font = new FontOfTimeFalse();
        GameInProgress game = assembler.createWithClock(Duration.ofMinutes(5), font);
        assertEquals(8, game.board().width());
        assertEquals(8, game.board().height());
        assertEquals(32, game.board().pieces().size());
        assertEquals(Side.WHITE, game.turn());
        assertEquals(GameState.IN_PROGRESS, game.state());
        font.advance(Duration.ofSeconds(30));
        assertEquals(Optional.of(Duration.ofSeconds(270)), game.timeRemaining(Side.WHITE));
        assertEquals(Optional.of(Duration.ofMinutes(5)), game.timeRemaining(Side.BLACK));
    }

    @Test
    void clockAssembledFinishesGameRealByTime() {
        FontOfTimeFalse font = new FontOfTimeFalse();
        GameInProgress game = assembler.createWithClock(Duration.ofSeconds(10), font);
        font.advance(Duration.ofSeconds(10));
        game.verifyTime();
        assertEquals(FinishReason.TIME_EXHAUSTED, game.result().orElseThrow().reason());
        assertEquals(Optional.of(Side.BLACK), game.result().orElseThrow().winner());
    }

    @Test
    void consoleKeepsRulesRealAndFollowsWithoutClock() {
        GameInProgress game = assembler.createWithoutClock();
        assertTrue(game.timeRemaining(Side.WHITE).isEmpty());
        assertTrue(game.timeRemaining(Side.BLACK).isEmpty());
        game.move(move("f2", "f3"));
        game.move(move("e7", "e5"));
        game.move(move("g2", "g4"));
        game.move(move("d8", "h4"));
        assertEquals(GameState.CHECK_MATE, game.state());
        assertEquals(Side.BLACK, game.winner().orElseThrow().color());
    }

    @Test
    void gamesNewNotShareMemoryOfRepetition() {
        GameInProgress first = assembler.createWithoutClock();
        cycleOfKnights(first);
        GameInProgress second = assembler.createWithoutClock();
        cycleOfKnights(second);
        assertEquals(GameState.IN_PROGRESS, second.state());
        cycleOfKnights(first);
        assertEquals(GameState.DRAW_REPETITION, first.state());
        assertEquals(GameState.IN_PROGRESS, second.state());
    }

    @Test
    void useResignationAndAgreementReal() {
        GameInProgress resignation = assembler.createWithoutClock();
        resignation.resign(Side.WHITE);
        assertEquals(FinishReason.RESIGNATION, resignation.result().orElseThrow().reason());
        assertEquals(Side.BLACK, resignation.winner().orElseThrow().color());
        GameInProgress agreement = assembler.createWithoutClock();
        agreement.offerDraw(Side.WHITE);
        agreement.acceptDraw(Side.BLACK);
        assertEquals(FinishReason.AGREEMENT, agreement.result().orElseThrow().reason());
        assertTrue(agreement.winner().isEmpty());
    }

    @Test
    void inModeRandomDrawInjectedDecidesColorsAndTeams() {
        FixedColorLottery draw = FixedColorLottery.player1WithBlack();
        GameAssembler withDraw = new GameAssembler(draw, new FontOfTimeFalse());
        ConfiguredGame prepared = withDraw.prepareConfigured(configuration(Time.SIN_TIEMPO, Colors.ALEATORIO));
        assertEquals(1, draw.queries());
        assertEquals("Jugador 2", prepared.game().player(Side.WHITE).displayName());
        assertEquals("Jugador 1", prepared.game().player(Side.BLACK).displayName());
        assertEquals(Team.RIVER, prepared.whiteTeam());
        assertEquals(Team.BOCA, prepared.blackTeam());
    }

    @Test
    void withColorsChosenNotQueryDraw() {
        FixedColorLottery draw = FixedColorLottery.player1WithBlack();
        GameAssembler withDraw = new GameAssembler(draw, new FontOfTimeFalse());
        ConfiguredGame prepared = withDraw.prepareConfigured(
                configuration(Time.SIN_TIEMPO, Colors.JUGADOR_1_BLANCAS));
        assertEquals(0, draw.queries());
        assertEquals("Jugador 1", prepared.game().player(Side.WHITE).displayName());
        assertEquals(Team.BOCA, prepared.whiteTeam());
    }

    @Test
    void clockOfMenuUsesFontInjected() {
        FontOfTimeFalse font = new FontOfTimeFalse();
        GameAssembler withFont = new GameAssembler(FixedColorLottery.player1WithWhite(), font);
        GameInProgress game = withFont.prepareConfigured(
                configuration(Time.TRES_MINUTOS, Colors.JUGADOR_1_BLANCAS)).game();
        font.advance(Duration.ofSeconds(30));
        assertEquals(Optional.of(Duration.ofSeconds(150)), game.timeRemaining(Side.WHITE));
        assertEquals(Optional.of(Duration.ofMinutes(3)), game.timeRemaining(Side.BLACK));
    }

    @Test
    void createWithClockWithoutFontExplicitUsesInjected() {
        FontOfTimeFalse font = new FontOfTimeFalse();
        GameInProgress game = new GameAssembler(FixedColorLottery.player1WithWhite(), font)
                .createWithClock(Duration.ofMinutes(5));
        font.advance(Duration.ofSeconds(30));
        assertEquals(Optional.of(Duration.ofSeconds(270)), game.timeRemaining(Side.WHITE));
    }

    @Test
    void rejectsDrawOrFontNull() {
        assertThrows(NullPointerException.class, () -> new GameAssembler(null, new FontOfTimeFalse()));
        assertThrows(NullPointerException.class, () -> new GameAssembler(FixedColorLottery.player1WithWhite(), null));
    }

    @Test
    void withOptionOfMenuGameAllowsUndo() {
        GameConfiguration withUndo = new GameConfiguration(Mode.CLASICO, Time.SIN_TIEMPO, Rival.AMIGO,
                Team.BOCA, Team.RIVER, Colors.JUGADOR_1_BLANCAS, true);
        GameInProgress game = assembler.createConfigured(withUndo);
        game.move(move("e2", "e4"));
        assertTrue(game.canUndo());
        game.undo();
        assertTrue(game.history().isEmpty());
    }

    @Test
    void withoutOptionOfMenuNotCanUndo() {
        GameInProgress game = assembler.createConfigured(configuration(Time.SIN_TIEMPO, Colors.JUGADOR_1_BLANCAS));
        game.move(move("e2", "e4"));
        assertFalse(game.canUndo());
    }

    @Test
    void configurationNotAdmitsUndoWithTime() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfiguration(Mode.CLASICO,
                Time.CINCO_MINUTOS, Rival.AMIGO, Team.BOCA, Team.RIVER, Colors.ALEATORIO, true));
    }

    @Test
    void gameOfConsoleAllowsUndo() {
        GameInProgress game = assembler.createWithoutClockWithUndo();
        game.move(move("e2", "e4"));
        assertTrue(game.canUndo());
    }

    private GameConfiguration configuration(Time time, Colors colors) {
        return new GameConfiguration(Mode.CLASICO, time, Rival.AMIGO, Team.BOCA, Team.RIVER, colors);
    }

    private void cycleOfKnights(GameInProgress game) {
        game.move(move("g1", "f3"));
        game.move(move("g8", "f6"));
        game.move(move("f3", "g1"));
        game.move(move("f6", "g8"));
    }

    @Test
    void modeFootballPutsBallInD5AndPlaysWithGoals() {
        GameConfiguration football = new GameConfiguration(Mode.FUTBOL, Time.SIN_TIEMPO, Rival.AMIGO,
                Team.BOCA, Team.RIVER, Colors.JUGADOR_1_BLANCAS, true);
        GameInProgress game = assembler.createConfigured(football);
        assertEquals(Optional.of(pos("d5")), game.board().ball());
        assertTrue(game.allowsUndo());
    }

    @Test
    void modeClassicNotHasBall() {
        GameInProgress game = assembler.createConfigured(configuration(Time.SIN_TIEMPO, Colors.JUGADOR_1_BLANCAS));
        assertTrue(game.board().ball().isEmpty());
    }

    @Test
    void consoleCanPlayToFootball() {
        GameInProgress game = assembler.createWithoutClockWithUndo(Mode.FUTBOL);
        assertEquals(Optional.of(pos("d5")), game.board().ball());
        assertTrue(game.allowsUndo());
    }

    @Test
    void limitOfMovesOfMenuArrivesToGame() {
        GameConfiguration football = new GameConfiguration(Mode.FUTBOL, Time.SIN_TIEMPO, Rival.AMIGO,
                Team.BOCA, Team.RIVER, Colors.JUGADOR_1_BLANCAS, false, 40);
        assertEquals(Optional.of(40), assembler.createConfigured(football).moveLimit());
        assertEquals(Optional.of(90), assembler.createWithoutClockWithUndo(Mode.FUTBOL).moveLimit());
        assertEquals(Optional.of(30), assembler.createWithoutClockWithUndo(Mode.FUTBOL, 30).moveLimit());
    }

    @Test
    void configurationRejectsLimitsOutsideOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfiguration(Mode.FUTBOL,
                Time.SIN_TIEMPO, Rival.AMIGO, Team.BOCA, Team.RIVER, Colors.ALEATORIO, false,
                GameConfiguration.LIMIT_MINIMUM - 1));
        assertThrows(IllegalArgumentException.class, () -> new GameConfiguration(Mode.FUTBOL,
                Time.SIN_TIEMPO, Rival.AMIGO, Team.BOCA, Team.RIVER, Colors.ALEATORIO, false,
                GameConfiguration.LIMIT_MAXIMUM + 1));
    }

    @Test
    void modeFootballMarksNets() {
        GameInProgress game = assembler.createWithoutClockWithUndo(Mode.FUTBOL);
        assertEquals(Optional.of(Side.BLACK), game.board().netIn(pos("e8")));
        assertEquals(Optional.of(Side.WHITE), game.board().netIn(pos("d1")));
    }

    // ---------------- Contra la IA ----------------

    @Test
    void againstAiPlayer2IsAiWithItsColorAndItsName() {
        GameConfiguration againstAi = new GameConfiguration(Mode.CLASICO, Time.SIN_TIEMPO, Rival.IA,
                Team.BOCA, Team.RIVER, Colors.JUGADOR_2_BLANCAS, false, GameConfiguration.DEFAULT_LIMIT,
                java.util.Optional.of(core.ai.Difficulty.FACIL));

        var prepared = assembler.prepareConfigured(againstAi);

        assertEquals(Side.WHITE, prepared.rival().orElseThrow().side());
        assertEquals("IA (fácil)", prepared.game().player(Side.WHITE).displayName());
        assertEquals(Team.RIVER, prepared.whiteTeam());
    }

    @Test
    void againstAFriendNotHasAi() {
        var prepared = assembler.prepareConfigured(configuration(Time.SIN_TIEMPO, Colors.JUGADOR_1_BLANCAS));
        assertTrue(prepared.rival().isEmpty());
    }

    @Test
    void difficultyGoesOnlyAndAlwaysWithAi() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfiguration(Mode.CLASICO, Time.SIN_TIEMPO,
                Rival.IA, Team.BOCA, Team.RIVER, Colors.ALEATORIO, false));
        assertThrows(IllegalArgumentException.class, () -> new GameConfiguration(Mode.CLASICO, Time.SIN_TIEMPO,
                Rival.AMIGO, Team.BOCA, Team.RIVER, Colors.ALEATORIO, false, GameConfiguration.DEFAULT_LIMIT,
                java.util.Optional.of(core.ai.Difficulty.MEDIO)));
    }
}
