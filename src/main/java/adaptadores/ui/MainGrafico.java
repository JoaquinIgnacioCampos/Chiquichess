package adaptadores.ui;

import javafx.application.Application;

/** Lanzador común para ejecutar desde el IDE sin configurar el module-path a mano. */
public final class MainGrafico {
    private MainGrafico() {
    }

    public static void main(String[] args) {
        Application.launch(AplicacionAjedrez.class, args);
    }
}
