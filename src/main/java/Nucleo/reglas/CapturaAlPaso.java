package Nucleo.reglas;

import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.Retiro;
import Nucleo.jugadas.Traslado;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.List;
import java.util.Optional;

/**
 * Captura al paso: si en la jugada anterior un peón rival avanzó dos casillas y quedó al lado de un
 * peón propio, ese peón puede capturarlo moviéndose en diagonal a la casilla que el rival "saltó".
 * Solo vale en la jugada inmediata siguiente.
 *
 * La pieza capturada no está en la casilla de destino, sino al lado del peón que captura: la jugada
 * tiene un Retiro en otra casilla, cosa que una Jugada admite sin cambios. Que no deje al propio rey en
 * jaque (por ejemplo, si al sacar los dos peones de la fila el rey queda expuesto) lo controla
 * MovimientosLegales simulando la jugada completa.
 */
public class CapturaAlPaso implements ReglaEspecial {

    @Override
    public List<Jugada> jugadas(Tablero tablero, Posicion origen, List<Jugada> historial, IDetectorDeJaque detector) {
        Optional<Pieza> candidata = tablero.piezaEn(origen);
        if (candidata.isEmpty() || !candidata.get().esPeon() || historial.isEmpty()) {
            return List.of();
        }
        Pieza peon = candidata.get();
        Optional<Traslado> avanceDoble = avanceDobleDeUnPeonRival(historial.get(historial.size() - 1), peon);
        if (avanceDoble.isEmpty()) {
            return List.of();
        }
        Posicion rival = avanceDoble.get().destino();
        if (rival.fila() != origen.fila() || Math.abs(rival.columna() - origen.columna()) != 1) {
            return List.of();
        }
        Posicion destino = new Posicion(rival.columna(), origen.fila() + peon.color().direccionAvance());
        if (!tablero.contiene(destino) || tablero.piezaEn(destino).isPresent()) {
            return List.of();
        }
        return List.of(new Jugada(origen, destino, List.of(
                new Retiro(avanceDoble.get().pieza(), rival),
                new Traslado(peon, origen, destino))));
    }

    /** El traslado de la última jugada, si fue un peón del otro bando que avanzó dos casillas. */
    private static Optional<Traslado> avanceDobleDeUnPeonRival(Jugada ultima, Pieza peon) {
        Pieza movida = ultima.piezaMovida();
        if (!movida.esPeon() || movida.color() == peon.color()) {
            return Optional.empty();
        }
        return ultima.efectos().stream()
                .filter(efecto -> efecto instanceof Traslado t && t.pieza() == movida)
                .map(efecto -> (Traslado) efecto)
                .filter(t -> t.origen().columna() == t.destino().columna()
                        && Math.abs(t.destino().fila() - t.origen().fila()) == 2)
                .findFirst();
    }
}
