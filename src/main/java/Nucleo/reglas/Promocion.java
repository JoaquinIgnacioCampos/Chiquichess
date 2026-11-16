package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.Reemplazo;
import Nucleo.piezas.Alfil;
import Nucleo.piezas.Caballo;
import Nucleo.piezas.Pieza;
import Nucleo.piezas.Reina;
import Nucleo.piezas.Torre;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Promoción: el peón que llega a la última fila se convierte en otra pieza de su bando.
 *
 * No suma jugadas nuevas: transforma cada jugada de un peón que termina en la última fila (avance
 * o captura) en una variante por cada pieza posible, agregándole un {@link Reemplazo}. Así el peón ya
 * no puede quedarse como peón en la última fila. La primera opción es la que se usa si el jugador no
 * elige (la dama, en el ajedrez clásico).
 */
public class Promocion implements ReglaEspecial {

    private final List<Function<Bando, Pieza>> opciones;

    /** Ajedrez clásico: dama, torre, alfil o caballo, en ese orden. */
    public Promocion() {
        this(List.of(Reina::new, Torre::new, Alfil::new, Caballo::new));
    }

    /** @param opciones fábricas de las piezas a las que se puede promover; la primera es la elección por defecto */
    public Promocion(List<Function<Bando, Pieza>> opciones) {
        this.opciones = List.copyOf(Objects.requireNonNull(opciones, "opciones"));
        if (this.opciones.isEmpty()) {
            throw new IllegalArgumentException("Tiene que haber al menos una pieza para promover");
        }
    }

    @Override
    public List<Jugada> jugadas(Tablero tablero, Posicion origen, List<Jugada> historial, IDetectorDeJaque detector) {
        return List.of();
    }

    @Override
    public List<Jugada> transformar(Tablero tablero, Jugada jugada) {
        Pieza peon = jugada.piezaMovida();
        // Un peón que pasa la pelota (modo fútbol) no se mueve, así que no promueve.
        if (jugada.esPase() || !peon.esPeon() || jugada.destino().fila() != ultimaFila(tablero, peon.color())) {
            return List.of(jugada);
        }
        return opciones.stream()
                .map(fabrica -> jugada.con(new Reemplazo(peon, fabrica.apply(peon.color()), jugada.destino())))
                .toList();
    }

    private static int ultimaFila(ITablero tablero, Bando bando) {
        return bando == Bando.BLANCO ? tablero.alto() - 1 : 0;
    }
}
