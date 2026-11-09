package adaptadores.ui;

import Nucleo.bando.Bando;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.Objects;

/**
 * Pulso visual: la fuente y el cálculo de tiempo siguen siendo responsabilidad del núcleo. Si el tiempo
 * se agota, la partida avisa el final y la vista se redibuja por ese evento; acá solo se deja de latir.
 */
public final class ActualizadorRelojJavaFx implements AutoCloseable {
    private final ControladorTablero controlador;
    private final Runnable actualizarRelojes;
    private final Timeline actualizacion;

    public ActualizadorRelojJavaFx(ControladorTablero controlador, Runnable actualizarRelojes) {
        this.controlador = Objects.requireNonNull(controlador, "controlador");
        this.actualizarRelojes = Objects.requireNonNull(actualizarRelojes, "actualizarRelojes");
        actualizacion = new Timeline(new KeyFrame(Duration.millis(100), evento -> actualizar()));
        actualizacion.setCycleCount(Animation.INDEFINITE);
    }

    /** Se invoca en el hilo JavaFX al mostrar la ventana. */
    public void iniciar() {
        detener();
        actualizar();
        boolean tieneReloj = controlador.tiempoRestante(Bando.BLANCO).isPresent()
                || controlador.tiempoRestante(Bando.NEGRO).isPresent();
        if (!controlador.estado().estaFinalizada() && tieneReloj) actualizacion.playFromStart();
    }

    private void actualizar() {
        controlador.verificarTiempo();
        actualizarRelojes.run();
        if (controlador.estado().estaFinalizada()) detener();
    }

    public void detener() {
        actualizacion.stop();
    }

    @Override public void close() {
        detener();
    }
}
