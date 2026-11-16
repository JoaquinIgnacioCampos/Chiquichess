package adaptadores.ui;

import Nucleo.bando.Bando;
import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Sprites de las piezas de un club: una versión blanca y otra negra de cada pieza, porque un club
 * puede jugar con cualquiera de los dos colores. Cada imagen se carga una sola vez, ya que el tablero
 * reconstruye sus casillas en cada redibujado. Un archivo que falta o no se puede leer da vacío.
 */
public final class CatalogoSprites {
    private static final Map<Character, String> ARCHIVOS = Map.of(
            'R', "rey", 'D', "reina", 'T', "torre", 'A', "alfil", 'C', "caballo", 'P', "peon");

    private final String carpeta;
    private final Map<String, Optional<Image>> cargados = new HashMap<>();

    CatalogoSprites(String carpeta) {
        this.carpeta = Objects.requireNonNull(carpeta, "carpeta");
    }

    public static CatalogoSprites de(Equipo equipo) {
        Objects.requireNonNull(equipo, "equipo");
        return new CatalogoSprites("/sprites/" + equipo.name().toLowerCase(Locale.ROOT));
    }

    Optional<Image> sprite(char codigo, Bando bando) {
        Objects.requireNonNull(bando, "bando");
        String pieza = ARCHIVOS.get(codigo);
        if (pieza == null) return Optional.empty();
        String archivo = pieza + "-" + (bando == Bando.BLANCO ? "blanco" : "negro") + ".png";
        return cargados.computeIfAbsent(archivo, this::cargar);
    }

    private Optional<Image> cargar(String archivo) {
        var recurso = CatalogoSprites.class.getResource(carpeta + "/" + archivo);
        if (recurso == null) return Optional.empty();
        return Optional.of(new Image(recurso.toExternalForm(), false)).filter(imagen -> !imagen.isError());
    }
}
