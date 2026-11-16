package adaptadores.ui;

import Nucleo.bando.Bando;

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
public final class VistaReloj extends StackPane {
    private final TemaPanel tema;
    private final TextoLed digitos;
    private final Text sinLimite = new Text("Sin límite");

    public VistaReloj(Bando color, TemaPanel tema) {
        Objects.requireNonNull(color, "color");
        this.tema = Objects.requireNonNull(tema, "tema");
        setId(color == Bando.BLANCO ? "reloj-blanco" : "reloj-negro");
        setAccessibleRole(AccessibleRole.TEXT);
        setAccessibleHelp(color == Bando.BLANCO ? "Tiempo de blancas" : "Tiempo de negras");
        setMouseTransparent(true);
        setAlignment(Pos.CENTER_RIGHT);
        setMinSize(0, 16);
        setPrefSize(136, 28);
        setMaxHeight(28);
        digitos = new TextoLed(tema.textoSecundario());
        sinLimite.setFont(Font.font("Consolas", 13));
        sinLimite.setFill(tema.textoSecundario());
        getChildren().addAll(digitos, sinLimite);
        actualizar(Optional.empty(), false);
    }

    public void actualizar(Optional<Duration> restante, boolean activo) {
        Objects.requireNonNull(restante, "restante");
        boolean limitado = restante.isPresent();
        digitos.setVisible(limitado);
        digitos.setManaged(limitado);
        sinLimite.setVisible(!limitado);
        sinLimite.setManaged(!limitado);
        String texto = restante.map(this::formatear).orElse("Sin límite");
        if (limitado) digitos.setTexto(texto);
        digitos.setColor(activo ? tema.activo() : tema.textoSecundario());
        setAccessibleText(texto);
        getStyleClass().remove("reloj-activo");
        if (limitado && activo) getStyleClass().add("reloj-activo");
    }

    private String formatear(Duration duracion) {
        long segundos = duracion.isNegative() ? 0 : duracion.getSeconds();
        // Redondeo solo visual: una fracción positiva todavía se muestra como un segundo.
        if (!duracion.isNegative() && duracion.getNano() > 0 && segundos < Long.MAX_VALUE) segundos++;
        return String.format(Locale.ROOT, "%02d:%02d", segundos / 60, segundos % 60);
    }
}
