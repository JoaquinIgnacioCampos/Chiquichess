package adapters.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reproductor real con Java Sound ({@code javax.sound.sampled}), que viene con el JDK: no hace falta
 * ninguna dependencia. Entiende WAV, AIFF y AU.
 *
 * Todo el trabajo (leer archivos, abrir líneas) se hace en un hilo propio, así la interfaz no se traba
 * mientras carga una hinchada larga. Cada archivo se lee una sola vez y queda en memoria.
 *
 * Si la máquina no tiene salida de audio, avisa una vez por la consola de errores y sigue en silencio.
 * Si un archivo no se puede leer, avisa una vez por ese archivo y no lo vuelve a intentar.
 */
public final class JavaSoundPlayer implements AudioPlayer {

    /** Audio ya leído de un archivo, listo para abrir en una línea. */
    private record Loaded(AudioFormat format, byte[] data) {
    }

    /**
     * Por debajo de esto no se oye. Para callar una pista se corta su línea en vez de bajarle la ganancia:
     * la ganancia se aplica al audio que todavía no se mandó, y la línea guarda cerca de un segundo ya
     * mandado, que seguiría sonando.
     */
    private static final double SILENCE = 0.0001;

    private final ExecutorService thread = Executors.newSingleThreadExecutor(task -> {
        Thread t = new Thread(task, "sound");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, Optional<Loaded>> loadedItems = new ConcurrentHashMap<>();
    private final Set<Clip> open = ConcurrentHashMap.newKeySet();
    private volatile boolean noAudioOutput = false;

    @Override
    public void play(URL file, double volume) {
        Objects.requireNonNull(file, "file");
        if (volume <= SILENCE) return;
        execute(() -> open(file, volume).ifPresent(clip -> {
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) close(clip);
            });
            clip.start();
        }));
    }

    @Override
    public Track inLoop(URL file, double volume) {
        Objects.requireNonNull(file, "file");
        LoopingTrack track = new LoopingTrack();
        execute(() -> {
            if (track.stopped) return;
            open(file, volume).ifPresent(clip -> {
                track.clip = clip;
                if (volume > SILENCE) {
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                } else {
                    track.paused = true;
                }
            });
        });
        return track;
    }

    @Override
    public void close() {
        thread.shutdownNow();
        open.forEach(this::close);
    }

    private final class LoopingTrack implements Track {
        /** Solo se toca desde el hilo de sonido. */
        private Clip clip;
        /** Cortada por estar en silencio; vuelve a sonar desde donde quedó. Solo se toca desde el hilo de sonido. */
        private boolean paused = false;
        private volatile boolean stopped = false;

        @Override
        public void volume(double volume) {
            execute(() -> {
                if (clip == null) return;
                if (volume <= SILENCE) {
                    if (paused) return;
                    clip.stop();
                    clip.flush();
                    paused = true;
                } else {
                    fixVolume(clip, volume);
                    if (paused) {
                        clip.loop(Clip.LOOP_CONTINUOUSLY);
                        paused = false;
                    }
                }
            });
        }

        @Override
        public void stop() {
            stopped = true;
            execute(() -> {
                if (clip != null) close(clip);
                clip = null;
            });
        }
    }

    private void execute(Runnable task) {
        if (thread.isShutdown()) return;
        try {
            thread.execute(task);
        } catch (java.util.concurrent.RejectedExecutionException closed) {
            // Se cerró el reproductor entre la pregunta y el pedido: ese sonido ya no hace falta.
        }
    }

    private Optional<Clip> open(URL file, double volume) {
        if (noAudioOutput) return Optional.empty();
        Optional<Loaded> loaded = loadedItems.computeIfAbsent(file.toExternalForm(), key -> load(file));
        if (loaded.isEmpty()) return Optional.empty();
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(loaded.get().format(), loaded.get().data(), 0, loaded.get().data().length);
            fixVolume(clip, volume);
            open.add(clip);
            return Optional.of(clip);
        } catch (IllegalArgumentException formatOdd) {
            // La placa no acepta este formato: falla este archivo, no todo el sonido.
            System.err.println("ChiquiChess: the audio output does not accept the format of " + file
                    + " (" + formatOdd.getMessage() + ")");
            loadedItems.put(file.toExternalForm(), Optional.empty());
            return Optional.empty();
        } catch (LineUnavailableException | SecurityException withoutAudio) {
            noAudioOutput = true;
            System.err.println("ChiquiChess: no audio output available, playing without sound ("
                    + withoutAudio.getMessage() + ")");
            return Optional.empty();
        }
    }

    /** Lee el archivo entero y lo pasa a PCM si viene en otra codificación. Vacío si no se puede. */
    private static Optional<Loaded> load(URL file) {
        try (AudioInputStream original = AudioSystem.getAudioInputStream(file)) {
            AudioFormat format = original.getFormat();
            AudioInputStream pcm = original;
            // Muchas grabaciones vienen en 24 bits: se pasan a 16, que cualquier placa de sonido acepta.
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || format.getSampleSizeInBits() != 16) {
                AudioFormat destination = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16,
                        format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
                pcm = AudioSystem.getAudioInputStream(destination, original);
            }
            return Optional.of(new Loaded(pcm.getFormat(), pcm.readAllBytes()));
        } catch (UnsupportedAudioFileException | IOException | IllegalArgumentException failure) {
            System.err.println("ChiquiChess: could not read the sound " + file + " (" + failure.getMessage() + ")");
            return Optional.empty();
        }
    }

    /** Volumen de 0 a 1 pasado a decibeles, que es lo que entiende la línea. */
    private static void fixVolume(Clip clip, double volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float decibels = volume <= SILENCE ? gain.getMinimum() : (float) (20 * Math.log10(volume));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }

    /** Idempotente: al detenerse, el propio clip vuelve a pedir que se lo cierre. */
    private void close(Clip clip) {
        if (!open.remove(clip)) return;
        clip.stop();
        clip.close();
    }
}
