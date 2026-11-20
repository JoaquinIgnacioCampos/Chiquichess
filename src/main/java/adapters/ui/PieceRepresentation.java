package adapters.ui;

import core.pieces.Piece;
import javafx.scene.Node;

/** Traduce pieza y bando a un nodo visual; las vistas no saben si es un símbolo o un sprite. */
public interface PieceRepresentation {
    /** Lado de la casilla para el que se dibuja cada figura: el tablero las escala a partir de este valor. */
    double SIDE_BASE = 80;

    Node create(Piece piece, BoardTheme theme);
}
