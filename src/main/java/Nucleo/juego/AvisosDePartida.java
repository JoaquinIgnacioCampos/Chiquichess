package Nucleo.juego;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Los observadores de una partida (Observer): los adaptadores que quieren enterarse de lo que pasa.
 * Reciben los avisos en el orden en que se suscribieron.
 */
final class AvisosDePartida {

    private final List<ObservadorDePartida> observadores = new ArrayList<>();

    void agregar(ObservadorDePartida observador) {
        observadores.add(Objects.requireNonNull(observador, "observador"));
    }

    void quitar(ObservadorDePartida observador) {
        observadores.remove(observador);
    }

    void avisar(EventoDePartida evento) {
        // Copia: un observador puede desuscribirse mientras recibe el aviso.
        for (ObservadorDePartida observador : List.copyOf(observadores)) {
            observador.notificar(evento);
        }
    }
}
