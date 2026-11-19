package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.juego.Partida;
import adaptadores.EnsambladorPartida;
import adaptadores.audio.CatalogoDeSonidos;
import adaptadores.audio.ControlDeVolumen;
import adaptadores.audio.ReproductorDeAudio;
import adaptadores.audio.ReproductorJavaSound;
import adaptadores.audio.SonidosDePartida;
import adaptadores.audio.SonidosDelMenu;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.PartidaConfigurada;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** Arranque gráfico: ensambla el juego e inyecta la partida en la vista. */
public class AplicacionAjedrez extends Application {
    /** Tamaños de la ventana con sus bordes; en una pantalla más chica manda la pantalla. */
    private static final double ANCHO_PREFERIDO = 1296;
    private static final double ALTO_PREFERIDO = 858;
    private static final double ANCHO_MINIMO = 900;
    private static final double ALTO_MINIMO = 620;

    private ActualizadorRelojJavaFx actualizadorReloj;
    private PartidaConfigurada partidaActual;
    private SonidosDePartida sonidosActuales;
    private TurnoDeLaIa turnoDeLaIa;
    /** Hilos donde piensa la IA; son daemon para no impedir que el programa se cierre. */
    private final ExecutorService hilosDeLaIa = Executors.newCachedThreadPool(tarea -> {
        Thread hilo = new Thread(tarea, "ia-pensando");
        hilo.setDaemon(true);
        return hilo;
    });
    /** Uno solo para todo el programa: el volumen elegido sigue valiendo en las partidas siguientes. */
    private final ControlDeVolumen volumen = new ControlDeVolumen();
    private final CatalogoDeSonidos catalogoDeSonidos = new CatalogoDeSonidos();
    private ReproductorDeAudio reproductor;
    private SonidosDelMenu sonidosDelMenu;
    private VistaMenuPartida menuActual;

    @Override
    public void start(Stage ventana) {
        reproductor = new ReproductorJavaSound();
        sonidosDelMenu = new SonidosDelMenu(catalogoDeSonidos, reproductor, volumen);
        VistaMenuPartida menu = nuevoMenu(ventana);
        ventana.setTitle("ChiquiChess");
        ventana.setScene(new Scene(menu));
        ajustarAPantalla(ventana, Screen.getPrimary().getVisualBounds());
        ventana.setOnHidden(evento -> {
            cerrarMenu();
            cerrarPartidaActual();
        });
        ventana.show();
        menu.reproducirFondo(volumen.efectivo());
        // Al abrir el juego, y solo entonces: el personaje se ríe y se balancea.
        sonidosDelMenu.abrirJuego();
        menu.animarPersonaje();
    }

    private VistaMenuPartida nuevoMenu(Stage ventana) {
        VistaMenuPartida menu = new VistaMenuPartida(TemaPanel.marcador(), configuracion -> iniciarPartida(ventana, configuracion));
        menu.configurarSonido(volumen);
        menu.alElegirOpcion(sonidosDelMenu::opcionElegida);
        menuActual = menu;
        return menu;
    }

    /** La ventana arranca entera dentro de la pantalla, centrada y con la barra de título visible. */
    private void ajustarAPantalla(Stage ventana, Rectangle2D pantalla) {
        double ancho = Math.min(ANCHO_PREFERIDO, pantalla.getWidth());
        double alto = Math.min(ALTO_PREFERIDO, pantalla.getHeight());
        ventana.setWidth(ancho);
        ventana.setHeight(alto);
        ventana.setX(pantalla.getMinX() + (pantalla.getWidth() - ancho) / 2);
        ventana.setY(pantalla.getMinY() + (pantalla.getHeight() - alto) / 2);
        ventana.setMinWidth(Math.min(ANCHO_MINIMO, ancho));
        ventana.setMinHeight(Math.min(ALTO_MINIMO, alto));
    }

    private void iniciarPartida(Stage ventana, ConfiguracionPartida configuracion) {
        cerrarPartidaActual();
        sonidosDelMenu.salirDelMenu();
        var preparada = new EnsambladorPartida().prepararConfigurada(configuracion);
        partidaActual = preparada;
        Partida partida = preparada.partida();
        TemaTablero tema = new TemaTablero(
                Color.web("#9FBE70"), Color.web("#527B3D"),
                Color.web("#FFFBEF"), Color.web("#202733"),
                Color.web("#202733"), Color.web("#1B212B"),
                Color.web("#F7F769"), Color.web("#202733", 0.18), Color.web("#202733", 0.23),
                new Image("/tablero/pasto.jpg", 1280, 1280, true, true));
        ControladorTablero controlador = new ControladorTablero(partida, preparada.rival());
        TemaPanel temaPanel = TemaPanel.marcador();
        var clubes = java.util.Map.of(
                Bando.BLANCO, PresentacionClub.de(preparada.equipoBlancas(), temaPanel),
                Bando.NEGRO, PresentacionClub.de(preparada.equipoNegras(), temaPanel));
        RepresentacionPiezas representacion = new RepresentacionConSprites(java.util.Map.of(
                Bando.BLANCO, CatalogoSprites.de(preparada.equipoBlancas()),
                Bando.NEGRO, CatalogoSprites.de(preparada.equipoNegras())), new RepresentacionUnicode());
        VistaPartida vista = new VistaPartida(controlador, tema, representacion, temaPanel, clubes,
                new MensajesDeLaInterfaz());
        vista.configurarRendicion();
        vista.configurarTablas();
        vista.configurarDeshacer();
        vista.configurarSonido(volumen);
        vista.configurarNavegacion(() -> {
            if (partidaActual == preparada) volverAlMenu(ventana);
        }, () -> iniciarRevancha(ventana, configuracion, preparada));
        actualizadorReloj = new ActualizadorRelojJavaFx(controlador, vista::actualizarRelojes);
        cerrarMenu();
        ventana.getScene().setRoot(vista);
        SonidosDePartida sonidos = SonidosDePartida.conectar(partida, catalogoDeSonidos, reproductor,
                Map.of(Bando.BLANCO, preparada.equipoBlancas(), Bando.NEGRO, preparada.equipoNegras()),
                volumen, configuracion.modo() == ConfiguracionPartida.Modo.FUTBOL);
        sonidosActuales = sonidos;
        vista.alSeleccionarPieza(sonidos::piezaSeleccionada);
        actualizadorReloj.iniciar();
        // Último: si la IA juega con blancas, empieza a pensar ya con la vista y el sonido listos.
        preparada.rival().ifPresent(rival -> {
            turnoDeLaIa = new TurnoDeLaIa(partida, rival, hilosDeLaIa, Platform::runLater);
            turnoDeLaIa.iniciar();
        });
    }

    private void volverAlMenu(Stage ventana) {
        cerrarPartidaActual();
        ventana.getScene().setRoot(nuevoMenu(ventana));
        sonidosDelMenu.entrarAlMenu();
        menuActual.reproducirFondo(volumen.efectivo());
    }

    private void cerrarMenu() {
        if (menuActual == null) return;
        menuActual.detenerFondo();
        menuActual = null;
    }

    private void iniciarRevancha(Stage ventana, ConfiguracionPartida anterior, PartidaConfigurada preparada) {
        if (partidaActual != preparada || !preparada.partida().estado().estaFinalizada()) return;
        var colores = preparada.equipoBlancas() == anterior.equipoJugador1()
                ? ConfiguracionPartida.Colores.JUGADOR_2_BLANCAS : ConfiguracionPartida.Colores.JUGADOR_1_BLANCAS;
        iniciarPartida(ventana, new ConfiguracionPartida(anterior.modo(), anterior.tiempo(), anterior.rival(),
                anterior.equipoJugador1(), anterior.equipoJugador2(), colores, anterior.permitirDeshacer(),
                anterior.limiteDeJugadas(), anterior.dificultad()));
    }

    private void cerrarPartidaActual() {
        if (turnoDeLaIa != null) {
            turnoDeLaIa.close();
            turnoDeLaIa = null;
        }
        if (sonidosActuales != null) {
            sonidosActuales.close();
            sonidosActuales = null;
        }
        if (actualizadorReloj != null) {
            actualizadorReloj.close();
            actualizadorReloj = null;
        }
        if (partidaActual != null) {
            partidaActual.close();
            partidaActual = null;
        }
    }

    @Override public void stop() {
        cerrarMenu();
        cerrarPartidaActual();
        if (sonidosDelMenu != null) sonidosDelMenu.close();
        if (reproductor != null) reproductor.close();
        hilosDeLaIa.shutdownNow();
    }
}
