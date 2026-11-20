package adapters.ui;

import core.side.Side;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.Objects;

/**
 * Pulso visual: la fuente y el cálculo de tiempo siguen siendo responsabilidad del núcleo. Si el tiempo
 * se agota, la partida avisa el final y la vista se redibuja por ese evento; acá solo se deja de latir.
 */
public final class JavaFxClockUpdater implements AutoCloseable {
    private final BoardController controller;
    private final Runnable updateClocks;
    private final Timeline update;

    public JavaFxClockUpdater(BoardController controller, Runnable updateClocks) {
        this.controller = Objects.requireNonNull(controller, "controlador");
        this.updateClocks = Objects.requireNonNull(updateClocks, "actualizarRelojes");
        update = new Timeline(new KeyFrame(Duration.millis(100), event -> update()));
        update.setCycleCount(Animation.INDEFINITE);
    }

    /** Se invoca en el hilo JavaFX al mostrar la ventana. */
    public void start() {
        stop();
        update();
        boolean hasClock = controller.timeRemaining(Side.WHITE).isPresent()
                || controller.timeRemaining(Side.BLACK).isPresent();
        if (!controller.state().isFinished() && hasClock) update.playFromStart();
    }

    private void update() {
        controller.verifyTime();
        updateClocks.run();
        if (controller.state().isFinished()) stop();
    }

    public void stop() {
        update.stop();
    }

    @Override public void close() {
        stop();
    }
}
