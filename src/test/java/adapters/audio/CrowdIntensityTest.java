package adapters.audio;

import core.side.Side;
import core.rules.Scoreboard;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CrowdIntensityTest {

    private static double intensity(Side crowd, int moves, int white, int black) {
        return CrowdIntensity.de(crowd, moves, Optional.of(90), new Scoreboard(white, black));
    }

    @Test
    void growsToMeasureThatAdvancesMatch() {
        double onStart = intensity(Side.WHITE, 0, 0, 0);
        double toHalf = intensity(Side.WHITE, 45, 0, 0);
        double onFinal = intensity(Side.WHITE, 89, 0, 0);

        assertTrue(onStart < toHalf && toHalf < onFinal);
    }

    @Test
    void aFinalEvenSoundsMoreThatARout() {
        // Con 3 goles termina el partido, así que 2 a 0 ya es una goleada
        assertTrue(intensity(Side.WHITE, 80, 1, 0) > intensity(Side.WHITE, 80, 2, 0));
    }

    @Test
    void crowdOfThatWinsSingsMoreThatOfThatLoses() {
        assertTrue(intensity(Side.BLACK, 40, 0, 1) > intensity(Side.WHITE, 40, 0, 1));
        assertEquals(intensity(Side.WHITE, 40, 1, 1), intensity(Side.BLACK, 40, 1, 1));
    }

    @Test
    void staysBetweenZeroAndOne() {
        for (int moves = -5; moves <= 200; moves += 5) {
            for (Side side : Side.values()) {
                double value = intensity(side, moves, 1, 0);
                assertTrue(value >= 0 && value <= 1, "Fuera de rango: " + value);
            }
        }
    }

    @Test
    void withoutLimitOfMovesNotGrows() {
        Scoreboard tie = new Scoreboard(0, 0);
        assertEquals(CrowdIntensity.de(Side.WHITE, 0, Optional.empty(), tie),
                CrowdIntensity.de(Side.WHITE, 300, Optional.empty(), tie));
    }

    @Test
    void undoDownBecauseDependsOnlyOfState() {
        // Volver a la misma situación da el mismo valor: no hay nada acumulado
        assertEquals(intensity(Side.WHITE, 30, 0, 0), intensity(Side.WHITE, 30, 0, 0));
        assertTrue(intensity(Side.WHITE, 29, 0, 0) < intensity(Side.WHITE, 30, 0, 0));
    }
}
