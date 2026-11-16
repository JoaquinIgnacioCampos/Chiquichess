package adaptadores.ui;

import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Map;
import java.util.Objects;

/** Catálogo visual: escudos originales y colores de clubes, fuera del núcleo. */
public record PresentacionClub(Equipo equipo, Image escudo, TemaPanel tema) {
    private static final Map<Equipo, Estilo> ESTILOS = Map.of(
            Equipo.BOCA, new Estilo("boca.png", "#102B50", "#F9B923", "#315783"),
            Equipo.INDEPENDIENTE, new Estilo("independiente.png", "#321719", "#FF777D", "#7E3A41"),
            Equipo.RIVER, new Estilo("river.png", "#E8E5DF", "#A51D2D", "#FFFFFF"),
            Equipo.SAN_LORENZO, new Estilo("san-lorenzo.png", "#182941", "#FF6573", "#704354"),
            Equipo.RACING, new Estilo("racing.png", "#103342", "#66D8FA", "#3486A1"));

    public PresentacionClub {
        Objects.requireNonNull(equipo, "equipo");
        Objects.requireNonNull(escudo, "escudo");
        Objects.requireNonNull(tema, "tema");
    }

    public static PresentacionClub de(Equipo equipo, TemaPanel base) {
        Objects.requireNonNull(equipo, "equipo");
        Objects.requireNonNull(base, "base");
        Estilo estilo = ESTILOS.get(equipo);
        Color texto = equipo == Equipo.RIVER ? Color.web("#242424") : base.texto();
        Color textoSecundario = equipo == Equipo.RIVER ? Color.web("#505050") : base.textoSecundario();
        TemaPanel tema = new TemaPanel(base.fondo(), estilo.tarjeta(), texto, textoSecundario,
                estilo.acento(), base.activo(), base.alerta(), estilo.borde(), base.puntoApagado(),
                base.insigniaBlanca(), base.insigniaNegra());
        return new PresentacionClub(equipo, estilo.escudo(), tema);
    }

    static Image escudoDe(Equipo equipo) {
        return ESTILOS.get(equipo).escudo();
    }

    private record Estilo(Image escudo, Color tarjeta, Color acento, Color borde) {
        Estilo(String archivo, String tarjeta, String acento, String borde) {
            this(cargar(archivo), Color.web(tarjeta), Color.web(acento), Color.web(borde));
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
