package adapters.ui;

import core.board.Board;
import core.board.Position;
import core.side.Side;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las líneas del arco se pintan con la tiza del pasto y se ven claras sobre el césped (se mira el dibujo real). */
class GoalChalkTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // otro test ya lo inició
        }
    }

    /** Un pasto verde oscuro con algo de variación, como textura de prueba. */
    private static Image grass() {
        WritableImage image = new WritableImage(32, 32);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                double variation = ((x * 7 + y * 13) % 10) / 10.0;
                image.getPixelWriter().setColor(x, y, Color.color(0.15, 0.35 + 0.2 * variation, 0.1));
            }
        }
        return image;
    }

    private WritableImage draw(Board board, Image texture) throws Exception {
        BoardTheme theme = new BoardTheme(Color.web("#9FBE70"), Color.web("#527B3D"), Color.WHITE, Color.BLACK,
                Color.BLACK, Color.GRAY, Color.web("#F7F769"), Color.color(0, 0, 0, 0.18),
                Color.color(0, 0, 0, 0.23), texture);
        CompletableFuture<WritableImage> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                BoardView view = new BoardView(board, theme, new UnicodeRepresentation());
                Scene scene = new Scene(view, 640, 640);
                view.applyCss();
                view.layout();
                result.complete(scene.snapshot(null));
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result.get(20, TimeUnit.SECONDS);
    }

    private static double brightness(Color color) {
        return (color.getRed() + color.getGreen() + color.getBlue()) / 3;
    }

    @Test
    void goalLineWithTextureShowsLightOnTheGrass() throws Exception {
        Board board = new Board(8, 8);
        board.markNet(new Position(3, 7), Side.BLACK);   // fila de arriba: la línea de gol va en el borde de arriba
        PixelReader pixels = draw(board, grass()).getPixelReader();
        // casilla (3, 7): columna 3, primera fila de la pantalla; la línea ocupa los primeros 5 píxeles de alto
        Color line = pixels.getColor(3 * 80 + 40, 2);
        Color grassBeside = pixels.getColor(2 * 80 + 40, 40);   // el pasto, en medio de la casilla de al lado
        assertTrue(brightness(line) > 0.75, "la línea de gol tiene que verse clara: " + line);
        assertTrue(brightness(line) > brightness(grassBeside) + 0.2, "y más clara que el pasto de al lado");
    }

    @Test
    void goalLineWithoutTextureIsPlainWhite() throws Exception {
        Board board = new Board(8, 8);
        board.markNet(new Position(3, 7), Side.BLACK);
        Color line = draw(board, null).getPixelReader().getColor(3 * 80 + 40, 2);
        assertTrue(brightness(line) > 0.9, "sin textura la línea es blanca: " + line);
    }

    @Test
    void goalLineWithTheRealGrassOfTheGameShowsLight() throws Exception {
        Board board = new Board(8, 8);
        board.markNet(new Position(3, 7), Side.BLACK);
        Image realGrass = new Image("/tablero/pasto.jpg", 1280, 1280, true, true);
        PixelReader pixels = draw(board, realGrass).getPixelReader();
        for (int y = 0; y < 12; y += 2) {
            System.out.println("y=" + y + " " + pixels.getColor(3 * 80 + 40, y));
        }
        double brightest = 0;
        for (int y = 0; y < 80; y++) {
            brightest = Math.max(brightest, brightness(pixels.getColor(3 * 80 + 40, y)));
        }
        assertTrue(brightest > 0.75, "la línea de gol real tiene que verse clara: " + brightest);
    }
}
