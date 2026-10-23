package Nucleo.juego;

/**
 * Coordenada en el tablero (value object inmutable). Es la clave del Map del Tablero,
 * por eso al ser un record ya trae equals/hashCode correctos.
 * No valida límites: eso le corresponde al Tablero, que es quien conoce su tamaño.
 */
public record Posicion(int columna, int fila) {

    public Posicion desplazada(int deltaColumna, int deltaFila) {
        return new Posicion(columna + deltaColumna, fila + deltaFila);
    }
}
