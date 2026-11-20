package core.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Los observadores de una partida (Observer): los adaptadores que quieren enterarse de lo que pasa.
 * Reciben los avisos en el orden en que se suscribieron.
 */
final class GameNotices {

    private final List<GameObserver> observers = new ArrayList<>();

    void add(GameObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observador"));
    }

    void remove(GameObserver observer) {
        observers.remove(observer);
    }

    void inform(GameEvent event) {
        // Copia: un observador puede desuscribirse mientras recibe el aviso.
        for (GameObserver observer : List.copyOf(observers)) {
            observer.notify(event);
        }
    }
}
