package core.game;

import core.side.Side;

import java.util.Objects;

public record Player(String displayName, Side color) {

    public Player {
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(color, "color");
    }
}

