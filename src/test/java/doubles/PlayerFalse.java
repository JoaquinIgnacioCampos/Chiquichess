package doubles;

import adapters.audio.AudioPlayer;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** Reproductor que no hace ruido: anota qué se pidió tocar y cómo están las pistas en bucle. */
public class PlayerFalse implements AudioPlayer {

    /** Una pista en bucle con su último volumen. */
    public static final class TrackFalse implements Track {
        public final String file;
        public double volume;
        public boolean stopped;

        TrackFalse(String file, double volume) {
            this.file = file;
            this.volume = volume;
        }

        @Override
        public void volume(double volume) {
            this.volume = volume;
        }

        @Override
        public void stop() {
            stopped = true;
        }
    }

    /** Archivos tocados una vez, en orden, como "carpeta/archivo.wav" relativos a la raíz de los sonidos. */
    public final List<String> played = new ArrayList<>();
    public final List<Double> volumes = new ArrayList<>();
    public final List<TrackFalse> tracks = new ArrayList<>();
    public boolean closed;

    private static String displayName(URL file) {
        String path = file.getPath();
        int sounds = path.lastIndexOf("/sounds/");
        return sounds >= 0 ? path.substring(sounds + "/sounds/".length()) : path;
    }

    @Override
    public void play(URL file, double volume) {
        played.add(displayName(file));
        volumes.add(volume);
    }

    @Override
    public Track inLoop(URL file, double volume) {
        TrackFalse track = new TrackFalse(displayName(file), volume);
        tracks.add(track);
        return track;
    }

    /** Las pistas en bucle que siguen sonando. */
    public List<TrackFalse> sounding() {
        return tracks.stream().filter(p -> !p.stopped).toList();
    }

    @Override
    public void close() {
        closed = true;
    }
}
