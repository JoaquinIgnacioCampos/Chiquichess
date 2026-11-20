package adapters.audio;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El bucle sin corte: el final del audio se funde con el principio (se prueba sobre los números, sin sonar). */
class LoopCrossfadeTest {
    private static final AudioFormat MONO = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 100, 16, 1, 2, 100, false);

    /** Audio mono de 16 bits: cada muestra es un entero, en little endian. */
    private static byte[] audio(int... samples) {
        byte[] data = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            data[2 * i] = (byte) samples[i];
            data[2 * i + 1] = (byte) (samples[i] >> 8);
        }
        return data;
    }

    private static int[] samples(byte[] data) {
        int[] samples = new int[data.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (data[2 * i + 1] << 8) | (data[2 * i] & 0xFF);
        }
        return samples;
    }

    @Test
    void theLoopIsShorterByTheFadeAndStartsThatFarIn() {
        int[] original = new int[100];
        for (int i = 0; i < original.length; i++) original[i] = i * 100;
        // 0,1 s a 100 muestras por segundo = 10 muestras de fundido
        int[] loop = samples(JavaSoundPlayer.crossfaded(MONO, audio(original), 0.1));
        assertEquals(90, loop.length);
        // Lo que no se mezcla es el audio tal cual, desde la muestra 10
        for (int i = 0; i < 80; i++) assertEquals(original[10 + i], loop[i], "muestra " + i);
    }

    @Test
    void theEndOfTheLoopLeadsStraightIntoItsStart() {
        int[] original = new int[100];
        java.util.Arrays.fill(original, 1000);
        int[] loop = samples(JavaSoundPlayer.crossfaded(MONO, audio(original), 0.1));
        // En un audio parejo, la mezcla de la cola y el principio conserva el nivel: no hay salto ni bache
        for (int i = 70; i < 80; i++) assertTrue(loop[i] >= 1000, "no debe hundirse en el fundido: " + loop[i]);
        for (int value : loop) assertTrue(value <= 1500, "ni dispararse: " + value);
        // La última muestra del fundido es casi el principio del audio (la muestra 9), y la primera de la vuelta, la 10
        assertEquals(1000, loop[loop.length - 1], 150);
        assertEquals(1000, loop[0]);
    }

    @Test
    void aVeryShortAudioShrinksTheFadeToAQuarterOfItsLength() {
        int[] loop = samples(JavaSoundPlayer.crossfaded(MONO, audio(new int[40]), 5));
        assertEquals(30, loop.length); // 40 muestras, fundido de 10 (un cuarto)
    }

    @Test
    void anAudioThatIsNotSixteenBitsStaysAsItWas() {
        AudioFormat eight = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 100, 8, 1, 1, 100, false);
        byte[] data = new byte[50];
        assertArrayEquals(data, JavaSoundPlayer.crossfaded(eight, data, 0.1));
    }
}
