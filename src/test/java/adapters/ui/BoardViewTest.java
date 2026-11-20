package adapters.ui;

import core.side.Side;
import core.pieces.*;
import core.board.Position;
import core.board.Board;
import adapters.GameAssembler;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

import static doubles.Squares.move;
import static org.junit.jupiter.api.Assertions.*;

class BoardViewTest {

    /** La pelota y las marcas son imágenes: necesitan el toolkit de JavaFX iniciado, sea cual sea el orden de los tests. */
    @org.junit.jupiter.api.BeforeAll
    static void startJavaFx() {
        try {
            javafx.application.Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // otro test ya lo inició
        }
    }

    private final BoardTheme theme = new BoardTheme(
            javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
            javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
            javafx.scene.paint.Color.DARKSLATEGRAY, javafx.scene.paint.Color.GRAY);

    private StackPane square(javafx.scene.Node view, int column, int row) {
        return (StackPane) view.lookup("#casilla-" + column + "-" + row);
    }

    @Test
    void usesDimensionsRealEvenInBoardsRectangular() {
        BoardView view = new BoardView(new Board(10, 6), theme, new UnicodeRepresentation());
        assertEquals(60, view.lookupAll(".casilla").size());
        assertNotNull(square(view, 9, 5));
        assertNull(square(view, 10, 5));
        assertNull(square(view, 9, 6));
        assertEquals(5, GridPane.getRowIndex(square(view, 0, 0)));
        assertEquals(0, GridPane.getRowIndex(square(view, 0, 5)));
        assertEquals(9, GridPane.getColumnIndex(square(view, 9, 5)));
    }

    @Test
    void alternatesFromCoordinatesOfCoreWithTo1Dark() {
        BoardView view = new BoardView(new Board(3, 5), theme, new UnicodeRepresentation());
        assertEquals(javafx.scene.paint.Color.BROWN,
                square(view, 0, 0).getBackground().getFills().getFirst().getFill());
        assertEquals(javafx.scene.paint.Color.BEIGE,
                square(view, 1, 0).getBackground().getFills().getFirst().getFill());
        assertEquals(javafx.scene.paint.Color.BEIGE,
                square(view, 0, 1).getBackground().getFills().getFirst().getFill());
        assertEquals(javafx.scene.paint.Color.BROWN,
                square(view, 2, 4).getBackground().getFills().getFirst().getFill());
    }

    @Test
    void changesColorsToInjectOtherThemeWithoutModifyView() {
        BoardTheme otherTheme = new BoardTheme(javafx.scene.paint.Color.YELLOW,
                javafx.scene.paint.Color.BLUE, javafx.scene.paint.Color.WHITE,
                javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.BLACK,
                javafx.scene.paint.Color.NAVY);
        BoardView view = new BoardView(new Board(2, 2), otherTheme, new UnicodeRepresentation());
        assertEquals(javafx.scene.paint.Color.BLUE,
                square(view, 0, 0).getBackground().getFills().getFirst().getFill());
        assertEquals(javafx.scene.paint.Color.YELLOW,
                square(view, 1, 0).getBackground().getFills().getFirst().getFill());
    }

    @Test
    void whiteDownAndBlackUpAccordingPiecesOfCore() {
        Board board = new Board(4, 6);
        board.place(new Position(0, 0), new King(Side.WHITE));
        board.place(new Position(3, 5), new King(Side.BLACK));
        BoardView view = new BoardView(board, theme, new UnicodeRepresentation());
        assertEquals("♚", ((Text) square(view, 0, 0).getChildren().getFirst()).getText());
        assertEquals("♚", ((Text) square(view, 3, 5).getChildren().getFirst()).getText());
        assertEquals(5, GridPane.getRowIndex(square(view, 0, 0)));
        assertEquals(0, GridPane.getRowIndex(square(view, 3, 5)));
    }

    @Test
    void redrawQueryCurrentStateInPlaceOfACopyLocal() {
        var game = new GameAssembler().createWithoutClock();
        GameView view = new GameView(new BoardController(game), theme,
                new UnicodeRepresentation(), PanelTheme.scoreboard());
        assertFalse(square(view, 4, 1).getChildren().isEmpty());
        assertTrue(square(view, 4, 3).getChildren().isEmpty());
        game.move(move("e2", "e4"));
        view.redraw();
        assertTrue(square(view, 4, 1).getChildren().isEmpty());
        assertEquals("♟", ((Text) square(view, 4, 3).getChildren().getFirst()).getText());
        assertEquals(64, view.lookupAll(".casilla").size());
    }

    @Test
    void onResizeKeepsSquaresSquareAndBoardCentered() {
        BoardView view = new BoardView(new Board(10, 6), theme, new UnicodeRepresentation());
        view.resize(700, 700);
        view.layout();
        GridPane grid = (GridPane) view.getChildrenUnmodifiable().getFirst();
        assertEquals(700, grid.getWidth(), 0.01);
        assertEquals(420, grid.getHeight(), 0.01);
        assertEquals(0, grid.getLayoutX(), 0.01);
        assertEquals(140, grid.getLayoutY(), 0.01);
        assertEquals(square(view, 0, 0).getWidth(), square(view, 0, 0).getHeight(), 0.01);
    }

    @Test
    void drawsBallOnlyInItsSquare() {
        Board board = new Board(8, 8);
        board.placeBall(new Position(3, 4));
        BoardView view = new BoardView(board, theme, new UnicodeRepresentation());
        assertEquals(1, view.lookupAll(".pelota").size());
        assertNotNull(square(view, 3, 4).lookup("#pelota"));
    }

    @Test
    void ballFollowsToPieceThatCarries() {
        Board board = new Board(8, 8);
        board.place(new Position(3, 4), new Knight(Side.WHITE));
        board.placeBall(new Position(3, 4));
        BoardView view = new BoardView(board, theme, new UnicodeRepresentation());
        StackPane withBall = square(view, 3, 4);
        assertNotNull(withBall.lookup("#pelota"));
        assertEquals(2, withBall.getChildren().size()); // la pieza y la pelota
    }

    @Test
    void chessClassicNotHasBall() {
        BoardView view = new BoardView(new Board(8, 8), theme, new UnicodeRepresentation());
        assertTrue(view.lookupAll(".pelota").isEmpty());
    }

    @Test
    void marksSquaresOfNet() {
        Board board = new Board(8, 8);
        board.markNet(new Position(3, 7), Side.BLACK);
        board.markNet(new Position(4, 0), Side.WHITE);
        BoardView view = new BoardView(board, theme, new UnicodeRepresentation());
        assertEquals(2, view.lookupAll(".arco").size());
        assertNotNull(square(view, 3, 7).lookup("#arco-3-7"));
        assertNotNull(square(view, 4, 0).lookup("#arco-4-0"));
    }

    @Test
    void netStaysBelowOfPiece() {
        Board board = new Board(8, 8);
        board.markNet(new Position(4, 7), Side.BLACK);
        board.place(new Position(4, 7), new King(Side.BLACK));
        BoardView view = new BoardView(board, theme, new UnicodeRepresentation());
        StackPane square = square(view, 4, 7);
        assertTrue(square.getChildren().get(0).getStyleClass().contains("arco"));
        assertEquals(2, square.getChildren().size());
    }

    @Test
    void aDestinationOccupiedByAPartnerMarksAsPass() {
        Board board = new Board(8, 8);
        board.place(new Position(0, 0), new Rook(Side.WHITE));
        board.place(new Position(0, 4), new Knight(Side.WHITE));
        board.place(new Position(0, 6), new Knight(Side.BLACK));
        BoardView view = new BoardView(board, theme, new UnicodeRepresentation());
        view.redraw(java.util.Optional.of(new Position(0, 0)),
                java.util.Set.of(new Position(0, 4), new Position(0, 6)));
        assertEquals(1, square(view, 0, 4).lookupAll(".indicador-pase").size());
        assertTrue(square(view, 0, 4).lookupAll(".indicador-captura").isEmpty());
        assertEquals(1, square(view, 0, 6).lookupAll(".indicador-captura").size());
    }
    @Test
    void fromBlackBoardStaysTurnedWithTo1UpToRight() {
        BoardView view = new BoardView(new Board(8, 8), theme, new UnicodeRepresentation());
        view.lookFrom(core.side.Side.BLACK);
        assertEquals(7, GridPane.getColumnIndex(square(view, 0, 0)));
        assertEquals(0, GridPane.getRowIndex(square(view, 0, 0)));
        assertEquals(0, GridPane.getColumnIndex(square(view, 7, 7)));
        assertEquals(7, GridPane.getRowIndex(square(view, 7, 7)));
    }
}
