package Nucleo.jugadas;

import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Command: una jugada ya resuelta, con todo lo necesario para ejecutarla y deshacerla.
 * Origen y destino son la intención del jugador (la casilla que tocó y a la que fue); los efectos
 * dicen qué cambia en el tablero. Una jugada normal es [Retiro?, Traslado]; las jugadas especiales
 * usan la misma clase con otros efectos, sin modificar Jugada ni Partida.
 *
 * Guarda posiciones y no un Movimiento porque Movimiento pertenece al paquete juego, que depende
 * de este: usarlo acá crearía un ciclo entre paquetes.
 */
public record Jugada(Posicion origen, Posicion destino, List<Efecto> efectos) {

    public Jugada {
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
        efectos = List.copyOf(Objects.requireNonNull(efectos, "efectos"));
        if (efectos.stream().noneMatch(e -> e instanceof Traslado t && t.origen().equals(origen))) {
            throw new IllegalArgumentException("La jugada debe trasladar la pieza que está en " + origen);
        }
    }

    /** Jugada común: la pieza de origen va a destino y, si había una pieza rival ahí, la captura. */
    public static Jugada normal(ITablero tablero, Posicion origen, Posicion destino) {
        Pieza pieza = tablero.piezaEn(origen)
                .orElseThrow(() -> new IllegalArgumentException("No hay ninguna pieza en " + origen));
        List<Efecto> efectos = new ArrayList<>();
        tablero.piezaEn(destino).ifPresent(capturada -> efectos.add(new Retiro(capturada, destino)));
        efectos.add(new Traslado(pieza, origen, destino));
        return new Jugada(origen, destino, efectos);
    }

    /** Pieza que el jugador movió: la que se traslada desde el origen. */
    public Pieza piezaMovida() {
        return efectos.stream()
                .filter(e -> e instanceof Traslado t && t.origen().equals(origen))
                .map(e -> ((Traslado) e).pieza())
                .findFirst()
                .orElseThrow();
    }

    /** Pieza capturada, si la hubo. */
    public Optional<Pieza> capturada() {
        return efectos.stream()
                .filter(e -> e instanceof Retiro)
                .map(e -> ((Retiro) e).pieza())
                .findFirst();
    }

    /** Aplica los efectos en orden. Si uno falla, revierte los anteriores y deja el tablero como estaba. */
    public void aplicar(Tablero tablero) {
        List<Efecto> aplicados = new ArrayList<>();
        try {
            for (Efecto efecto : efectos) {
                efecto.aplicar(tablero);
                aplicados.add(efecto);
            }
        } catch (RuntimeException fallo) {
            for (int i = aplicados.size() - 1; i >= 0; i--) {
                aplicados.get(i).revertir(tablero);
            }
            throw fallo;
        }
    }

    /** Deshace los efectos en orden inverso. Si uno falla, vuelve a aplicar los ya revertidos. */
    public void revertir(Tablero tablero) {
        List<Efecto> revertidos = new ArrayList<>();
        try {
            for (int i = efectos.size() - 1; i >= 0; i--) {
                efectos.get(i).revertir(tablero);
                revertidos.add(efectos.get(i));
            }
        } catch (RuntimeException fallo) {
            for (int i = revertidos.size() - 1; i >= 0; i--) {
                revertidos.get(i).aplicar(tablero);
            }
            throw fallo;
        }
    }
}
