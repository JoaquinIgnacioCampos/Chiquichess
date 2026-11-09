package adaptadores.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Volumen general y silencio que eligió el usuario. Vive mientras el programa está abierto, así se
 * conserva de una partida a la siguiente. Avisa a quien lo escuche cuando cambia.
 */
public final class ControlDeVolumen {
    public static final double VOLUMEN_POR_DEFECTO = 0.8;

    private double volumen = VOLUMEN_POR_DEFECTO;
    private boolean silenciado = false;
    private final List<Runnable> oyentes = new ArrayList<>();

    /** Volumen elegido, de 0 a 1, aunque esté silenciado. */
    public double volumen() {
        return volumen;
    }

    public boolean silenciado() {
        return silenciado;
    }

    /** Lo que efectivamente hay que aplicar: 0 si está silenciado. */
    public double efectivo() {
        return silenciado ? 0 : volumen;
    }

    public void cambiarVolumen(double nuevo) {
        if (Double.isNaN(nuevo)) throw new IllegalArgumentException("Volumen inválido");
        volumen = Math.max(0, Math.min(1, nuevo));
        avisar();
    }

    public void silenciar(boolean silenciar) {
        silenciado = silenciar;
        avisar();
    }

    public void alCambiar(Runnable oyente) {
        oyentes.add(Objects.requireNonNull(oyente, "oyente"));
    }

    public void dejarDeEscuchar(Runnable oyente) {
        oyentes.remove(oyente);
    }

    private void avisar() {
        for (Runnable oyente : List.copyOf(oyentes)) {
            oyente.run();
        }
    }
}
