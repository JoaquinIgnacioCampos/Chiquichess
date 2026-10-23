package Nucleo.juego;

import Nucleo.piezas.Color;
import java.util.Objects;

public record Jugador(String nombre, Color color) {

    public Jugador {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(color, "color");
    }
}

