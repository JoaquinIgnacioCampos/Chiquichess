package dobles;

import Nucleo.reglas.CondicionDeFinalizacion;
import Nucleo.reglas.ContextoDeJugada;
import Nucleo.reglas.EstadoPartida;

import java.util.Optional;

/** Stub: responde siempre lo mismo, sin importar el contexto que reciba. */
public class StubCondicion implements CondicionDeFinalizacion {

    private final Optional<EstadoPartida> respuesta;

    private StubCondicion(Optional<EstadoPartida> respuesta) {
        this.respuesta = respuesta;
    }

    /** Condición que siempre termina la partida con ese estado. */
    public static StubCondicion siempre(EstadoPartida estado) {
        return new StubCondicion(Optional.of(estado));
    }

    /** Condición que nunca se cumple. */
    public static StubCondicion nunca() {
        return new StubCondicion(Optional.empty());
    }

    @Override
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        return respuesta;
    }
}
