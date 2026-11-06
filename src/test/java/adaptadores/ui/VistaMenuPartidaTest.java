package adaptadores.ui;

import adaptadores.configuracion.ConfiguracionPartida;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** La opción "Permitir deshacer" del menú: solo se habilita sin tiempo y viaja en la configuración. */
class VistaMenuPartidaTest {

    /** Los controles de JavaFX (botones, etiquetas) necesitan el toolkit iniciado; si ya lo estaba, no pasa nada. */
    @BeforeAll
    static void iniciarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaIniciado) {
            // otro test ya lo inició
        }
    }

    private final List<ConfiguracionPartida> emitidas = new ArrayList<>();
    private final VistaMenuPartida menu = new VistaMenuPartida(TemaPanel.marcador(), emitidas::add);

    /** Las opciones están dentro de un ScrollPane: sin ventana dibujada, hay que buscar en su contenido. */
    private javafx.scene.Node buscar(String id) {
        return ((ScrollPane) menu.lookup("#opciones-menu")).getContent().lookup("#" + id);
    }

    private ToggleButton boton(String id) {
        return (ToggleButton) buscar(id);
    }

    private void elegirTodoMenosElTiempo() {
        boton("modo-clasico").setSelected(true);
        boton("rival-amigo").setSelected(true);
        boton("jugador-1-boca").setSelected(true);
        boton("jugador-2-river").setSelected(true);
        boton("color-jugador-1").setSelected(true);
    }

    @Test
    void sinElegirTiempoLaOpcionEstaDeshabilitada() {
        assertTrue(boton("permitir-deshacer").isDisable());
    }

    @Test
    void conSinTiempoSeHabilitaYViajaEnLaConfiguracion() {
        elegirTodoMenosElTiempo();
        boton("tiempo-sin_tiempo").setSelected(true);
        assertFalse(boton("permitir-deshacer").isDisable());

        boton("permitir-deshacer").setSelected(true);
        ((Button) buscar("jugar")).fire();

        assertEquals(1, emitidas.size());
        assertTrue(emitidas.get(0).permitirDeshacer());
    }

    @Test
    void elegirUnTiempoApagaYDeshabilitaLaOpcion() {
        elegirTodoMenosElTiempo();
        boton("tiempo-sin_tiempo").setSelected(true);
        boton("permitir-deshacer").setSelected(true);

        boton("tiempo-cinco_minutos").setSelected(true);

        assertTrue(boton("permitir-deshacer").isDisable());
        assertFalse(boton("permitir-deshacer").isSelected());
        ((Button) buscar("jugar")).fire();
        assertFalse(emitidas.get(0).permitirDeshacer());
    }

    @Test
    void laOpcionNoEsObligatoriaParaJugar() {
        elegirTodoMenosElTiempo();
        boton("tiempo-sin_tiempo").setSelected(true);
        assertFalse(buscar("jugar").isDisable());
    }

    @Test
    void elModoFutbolViajaEnLaConfiguracion() {
        elegirTodoMenosElTiempo();
        boton("modo-futbol").setSelected(true);
        boton("tiempo-sin_tiempo").setSelected(true);

        ((Button) buscar("jugar")).fire();

        assertEquals(ConfiguracionPartida.Modo.FUTBOL, emitidas.get(0).modo());
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.control.Spinner<Integer> limite() {
        return (javafx.scene.control.Spinner<Integer>) buscar("limite-jugadas");
    }

    @Test
    void elLimiteDeJugadasSoloSeHabilitaEnElModoFutbol() {
        assertTrue(limite().isDisable());
        boton("modo-futbol").setSelected(true);
        assertFalse(limite().isDisable());
        boton("modo-clasico").setSelected(true);
        assertTrue(limite().isDisable());
    }

    @Test
    void elLimiteEscritoViajaEnLaConfiguracion() {
        elegirTodoMenosElTiempo();
        boton("modo-futbol").setSelected(true);
        boton("tiempo-sin_tiempo").setSelected(true);
        limite().getEditor().setText("40");

        ((Button) buscar("jugar")).fire();

        assertEquals(40, emitidas.get(0).limiteDeJugadas());
    }

    @Test
    void unLimiteFueraDeRangoSeAjustaYUnTextoInvalidoSeDescarta() {
        elegirTodoMenosElTiempo();
        boton("modo-futbol").setSelected(true);
        boton("tiempo-sin_tiempo").setSelected(true);

        limite().getEditor().setText("5000");
        ((Button) buscar("jugar")).fire();
        limite().getEditor().setText("muchas");
        ((Button) buscar("jugar")).fire();

        assertEquals(ConfiguracionPartida.LIMITE_MAXIMO, emitidas.get(0).limiteDeJugadas());
        assertEquals(ConfiguracionPartida.LIMITE_MAXIMO, emitidas.get(1).limiteDeJugadas());
    }
}
