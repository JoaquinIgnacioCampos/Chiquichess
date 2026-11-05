package adaptadores.ui;

import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Map;
import java.util.Objects;

/** Catálogo visual: escudos originales y colores de clubes, fuera del núcleo. */
public record PresentacionClub(Equipo equipo, Image escudo, TemaPanel tema) {
    private static final Map<Equipo, Image> ESCUDOS = Map.of(
            Equipo.BOCA, cargar("boca.png"),
            Equipo.INDEPENDIENTE, cargar("independiente.png"),
            Equipo.RIVER, cargar("river.png"));

    public PresentacionClub {
        Objects.requireNonNull(equipo, "equipo");
        Objects.requireNonNull(escudo, "escudo");
        Objects.requireNonNull(tema, "tema");
    }

    public static PresentacionClub de(Equipo equipo, TemaPanel base) {
        Objects.requireNonNull(equipo, "equipo");
        Objects.requireNonNull(base, "base");
        Color fondo = Color.web(switch (equipo) {
            case BOCA -> "#102B50";
            case RIVER -> "#29171C";
            case INDEPENDIENTE -> "#321719";
        });
        Color acento = Color.web(equipo == Equipo.BOCA ? "#F9B923" : "#FF777D");
        Color borde = Color.web(equipo == Equipo.BOCA ? "#315783" : "#7E3A41");
        TemaPanel tema = new TemaPanel(base.fondo(), fondo, base.texto(), base.textoSecundario(),
                acento, base.activo(), base.alerta(), borde, base.puntoApagado(),
                base.insigniaBlanca(), base.insigniaNegra());
        return new PresentacionClub(equipo, ESCUDOS.get(equipo), tema);
    }

    private static Image cargar(String archivo) {
        var recurso = Objects.requireNonNull(PresentacionClub.class.getResource("/escudos/" + archivo),
                "Falta el escudo " + archivo);
        Image imagen = new Image(recurso.toExternalForm(), false);
        if (imagen.isError()) throw new IllegalStateException("No se pudo cargar el escudo " + archivo, imagen.getException());
        return imagen;
    }
}
