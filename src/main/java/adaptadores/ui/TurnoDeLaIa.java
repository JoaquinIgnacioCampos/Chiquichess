package adaptadores.ui;

import Nucleo.juego.SituacionDePartida;
import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.juego.ObservadorDePartida;
import Nucleo.juego.Movimiento;
import Nucleo.juego.PartidaEnJuego;
import adaptadores.RivalArtificial;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Hace jugar a la IA en la ventana sin trabarla. Cuando le toca, toma una copia de la situación en el
 * hilo de la interfaz, piensa en otro hilo y vuelve al hilo de la interfaz para jugar con
 * {@code Partida.mover}. Así el tablero, los relojes y los botones siguen respondiendo mientras piensa.
 *
 * Se entera de que le toca escuchando los eventos de la partida. Si la partida cambia mientras piensa
 * (la persona deshizo, se acabó el tiempo, se volvió al menú), lo pensado se descarta.
 *
 * Mueve recién cuando pasó el tiempo que tardaría una persona de su nivel (la demora de la
 * {@link RivalArtificial.Respuesta}): una respuesta instantánea se siente como un salto, no como un rival.
 *
 * Los hilos se reciben por constructor: en los tests, los dos son "ejecutar ya", y todo pasa en orden.
 */
public final class TurnoDeLaIa implements AutoCloseable {
    private final PartidaEnJuego partida;
    private final RivalArtificial rival;
    private final Executor hiloDePensar;
    private final Consumer<Runnable> hiloDeLaInterfaz;
    /** Cambia con cada evento: si al terminar de pensar no es la misma, lo pensado ya no sirve. */
    private int version = 0;
    private boolean pensando = false;
    private boolean cerrado = false;
    private final ObservadorDePartida alCambiar = evento -> {
        version++;
        programar();
    };

    public TurnoDeLaIa(PartidaEnJuego partida, RivalArtificial rival, Executor hiloDePensar,
                       Consumer<Runnable> hiloDeLaInterfaz) {
        this.partida = Objects.requireNonNull(partida, "partida");
        this.rival = Objects.requireNonNull(rival, "rival");
        this.hiloDePensar = Objects.requireNonNull(hiloDePensar, "hiloDePensar");
        this.hiloDeLaInterfaz = Objects.requireNonNull(hiloDeLaInterfaz, "hiloDeLaInterfaz");
    }

    /** Empieza a escuchar la partida y, si la IA empieza (juega con blancas), se pone a pensar. */
    public void iniciar() {
        partida.agregarObservador(alCambiar);
        programar();
    }

    /** true mientras la IA está pensando una jugada. */
    public boolean pensando() {
        return pensando;
    }

    private void programar() {
        if (cerrado || pensando || !rival.leToca()) return;
        pensando = true;
        int pedido = version;
        // La copia se toma acá, en el hilo de la interfaz; el otro hilo nunca toca la partida.
        SituacionDePartida situacion = partida.situacion();
        var tiempoRestante = partida.tiempoRestante(rival.bando());
        hiloDePensar.execute(() -> {
            long inicio = System.nanoTime();
            Movimiento elegido = null;
            try {
                RivalArtificial.Respuesta respuesta = rival.pensar(situacion, tiempoRestante);
                elegido = respuesta.movimiento();
                esperarHasta(inicio, respuesta.demora());
            } catch (RuntimeException fallo) {
                // Sin jugada (no debería pasar: si no hay jugadas, la partida ya terminó).
            }
            Movimiento jugada = elegido;
            hiloDeLaInterfaz.accept(() -> jugar(jugada, pedido));
        });
    }

    private void jugar(Movimiento jugada, int pedido) {
        pensando = false;
        if (cerrado) return;
        if (jugada == null || pedido != version || !rival.leToca()) {
            programar(); // La partida cambió mientras pensaba: si le sigue tocando, vuelve a pensar.
            return;
        }
        try {
            partida.mover(jugada); // El evento de la jugada redibuja la vista y hace sonar el movimiento.
        } catch (MovimientoInvalidoException imposible) {
            programar();
        }
    }

    /** Espera hasta que pase la demora desde que empezó a pensar (si pensó más, no espera nada). */
    private static void esperarHasta(long inicio, Duration demora) {
        long falta = demora.toNanos() - (System.nanoTime() - inicio);
        if (falta <= 0) return;
        try {
            Thread.sleep(Duration.ofNanos(falta));
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
        }
    }

    /** Deja de escuchar la partida; si estaba pensando, lo que piense se descarta. */
    @Override
    public void close() {
        cerrado = true;
        partida.quitarObservador(alCambiar);
    }
}
