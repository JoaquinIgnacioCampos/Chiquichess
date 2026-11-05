package dobles;

import Nucleo.reglas.IDetectorDeJaque;
import Nucleo.tablero.ITablero;

import Nucleo.bando.Bando;
/** Stub: responde siempre "en jaque" o siempre "sin jaque", sin analizar el tablero. */
public class StubDetectorDeJaque implements IDetectorDeJaque {

    private final boolean respuesta;

    private StubDetectorDeJaque(boolean respuesta) {
        this.respuesta = respuesta;
    }

    public static StubDetectorDeJaque siempreEnJaque() {
        return new StubDetectorDeJaque(true);
    }

    public static StubDetectorDeJaque nuncaEnJaque() {
        return new StubDetectorDeJaque(false);
    }

    @Override
    public boolean estaEnJaque(ITablero tablero, Bando color) {
        return respuesta;
    }
}
