package adapters.ui;

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
final class WindowsTitleBar {
    private static final int DARK_MODE = 20;
    private static final int BORDER_COLOR = 34;
    private static final int BACKGROUND_COLOR = 35;
    private static final int TEXT_COLOR = 36;

    private interface User32 extends Library {
        Pointer FindWindowW(WString classValue, WString title);
    }

    private interface Dwmapi extends Library {
        int DwmSetWindowAttribute(Pointer ventana, int attribute, IntByReference value, int size);
    }

    private WindowsTitleBar() {
    }

    /** Se llama con la ventana ya mostrada, porque recién entonces existe para Windows. */
    static void paint(Stage ventana, Color background, Color texto) {
        if (!System.getProperty("os.name", "").startsWith("Windows")) return;
        try {
            Pointer hwnd = Native.load("user32", User32.class).FindWindowW(null, new WString(ventana.getTitle()));
            if (hwnd == null) return;
            Dwmapi dwm = Native.load("dwmapi", Dwmapi.class);
            dwm.DwmSetWindowAttribute(hwnd, DARK_MODE, new IntByReference(1), 4);
            dwm.DwmSetWindowAttribute(hwnd, BACKGROUND_COLOR, new IntByReference(colorRef(background)), 4);
            dwm.DwmSetWindowAttribute(hwnd, BORDER_COLOR, new IntByReference(colorRef(background)), 4);
            dwm.DwmSetWindowAttribute(hwnd, TEXT_COLOR, new IntByReference(colorRef(texto)), 4);
        } catch (Throwable withoutSupport) {
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
