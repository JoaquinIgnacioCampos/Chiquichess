package adapters;

import adapters.configuration.ColorLottery;

import java.util.concurrent.ThreadLocalRandom;

/** Sorteo real: cada color tiene la misma probabilidad. */
public class RandomColorLottery implements ColorLottery {
    @Override
    public boolean player1PlaysWithWhite() {
        return ThreadLocalRandom.current().nextBoolean();
    }
}
