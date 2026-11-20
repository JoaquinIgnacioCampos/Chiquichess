package adapters.ui;

import core.side.Side;
import core.game.GameInProgress;
import adapters.GameAssembler;
import adapters.audio.SoundCatalog;
import adapters.audio.VolumeControl;
import adapters.audio.AudioPlayer;
import adapters.audio.JavaSoundPlayer;
import adapters.audio.GameSounds;
import adapters.audio.MenuSounds;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import adapters.configuration.GameConfiguration;
import adapters.configuration.SettingsFile;
import adapters.configuration.ConfiguredGame;
import adapters.configuration.Preferences;
import adapters.configuration.MenuSelection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** Arranque gráfico: ensambla el juego e inyecta la partida en la vista. */
public class ChessApplication extends Application {
    /** Tamaños de la ventana con sus bordes; en una pantalla más chica manda la pantalla. */
    private static final double WIDTH_PREFERRED = 1296;
    private static final double HEIGHT_PREFERRED = 858;
    private static final double WIDTH_MINIMUM = 900;
    private static final double HEIGHT_MINIMUM = 620;

    private JavaFxClockUpdater updaterClock;
    private ConfiguredGame currentGame;
    private GameSounds soundsCurrent;
    private AiTurn turnOfAi;
    /** Hilos donde piensa la IA; son daemon para no impedir que el programa se cierre. */
    private final ExecutorService aiThreads = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "ia-pensando");
        thread.setDaemon(true);
        return thread;
    });
    /** Uno solo para todo el programa: el volumen elegido sigue valiendo en las partidas siguientes. */
    private final VolumeControl volume = new VolumeControl();
    private final SoundCatalog soundCatalog = new SoundCatalog();
    private AudioPlayer player;
    private MenuSounds menuSounds;
    private IntroScreen intro;
    /** Lo que muestra elegido el menú: la partida habitual al abrir, y después lo último que se jugó. */
    private MenuSelection menuSelection = MenuSelection.usual();
    /** Opciones y sonido guardados entre una vez y la otra, en la carpeta desde la que se abre el juego. */
    private final SettingsFile settingsFile =
            new SettingsFile(java.nio.file.Path.of("configuracion.txt"));

    @Override
    public void start(Stage ventana) {
        Preferences saved = settingsFile.read();
        menuSelection = saved.selection();
        volume.changeVolume(saved.volume());
        volume.mute(saved.muted());
        player = new JavaSoundPlayer();
        menuSounds = new MenuSounds(soundCatalog, player, volume);
        ventana.setTitle("ChiquiChess");
        // El personaje de la portada también es el ícono de la ventana y de la barra de tareas.
        ventana.getIcons().add(new Image("/personaje/chiquichess.png"));
        // Arranca con la intro: video, logo y un botón para comenzar, que lleva al menú.
        intro = new IntroScreen(PanelTheme.scoreboard(), volume, () -> enterMenu(ventana));
        ventana.setScene(new Scene(intro));
        adjustToScreen(ventana, Screen.getPrimary().getVisualBounds());
        ventana.setOnHidden(event -> closeCurrentGame());
        // Arranca maximizada; el tamaño calculado queda para cuando se restaura.
        ventana.setMaximized(true);
        ventana.show();
        WindowsTitleBar.paint(ventana, PanelTheme.scoreboard().background(), PanelTheme.scoreboard().texto());
        intro.play();
    }

    /** Al tocar "Comenzar": el menú, con la risa y el balanceo del personaje que se dan al abrir el juego. */
    private void enterMenu(Stage ventana) {
        GameMenuView menu = newMenu(ventana);
        menu.setStyle(menu.getStyle() + PixelStyle.familyCss());
        ventana.getScene().setRoot(menu);
        menuSounds.openGame();
        menu.animateCharacter();
    }

    private GameMenuView newMenu(Stage ventana) {
        GameMenuView menu = new GameMenuView(PanelTheme.scoreboard(), configuration -> {
            menuSelection = MenuSelection.de(configuration);
            savePreferences();
            startGame(ventana, configuration);
        });
        menu.preselect(menuSelection);
        menu.configureSound(volume);
        menu.onChooseOption(menuSounds::chosenOption);
        return menu;
    }

    /** La ventana arranca entera dentro de la pantalla, centrada y con la barra de título visible. */
    private void adjustToScreen(Stage ventana, Rectangle2D screen) {
        double width = Math.min(WIDTH_PREFERRED, screen.getWidth());
        double height = Math.min(HEIGHT_PREFERRED, screen.getHeight());
        ventana.setWidth(width);
        ventana.setHeight(height);
        ventana.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
        ventana.setY(screen.getMinY() + (screen.getHeight() - height) / 2);
        ventana.setMinWidth(Math.min(WIDTH_MINIMUM, width));
        ventana.setMinHeight(Math.min(HEIGHT_MINIMUM, height));
    }

    private void startGame(Stage ventana, GameConfiguration configuration) {
        closeCurrentGame();
        menuSounds.leaveMenu();
        var prepared = new GameAssembler().prepareConfigured(configuration);
        currentGame = prepared;
        GameInProgress game = prepared.game();
        BoardTheme theme = new BoardTheme(
                Color.web("#9FBE70"), Color.web("#527B3D"),
                Color.web("#FFFBEF"), Color.web("#202733"),
                Color.web("#202733"), Color.web("#1B212B"),
                Color.web("#F7F769"), Color.web("#202733", 0.18), Color.web("#202733", 0.23),
                new Image("/tablero/pasto.jpg", 1280, 1280, true, true));
        BoardController controller = new BoardController(game, prepared.rival());
        PanelTheme themePanel = PanelTheme.scoreboard();
        var clubs = java.util.Map.of(
                Side.WHITE, ClubPresentation.de(prepared.whiteTeam(), themePanel),
                Side.BLACK, ClubPresentation.de(prepared.blackTeam(), themePanel));
        PieceRepresentation representation = new SpriteRepresentation(java.util.Map.of(
                Side.WHITE, SpriteCatalog.de(prepared.whiteTeam()),
                Side.BLACK, SpriteCatalog.de(prepared.blackTeam())), new UnicodeRepresentation());
        GameView view = new GameView(controller, theme, representation, themePanel, clubs,
                new InterfaceMessages());
        view.configureResignation();
        view.configureDraw();
        view.configureUndo();
        view.configureSound(volume);
        view.configureNavigation(() -> {
            if (currentGame == prepared) returnToMenu(ventana);
        }, () -> startRematch(ventana, configuration, prepared));
        updaterClock = new JavaFxClockUpdater(controller, view::updateClocks);
        // Contra la IA, el tablero se ve siempre desde el lado de la persona, aunque juegue con negras.
        prepared.rival().ifPresent(rival -> view.lookFrom(rival.side().opposite()));
        ventana.getScene().setRoot(view);
        GameSounds sounds = GameSounds.connect(game, soundCatalog, player,
                Map.of(Side.WHITE, prepared.whiteTeam(), Side.BLACK, prepared.blackTeam()),
                volume, configuration.mode() == GameConfiguration.Mode.FUTBOL);
        soundsCurrent = sounds;
        view.onSelectPiece(sounds::selectedPiece);
        updaterClock.start();
        // Último: si la IA juega con blancas, empieza a pensar ya con la vista y el sonido listos.
        prepared.rival().ifPresent(rival -> {
            turnOfAi = new AiTurn(game, rival, aiThreads, Platform::runLater);
            turnOfAi.start();
        });
    }

    private void returnToMenu(Stage ventana) {
        closeCurrentGame();
        ventana.getScene().setRoot(newMenu(ventana));
        menuSounds.enterMenu();
    }

    private void startRematch(Stage ventana, GameConfiguration previous, ConfiguredGame prepared) {
        if (currentGame != prepared || !prepared.game().state().isFinished()) return;
        var colors = prepared.whiteTeam() == previous.teamPlayer1()
                ? GameConfiguration.Colors.JUGADOR_2_BLANCAS : GameConfiguration.Colors.JUGADOR_1_BLANCAS;
        startGame(ventana, new GameConfiguration(previous.mode(), previous.time(), previous.rival(),
                previous.teamPlayer1(), previous.teamPlayer2(), colors, previous.allowUndo(),
                previous.moveLimit(), previous.difficulty()));
    }

    private void closeCurrentGame() {
        if (turnOfAi != null) {
            turnOfAi.close();
            turnOfAi = null;
        }
        if (soundsCurrent != null) {
            soundsCurrent.close();
            soundsCurrent = null;
        }
        if (updaterClock != null) {
            updaterClock.close();
            updaterClock = null;
        }
        if (currentGame != null) {
            currentGame.close();
            currentGame = null;
        }
    }

    private void savePreferences() {
        settingsFile.save(new Preferences(menuSelection, volume.volume(), volume.muted()));
    }

    @Override public void stop() {
        savePreferences();
        if (intro != null) intro.stop();
        closeCurrentGame();
        if (menuSounds != null) menuSounds.close();
        if (player != null) player.close();
        aiThreads.shutdownNow();
    }
}
