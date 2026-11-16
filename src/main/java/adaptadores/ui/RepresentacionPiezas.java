package adaptadores.ui;

import Nucleo.piezas.Pieza;
import javafx.scene.Node;

/** Traduce pieza y bando a un nodo visual; las vistas no saben si es un símbolo o un sprite. */
public interface RepresentacionPiezas {
    /** Lado de la casilla para el que se dibuja cada figura: el tablero las escala a partir de este valor. */
    double LADO_BASE = 80;

    Node crear(Pieza pieza, TemaTablero tema);
}
