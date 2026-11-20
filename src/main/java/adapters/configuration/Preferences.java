package adapters.configuration;

import java.util.Objects;

/**
 * Lo que se guarda de una vez para la otra: las opciones del menú y el sonido.
 *
 * @param volumen    último volumen audible elegido, de 0 a 1
 * @param silenciado si el sonido estaba silenciado
 */
public record Preferences(MenuSelection selection, double volume, boolean muted) {
    public static final double DEFAULT_VOLUME = adapters.audio.VolumeControl.DEFAULT_VOLUME;

    public Preferences {
        Objects.requireNonNull(selection, "selection");
        if (Double.isNaN(volume) || volume <= 0 || volume > 1) {
            throw new IllegalArgumentException("The volume must be between 0 (exclusive) and 1");
        }
    }

    public static Preferences byDefault() {
        return new Preferences(MenuSelection.usual(), DEFAULT_VOLUME, false);
    }
}
