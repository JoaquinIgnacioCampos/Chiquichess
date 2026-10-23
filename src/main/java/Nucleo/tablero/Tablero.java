package Nucleo.tablero;

import Nucleo.juego.Posicion;
import Nucleo.piezas.Pieza;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class Tablero implements ITablero {

    private final int ancho;
    private final int alto;
    private final Map<Posicion, Pieza> piezas = new HashMap<>();

    public Tablero(int ancho, int alto) {
        if (ancho <= 0 || alto <= 0) {
            throw new IllegalArgumentException("Las dimensiones del tablero deben ser positivas");
        }
        this.ancho = ancho;
        this.alto = alto;
    }

    private Tablero(Tablero otro) {
        this(otro.ancho, otro.alto);
        this.piezas.putAll(otro.piezas); // las piezas son inmutables: se pueden compartir
    }

    @Override
    public int ancho() {
        return ancho;
    }

    @Override
    public int alto() {
        return alto;
    }

    @Override
    public boolean contiene(Posicion posicion) {
        return posicion.columna() >= 0 && posicion.columna() < ancho
                && posicion.fila() >= 0 && posicion.fila() < alto;
    }

    @Override
    public Optional<Pieza> piezaEn(Posicion posicion) {
        return Optional.ofNullable(piezas.get(posicion));
    }

    @Override
    public Map<Posicion, Pieza> piezas() {
        return Collections.unmodifiableMap(piezas);
    }

    /** Coloca la pieza; si había otra en esa casilla, la reemplaza. */
    public void colocar(Posicion posicion, Pieza pieza) {
        if (!contiene(posicion)) {
            throw new IllegalArgumentException("Posición fuera del tablero: " + posicion);
        }
        piezas.put(posicion, pieza);
    }

    /** Retira y devuelve la pieza de esa casilla, si había alguna. */
    public Optional<Pieza> quitar(Posicion posicion) {
        return Optional.ofNullable(piezas.remove(posicion));
    }

    /** Copia independiente, útil para simular jugadas sin alterar el tablero real. */
    public Tablero copiar() {
        return new Tablero(this);
    }
}
