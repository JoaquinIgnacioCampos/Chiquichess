package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.juego.*;
import Nucleo.reglas.*;
import Nucleo.tablero.Posicion;
import Nucleo.tiempo.SinReloj;
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

    private VistaPartida vista(PartidaEnJuego partida) {
        return new VistaPartida(new ControladorTablero(partida), temaTablero,
                new RepresentacionUnicode(), temaPanel);
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
        PartidaEnJuego partida = new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Lucía Fernández", Bando.BLANCO), new Jugador("Tomás Rodríguez", Bando.NEGRO),
                new ReglasDePartida(new DetectorDeJaque(), List.of()), new SinReloj());
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
        PartidaEnJuego partida = new EnsambladorPartida().crearSinReloj();
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
        PartidaEnJuego partida = new EnsambladorPartida().crearSinReloj();
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
            PartidaEnJuego partida = new Partida(new DisposicionEstandar().crearTablero(),
                    new Jugador("B", Bando.BLANCO), new Jugador("N", Bando.NEGRO),
                    new ReglasDePartida(new DetectorDeJaque(), List.of(c -> Optional.of(estado))), new SinReloj());
            VistaPartida vista = vista(partida);
            assertEquals("TABLAS", texto(vista, "marcador-principal"));
            assertEquals(motivo, texto(vista, "motivo-final"));
            assertTrue(vista.lookupAll(".jugador-activo").isEmpty());
        });
    }

    @Test
    void rechazoPorTiempoTambienActualizaElResultadoEnElPanel() {
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        PartidaEnJuego partida = new EnsambladorPartida().crearConReloj(Duration.ofSeconds(1), fuente);
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
        PartidaEnJuego partida = new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Lucía Fernández", Bando.BLANCO), new Jugador("Tomás Rodríguez", Bando.NEGRO),
                new ReglasDePartida(new DetectorDeJaque(), List.of()), new SinReloj());
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

    // ---------------- Modo fútbol ----------------

    /** Torre blanca con la pelota en d2, a un movimiento del gol en d8. */
    private Partida futbolConTorreEnD2() {
        return futbolConTorreEnD2(ReglasDePartida.futbol());
    }

    private Partida futbolConTorreEnD2(ReglasDePartida reglas) {
        Nucleo.tablero.Tablero tablero = dobles.TableroFake.desde(
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...T....",
                "....R...");
        new Nucleo.reglas.ReglaDeLaPelota().prepararCancha(tablero);
        tablero.colocarPelota(pos("d2"));
        return new Partida(tablero, new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                reglas, new Nucleo.tiempo.SinReloj());
    }

    @Test
    void enElModoFutbolMuestraElMarcadorYLasJugadas() {
        VistaPartida vista = vista(futbolConTorreEnD2());
        assertEquals("BLANCAS 0 - 0 NEGRAS", texto(vista, "goles-partida"));
        assertEquals("Jugada 0 de 90", texto(vista, "jugadas-futbol"));
        assertEquals("EN JUEGO", texto(vista, "estado-partida"));
    }

    @Test
    void anunciaElGolYActualizaElMarcador() {
        VistaPartida vista = vista(futbolConTorreEnD2());
        jugar(vista, "d2d8");
        assertEquals("¡GOL!", texto(vista, "estado-partida"));
        assertEquals("BLANCAS 1 - 0 NEGRAS", texto(vista, "goles-partida"));
        assertEquals("Jugada 1 de 90", texto(vista, "jugadas-futbol"));

        jugar(vista, "h8g7");
        assertEquals("EN JUEGO", texto(vista, "estado-partida"));
        assertEquals("BLANCAS 1 - 0 NEGRAS", texto(vista, "goles-partida"));
    }

    @Test
    void deshacerElGolDejaDeAnunciarloYRehacerloLoVuelveAAnunciar() {
        // Arrange
        PartidaEnJuego partida = futbolConTorreEnD2(ReglasDePartida.futbol().conDeshacer());
        VistaPartida vista = vista(partida);
        jugar(vista, "d2d8");

        // Act + Assert: se deshace y rehace desde la partida, no desde la vista; la vista se entera por los eventos
        partida.deshacer();
        assertEquals("EN JUEGO", texto(vista, "estado-partida"));
        assertEquals("BLANCAS 0 - 0 NEGRAS", texto(vista, "goles-partida"));
        partida.rehacer();
        assertEquals("¡GOL!", texto(vista, "estado-partida"));
        assertEquals("BLANCAS 1 - 0 NEGRAS", texto(vista, "goles-partida"));
    }

    @Test
    void elFinalPorTiempoSeMuestraPorElEventoSinRedibujarAMano() {
        // Arrange
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        PartidaEnJuego partida = new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("Beto", Bando.NEGRO), ReglasDePartida.clasicas(),
                new Nucleo.tiempo.RelojAjedrez(Duration.ofMinutes(1), Duration.ofMinutes(1), fuente));
        VistaPartida vista = vista(partida);

        // Act: lo que hace el reloj de la interfaz en cada pulso
        fuente.avanzar(Duration.ofMinutes(2));
        partida.verificarTiempo();

        // Assert
        assertEquals("RESULTADO FINAL", texto(vista, "titulo-estado"));
        assertEquals("NEGRAS", texto(vista, "marcador-principal"));
    }

    @Test
    void elAjedrezClasicoNoMuestraGoles() {
        VistaPartida vista = vista(new EnsambladorPartida().crearSinReloj());
        assertEquals("", texto(vista, "goles-partida"));
        assertFalse(vista.lookup("#goles-partida").isVisible());
    }

    @Test
    void muestraElLimiteDeJugadasElegido() {
        VistaPartida vista = vista(new EnsambladorPartida().crearSinRelojConDeshacer(
                adaptadores.configuracion.ConfiguracionPartida.Modo.FUTBOL, 40));
        assertEquals("Jugada 0 de 40", texto(vista, "jugadas-futbol"));
    }

    // ---------------- Cartel de gol ----------------

    @Test
    void sinGolNoHayCartel() {
        VistaPartida vista = vista(futbolConTorreEnD2());
        assertFalse(vista.lookup("#cartel-gol").isVisible());
    }

    @Test
    void elGolMuestraElCartelDeLucesConElNombreDelBando() {
        VistaPartida vista = vista(futbolConTorreEnD2());

        jugar(vista, "d2d8");

        assertTrue(vista.lookup("#cartel-gol").isVisible());
        assertEquals("¡GOL!", texto(vista, "gol-luces"));
        assertEquals("BLANCAS", texto(vista, "gol-equipo"));
    }

    @Test
    void conClubesElCartelLlevaElEscudoYElColorDelEquipoQueHizoElGol() {
        var clubes = Map.of(
                Bando.BLANCO, PresentacionClub.de(adaptadores.configuracion.ConfiguracionPartida.Equipo.ARGENTINA, temaPanel),
                Bando.NEGRO, PresentacionClub.de(adaptadores.configuracion.ConfiguracionPartida.Equipo.BOCA, temaPanel));
        VistaPartida vista = new VistaPartida(new ControladorTablero(futbolConTorreEnD2()), temaTablero,
                new RepresentacionUnicode(), temaPanel, clubes);

        jugar(vista, "d2d8");

        assertEquals("ARGENTINA", texto(vista, "gol-equipo"));
        var cartel = vista.lookup("#cartel-gol");
        var escudo = (javafx.scene.image.ImageView) cartel.lookup(".image-view");
        assertSame(clubes.get(Bando.BLANCO).escudo(), escudo.getImage());
        assertTrue(escudo.isVisible());
    }
}
