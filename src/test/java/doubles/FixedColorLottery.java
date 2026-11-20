package doubles;

import adapters.configuration.ColorLottery;

/** Stub con registro de uso: siempre da el mismo resultado y cuenta cuántas veces se lo consultó. */
public class FixedColorLottery implements ColorLottery {

    private final boolean player1White;
    private int queries = 0;

    private FixedColorLottery(boolean player1White) {
        this.player1White = player1White;
    }

    public static FixedColorLottery player1WithWhite() {
        return new FixedColorLottery(true);
    }

    public static FixedColorLottery player1WithBlack() {
        return new FixedColorLottery(false);
    }

    @Override
    public boolean player1PlaysWithWhite() {
        queries++;
        return player1White;
    }

    public int queries() {
        return queries;
    }
}
