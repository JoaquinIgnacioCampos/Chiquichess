package adaptadores.audio;

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
public final class ReproductorJavaSound implements ReproductorDeAudio {

    /** Audio ya leído de un archivo, listo para abrir en una línea. */
    private record Cargado(AudioFormat formato, byte[] datos) {
    }

    /**
     * Por debajo de esto no se oye. Para callar una pista se corta su línea en vez de bajarle la ganancia:
     * la ganancia se aplica al audio que todavía no se mandó, y la línea guarda cerca de un segundo ya
     * mandado, que seguiría sonando.
     */
    private static final double SILENCIO = 0.0001;

    private final ExecutorService hilo = Executors.newSingleThreadExecutor(tarea -> {
        Thread t = new Thread(tarea, "sonido");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, Optional<Cargado>> cargados = new ConcurrentHashMap<>();
    private final Set<Clip> abiertos = ConcurrentHashMap.newKeySet();
    private volatile boolean sinSalidaDeAudio = false;

    @Override
    public void reproducir(URL archivo, double volumen) {
        Objects.requireNonNull(archivo, "archivo");
        if (volumen <= SILENCIO) return;
        ejecutar(() -> abrir(archivo, volumen).ifPresent(clip -> {
            clip.addLineListener(evento -> {
                if (evento.getType() == LineEvent.Type.STOP) cerrar(clip);
            });
            clip.start();
        }));
    }

    @Override
    public Pista enBucle(URL archivo, double volumen) {
        Objects.requireNonNull(archivo, "archivo");
        PistaEnBucle pista = new PistaEnBucle();
        ejecutar(() -> {
            if (pista.detenida) return;
            abrir(archivo, volumen).ifPresent(clip -> {
                pista.clip = clip;
                if (volumen > SILENCIO) {
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                } else {
                    pista.pausada = true;
                }
            });
        });
        return pista;
    }

    @Override
    public void close() {
        hilo.shutdownNow();
        abiertos.forEach(this::cerrar);
    }

    private final class PistaEnBucle implements Pista {
        /** Solo se toca desde el hilo de sonido. */
        private Clip clip;
        /** Cortada por estar en silencio; vuelve a sonar desde donde quedó. Solo se toca desde el hilo de sonido. */
        private boolean pausada = false;
        private volatile boolean detenida = false;

        @Override
        public void volumen(double volumen) {
            ejecutar(() -> {
                if (clip == null) return;
                if (volumen <= SILENCIO) {
                    if (pausada) return;
                    clip.stop();
                    clip.flush();
                    pausada = true;
                } else {
                    fijarVolumen(clip, volumen);
                    if (pausada) {
                        clip.loop(Clip.LOOP_CONTINUOUSLY);
                        pausada = false;
                    }
                }
            });
        }

        @Override
        public void detener() {
            detenida = true;
            ejecutar(() -> {
                if (clip != null) cerrar(clip);
                clip = null;
            });
        }
    }

    private void ejecutar(Runnable tarea) {
        if (hilo.isShutdown()) return;
        try {
            hilo.execute(tarea);
        } catch (java.util.concurrent.RejectedExecutionException cerrado) {
            // Se cerró el reproductor entre la pregunta y el pedido: ese sonido ya no hace falta.
        }
    }

    private Optional<Clip> abrir(URL archivo, double volumen) {
        if (sinSalidaDeAudio) return Optional.empty();
        Optional<Cargado> cargado = cargados.computeIfAbsent(archivo.toExternalForm(), clave -> cargar(archivo));
        if (cargado.isEmpty()) return Optional.empty();
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(cargado.get().formato(), cargado.get().datos(), 0, cargado.get().datos().length);
            fijarVolumen(clip, volumen);
            abiertos.add(clip);
            return Optional.of(clip);
        } catch (IllegalArgumentException formatoRaro) {
            // La placa no acepta este formato: falla este archivo, no todo el sonido.
            System.err.println("ChiquiChess: la salida de audio no acepta el formato de " + archivo
                    + " (" + formatoRaro.getMessage() + ")");
            cargados.put(archivo.toExternalForm(), Optional.empty());
            return Optional.empty();
        } catch (LineUnavailableException | SecurityException sinAudio) {
            sinSalidaDeAudio = true;
            System.err.println("ChiquiChess: no hay salida de audio disponible, se juega sin sonido ("
                    + sinAudio.getMessage() + ")");
            return Optional.empty();
        }
    }

    /** Lee el archivo entero y lo pasa a PCM si viene en otra codificación. Vacío si no se puede. */
    private static Optional<Cargado> cargar(URL archivo) {
        try (AudioInputStream original = AudioSystem.getAudioInputStream(archivo)) {
            AudioFormat formato = original.getFormat();
            AudioInputStream pcm = original;
            // Muchas grabaciones vienen en 24 bits: se pasan a 16, que cualquier placa de sonido acepta.
            if (formato.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || formato.getSampleSizeInBits() != 16) {
                AudioFormat destino = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, formato.getSampleRate(), 16,
                        formato.getChannels(), formato.getChannels() * 2, formato.getSampleRate(), false);
                pcm = AudioSystem.getAudioInputStream(destino, original);
            }
            return Optional.of(new Cargado(pcm.getFormat(), pcm.readAllBytes()));
        } catch (UnsupportedAudioFileException | IOException | IllegalArgumentException fallo) {
            System.err.println("ChiquiChess: no se pudo leer el sonido " + archivo + " (" + fallo.getMessage() + ")");
            return Optional.empty();
        }
    }

    /** Volumen de 0 a 1 pasado a decibeles, que es lo que entiende la línea. */
    private static void fijarVolumen(Clip clip, double volumen) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl ganancia = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float decibeles = volumen <= SILENCIO ? ganancia.getMinimum() : (float) (20 * Math.log10(volumen));
        ganancia.setValue(Math.max(ganancia.getMinimum(), Math.min(ganancia.getMaximum(), decibeles)));
    }

    /** Idempotente: al detenerse, el propio clip vuelve a pedir que se lo cierre. */
    private void cerrar(Clip clip) {
        if (!abiertos.remove(clip)) return;
        clip.stop();
        clip.close();
    }
}
