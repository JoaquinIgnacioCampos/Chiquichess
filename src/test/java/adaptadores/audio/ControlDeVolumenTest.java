package adaptadores.audio;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControlDeVolumenTest {
    private final ControlDeVolumen control = new ControlDeVolumen();
    private final List<String> avisos = new ArrayList<>();

    ControlDeVolumenTest() {
        control.alCambiar(() -> avisos.add(control.silenciado() + " " + control.efectivo()));
    }

    @Test
    void llevarElVolumenACeroSilenciaYConservaElAnterior() {
        control.cambiarVolumen(0.7);
        control.cambiarVolumen(0);
        assertTrue(control.silenciado());
        assertEquals(0, control.efectivo());
        assertEquals(0.7, control.volumen(), 1e-9);
    }

    @Test
    void subirElVolumenDesdeElSilencioReactivaElSonido() {
        control.silenciar(true);
        control.cambiarVolumen(0.3);
        assertFalse(control.silenciado());
        assertEquals(0.3, control.efectivo(), 1e-9);
    }

    @Test
    void reactivarElSonidoRecuperaElUltimoVolumen() {
        control.cambiarVolumen(0.6);
        control.silenciar(true);
        control.silenciar(false);
        assertEquals(0.6, control.efectivo(), 1e-9);
    }

    @Test
    void soloAvisaCuandoCambiaAlgo() {
        control.silenciar(true);
        control.silenciar(true);
        control.cambiarVolumen(0);
        assertEquals(List.of("true 0.0"), avisos);
    }
}
