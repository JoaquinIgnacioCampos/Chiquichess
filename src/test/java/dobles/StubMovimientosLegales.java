package dobles;

import Nucleo.piezas.Color;
import Nucleo.reglas.MovimientosLegales;
import Nucleo.tablero.Tablero;

/**
 * Stub: fija la respuesta de "¿existe alguna jugada legal?" sin recorrer el tablero.
 * El resto del comportamiento es el de MovimientosLegales con un detector que nunca ve jaque.
 */
public class StubMovimientosLegales extends MovimientosLegales {

    private final boolean existeAlguno;

    public StubMovimientosLegales(boolean existeAlguno) {
        super(StubDetectorDeJaque.nuncaEnJaque());
        this.existeAlguno = existeAlguno;
    }

    @Override
    public boolean existeAlguno(Tablero tablero, Color color) {
        return existeAlguno;
    }
}
