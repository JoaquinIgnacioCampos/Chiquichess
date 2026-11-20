package adapters.ui;

import core.side.Side;
import core.pieces.Piece;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Dibuja cada pieza con el sprite del club de su bando; si no lo hay, delega en otra representación. */
public final class SpriteRepresentation implements PieceRepresentation {
    /**
     * Alto del sprite, en casillas: pasa un poco el de la casilla y la pieza sobresale hacia arriba, como en
     * un tablero con piezas de volumen. Así el busto se ve más grande y con más detalle.
     */
    private static final double HEIGHT_SPRITE = SIDE_BASE * 1.15;
    /** Relación ancho/alto de los sprites (256 x 384). */
    private static final double PROPORTION = 2.0 / 3.0;
    /** Espacio entre la base del sprite y el borde de abajo de la casilla. */
    private static final double MARGIN_BASE = SIDE_BASE * 0.02;

    private final Map<Side, SpriteCatalog> catalogs;
    private final PieceRepresentation fallback;

    public SpriteRepresentation(Map<Side, SpriteCatalog> catalogs, PieceRepresentation fallback) {
        this.catalogs = Map.copyOf(Objects.requireNonNull(catalogs, "catalogos"));
        this.fallback = Objects.requireNonNull(fallback, "respaldo");
    }

    @Override
    public Node create(Piece piece, BoardTheme theme) {
        Objects.requireNonNull(piece, "pieza");
        Objects.requireNonNull(theme, "tema");
        return Optional.ofNullable(catalogs.get(piece.color()))
                .flatMap(catalog -> catalog.sprite(piece.code(), piece.color()))
                .<Node>map(sprite -> figure(sprite, piece))
                .orElseGet(() -> fallback.create(piece, theme));
    }

    private Node figure(Image sprite, Piece piece) {
        ImageView image = new ImageView(sprite);
        image.setFitHeight(HEIGHT_SPRITE);
        image.setFitWidth(HEIGHT_SPRITE * PROPORTION);
        image.setPreserveRatio(true);
        image.setSmooth(true);
        image.setMouseTransparent(true);
        // El contenedor mide justo una casilla (así se escala y se centra como cualquier otra pieza) y el
        // sprite apoya su base abajo y sobresale hacia arriba, sin recortarse.
        Pane figure = new Pane(image);
        figure.setMinSize(SIDE_BASE, SIDE_BASE);
        figure.setPrefSize(SIDE_BASE, SIDE_BASE);
        figure.setMaxSize(SIDE_BASE, SIDE_BASE);
        image.setLayoutX((SIDE_BASE - HEIGHT_SPRITE * PROPORTION) / 2);
        image.setLayoutY(SIDE_BASE - MARGIN_BASE - HEIGHT_SPRITE);
        figure.setMouseTransparent(true);
        figure.setAccessibleText(piece.displayName() + " " + (piece.color() == core.side.Side.WHITE ? "BLANCO" : "NEGRO"));
        return figure;
    }
}
