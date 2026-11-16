package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.piezas.Pieza;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Dibuja cada pieza con el sprite del club de su bando; si no lo hay, delega en otra representación. */
public final class RepresentacionConSprites implements RepresentacionPiezas {
    private static final double LADO_SPRITE = LADO_BASE * 0.85;

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
        ImageView figura = new ImageView(sprite);
        figura.setFitWidth(LADO_SPRITE);
        figura.setFitHeight(LADO_SPRITE);
        figura.setPreserveRatio(true);
        figura.setSmooth(true);
        figura.setMouseTransparent(true);
        figura.setAccessibleText(pieza.nombre() + " " + pieza.color());
        return figura;
    }
}
