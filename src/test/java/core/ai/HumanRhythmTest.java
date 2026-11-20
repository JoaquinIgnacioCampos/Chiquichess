package core.ia;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Cuánto tarda la IA en responder, como una persona de su nivel. El azar se pasa como números fijos. */
class HumanRhythmTest {

    private static final Optional<Duration> WITHOUT_CLOCK = Optional.empty();
    /** Una jugada del medio de la partida, con muchas opciones y sin jaque. */
    private static final int MIDDLE_GAME = 30;

    private static Duration common(Difficulty difficulty, double chance) {
        return HumanRhythm.time(difficulty, 30, false, MIDDLE_GAME, WITHOUT_CLOCK, chance, chance);
    }

    @Test
    void eachDifficultyTakesInsideOfItsRange() {
        for (Difficulty difficulty : Difficulty.values()) {
            assertEquals(difficulty.responseMinimum(), common(difficulty, 0));
            assertEquals(difficulty.responseMaximum(), common(difficulty, 1));
        }
        assertEquals(Duration.ofSeconds(2), common(Difficulty.FACIL, 0.5));
    }

    @Test
    void beginnerPlaysMoreFastThatStrong() {
        assertTrue(common(Difficulty.FACIL, 0.5).compareTo(common(Difficulty.MEDIO, 0.5)) < 0);
        assertTrue(common(Difficulty.MEDIO, 0.5).compareTo(common(Difficulty.DIFICIL, 0.5)) < 0);
    }

    @Test
    void withAAloneMovePossibleRespondsInASecond() {
        Duration forced = HumanRhythm.time(Difficulty.DIFICIL, 1, true, MIDDLE_GAME, WITHOUT_CLOCK, 1, 1);
        assertTrue(forced.toMillis() >= 900 && forced.toMillis() <= 1200, forced.toString());
    }

    @Test
    void inCheckWithFewOutputsTakesItsMinimum() {
        assertEquals(Difficulty.MEDIO.responseMinimum(),
                HumanRhythm.time(Difficulty.MEDIO, 3, true, MIDDLE_GAME, WITHOUT_CLOCK, 1, 1));
    }

    @Test
    void inOpeningPlaysMoreFast() {
        Duration opening = HumanRhythm.time(Difficulty.MEDIO, 20, false, 2, WITHOUT_CLOCK, 0.5, 0.5);
        assertEquals(common(Difficulty.MEDIO, 0.5).dividedBy(2), opening);
    }

    @Test
    void withClockNeverTakesMoreThatAFractionOfItThatHimStays() {
        Optional<Duration> littleTime = Optional.of(Duration.ofSeconds(80));
        Duration time = HumanRhythm.time(Difficulty.DIFICIL, 30, false, MIDDLE_GAME, littleTime, 1, 1);
        assertEquals(Duration.ofSeconds(80).dividedBy(HumanRhythm.REMAINING_TIME_FRACTION), time);
    }

    @Test
    void aPositionComplicatedDoesDoubtExceptInOpening() {
        Duration fast = Difficulty.MEDIO.responseMinimum();
        Duration little = HumanRhythm.extendByComplexity(fast, Difficulty.MEDIO, MIDDLE_GAME, WITHOUT_CLOCK, 0);
        Duration much = HumanRhythm.extendByComplexity(fast, Difficulty.MEDIO, MIDDLE_GAME, WITHOUT_CLOCK, 1);
        assertTrue(little.compareTo(fast) > 0);
        assertTrue(much.compareTo(little) > 0);
        assertEquals(Difficulty.MEDIO.responseMaximum(), much);
        assertEquals(fast, HumanRhythm.extendByComplexity(fast, Difficulty.MEDIO, 2, WITHOUT_CLOCK, 1));
    }
}
