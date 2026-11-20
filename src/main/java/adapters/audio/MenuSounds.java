package adapters.audio;

import java.util.Objects;
import java.util.Optional;

/**
 * Sonido de la pantalla de selección: la risa del personaje la primera vez que se abre el juego y un
 * ambiente suave en bucle mientras se eligen las opciones. El ambiente se calla al empezar una partida
 * y vuelve al volver al menú. Sigue el volumen y el silencio del {@link ControlDeVolumen}, el mismo
 * de la partida.
 *
 * Como el resto del audio, si falta un archivo ese sonido no suena y el juego sigue igual.
 */
public final class MenuSounds implements AutoCloseable {
    /** El ambiente acompaña: suena bastante más bajo que los efectos. */
    static final double VOLUME_AMBIENT = 0.35;

    private final SoundCatalog catalog;
    private final AudioPlayer player;
    private final VolumeControl control;
    private final Runnable onChangeVolume = this::adjustVolume;
    private Optional<AudioPlayer.Track> ambient = Optional.empty();
    private boolean laughed = false;

    public MenuSounds(SoundCatalog catalog, AudioPlayer player, VolumeControl control) {
        this.catalog = Objects.requireNonNull(catalog, "catalogo");
        this.player = Objects.requireNonNull(player, "reproductor");
        this.control = Objects.requireNonNull(control, "control");
        control.onChange(onChangeVolume);
    }

    /** Al abrir el juego: la risa (solo la primera vez) y el ambiente del menú. */
    public void openGame() {
        if (!laughed) {
            laughed = true;
            catalog.search(Sound.LAUGH, Optional.empty())
                    .ifPresent(file -> player.play(file, control.effective()));
        }
        enterMenu();
    }

    /** La persona tocó una opción del menú: el mismo clic suave que al elegir una pieza. */
    public void chosenOption() {
        catalog.search(Sound.SELECTION, Optional.empty()).ifPresent(file ->
                player.play(file, control.effective() * GameSounds.VOLUME_SELECTION));
    }

    /** Empieza el ambiente, si no estaba sonando. */
    public void enterMenu() {
        if (ambient.isPresent()) return;
        ambient = catalog.search(Sound.AMBIENT_MENU, Optional.empty())
                .map(file -> player.inLoop(file, ambientVolume()));
    }

    /** Calla el ambiente: empieza una partida. */
    public void leaveMenu() {
        ambient.ifPresent(AudioPlayer.Track::stop);
        ambient = Optional.empty();
    }

    private void adjustVolume() {
        ambient.ifPresent(track -> track.volume(ambientVolume()));
    }

    private double ambientVolume() {
        return control.effective() * VOLUME_AMBIENT;
    }

    @Override
    public void close() {
        leaveMenu();
        control.stopListening(onChangeVolume);
    }
}
