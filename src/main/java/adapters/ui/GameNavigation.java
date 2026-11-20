package adapters.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;

import java.util.Objects;

/** Navegación de presentación: consulta al núcleo y emite solicitudes al ensamblado. */
final class GameNavigation {
    private final BoardController controller;
    private final Button returnValue;
    private final Button rematch;
    private final StackPane confirmation = new StackPane();

    GameNavigation(BoardController controller, PanelTheme theme, Runnable onMenu, Runnable onRematch) {
        this.controller = Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(onMenu, "onMenu");
        Objects.requireNonNull(onRematch, "onRematch");
        returnValue = HeaderButton.create("Back to the menu", "return-menu", theme.texto(), theme.background(),
                PixelIcon.HOUSE, false);
        rematch = HeaderButton.create("Rematch", "rematch", theme.accent(), theme.background());
        confirmation.setId("confirmation-return-menu");
        confirmation.setVisible(false);
        confirmation.setManaged(false);
        confirmation.setAlignment(Pos.CENTER);
        confirmation.setBackground(new Background(new BackgroundFill(
                theme.background().deriveColor(0, 1, 1, .8), CornerRadii.EMPTY, Insets.EMPTY)));
        var asks = new JavaFxActionConfirmation(theme,
                "Back to the menu?\nThis game will be discarded.", "Back to the menu", "Cancel",
                "return-menu", theme.alerta(), () -> {
                    closeConfirmation();
                    onMenu.run();
                }, this::closeConfirmation);
        asks.setMaxSize(380, Region.USE_PREF_SIZE);
        StackPane.setMargin(asks, new Insets(20));
        confirmation.getChildren().add(asks);
        returnValue.setOnAction(e -> {
            controller.verifyTime();
            if (controller.state().isFinished()) {
                onMenu.run();
            } else {
                confirmation.setManaged(true);
                confirmation.setVisible(true);
                asks.focusCancel();
            }
        });
        rematch.setOnAction(e -> {
            controller.verifyTime();
            if (controller.state().isFinished()) onRematch.run();
        });
        update();
    }

    Button returnValue() { return returnValue; }
    Button rematch() { return rematch; }
    StackPane confirmation() { return confirmation; }

    void cancelWithEscape(KeyEvent event) {
        if (confirmation.isVisible() && event.getCode() == KeyCode.ESCAPE) {
            closeConfirmation();
            event.consume();
        }
    }

    void update() {
        boolean finished = controller.state().isFinished();
        rematch.setVisible(finished);
        rematch.setManaged(finished);
        rematch.setDisable(!finished);
        if (finished) closeConfirmation();
    }

    private void closeConfirmation() {
        boolean open = confirmation.isVisible();
        confirmation.setVisible(false);
        confirmation.setManaged(false);
        if (open) returnValue.requestFocus();
    }
}
