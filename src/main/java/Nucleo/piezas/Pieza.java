package Nucleo.piezas;

import Nucleo.tablero.ITablero;
import Nucleo.juego.Posicion;
import Nucleo.movimientos.ReglaMovimiento;

import java.util.Objects;
import java.util.Set;

/**
 * Una pieza es un color más una regla de movimiento (composición sobre herencia).
 * Las subclases concretas solo eligen cómo componer su regla; para agregar una pieza
 * nueva se crea una clase nueva sin tocar ninguna existente (Open/Closed).
 * Las piezas son inmutables y no conocen su posición.
 */
public abstract class Pieza {

    private final Color color;
    private final ReglaMovimiento regla;

    protected Pieza(Color color, ReglaMovimiento regla) {
        this.color = Objects.requireNonNull(color, "color");
        this.regla = Objects.requireNonNull(regla, "regla");
    }

    public Color color() {
        return color;
    }

    /** Destinos según las reglas propias de la pieza (sin considerar si deja al rey en jaque). */
    public Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen) {
        return regla.destinosPosibles(tablero, origen, color);
    }

    /** Casillas que la pieza amenaza desde esa posición. */
    public Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen) {
        return regla.casillasAtacadas(tablero, origen, color);
    }

    /** Las piezas que son el rey sobreescriben esto; lo usa el detector de jaque. */
    public boolean esRey() {
        return false;
    }

    /** Los peones sobreescriben esto; lo usa la regla de los 50 movimientos (un movimiento de peón es "progreso"). */
    public boolean esPeon() {
        return false;
    }

    public abstract String nombre();

    /** Letra que identifica a la pieza (P, T, C, A, D, R). */
    public abstract char codigo();
}