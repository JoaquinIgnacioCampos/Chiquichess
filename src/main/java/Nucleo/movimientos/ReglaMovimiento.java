package Nucleo.movimientos;

import Nucleo.tablero.ITablero;
import java.util.Set;

import Nucleo.tablero.Posicion;
import Nucleo.bando.Bando;
/**
 * Estrategia de movimiento. Es la pieza clave de la composición: los movimientos
 * base (deslizamiento, salto) y los modificadores (SoloSiVacio, SoloSiCaptura, SoloSi)
 * implementan esta interfaz y se combinan entre sí (Composite + Decorator).
 *
 * Se distinguen dos conceptos porque no siempre coinciden:
 *  - destinosPosibles: a dónde puede ir la pieza (incluye capturas).
 *  - casillasAtacadas: qué casillas amenaza. Es lo que se usa para detectar jaque.
 *    Ejemplo: el peón avanza a casillas vacías pero ataca las diagonales.
 */
public interface ReglaMovimiento {

    Set<Posicion> destinosPosibles(ITablero tablero, Posicion origen, Bando color);

    Set<Posicion> casillasAtacadas(ITablero tablero, Posicion origen, Bando color);
}
