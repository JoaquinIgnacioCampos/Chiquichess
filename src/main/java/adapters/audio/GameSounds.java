package adapters.audio;

import core.side.Side;
import core.game.GameEvent;
import core.game.GameObserver;
import core.game.GameQuery;
import core.rules.Scoreboard;
import adapters.audio.SoundScript.Cue;
import adapters.configuration.GameConfiguration.Team;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de sonido de una partida: escucha sus eventos (Observer), le pregunta al
 * {@link SoundScript} qué tiene que sonar, busca el archivo del club que corresponda en el
 * {@link SoundCatalog} y se lo pasa al {@link AudioPlayer}.
 *
 * En el modo fútbol mantiene además las dos hinchadas sonando en bucle, con el volumen que da
 * {@link CrowdIntensity} después de cada evento. Cuando la partida termina, las hinchadas se
 * callan y queda el sonido del final; si se deshace ese final, vuelven.
 */
public final class GameSounds implements GameObserver, AutoCloseable {
    /** El clic de seleccionar una pieza es más suave que un movimiento. */
    static final double VOLUME_SELECTION = 0.6;
    /** Las hinchadas suenan más bajo que los efectos, para que no los tapen. */
    static final double VOLUME_CROWD = 0.6;

    private final GameQuery game;
    private final SoundCatalog catalog;
    private final AudioPlayer player;
    private final Map<Side, Team> teams;
    private final VolumeControl control;
    private final SoundScript script;
    private final boolean football;
    private final Map<Side, AudioPlayer.Track> crowds = new EnumMap<>(Side.class);
    private final Runnable onChangeVolume = this::updateCrowds;
    private boolean closed = false;

    private GameSounds(GameQuery game, SoundCatalog catalog, AudioPlayer player,
                             Map<Side, Team> teams, VolumeControl control, boolean football, Optional<Side> person) {
        this.game = Objects.requireNonNull(game, "partida");
        this.catalog = Objects.requireNonNull(catalog, "catalogo");
        this.player = Objects.requireNonNull(player, "reproductor");
        this.teams = Map.copyOf(Objects.requireNonNull(teams, "equipos"));
        this.control = Objects.requireNonNull(control, "control");
        this.football = football;
        this.script = new SoundScript(football, person);
    }

    /**
     * Engancha el sonido a la partida: la escucha desde ahora, toca lo que suena al empezar y, en el
     * modo fútbol, pone a cantar a las hinchadas. Cerrarlo lo desengancha.
     *
     * @param teams el club de cada bando, para los sonidos propios de cada uno
     */
    public static GameSounds connect(GameQuery game, SoundCatalog catalog, AudioPlayer player,
                                            Map<Side, Team> teams, VolumeControl control, boolean football) {
        return connect(game, catalog, player, teams, control, football, Optional.empty());
    }

    /**
     * Igual que la anterior, sabiendo el bando de la persona cuando juega contra la IA: si pierde, suena la
     * derrota en lugar de la victoria del rival.
     */
    public static GameSounds connect(GameQuery game, SoundCatalog catalog, AudioPlayer player,
                                            Map<Side, Team> teams, VolumeControl control, boolean football,
                                            Optional<Side> person) {
        GameSounds sounds = new GameSounds(game, catalog, player, teams, control, football, person);
        game.addObserver(sounds);
        control.onChange(sounds.onChangeVolume);
        sounds.script.onStart().forEach(sounds::play);
        sounds.updateCrowds();
        return sounds;
    }

    @Override
    public void notify(GameEvent event) {
        if (closed) return;
        script.para(event).forEach(this::play);
        updateCrowds();
    }

    /**
     * La persona tocó una pieza suya para moverla. No es un evento de la partida (la partida no cambió):
     * lo avisa la vista.
     */
    public void selectedPiece() {
        if (closed) return;
        catalog.search(Sound.SELECTION, Optional.empty())
                .ifPresent(file -> player.play(file, control.effective() * VOLUME_SELECTION));
    }

    private void play(Cue cue) {
        catalog.search(cue.sound(), cue.side().map(teams::get))
                .ifPresent(file -> player.play(file, control.effective()));
    }

    private void updateCrowds() {
        if (!football || closed) return;
        if (game.state().isFinished()) {
            stopCrowds();
            return;
        }
        Map<Side, URL> files = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            catalog.search(Sound.CROWD, Optional.ofNullable(teams.get(side)))
                    .ifPresent(file -> files.put(side, file));
        }
        // Si los dos clubes usan el mismo archivo, suena una sola hinchada: dos copias iguales se pisan.
        boolean shared = files.size() == 2 && files.get(Side.WHITE).toExternalForm()
                .equals(files.get(Side.BLACK).toExternalForm());
        if (shared) files.remove(Side.BLACK);
        for (Side side : Side.values()) {
            URL file = files.get(side);
            if (file == null) {
                Optional.ofNullable(crowds.remove(side)).ifPresent(AudioPlayer.Track::stop);
                continue;
            }
            double intensity = shared
                    ? Math.max(intensity(Side.WHITE), intensity(Side.BLACK))
                    : intensity(side);
            double volume = control.effective() * VOLUME_CROWD * intensity;
            AudioPlayer.Track track = crowds.get(side);
            if (track == null) {
                crowds.put(side, player.inLoop(file, volume));
            } else {
                track.volume(volume);
            }
        }
    }

    private double intensity(Side side) {
        return CrowdIntensity.de(side, game.history().size(), game.moveLimit(),
                game.scoreboard().orElse(new Scoreboard(0, 0)));
    }

    private void stopCrowds() {
        crowds.values().forEach(AudioPlayer.Track::stop);
        crowds.clear();
    }

    /** Desengancha el sonido de la partida y calla a las hinchadas. No cierra el reproductor, que es compartido. */
    @Override
    public void close() {
        if (closed) return;
        stopCrowds();
        closed = true;
        game.removeObserver(this);
        control.stopListening(onChangeVolume);
    }
}
