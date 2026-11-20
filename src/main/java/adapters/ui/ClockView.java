package adapters.ui;

import core.side.Side;

import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Presenta una duración del núcleo, sin descontar tiempo ni determinar resultados. */
public final class ClockView extends StackPane {
    private final PanelTheme theme;
    private final LedText digits;
    private final Text withoutLimit = new Text("Sin límite");

    public ClockView(Side color, PanelTheme theme) {
        Objects.requireNonNull(color, "color");
        this.theme = Objects.requireNonNull(theme, "tema");
        setId(color == Side.WHITE ? "reloj-blanco" : "reloj-negro");
        setAccessibleRole(AccessibleRole.TEXT);
        setAccessibleHelp(color == Side.WHITE ? "Tiempo de blancas" : "Tiempo de negras");
        setMouseTransparent(true);
        setAlignment(Pos.CENTER_RIGHT);
        setMinSize(0, 16);
        setPrefSize(136, 28);
        setMaxHeight(28);
        digits = new LedText(theme.textSecondary());
        withoutLimit.setFont(PixelStyle.font(13, false));
        withoutLimit.setFill(theme.textSecondary());
        getChildren().addAll(digits, withoutLimit);
        update(Optional.empty(), false);
    }

    public void update(Optional<Duration> remaining, boolean activo) {
        Objects.requireNonNull(remaining, "restante");
        boolean limited = remaining.isPresent();
        digits.setVisible(limited);
        digits.setManaged(limited);
        withoutLimit.setVisible(!limited);
        withoutLimit.setManaged(!limited);
        String texto = remaining.map(this::format).orElse("Sin límite");
        if (limited) digits.setText(texto);
        digits.setColor(activo ? theme.activo() : theme.textSecondary());
        setAccessibleText(texto);
        getStyleClass().remove("reloj-activo");
        if (limited && activo) getStyleClass().add("reloj-activo");
    }

    private String format(Duration duration) {
        long seconds = duration.isNegative() ? 0 : duration.getSeconds();
        // Redondeo solo visual: una fracción positiva todavía se muestra como un segundo.
        if (!duration.isNegative() && duration.getNano() > 0 && seconds < Long.MAX_VALUE) seconds++;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
