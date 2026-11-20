package adapters.ui;

import core.game.GameSituation;
import core.game.InvalidMoveException;
import core.game.GameObserver;
import core.game.MoveRequest;
import core.game.GameInProgress;
import adapters.ArtificialRival;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Hace jugar a la IA en la ventana sin trabarla. Cuando le toca, toma una copia de la situación en el
 * hilo de la interfaz, piensa en otro hilo y vuelve al hilo de la interfaz para jugar con
 * {@code Game.move}. Así el tablero, los relojes y los botones siguen respondiendo mientras piensa.
 *
 * Se entera de que le toca escuchando los eventos de la partida. Si la partida cambia mientras piensa
 * (la persona deshizo, se acabó el tiempo, se volvió al menú), lo pensado se descarta.
 *
 * Mueve recién cuando pasó el tiempo que tardaría una persona de su nivel (la demora de la
 * {@link ArtificialRival.Response}): una respuesta instantánea se siente como un salto, no como un rival.
 *
 * Los hilos se reciben por constructor: en los tests, los dos son "ejecutar ya", y todo pasa en orden.
 */
public final class AiTurn implements AutoCloseable {
    private final GameInProgress game;
    private final ArtificialRival rival;
    private final Executor thinkingThread;
    private final Consumer<Runnable> interfaceThread;
    /** Cambia con cada evento: si al terminar de pensar no es la misma, lo pensado ya no sirve. */
    private int version = 0;
    private boolean thinking = false;
    private boolean closed = false;
    private final GameObserver onChange = event -> {
        version++;
        schedule();
    };

    public AiTurn(GameInProgress game, ArtificialRival rival, Executor thinkingThread,
                       Consumer<Runnable> interfaceThread) {
        this.game = Objects.requireNonNull(game, "partida");
        this.rival = Objects.requireNonNull(rival, "rival");
        this.thinkingThread = Objects.requireNonNull(thinkingThread, "hiloDePensar");
        this.interfaceThread = Objects.requireNonNull(interfaceThread, "hiloDeLaInterfaz");
    }

    /** Empieza a escuchar la partida y, si la IA empieza (juega con blancas), se pone a pensar. */
    public void start() {
        game.addObserver(onChange);
        schedule();
    }

    /** true mientras la IA está pensando una jugada. */
    public boolean thinking() {
        return thinking;
    }

    private void schedule() {
        if (closed || thinking || !rival.himPlays()) return;
        thinking = true;
        int request = version;
        // La copia se toma acá, en el hilo de la interfaz; el otro hilo nunca toca la partida.
        GameSituation situation = game.situation();
        var timeRemaining = game.timeRemaining(rival.side());
        thinkingThread.execute(() -> {
            long start = System.nanoTime();
            MoveRequest chosen = null;
            try {
                ArtificialRival.Response response = rival.think(situation, timeRemaining);
                chosen = response.movement();
                waitUntil(start, response.delay());
            } catch (RuntimeException failure) {
                // Sin jugada (no debería pasar: si no hay jugadas, la partida ya terminó).
            }
            MoveRequest move = chosen;
            interfaceThread.accept(() -> play(move, request));
        });
    }

    private void play(MoveRequest move, int request) {
        thinking = false;
        if (closed) return;
        if (move == null || request != version || !rival.himPlays()) {
            schedule(); // La partida cambió mientras pensaba: si le sigue tocando, vuelve a pensar.
            return;
        }
        try {
            game.move(move); // El evento de la jugada redibuja la vista y hace sonar el movimiento.
        } catch (InvalidMoveException impossible) {
            schedule();
        }
    }

    /** Espera hasta que pase la demora desde que empezó a pensar (si pensó más, no espera nada). */
    private static void waitUntil(long start, Duration delay) {
        long missing = delay.toNanos() - (System.nanoTime() - start);
        if (missing <= 0) return;
        try {
            Thread.sleep(Duration.ofNanos(missing));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** Deja de escuchar la partida; si estaba pensando, lo que piense se descarta. */
    @Override
    public void close() {
        closed = true;
        game.removeObserver(onChange);
    }
}
