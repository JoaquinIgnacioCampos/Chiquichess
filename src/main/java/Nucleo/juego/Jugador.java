package Nucleo.juego;

import Nucleo.bando.Bando;

import java.util.Objects;

public record Jugador(String nombre, Bando color) {

    public Jugador {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(color, "color");
    }
}

