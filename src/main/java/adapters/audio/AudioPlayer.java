package adapters.audio;

import java.net.URL;

/**
 * Lo único que el resto del audio necesita de la tecnología de sonido. La implementación real es
 * {@link ReproductorJavaSound}; los tests usan uno falso que solo anota qué se pidió.
 *
 * Los volúmenes van de 0 (mudo) a 1 (completo). Si un archivo no se puede reproducir, no se lanza
 * nada: el juego sigue sin ese sonido.
 */
public interface AudioPlayer extends AutoCloseable {

    /** Reproduce el archivo una vez, sin esperar a que termine. */
    void play(URL file, double volume);

    /** Empieza a repetir el archivo hasta que se detenga la pista. */
    Track inLoop(URL file, double volume);

    /** Corta todo lo que esté sonando y libera los recursos. */
    @Override
    void close();

    /** Un sonido que se repite; se le puede cambiar el volumen mientras suena. */
    interface Track {
        void volume(double volume);

        void stop();
    }
}
