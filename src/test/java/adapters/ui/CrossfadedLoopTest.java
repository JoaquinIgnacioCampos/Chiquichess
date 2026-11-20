package adapters.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La curva del fundido cruzado con el que se repiten los audios (la de la intro y la del video del costado). */
class CrossfadedLoopTest {

    @Test
    void firstRunOfAudioStartsWholeAndAllEndInSilence() {
        assertEquals(1, CrossfadedLoop.gain(0, 10, false), 1e-9);
        assertEquals(1, CrossfadedLoop.gain(5, 10, false), 1e-9);
        assertEquals(0, CrossfadedLoop.gain(10, 10, false), 1e-9);
    }

    @Test
    void runsNextEnterWithAFade() {
        assertEquals(0, CrossfadedLoop.gain(0, 10, true), 1e-9);
        assertEquals(1, CrossfadedLoop.gain(CrossfadedLoop.FADE, 10, true), 1e-9);
        double half = CrossfadedLoop.gain(CrossfadedLoop.FADE / 2, 10, true);
        assertTrue(half > 0 && half < 1);
    }

    @Test
    void onOverlapEndOfARunWithStartOfNextPowerKeeps() {
        for (double t = 0; t <= CrossfadedLoop.FADE; t += 0.1) {
            double leaves = CrossfadedLoop.gain(10 - CrossfadedLoop.FADE + t, 10, false);
            double enters = CrossfadedLoop.gain(t, 10, true);
            assertEquals(1, leaves * leaves + enters * enters, 1e-9, "t=" + t);
        }
    }
}
