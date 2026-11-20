package adaptadores.ui;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Pinta la barra de título de la ventana con los colores del juego. JavaFX no lo permite, así que se le
 * pide directamente al administrador de ventanas de Windows (DWM). Windows 11 acepta un color exacto;
 * Windows 10 solo el modo oscuro. En otros sistemas, o si algo falla, la barra queda como estaba.
 */
final class BarraDeTituloWindows {
    private static final int MODO_OSCURO = 20;
    private static final int COLOR_DE_BORDE = 34;
    private static final int COLOR_DE_FONDO = 35;
    private static final int COLOR_DE_TEXTO = 36;

    private interface User32 extends Library {
        Pointer FindWindowW(WString clase, WString titulo);
    }

    private interface Dwmapi extends Library {
        int DwmSetWindowAttribute(Pointer ventana, int atributo, IntByReference valor, int tamano);
    }

    private BarraDeTituloWindows() {
    }

    /** Se llama con la ventana ya mostrada, porque recién entonces existe para Windows. */
    static void pintar(Stage ventana, Color fondo, Color texto) {
        if (!System.getProperty("os.name", "").startsWith("Windows")) return;
        try {
            Pointer hwnd = Native.load("user32", User32.class).FindWindowW(null, new WString(ventana.getTitle()));
            if (hwnd == null) return;
            Dwmapi dwm = Native.load("dwmapi", Dwmapi.class);
            dwm.DwmSetWindowAttribute(hwnd, MODO_OSCURO, new IntByReference(1), 4);
            dwm.DwmSetWindowAttribute(hwnd, COLOR_DE_FONDO, new IntByReference(colorRef(fondo)), 4);
            dwm.DwmSetWindowAttribute(hwnd, COLOR_DE_BORDE, new IntByReference(colorRef(fondo)), 4);
            dwm.DwmSetWindowAttribute(hwnd, COLOR_DE_TEXTO, new IntByReference(colorRef(texto)), 4);
        } catch (Throwable sinSoporte) {
            // Sin JNA o en un Windows sin DWM: la barra queda con su color de siempre.
        }
    }

    /** Windows guarda los colores como 0x00BBGGRR. */
    private static int colorRef(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return (b << 16) | (g << 8) | r;
    }
}
