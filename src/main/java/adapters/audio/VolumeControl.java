package adapters.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Volumen general y silencio que eligió el usuario. Vive mientras el programa está abierto, así se
 * conserva de una partida a la siguiente. Avisa a quien lo escuche cuando cambia.
 *
 * Silenciar y llevar el volumen a cero son lo mismo: subirlo desde cero reactiva el sonido, y reactivarlo
 * con el silencio recupera el último volumen que se usó.
 */
public final class VolumeControl {
    /** Moderado: la risa del personaje suena apenas se abre el juego, antes de que se pueda bajar. */
    public static final double DEFAULT_VOLUME = 0.5;

    private double volume = DEFAULT_VOLUME;
    private boolean muted = false;
    private final List<Runnable> listeners = new ArrayList<>();

    /** Último volumen audible elegido, de 0 a 1: se conserva mientras está silenciado. */
    public double volume() {
        return volume;
    }

    public boolean muted() {
        return muted;
    }

    /** Lo que efectivamente hay que aplicar: 0 si está silenciado. */
    public double effective() {
        return muted ? 0 : volume;
    }

    /** Cero silencia; un valor mayor fija el volumen y reactiva el sonido si estaba silenciado. */
    public void changeVolume(double newValue) {
        if (Double.isNaN(newValue)) throw new IllegalArgumentException("Volumen inválido");
        double adjusted = Math.max(0, Math.min(1, newValue));
        if (adjusted == 0) {
            mute(true);
            return;
        }
        if (!muted && adjusted == volume) return;
        volume = adjusted;
        muted = false;
        inform();
    }

    public void mute(boolean mute) {
        if (mute == muted) return;
        muted = mute;
        inform();
    }

    public void onChange(Runnable listener) {
        listeners.add(Objects.requireNonNull(listener, "oyente"));
    }

    public void stopListening(Runnable listener) {
        listeners.remove(listener);
    }

    private void inform() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
