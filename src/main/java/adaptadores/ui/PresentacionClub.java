package adaptadores.ui;

import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Catálogo visual: escudos y colores de cada equipo, fuera del núcleo.
 *
 * El escudo de un equipo es {@code /escudos/<equipo>.png}, con el nombre de su constante de
 * {@link Equipo} en minúsculas (por ejemplo, {@code argentina.png}). Si falta, el programa no arranca:
 * todo equipo tiene que tener el suyo.
 */
public record PresentacionClub(Equipo equipo, Image escudo, TemaPanel tema) {
    private static final Map<Equipo, Image> ESCUDOS = new EnumMap<>(Equipo.class);

    static {
        for (Equipo equipo : Equipo.values()) {
            ESCUDOS.put(equipo, cargar(archivoDelEscudo(equipo)));
        }
    }

    public PresentacionClub {
        Objects.requireNonNull(equipo, "equipo");
        Objects.requireNonNull(escudo, "escudo");
        Objects.requireNonNull(tema, "tema");
    }

    public static PresentacionClub de(Equipo equipo, TemaPanel base) {
        Objects.requireNonNull(equipo, "equipo");
        Objects.requireNonNull(base, "base");
        // Fondo de la tarjeta, acento (nombre del club y detalles) y borde, según los colores de cada equipo.
        String[] colores = switch (equipo) {
            case BOCA -> new String[]{"#102B50", "#F9B923", "#315783"};
            case RIVER -> new String[]{"#29171C", "#FF777D", "#7E3A41"};
            case INDEPENDIENTE -> new String[]{"#321719", "#FF777D", "#7E3A41"};
            case ARGENTINA -> new String[]{"#14304D", "#75AADB", "#3D6E9C"};
        };
        TemaPanel tema = new TemaPanel(base.fondo(), Color.web(colores[0]), base.texto(), base.textoSecundario(),
                Color.web(colores[1]), base.activo(), base.alerta(), Color.web(colores[2]), base.puntoApagado(),
                base.insigniaBlanca(), base.insigniaNegra());
        return new PresentacionClub(equipo, ESCUDOS.get(equipo), tema);
    }

    /** Nombre del archivo del escudo de ese equipo, dentro de {@code /escudos/}. */
    static String archivoDelEscudo(Equipo equipo) {
        return equipo.name().toLowerCase(Locale.ROOT) + ".png";
    }

    private static Image cargar(String archivo) {
        var recurso = Objects.requireNonNull(PresentacionClub.class.getResource("/escudos/" + archivo),
                "Falta el escudo " + archivo);
        Image imagen = new Image(recurso.toExternalForm(), false);
        if (imagen.isError()) throw new IllegalStateException("No se pudo cargar el escudo " + archivo, imagen.getException());
        return imagen;
    }
}
