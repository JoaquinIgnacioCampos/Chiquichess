package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.juego.Partida;
import adaptadores.EnsambladorPartida;
import adaptadores.audio.CatalogoDeSonidos;
import adaptadores.audio.ControlDeVolumen;
import adaptadores.audio.ReproductorDeAudio;
import adaptadores.audio.ReproductorJavaSound;
import adaptadores.audio.SonidosDePartida;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.PartidaConfigurada;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
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

    @Override
    public void start(Stage ventana) {
        reproductor = new ReproductorJavaSound();
        VistaMenuPartida menu = new VistaMenuPartida(TemaPanel.marcador(), configuracion -> iniciarPartida(ventana, configuracion));
        ventana.setTitle("ChiquiChess");
        ventana.setScene(new Scene(menu));
        ajustarAPantalla(ventana, Screen.getPrimary().getVisualBounds());
        ventana.setOnHidden(evento -> cerrarPartidaActual());
        ventana.show();
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
        var preparada = new EnsambladorPartida().prepararConfigurada(configuracion);
        partidaActual = preparada;
        Partida partida = preparada.partida();
        TemaTablero tema = new TemaTablero(
                Color.web("#F0D9B5"), Color.web("#B58863"),
                Color.web("#FFFBEF"), Color.web("#202733"),
                Color.web("#202733"), Color.web("#1B212B"),
                Color.web("#F7F769"), Color.web("#202733", 0.18), Color.web("#202733", 0.23));
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
        ventana.getScene().setRoot(vista);
        sonidosActuales = SonidosDePartida.conectar(partida, catalogoDeSonidos, reproductor,
                Map.of(Bando.BLANCO, preparada.equipoBlancas(), Bando.NEGRO, preparada.equipoNegras()),
                volumen, configuracion.modo() == ConfiguracionPartida.Modo.FUTBOL);
        actualizadorReloj.iniciar();
        // Último: si la IA juega con blancas, empieza a pensar ya con la vista y el sonido listos.
        preparada.rival().ifPresent(rival -> {
            turnoDeLaIa = new TurnoDeLaIa(partida, rival, hilosDeLaIa, Platform::runLater, TurnoDeLaIa.PAUSA_MINIMA);
            turnoDeLaIa.iniciar();
        });
    }

    private void volverAlMenu(Stage ventana) {
        cerrarPartidaActual();
        ventana.getScene().setRoot(new VistaMenuPartida(TemaPanel.marcador(),
                configuracion -> iniciarPartida(ventana, configuracion)));
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
        cerrarPartidaActual();
        if (reproductor != null) reproductor.close();
        hilosDeLaIa.shutdownNow();
    }
}
