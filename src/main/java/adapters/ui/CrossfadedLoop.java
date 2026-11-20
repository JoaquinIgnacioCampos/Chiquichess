package adapters.ui;

import adapters.audio.VolumeControl;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Un audio que se repite en bucle con un fundido cruzado entre vuelta y vuelta, para que no se note el corte:
 * cuando falta poco para que termine una pasada empieza la siguiente, y mientras una baja la otra sube.
 * Sigue el volumen y el silencio del {@link VolumeControl} del juego. Si el audio no se puede reproducir avisa
 * por la consola de errores y el juego sigue sin él.
 */
final class CrossfadedLoop {
    /** Segundos que dura el fundido entre una vuelta y la siguiente. */
    static final double FADE = 1.5;
    /** Cuánto antes del fundido se prepara la voz siguiente, para que arranque justo a tiempo. */
    private static final double LEAD = 1.0;

    /** Una pasada del audio: su reproductor y la voz que le sigue, cuando ya se preparó. */
    private static final class Voice {
        final MediaPlayer player;
        final boolean withInput;
        Voice next;
        boolean nextStarted;

        Voice(MediaPlayer player, boolean withInput) {
            this.player = player;
            this.withInput = withInput;
        }
    }

    private final String resource;
    private final VolumeControl volume;
    private final double level;
    private final String name;
    private final Runnable applyVolume = this::applyVolume;
    /** Voces sonando a la vez: una sola, salvo en el fundido de una vuelta a la siguiente. */
    private final List<Voice> voices = new ArrayList<>();
    private boolean sounding;

    /**
     * @param resource ruta del audio dentro de los recursos (por ejemplo {@code /intro/intro.m4a})
     * @param level    qué tanto del volumen del juego usa este audio, de 0 a 1
     * @param name     cómo se lo llama en los avisos de error
     */
    CrossfadedLoop(String resource, VolumeControl volume, double level, String name) {
        this.resource = Objects.requireNonNull(resource, "recurso");
        this.volume = Objects.requireNonNull(volume, "volumen");
        this.level = level;
        this.name = Objects.requireNonNull(name, "nombre");
    }

    /**
     * Qué parte del volumen suena en el segundo {@code t} de una pasada de {@code duration} segundos: sube al
     * empezar (salvo la primera pasada, que arranca entera) y baja al terminar, con curvas de potencia
     * constante: al sumarse la que termina con la que empieza, el volumen no se hunde ni se dispara.
     */
    static double gain(double t, double duration, boolean withInput) {
        double input = withInput ? Math.sin(Math.PI / 2 * clamp(t / FADE)) : 1;
        double output = Math.cos(Math.PI / 2 * clamp((t - (duration - FADE)) / FADE));
        return input * output;
    }

    private static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }

    /** Empieza a sonar; la primera pasada arranca entera, sin fundido de entrada. */
    void play() {
        if (sounding) return;
        sounding = true;
        Voice first = createVoice(false);
        if (first != null) first.player.play();
        volume.onChange(applyVolume);
    }

    /** Corta todo lo que suene y deja de escuchar el volumen. */
    void stop() {
        sounding = false;
        volume.stopListening(applyVolume);
        new ArrayList<>(voices).forEach(this::releaseVoice);
    }

    /** Prepara una pasada del audio (sin arrancarla). Cada una avisa cuándo toca preparar y soltar la siguiente. */
    private Voice createVoice(boolean withInput) {
        try {
            var url = CrossfadedLoop.class.getResource(resource);
            if (url == null) return null;
            Media media = new Media(url.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            Voice voice = new Voice(player, withInput);
            voices.add(voice);
            player.setOnError(() -> {
                System.err.println(name + " sin audio: no se pudo reproducir: " + player.getError());
                releaseVoice(voice);
            });
            player.setOnEndOfMedia(() -> Platform.runLater(() -> releaseVoice(voice)));
            player.currentTimeProperty().addListener((p, before, now) -> onAdvance(voice, media, now.toSeconds()));
            applyVolume(voice, 0, media.getDuration().toSeconds());
            return voice;
        } catch (RuntimeException | LinkageError failure) {
            System.err.println(name + " sin audio: " + failure);
            return null;
        }
    }

    private void onAdvance(Voice voice, Media media, double t) {
        double duration = media.getDuration().toSeconds();
        if (!sounding || Double.isNaN(duration) || Double.isInfinite(duration)) return;
        applyVolume(voice, t, duration);
        if (voice.next == null && t >= duration - FADE - LEAD) voice.next = createVoice(true);
        if (voice.next != null && !voice.nextStarted && t >= duration - FADE) {
            voice.nextStarted = true;
            voice.next.player.play();
        }
    }

    private void applyVolume(Voice voice, double t, double duration) {
        double base = Double.isNaN(duration) || Double.isInfinite(duration) ? (voice.withInput ? 0 : 1)
                : gain(t, duration, voice.withInput);
        voice.player.setVolume(volume.effective() * level * base);
    }

    /** Cambió el volumen del juego: cada voz lo aplica sobre su fundido. */
    private void applyVolume() {
        for (Voice voice : new ArrayList<>(voices)) {
            applyVolume(voice, voice.player.getCurrentTime().toSeconds(), voice.player.getTotalDuration().toSeconds());
        }
    }

    private void releaseVoice(Voice voice) {
        if (!voices.remove(voice)) return;
        voice.player.stop();
        voice.player.dispose();
    }
}
