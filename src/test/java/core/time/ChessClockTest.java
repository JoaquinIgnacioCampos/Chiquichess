package core.time;

import core.side.Side;

import doubles.FontOfTimeFalse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ChessClockTest {
    private final FontOfTimeFalse font = new FontOfTimeFalse();

    private ChessClock clock() {
        return new ChessClock(Duration.ofMinutes(5), Duration.ofMinutes(3), font);
    }

    @Test
    void thirtySecondsOnlyDeductToColorActive() {
        ChessClock clock = clock();
        clock.start(Side.WHITE);
        font.advance(Duration.ofSeconds(30));
        assertEquals(Optional.of(Duration.ofSeconds(270)), clock.timeRemaining(Side.WHITE));
        assertEquals(Optional.of(Duration.ofSeconds(180)), clock.timeRemaining(Side.BLACK));
        // Consultar no acumula descuentos ni cambia el turno.
        assertEquals(Optional.of(Duration.ofSeconds(270)), clock.timeRemaining(Side.WHITE));
    }

    @Test
    void changeTurnConsolidatesTimeAndActiveToOtherColor() {
        ChessClock clock = clock();
        clock.start(Side.WHITE);
        font.advance(Duration.ofSeconds(30));
        clock.changeTurn();
        font.advance(Duration.ofSeconds(12));
        assertEquals(Optional.of(Duration.ofSeconds(270)), clock.timeRemaining(Side.WHITE));
        assertEquals(Optional.of(Duration.ofSeconds(168)), clock.timeRemaining(Side.BLACK));
        clock.changeTurn();
        font.advance(Duration.ofSeconds(5));
        assertEquals(Optional.of(Duration.ofSeconds(265)), clock.timeRemaining(Side.WHITE));
        assertEquals(Optional.of(Duration.ofSeconds(168)), clock.timeRemaining(Side.BLACK));
    }

    @Test
    void stopFreezesBothTimesAndIsIdempotent() {
        ChessClock clock = clock();
        clock.start(Side.WHITE);
        font.advance(Duration.ofSeconds(20));
        clock.stop();
        font.advance(Duration.ofHours(1));
        clock.stop();
        assertEquals(Optional.of(Duration.ofSeconds(280)), clock.timeRemaining(Side.WHITE));
        assertEquals(Optional.of(Duration.ofSeconds(180)), clock.timeRemaining(Side.BLACK));
        assertThrows(IllegalStateException.class, clock::changeTurn);
    }

    @Test
    void exactZeroExhaustsTimeAndNeverShowNegative() {
        ChessClock clock = clock();
        clock.start(Side.BLACK);
        font.advance(Duration.ofSeconds(179));
        assertFalse(clock.timeExhausted(Side.BLACK));
        font.advance(Duration.ofSeconds(1));
        assertTrue(clock.timeExhausted(Side.BLACK));
        font.advance(Duration.ofMinutes(10));
        assertEquals(Optional.of(Duration.ZERO), clock.timeRemaining(Side.BLACK));
        assertFalse(clock.timeExhausted(Side.WHITE));
        clock.stop();
        assertEquals(Optional.of(Duration.ZERO), clock.timeRemaining(Side.BLACK));
    }

    @Test
    void notDeductsBeforeOfStartAndNotCanRestart() {
        ChessClock clock = clock();
        font.advance(Duration.ofHours(1));
        assertEquals(Optional.of(Duration.ofMinutes(5)), clock.timeRemaining(Side.WHITE));
        assertThrows(IllegalStateException.class, clock::changeTurn);
        clock.start(Side.WHITE);
        assertThrows(IllegalStateException.class, () -> clock.start(Side.BLACK));
        clock.stop();
        assertThrows(IllegalStateException.class, () -> clock.start(Side.WHITE));
    }

    @Test
    void requiresTimesInitialPositiveAndDependenciesNotNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChessClock(Duration.ZERO, Duration.ofMinutes(1), font));
        assertThrows(IllegalArgumentException.class,
                () -> new ChessClock(Duration.ofMinutes(1), Duration.ofSeconds(-1), font));
        assertThrows(NullPointerException.class,
                () -> new ChessClock(null, Duration.ofMinutes(1), font));
        assertThrows(NullPointerException.class,
                () -> new ChessClock(Duration.ofMinutes(1), Duration.ofMinutes(1), null));
    }

    @Test
    void clockWithoutLimitNeverExhaustsNorPublicADurationFake() {
        GameClock clock = new NoClock();
        clock.start(Side.WHITE);
        clock.changeTurn();
        clock.stop();
        for (Side color : Side.values()) {
            assertTrue(clock.timeRemaining(color).isEmpty());
            assertFalse(clock.timeExhausted(color));
        }
    }

    @Test
    void colorNullNotStartsNorModifiesClock() {
        ChessClock clock = clock();
        assertThrows(NullPointerException.class, () -> clock.start(null));
        clock.start(Side.WHITE);
        assertThrows(NullPointerException.class, () -> clock.timeRemaining(null));
        assertThrows(NullPointerException.class, () -> clock.timeExhausted(null));
        assertEquals(Optional.of(Duration.ofMinutes(5)), clock.timeRemaining(Side.WHITE));
    }
}
