package adapters.ui;

import javafx.application.Application;

/** Lanzador común para ejecutar desde el IDE sin configurar el module-path a mano. */
public final class GraphicalMain {
    private GraphicalMain() {
    }

    public static void main(String[] args) {
        Application.launch(ChessApplication.class, args);
    }
}
