package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.juego.*;
import Nucleo.reglas.*;
import Nucleo.tablero.Posicion;
import adaptadores.EnsambladorPartida;
import dobles.FuenteDeTiempoFalsa;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dobles.Casillas.*;
import static org.junit.jupiter.api.Assertions.*;

class PanelEstadoPartidaTest {
    private final TemaPanel temaPanel = TemaPanel.marcador();
    private final TemaTablero temaTablero = new TemaTablero(
            javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
            javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
            javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.GRAY);

    private VistaPartida vista(Partida partida) {
        return new VistaPartida(new ControladorTablero(partida), temaTablero,
                new RepresentacionPiezas(), temaPanel);
    }

    private String texto(VistaPartida vista, String id) {
        Node nodo = vista.lookup("#" + id);
        assertNotNull(nodo, id);
        return nodo instanceof Text texto ? texto.getText() : nodo.getAccessibleText();
    }

    private void clic(VistaPartida vista, String casilla) {
        Posicion posicion = pos(casilla);
        vista.lookup("#casilla-" + posicion.columna() + "-" + posicion.fila())
                .fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, true, null));
    }

    private void jugar(VistaPartida vista, String... jugadas) {
        for (String jugada : jugadas) {
            clic(vista, jugada.substring(0, 2));
            clic(vista, jugada.substring(2));
        }
    }

    @Test
    void muestraNombresDelNucleoYElTurnoInicial() {
        Partida partida = new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Lucía Fernández", Bando.BLANCO), new Jugador("Tomás Rodríguez", Bando.NEGRO),
                new DetectorDeJaque(), List.of());
        VistaPartida vista = vista(partida);
        assertEquals("Lucía Fernández", texto(vista, "nombre-blanco"));
        assertEquals("Tomás Rodríguez", texto(vista, "nombre-negro"));
        assertEquals("TURNO DE", texto(vista, "marcador-encabezado"));
        assertEquals("BLANCAS", texto(vista, "marcador-principal"));
        assertEquals("EN JUEGO", texto(vista, "estado-partida"));
        assertTrue(vista.lookup("#jugador-blanco").getStyleClass().contains("jugador-activo"));
        assertFalse(vista.lookup("#jugador-negro").getStyleClass().contains("jugador-activo"));
    }

    @Test
    void unMovimientoPorClicActualizaTurnoYTarjetaActiva() {
        VistaPartida vista = vista(new EnsambladorPartida().crearSinReloj());
        jugar(vista, "e2e4");
        assertEquals("NEGRAS", texto(vista, "marcador-principal"));
        assertTrue(vista.lookup("#jugador-negro").getStyleClass().contains("jugador-activo"));
        assertFalse(vista.lookup("#jugador-blanco").getStyleClass().contains("jugador-activo"));
    }

    @Test
    void cancelarUnaSeleccionNoCambiaElTurnoQueMuestraElPanel() {
        VistaPartida vista = vista(new EnsambladorPartida().crearSinReloj());
        clic(vista, "e2");
        clic(vista, "e6");
        assertEquals("BLANCAS", texto(vista, "marcador-principal"));
        assertEquals("EN JUEGO", texto(vista, "estado-partida"));
    }

    @Test
    void elAvisoDeJaqueApareceYDesapareceConElEstadoDelNucleo() {
        VistaPartida vista = vista(new EnsambladorPartida().crearSinReloj());
        jugar(vista, "e2e4", "f7f6", "d1h5");
        assertEquals("NEGRAS", texto(vista, "marcador-principal"));
        assertEquals("¡JAQUE!", texto(vista, "estado-partida"));
        assertEquals(temaPanel.alerta(), ((Text) vista.lookup("#estado-partida")).getFill());
        jugar(vista, "g7g6");
        assertEquals("BLANCAS", texto(vista, "marcador-principal"));
        assertEquals("EN JUEGO", texto(vista, "estado-partida"));
        assertEquals(temaPanel.activo(), ((Text) vista.lookup("#estado-partida")).getFill());
    }

    @Test
    void jaqueMateMuestraGanadorYMotivoSinTarjetasActivas() {
        VistaPartida vista = vista(new EnsambladorPartida().crearSinReloj());
        jugar(vista, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals("GANAN LAS", texto(vista, "marcador-encabezado"));
        assertEquals("NEGRAS", texto(vista, "marcador-principal"));
        assertEquals("VICTORIA", texto(vista, "estado-partida"));
        assertEquals("Por jaque mate", texto(vista, "motivo-final"));
        assertTrue(vista.lookupAll(".jugador-activo").isEmpty());
    }

    @Test
    void rendicionUsaElGanadorExplicitoDelResultado() {
        Partida partida = new EnsambladorPartida().crearSinReloj();
        VistaPartida vista = vista(partida);
        partida.mover(mov("e2", "e4"));
        partida.rendirse(Bando.NEGRO);
        vista.redibujar();
        assertEquals("GANAN LAS", texto(vista, "marcador-encabezado"));
        assertEquals("BLANCAS", texto(vista, "marcador-principal"));
        assertEquals("Por rendición", texto(vista, "motivo-final"));
        assertTrue(vista.lookupAll(".jugador-activo").isEmpty());
    }

    @Test
    void tablasPorAcuerdoMuestraEmpateYSuMotivo() {
        Partida partida = new EnsambladorPartida().crearSinReloj();
        VistaPartida vista = vista(partida);
        partida.ofrecerTablas(Bando.BLANCO);
        partida.aceptarTablas(Bando.NEGRO);
        vista.redibujar();
        assertEquals("TABLAS", texto(vista, "marcador-principal"));
        assertEquals("EMPATE", texto(vista, "estado-partida"));
        assertEquals("Por acuerdo", texto(vista, "motivo-final"));
        assertTrue(vista.lookupAll(".jugador-activo").isEmpty());
    }

    @Test
    void distingueLosTresMotivosDeTablasAutomaticas() {
        Map<EstadoPartida, String> casos = Map.of(
                EstadoPartida.TABLAS_AHOGADO, "Por ahogado",
                EstadoPartida.TABLAS_REPETICION, "Por repetición",
                EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS, "Regla de los 50 movimientos");
        casos.forEach((estado, motivo) -> {
            Partida partida = new Partida(new DisposicionEstandar().crearTablero(),
                    new Jugador("B", Bando.BLANCO), new Jugador("N", Bando.NEGRO),
                    new DetectorDeJaque(), List.of(c -> Optional.of(estado)));
            VistaPartida vista = vista(partida);
            assertEquals("TABLAS", texto(vista, "marcador-principal"));
            assertEquals(motivo, texto(vista, "motivo-final"));
            assertTrue(vista.lookupAll(".jugador-activo").isEmpty());
        });
    }

    @Test
    void rechazoPorTiempoTambienActualizaElResultadoEnElPanel() {
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        Partida partida = new EnsambladorPartida().crearConReloj(Duration.ofSeconds(1), fuente);
        VistaPartida vista = vista(partida);
        clic(vista, "e2");
        fuente.avanzar(Duration.ofSeconds(1));
        clic(vista, "e4");
        assertEquals("NEGRAS", texto(vista, "marcador-principal"));
        assertEquals("Por tiempo agotado", texto(vista, "motivo-final"));
        assertFalse(texto(vista, "mensaje-movimiento").isBlank());
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void panelMantieneEl35PorCientoYTableroCasillasCuadradasAlRedimensionar() {
        VistaPartida vista = vista(new EnsambladorPartida().crearSinReloj());
        for (int[] tamano : List.of(new int[]{900, 620}, new int[]{1280, 820}, new int[]{1600, 900})) {
            vista.resize(tamano[0], tamano[1]);
            vista.layout();
            Region panel = (Region) vista.lookup("#panel-estado");
            Region juego = (Region) vista.lookup("#area-juego");
            Region casilla = (Region) vista.lookup("#casilla-0-0");
            assertEquals(tamano[0] * 0.35, panel.getWidth(), 0.01);
            assertEquals(tamano[0] * 0.65, juego.getWidth(), 0.01);
            assertEquals(casilla.getWidth(), casilla.getHeight(), 1.01);
            assertEquals(64, vista.lookupAll(".casilla").size());
        }
    }

    @Test
    void losNombresSeMantienenDentroDelPanelEnLaVentanaMinima() {
        Partida partida = new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Lucía Fernández", Bando.BLANCO), new Jugador("Tomás Rodríguez", Bando.NEGRO),
                new DetectorDeJaque(), List.of());
        VistaPartida vista = vista(partida);
        vista.resize(900, 620);
        vista.layout();
        var limites = vista.lookup("#panel-estado").localToScene(vista.lookup("#panel-estado").getLayoutBounds());
        for (String id : List.of("nombre-blanco", "nombre-negro")) {
            Node nombre = vista.lookup("#" + id);
            var caja = nombre.localToScene(nombre.getLayoutBounds());
            assertTrue(caja.getMaxX() <= limites.getMaxX() + 1);
            assertTrue(caja.getMaxY() <= limites.getMaxY() + 1);
            assertTrue(caja.getMinX() >= limites.getMinX() - 1);
        }
    }
}
