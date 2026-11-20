package adaptadores.ui;

import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Catálogo visual: escudo y colores de cada equipo, fuera del núcleo.
 *
 * El estilo de cada equipo se define en un {@code switch}: un equipo nuevo sin escudo ni colores no
 * compila. Los escudos van en {@code /escudos/}; si falta el archivo, el programa no arranca.
 */
public record PresentacionClub(Equipo equipo, Image escudo, TemaPanel tema) {
    /** Cada escudo se carga una sola vez. */
    private static final Map<Equipo, Estilo> ESTILOS = new EnumMap<>(Equipo.class);

    static {
        for (Equipo equipo : Equipo.values()) {
            ESTILOS.put(equipo, estiloDe(equipo));
        }
    }

    public PresentacionClub {
        Objects.requireNonNull(equipo, "equipo");
        Objects.requireNonNull(escudo, "escudo");
        Objects.requireNonNull(tema, "tema");
    }

    /** Escudo, fondo de la tarjeta, acento (nombre del club y detalles) y borde de cada equipo. */
    private static Estilo estiloDe(Equipo equipo) {
        return switch (equipo) {
            case BOCA -> new Estilo("boca.png", "#102B50", "#F9B923", "#315783");
            // Tarjeta clara: necesita texto oscuro para que se lea.
            case RIVER -> new Estilo("river.png", "#E8E5DF", "#A51D2D", "#FFFFFF", "#242424", "#505050");
            case INDEPENDIENTE -> new Estilo("independiente.png", "#321719", "#FF777D", "#7E3A41");
            case ARGENTINA -> new Estilo("argentina.png", "#14304D", "#75AADB", "#3D6E9C");
        };
    }

    public static PresentacionClub de(Equipo equipo, TemaPanel base) {
        Objects.requireNonNull(equipo, "equipo");
        Objects.requireNonNull(base, "base");
        Estilo estilo = ESTILOS.get(equipo);
        Color texto = estilo.texto().orElse(base.texto());
        Color textoSecundario = estilo.textoSecundario().orElse(base.textoSecundario());
        TemaPanel tema = new TemaPanel(base.fondo(), estilo.tarjeta(), texto, textoSecundario,
                estilo.acento(), base.activo(), base.alerta(), estilo.borde(), base.puntoApagado(),
                base.insigniaBlanca(), base.insigniaNegra());
        return new PresentacionClub(equipo, estilo.escudo(), tema);
    }

    /** El escudo de un equipo, sin armar sus colores (lo usa el menú). */
    static Image escudoDe(Equipo equipo) {
        return ESTILOS.get(Objects.requireNonNull(equipo, "equipo")).escudo();
    }

    /** Nombre del archivo del escudo de ese equipo, dentro de {@code /escudos/}. */
    static String archivoDelEscudo(Equipo equipo) {
        return ESTILOS.get(Objects.requireNonNull(equipo, "equipo")).archivo();
    }

    /** Si no trae colores de texto, la tarjeta usa los del tema base (pensados para fondos oscuros). */
    private record Estilo(String archivo, Image escudo, Color tarjeta, Color acento, Color borde,
                          Optional<Color> texto, Optional<Color> textoSecundario) {
        Estilo(String archivo, String tarjeta, String acento, String borde) {
            this(archivo, cargar(archivo), Color.web(tarjeta), Color.web(acento), Color.web(borde),
                    Optional.empty(), Optional.empty());
        }

        Estilo(String archivo, String tarjeta, String acento, String borde, String texto, String textoSecundario) {
            this(archivo, cargar(archivo), Color.web(tarjeta), Color.web(acento), Color.web(borde),
                    Optional.of(Color.web(texto)), Optional.of(Color.web(textoSecundario)));
        }
    }

    private static Image cargar(String archivo) {
        var recurso = Objects.requireNonNull(PresentacionClub.class.getResource("/escudos/" + archivo),
                "Falta el escudo " + archivo);
        Image imagen = new Image(recurso.toExternalForm(), false);
        if (imagen.isError()) throw new IllegalStateException("No se pudo cargar el escudo " + archivo, imagen.getException());
        return imagen;
    }
}
