package adapters.ui;

import core.side.Side;
import core.game.*;
import core.rules.*;
import core.board.Position;
import core.time.NoClock;
import adapters.GameAssembler;
import doubles.FontOfTimeFalse;
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

import static doubles.Squares.*;
import static org.junit.jupiter.api.Assertions.*;

class GameStatePanelTest {
    private final PanelTheme themePanel = PanelTheme.scoreboard();
    private final BoardTheme themeBoard = new BoardTheme(
            javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
            javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
            javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.GRAY);

    private GameView view(GameInProgress game) {
        return new GameView(new BoardController(game), themeBoard,
                new UnicodeRepresentation(), themePanel);
    }

    private String texto(GameView view, String id) {
        Node node = view.lookup("#" + id);
        assertNotNull(node, id);
        return node instanceof Text texto ? texto.getText() : node.getAccessibleText();
    }

    private void click(GameView view, String square) {
        Position position = pos(square);
        view.lookup("#square-" + position.column() + "-" + position.row())
                .fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, true, null));
    }

    private void play(GameView view, String... moves) {
        for (String move : moves) {
            click(view, move.substring(0, 2));
            click(view, move.substring(2));
        }
    }

    @Test
    void showsNamesOfCoreAndInitialTurn() {
        GameInProgress game = new Game(new StandardLayout().createBoard(),
                new Player("Lucía Fernández", Side.WHITE), new Player("Tomás Rodríguez", Side.BLACK),
                new GameRules(new CheckDetector(), List.of()), new NoClock());
        GameView view = view(game);
        assertEquals("Lucía Fernández", texto(view, "displayName-white"));
        assertEquals("Tomás Rodríguez", texto(view, "displayName-black"));
        assertEquals("TURN OF", texto(view, "scoreboard-heading"));
        assertEquals("WHITE", texto(view, "scoreboard-principal"));
        assertEquals("IN PLAY", texto(view, "state-game"));
        assertTrue(view.lookup("#player-white").getStyleClass().contains("player-active"));
        assertFalse(view.lookup("#player-black").getStyleClass().contains("player-active"));
    }

    @Test
    void aMovementByClickUpdatesTurnAndActiveCard() {
        GameView view = view(new GameAssembler().createWithoutClock());
        play(view, "e2e4");
        assertEquals("BLACK", texto(view, "scoreboard-principal"));
        assertTrue(view.lookup("#player-black").getStyleClass().contains("player-active"));
        assertFalse(view.lookup("#player-white").getStyleClass().contains("player-active"));
    }

    @Test
    void cancelASelectionNotChangesTurnThatShowsPanel() {
        GameView view = view(new GameAssembler().createWithoutClock());
        click(view, "e2");
        click(view, "e6");
        assertEquals("WHITE", texto(view, "scoreboard-principal"));
        assertEquals("IN PLAY", texto(view, "state-game"));
    }

    @Test
    void noticeOfCheckAppearsAndDisappearsWithStateOfCore() {
        GameView view = view(new GameAssembler().createWithoutClock());
        play(view, "e2e4", "f7f6", "d1h5");
        assertEquals("BLACK", texto(view, "scoreboard-principal"));
        assertEquals("CHECK!", texto(view, "state-game"));
        assertEquals(themePanel.alerta(), ((Text) view.lookup("#state-game")).getFill());
        play(view, "g7g6");
        assertEquals("WHITE", texto(view, "scoreboard-principal"));
        assertEquals("IN PLAY", texto(view, "state-game"));
        assertEquals(themePanel.activo(), ((Text) view.lookup("#state-game")).getFill());
    }

    @Test
    void checkMateShowsWinnerAndReasonWithoutCardsActive() {
        GameView view = view(new GameAssembler().createWithoutClock());
        play(view, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals("WINNER:", texto(view, "scoreboard-heading"));
        assertEquals("BLACK", texto(view, "scoreboard-principal"));
        assertEquals("VICTORIA", texto(view, "state-game"));
        assertEquals("By checkmate", texto(view, "reason-final"));
        assertTrue(view.lookupAll(".player-active").isEmpty());
    }

    @Test
    void resignationUsesWinnerExplicitOfResult() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        GameView view = view(game);
        game.move(move("e2", "e4"));
        game.resign(Side.BLACK);
        view.redraw();
        assertEquals("WINNER:", texto(view, "scoreboard-heading"));
        assertEquals("WHITE", texto(view, "scoreboard-principal"));
        assertEquals("By resignation", texto(view, "reason-final"));
        assertTrue(view.lookupAll(".player-active").isEmpty());
    }

    @Test
    void drawByAgreementShowsTieAndItsReason() {
        GameInProgress game = new GameAssembler().createWithoutClock();
        GameView view = view(game);
        game.offerDraw(Side.WHITE);
        game.acceptDraw(Side.BLACK);
        view.redraw();
        assertEquals("DRAW", texto(view, "scoreboard-principal"));
        assertEquals("TIE", texto(view, "state-game"));
        assertEquals("By agreement", texto(view, "reason-final"));
        assertTrue(view.lookupAll(".player-active").isEmpty());
    }

    @Test
    void distinguishesThreeReasonsOfDrawAutomatic() {
        Map<GameState, String> cases = Map.of(
                GameState.DRAW_STALEMATE, "By stalemate",
                GameState.DRAW_REPETITION, "By repetition",
                GameState.DRAW_RULE_50_MOVEMENTS, "50-move rule");
        cases.forEach((state, reason) -> {
            GameInProgress game = new Game(new StandardLayout().createBoard(),
                    new Player("B", Side.WHITE), new Player("N", Side.BLACK),
                    new GameRules(new CheckDetector(), List.of(c -> Optional.of(state))), new NoClock());
            GameView view = view(game);
            assertEquals("DRAW", texto(view, "scoreboard-principal"));
            assertEquals(reason, texto(view, "reason-final"));
            assertTrue(view.lookupAll(".player-active").isEmpty());
        });
    }

    @Test
    void rejectionByTimeAlsoUpdatesResultInPanel() {
        FontOfTimeFalse font = new FontOfTimeFalse();
        GameInProgress game = new GameAssembler().createWithClock(Duration.ofSeconds(1), font);
        GameView view = view(game);
        click(view, "e2");
        font.advance(Duration.ofSeconds(1));
        click(view, "e4");
        assertEquals("BLACK", texto(view, "scoreboard-principal"));
        assertEquals("On time", texto(view, "reason-final"));
        assertFalse(texto(view, "message-movement").isBlank());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void panelKeeps35ByHundredAndBoardSquaresSquareToResize() {
        GameView view = view(new GameAssembler().createWithoutClock());
        for (int[] size : List.of(new int[]{900, 620}, new int[]{1280, 820}, new int[]{1600, 900})) {
            view.resize(size[0], size[1]);
            view.layout();
            Region panel = (Region) view.lookup("#panel-state");
            Region game = (Region) view.lookup("#area-game");
            Region square = (Region) view.lookup("#square-0-0");
            assertEquals(size[0] * 0.35, panel.getWidth(), 0.01);
            assertEquals(size[0] * 0.65, game.getWidth(), 0.01);
            assertEquals(square.getWidth(), square.getHeight(), 1.01);
            assertEquals(64, view.lookupAll(".square").size());
        }
    }

    @Test
    void namesKeepInsideOfPanelInWindowMinimum() {
        GameInProgress game = new Game(new StandardLayout().createBoard(),
                new Player("Lucía Fernández", Side.WHITE), new Player("Tomás Rodríguez", Side.BLACK),
                new GameRules(new CheckDetector(), List.of()), new NoClock());
        GameView view = view(game);
        view.resize(900, 620);
        view.layout();
        var limits = view.lookup("#panel-state").localToScene(view.lookup("#panel-state").getLayoutBounds());
        for (String id : List.of("displayName-white", "displayName-black")) {
            Node displayName = view.lookup("#" + id);
            var box = displayName.localToScene(displayName.getLayoutBounds());
            assertTrue(box.getMaxX() <= limits.getMaxX() + 1);
            assertTrue(box.getMaxY() <= limits.getMaxY() + 1);
            assertTrue(box.getMinX() >= limits.getMinX() - 1);
        }
    }

    // ---------------- Modo fútbol ----------------

    /** Torre blanca con la pelota en d2, a un movimiento del gol en d8. */
    private Game footballWithRookInD2() {
        return footballWithRookInD2(GameRules.football());
    }

    private Game footballWithRookInD2(GameRules rules) {
        core.board.Board board = doubles.BoardFake.from(
                ".......k",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...R....",
                "....K...");
        new core.rules.BallRule().preparePitch(board);
        board.placeBall(pos("d2"));
        return new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                rules, new core.time.NoClock());
    }

    @Test
    void inModeFootballShowsScoreboardAndMoves() {
        GameView view = view(footballWithRookInD2());
        assertEquals("WHITE 0 - 0 BLACK", texto(view, "goals-game"));
        assertEquals("Move 0 of 90", texto(view, "moves-football"));
        assertEquals("IN PLAY", texto(view, "state-game"));
    }

    @Test
    void announcesGoalAndUpdatesScoreboard() {
        GameView view = view(footballWithRookInD2());
        play(view, "d2d8");
        assertEquals("GOAL!", texto(view, "state-game"));
        assertEquals("WHITE 1 - 0 BLACK", texto(view, "goals-game"));
        assertEquals("Move 1 of 90", texto(view, "moves-football"));

        play(view, "h8g7");
        assertEquals("IN PLAY", texto(view, "state-game"));
        assertEquals("WHITE 1 - 0 BLACK", texto(view, "goals-game"));
    }

    @Test
    void undoGoalLeavesOfAnnounceItAndRedoItItReturnsToAnnounce() {
        // Arrange
        GameInProgress game = footballWithRookInD2(GameRules.football().withUndo());
        GameView view = view(game);
        play(view, "d2d8");

        // Act + Assert: se deshace y rehace desde la partida, no desde la vista; la vista se entera por los eventos
        game.undo();
        assertEquals("IN PLAY", texto(view, "state-game"));
        assertEquals("WHITE 0 - 0 BLACK", texto(view, "goals-game"));
        game.redo();
        assertEquals("GOAL!", texto(view, "state-game"));
        assertEquals("WHITE 1 - 0 BLACK", texto(view, "goals-game"));
    }

    @Test
    void finalByTimeShowsByEventWithoutRedrawToHand() {
        // Arrange
        FontOfTimeFalse font = new FontOfTimeFalse();
        GameInProgress game = new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("Beto", Side.BLACK), GameRules.classic(),
                new core.time.ChessClock(Duration.ofMinutes(1), Duration.ofMinutes(1), font));
        GameView view = view(game);

        // Act: lo que hace el reloj de la interfaz en cada pulso
        font.advance(Duration.ofMinutes(2));
        game.verifyTime();

        // Assert
        assertEquals("FINAL RESULT", texto(view, "title-state"));
        assertEquals("BLACK", texto(view, "scoreboard-principal"));
    }

    @Test
    void chessClassicNotShowsGoals() {
        GameView view = view(new GameAssembler().createWithoutClock());
        assertEquals("", texto(view, "goals-game"));
        assertFalse(view.lookup("#goals-game").isVisible());
    }

    @Test
    void showsLimitOfMovesChosen() {
        GameView view = view(new GameAssembler().createWithoutClockWithUndo(
                adapters.configuration.GameConfiguration.Mode.FOOTBALL, 40));
        assertEquals("Move 0 of 40", texto(view, "moves-football"));
    }

    // ---------------- Cartel de gol ----------------

    @Test
    void withoutGoalNotHasSign() {
        GameView view = view(footballWithRookInD2());
        assertFalse(view.lookup("#sign-goal").isVisible());
    }

    @Test
    void goalShowsSignOfLightsWithNameOfSide() {
        GameView view = view(footballWithRookInD2());

        play(view, "d2d8");

        assertTrue(view.lookup("#sign-goal").isVisible());
        assertEquals("GOAL!", texto(view, "goal-lights"));
        assertEquals("WHITE", texto(view, "goal-team"));
    }

    @Test
    void withClubsSignCarriesCrestAndColorOfTeamThatDidGoal() {
        var clubs = Map.of(
                Side.WHITE, ClubPresentation.de(adapters.configuration.GameConfiguration.Team.ARGENTINA, themePanel),
                Side.BLACK, ClubPresentation.de(adapters.configuration.GameConfiguration.Team.BOCA, themePanel));
        GameView view = new GameView(new BoardController(footballWithRookInD2()), themeBoard,
                new UnicodeRepresentation(), themePanel, clubs);

        play(view, "d2d8");

        assertEquals("ARGENTINA", texto(view, "goal-team"));
        var sign = view.lookup("#sign-goal");
        var crest = (javafx.scene.image.ImageView) sign.lookup(".image-view");
        assertSame(clubs.get(Side.WHITE).crest(), crest.getImage());
        assertTrue(crest.isVisible());
    }
}
