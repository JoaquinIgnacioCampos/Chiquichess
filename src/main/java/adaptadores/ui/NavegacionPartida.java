package adaptadores.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Objects;

/** Navegación de presentación: consulta al núcleo y emite solicitudes al ensamblado. */
final class NavegacionPartida {
    private final ControladorTablero controlador;
    private final Button volver;
    private final Button revancha;
    private final StackPane confirmacion = new StackPane();

    NavegacionPartida(ControladorTablero controlador, TemaPanel tema, Runnable alMenu, Runnable alRevancha) {
        this.controlador = Objects.requireNonNull(controlador, "controlador");
        Objects.requireNonNull(alMenu, "alMenu");
        Objects.requireNonNull(alRevancha, "alRevancha");
        volver = boton("Volver al menú", "volver-menu", tema.texto(), tema.fondo());
        revancha = boton("Revancha", "revancha", tema.acento(), tema.fondo());
        confirmacion.setId("confirmacion-volver-menu");
        confirmacion.setVisible(false);
        confirmacion.setManaged(false);
        confirmacion.setAlignment(Pos.CENTER);
        confirmacion.setBackground(new Background(new BackgroundFill(
                tema.fondo().deriveColor(0, 1, 1, .8), CornerRadii.EMPTY, Insets.EMPTY)));
        var pregunta = new ConfirmacionAccionJavaFx(tema,
                "¿Volver al menú?\nSe descartará esta partida.", "Volver al menú", "Cancelar",
                "volver-menu", tema.alerta(), () -> {
                    cerrarConfirmacion();
                    alMenu.run();
                }, this::cerrarConfirmacion);
        pregunta.setMaxSize(380, Region.USE_PREF_SIZE);
        StackPane.setMargin(pregunta, new Insets(20));
        confirmacion.getChildren().add(pregunta);
        volver.setOnAction(e -> {
            controlador.verificarTiempo();
            if (controlador.estado().estaFinalizada()) {
                alMenu.run();
            } else {
                confirmacion.setManaged(true);
                confirmacion.setVisible(true);
                pregunta.enfocarCancelar();
            }
        });
        revancha.setOnAction(e -> {
            controlador.verificarTiempo();
            if (controlador.estado().estaFinalizada()) alRevancha.run();
        });
        actualizar();
    }

    Button volver() { return volver; }
    Button revancha() { return revancha; }
    StackPane confirmacion() { return confirmacion; }

    void cancelarConEscape(KeyEvent evento) {
        if (confirmacion.isVisible() && evento.getCode() == KeyCode.ESCAPE) {
            cerrarConfirmacion();
            evento.consume();
        }
    }

    void actualizar() {
        boolean finalizada = controlador.estado().estaFinalizada();
        revancha.setVisible(finalizada);
        revancha.setManaged(finalizada);
        revancha.setDisable(!finalizada);
        if (finalizada) cerrarConfirmacion();
    }

    private void cerrarConfirmacion() {
        boolean abierta = confirmacion.isVisible();
        confirmacion.setVisible(false);
        confirmacion.setManaged(false);
        if (abierta) volver.requestFocus();
    }

    private Button boton(String texto, String id, Color fondo, Color tinta) {
        Button boton = new Button(texto);
        boton.setId(id);
        boton.setFont(Font.font("System", FontWeight.BOLD, 13));
        boton.setPadding(new Insets(8, 14, 8, 14));
        boton.setCursor(Cursor.HAND);
        Runnable pintar = () -> boton.setStyle("-fx-background-color: " + css(fondo)
                + "; -fx-text-fill: " + css(tinta) + "; -fx-background-radius: 4; -fx-border-radius: 4;"
                + " -fx-border-width: 2; -fx-border-color: " + css(boton.isFocused() ? tinta : fondo) + ";");
        boton.focusedProperty().addListener((p, antes, ahora) -> pintar.run());
        pintar.run();
        return boton;
    }

    private String css(Color color) {
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
    }
}
