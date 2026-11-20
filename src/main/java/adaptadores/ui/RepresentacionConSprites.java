package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.piezas.Pieza;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Dibuja cada pieza con el sprite del club de su bando; si no lo hay, delega en otra representación. */
public final class RepresentacionConSprites implements RepresentacionPiezas {
    /**
     * Alto del sprite, en casillas: pasa un poco el de la casilla y la pieza sobresale hacia arriba, como en
     * un tablero con piezas de volumen. Así el busto se ve más grande y con más detalle.
     */
    private static final double ALTO_SPRITE = LADO_BASE * 1.15;
    /** Relación ancho/alto de los sprites (256 x 384). */
    private static final double PROPORCION = 2.0 / 3.0;
    /** Espacio entre la base del sprite y el borde de abajo de la casilla. */
    private static final double MARGEN_BASE = LADO_BASE * 0.02;

    private final Map<Bando, CatalogoSprites> catalogos;
    private final RepresentacionPiezas respaldo;

    public RepresentacionConSprites(Map<Bando, CatalogoSprites> catalogos, RepresentacionPiezas respaldo) {
        this.catalogos = Map.copyOf(Objects.requireNonNull(catalogos, "catalogos"));
        this.respaldo = Objects.requireNonNull(respaldo, "respaldo");
    }

    @Override
    public Node crear(Pieza pieza, TemaTablero tema) {
        Objects.requireNonNull(pieza, "pieza");
        Objects.requireNonNull(tema, "tema");
        return Optional.ofNullable(catalogos.get(pieza.color()))
                .flatMap(catalogo -> catalogo.sprite(pieza.codigo(), pieza.color()))
                .<Node>map(sprite -> figura(sprite, pieza))
                .orElseGet(() -> respaldo.crear(pieza, tema));
    }

    private Node figura(Image sprite, Pieza pieza) {
        ImageView imagen = new ImageView(sprite);
        imagen.setFitHeight(ALTO_SPRITE);
        imagen.setFitWidth(ALTO_SPRITE * PROPORCION);
        imagen.setPreserveRatio(true);
        imagen.setSmooth(true);
        imagen.setMouseTransparent(true);
        // El contenedor mide justo una casilla (así se escala y se centra como cualquier otra pieza) y el
        // sprite apoya su base abajo y sobresale hacia arriba, sin recortarse.
        Pane figura = new Pane(imagen);
        figura.setMinSize(LADO_BASE, LADO_BASE);
        figura.setPrefSize(LADO_BASE, LADO_BASE);
        figura.setMaxSize(LADO_BASE, LADO_BASE);
        imagen.setLayoutX((LADO_BASE - ALTO_SPRITE * PROPORCION) / 2);
        imagen.setLayoutY(LADO_BASE - MARGEN_BASE - ALTO_SPRITE);
        figura.setMouseTransparent(true);
        figura.setAccessibleText(pieza.nombre() + " " + pieza.color());
        return figura;
    }
}
