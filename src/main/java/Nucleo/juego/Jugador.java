package Nucleo.juego;

import java.util.Objects;

import Nucleo.bando.Bando;
public record Jugador(String nombre, Bando color) {

    public Jugador {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(color, "color");
    }
}

