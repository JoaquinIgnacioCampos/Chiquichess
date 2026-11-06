package Nucleo.tablero;


import Nucleo.piezas.Pieza;

import java.util.Map;
import java.util.Optional;

/**
 * Vista de solo lectura del tablero. Las reglas de movimiento, el detector de jaque
 * y los adaptadores de salida dependen de esta interfaz y no del Tablero concreto
 * (Principio de Segregación de Interfaces e Inversión de Dependencias).
 */
public interface ITablero {

    int ancho();

    int alto();

    /** true si la posición está dentro de los límites del tablero. */
    boolean contiene(Posicion posicion);

    /** Pieza en esa posición; vacío si la casilla está libre o fuera del tablero. */
    Optional<Pieza> piezaEn(Posicion posicion);

    /** Vista no modificable de todas las piezas y sus posiciones. */
    Map<Posicion, Pieza> piezas();

    /**
     * Casilla donde está la pelota del modo fútbol; vacío si en este tablero no se juega con pelota
     * (el ajedrez clásico). La pelota no es una pieza: no ocupa la casilla ni bloquea a nadie.
     */
    Optional<Posicion> pelota();
}