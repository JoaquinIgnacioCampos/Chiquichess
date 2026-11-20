package adapters.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** El logo de ChiquiChess, que reemplaza al nombre escrito en el menú y en la cabecera de la partida. */
final class Logo {
    private static final String RESOURCE = "/intro/logo.png";
    private static Image image;

    private Logo() {
    }

    private static Image image() {
        if (image == null) image = new Image(RESOURCE);
        return image;
    }

    /** Una vista del logo, lista para ponerle el tamaño; mantiene la proporción de la imagen. */
    static ImageView create() {
        ImageView logo = new ImageView(image());
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setAccessibleText("ChiquiChess");
        return logo;
    }
}
