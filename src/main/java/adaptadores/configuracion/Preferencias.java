package adaptadores.configuracion;

import java.util.Objects;

/**
 * Lo que se guarda de una vez para la otra: las opciones del menú y el sonido.
 *
 * @param volumen    último volumen audible elegido, de 0 a 1
 * @param silenciado si el sonido estaba silenciado
 */
public record Preferencias(SeleccionDelMenu seleccion, double volumen, boolean silenciado) {
    public static final double VOLUMEN_POR_DEFECTO = adaptadores.audio.ControlDeVolumen.VOLUMEN_POR_DEFECTO;

    public Preferencias {
        Objects.requireNonNull(seleccion, "seleccion");
        if (Double.isNaN(volumen) || volumen <= 0 || volumen > 1) {
            throw new IllegalArgumentException("El volumen debe estar entre 0 (sin incluir) y 1");
        }
    }

    public static Preferencias porDefecto() {
        return new Preferencias(SeleccionDelMenu.habitual(), VOLUMEN_POR_DEFECTO, false);
    }
}
