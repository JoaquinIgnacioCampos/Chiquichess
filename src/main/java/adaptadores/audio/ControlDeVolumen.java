package adaptadores.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Volumen general y silencio que eligió el usuario. Vive mientras el programa está abierto, así se
 * conserva de una partida a la siguiente. Avisa a quien lo escuche cuando cambia.
 *
 * Silenciar y llevar el volumen a cero son lo mismo: subirlo desde cero reactiva el sonido, y reactivarlo
 * con el silencio recupera el último volumen que se usó.
 */
public final class ControlDeVolumen {
    /** Moderado: la risa del personaje suena apenas se abre el juego, antes de que se pueda bajar. */
    public static final double VOLUMEN_POR_DEFECTO = 0.5;

    private double volumen = VOLUMEN_POR_DEFECTO;
    private boolean silenciado = false;
    private final List<Runnable> oyentes = new ArrayList<>();

    /** Último volumen audible elegido, de 0 a 1: se conserva mientras está silenciado. */
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

    /** Cero silencia; un valor mayor fija el volumen y reactiva el sonido si estaba silenciado. */
    public void cambiarVolumen(double nuevo) {
        if (Double.isNaN(nuevo)) throw new IllegalArgumentException("Volumen inválido");
        double ajustado = Math.max(0, Math.min(1, nuevo));
        if (ajustado == 0) {
            silenciar(true);
            return;
        }
        if (!silenciado && ajustado == volumen) return;
        volumen = ajustado;
        silenciado = false;
        avisar();
    }

    public void silenciar(boolean silenciar) {
        if (silenciar == silenciado) return;
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
