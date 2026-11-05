package dobles;

import Nucleo.bando.Bando;
import Nucleo.reglas.IDetectorDeJaque;
import Nucleo.reglas.MovimientosLegales;
import Nucleo.tablero.Tablero;

/**
 * Stub: fija la respuesta de "¿existe alguna jugada legal?" sin recorrer el tablero.
 * El resto del comportamiento es el de MovimientosLegales con el detector indicado.
 */
public class StubMovimientosLegales extends MovimientosLegales {

    private final boolean existeAlguno;

    /** Usa un detector que nunca ve jaque. */
    public StubMovimientosLegales(boolean existeAlguno) {
        this(existeAlguno, StubDetectorDeJaque.nuncaEnJaque());
    }

    /** Partida exige que el detector sea el mismo que recibe ella: pasar acá esa misma instancia. */
    public StubMovimientosLegales(boolean existeAlguno, IDetectorDeJaque detector) {
        super(detector);
        this.existeAlguno = existeAlguno;
    }

    @Override
    public boolean existeAlguno(Tablero tablero, Bando color) {
        return existeAlguno;
    }
}
