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
    private javafx.scene.Node buscarEnOpciones(String id) {
        return ((ScrollPane) menu.lookup("#opciones-menu")).getContent().lookup("#" + id);
    }

    private javafx.scene.Node buscar(String id) {
        javafx.scene.Node enOpciones = buscarEnOpciones(id);
        return enOpciones != null ? enOpciones : menu.lookup("#" + id);
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
    void laPortadaMuestraLaImagenDelPersonaje() {
        var espacio = (javafx.scene.layout.StackPane) menu.lookup("#espacio-personaje");
        var figura = assertInstanceOf(javafx.scene.image.ImageView.class, espacio.getChildren().getFirst());
        assertFalse(figura.getImage().isError());
        assertEquals(500, figura.getImage().getWidth());
    }

    @Test
    void jugarQuedaFueraDelDesplazamientoParaVerseSiempre() {
        assertNull(buscarEnOpciones("jugar"));
        assertNotNull(menu.lookup("#jugar"));
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

    // ---------------- Rival IA ----------------

    @Test
    void laDificultadSoloSeHabilitaContraLaIaYEsObligatoria() {
        elegirTodoMenosElTiempo();
        boton("tiempo-sin_tiempo").setSelected(true);
        assertTrue(boton("dificultad-medio").isDisable());

        boton("rival-ia").setSelected(true);

        assertFalse(boton("dificultad-medio").isDisable());
        assertTrue(buscar("jugar").isDisable());
        assertTrue(((javafx.scene.control.Label) menu.lookup("#selecciones-pendientes")).getText().contains("dificultad"));
    }

    @Test
    void laDificultadElegidaViajaEnLaConfiguracion() {
        elegirTodoMenosElTiempo();
        boton("tiempo-sin_tiempo").setSelected(true);
        boton("rival-ia").setSelected(true);
        boton("dificultad-dificil").setSelected(true);

        ((Button) buscar("jugar")).fire();

        assertEquals(ConfiguracionPartida.Rival.IA, emitidas.getFirst().rival());
        assertEquals(java.util.Optional.of(Nucleo.ia.Dificultad.DIFICIL), emitidas.getFirst().dificultad());
    }

    @Test
    void volverAElegirAmigoBorraLaDificultad() {
        elegirTodoMenosElTiempo();
        boton("tiempo-sin_tiempo").setSelected(true);
        boton("rival-ia").setSelected(true);
        boton("dificultad-facil").setSelected(true);

        boton("rival-amigo").setSelected(true);

        assertFalse(boton("dificultad-facil").isSelected());
        ((Button) buscar("jugar")).fire();
        assertTrue(emitidas.getFirst().dificultad().isEmpty());
    }

    // ---------------- Portada y sonido ----------------

    @Test
    void alAbrirElPersonajeSeBalanceaUnosSegundosYVuelveAQuedarQuieto() {
        var risa = menu.animarPersonaje();
        var personaje = buscar("personaje");

        assertEquals(javafx.animation.Animation.Status.RUNNING, risa.getStatus());
        double segundos = risa.getTotalDuration().toSeconds();
        assertTrue(segundos > 2 && segundos < 4, "dura " + segundos + " s");
        risa.stop();
        risa.getOnFinished().handle(null);
        assertEquals(0, personaje.getRotate());
        assertEquals(1, personaje.getScaleX());
    }

    @Test
    void elBotonDeSonidoDelMenuUsaElMismoControlQueLaPartida() {
        var control = new adaptadores.audio.ControlDeVolumen();
        menu.configurarSonido(control);
        Button silencio = (Button) menu.lookup("#silencio-menu");

        silencio.fire();

        assertTrue(control.silenciado());
        assertEquals("Sonido: no", silencio.getText());
    }

    @Test
    void tocarUnaOpcionAvisaYLasSeleccionesDelPropioMenuNo() {
        int[] avisos = {0};
        menu.alElegirOpcion(() -> avisos[0]++);

        boton("modo-clasico").fire();
        boton("jugador-1-argentina").fire();
        boton("rival-amigo").setSelected(true);   // lo hace el programa, no un clic: no avisa

        assertEquals(2, avisos[0]);
    }
    @Test
    void laSeleccionHabitualSoloDejaPendientesLosEquipos() {
        menu.preseleccionar(adaptadores.configuracion.SeleccionDelMenu.habitual());
        assertTrue(boton("modo-clasico").isSelected());
        assertTrue(boton("tiempo-diez_minutos").isSelected());
        assertTrue(boton("rival-amigo").isSelected());
        assertTrue(boton("color-aleatorio").isSelected());
        assertFalse(boton("permitir-deshacer").isSelected());
        assertTrue(buscar("jugar").isDisable());
        assertEquals("Falta elegir: equipo de Jugador 1, equipo de Jugador 2.",
                ((javafx.scene.control.Label) buscar("selecciones-pendientes")).getText());

        boton("jugador-1-boca").setSelected(true);
        boton("jugador-2-river").setSelected(true);
        ((Button) buscar("jugar")).fire();
        assertEquals(ConfiguracionPartida.Colores.ALEATORIO, emitidas.getFirst().colores());
        assertEquals(ConfiguracionPartida.Tiempo.DIEZ_MINUTOS, emitidas.getFirst().tiempo());
    }

    @Test
    void unMenuNuevoRecuperaLaUltimaSeleccionCompleta() {
        var anterior = new ConfiguracionPartida(ConfiguracionPartida.Modo.FUTBOL, ConfiguracionPartida.Tiempo.SIN_TIEMPO,
                ConfiguracionPartida.Rival.IA, ConfiguracionPartida.Equipo.RIVER, ConfiguracionPartida.Equipo.BOCA,
                ConfiguracionPartida.Colores.JUGADOR_2_BLANCAS, true, 120,
                java.util.Optional.of(Nucleo.ia.Dificultad.DIFICIL));
        menu.preseleccionar(adaptadores.configuracion.SeleccionDelMenu.de(anterior));
        ((Button) buscar("jugar")).fire();
        assertEquals(List.of(anterior), emitidas);
    }

    @Test
    void elVolumenDelMenuCambiaElControlCompartido() {
        var control = new adaptadores.audio.ControlDeVolumen();
        menu.configurarSonido(control);
        ((javafx.scene.control.Slider) menu.lookup("#volumen-menu")).setValue(0.2);
        assertEquals(0.2, control.volumen(), 1e-9);
    }
    @Test
    void elSilencioLlevaElVolumenACeroYSubirloLoReactiva() {
        var control = new adaptadores.audio.ControlDeVolumen();
        menu.configurarSonido(control);
        Button silencio = (Button) menu.lookup("#silencio-menu");
        var volumen = (javafx.scene.control.Slider) menu.lookup("#volumen-menu");
        double antes = volumen.getValue();

        silencio.fire();
        assertEquals(0, volumen.getValue());
        assertEquals("Sonido: no", silencio.getText());

        volumen.setValue(0.4);
        assertFalse(control.silenciado());
        assertEquals("Sonido: sí", silencio.getText());

        silencio.fire();
        silencio.fire();
        assertEquals(0.4, volumen.getValue(), 1e-9);
        assertNotEquals(antes, volumen.getValue());
    }
}
