package adapters.audio;

import java.util.Optional;

/**
 * Cada cosa que puede sonar. El nombre de archivo es la convención para reemplazar sonidos: basta con
 * dejar, por ejemplo, {@code gol.wav} en la carpeta de un club (ver {@link SoundCatalog}).
 */
public enum Sound {
    MOVEMENT("movimiento", false),
    CAPTURE("captura", false),
    CASTLING("enroque", false),
    PROMOTION("promocion", false),
    CHECK("jaque", false),
    PASS("pase", false),
    GOAL("gol", true),
    VICTORIA("victoria", true),
    DRAW("tablas", false),
    WHISTLE_INITIAL("silbato-inicial", false),
    WHISTLE_FINAL("silbato-final", false),
    REWIND("rebobinar", false),
    /** Ambiente del modo fútbol: suena en bucle y su volumen sigue a la partida (ver {@link CrowdIntensity}). */
    CROWD("hinchada", true),
    /** Al tocar una pieza propia para moverla: un clic corto y bajo (no es un evento de la partida). */
    SELECTION("seleccion", false),
    /** Fuera de la partida: la risa del personaje al abrir el juego (ver {@link MenuSounds}). */
    LAUGH("risa", false),
    /** Fuera de la partida: ambiente suave en bucle mientras se eligen las opciones. */
    AMBIENT_MENU("menu", false);

    private final String file;
    private final boolean teamSpecific;

    Sound(String file, boolean teamSpecific) {
        this.file = file;
        this.teamSpecific = teamSpecific;
    }

    /** Nombre del archivo sin extensión. */
    public String file() {
        return file;
    }

    /** true si cada club puede tener el suyo; si un club no lo tiene, se usa el común. */
    public boolean teamSpecific() {
        return teamSpecific;
    }

    /** Qué sonido usar si falta el archivo de este: así alcanza con unos pocos archivos para empezar. */
    public Optional<Sound> alternative() {
        return switch (this) {
            case CAPTURE, CASTLING, PROMOTION, PASS -> Optional.of(MOVEMENT);
            case WHISTLE_FINAL -> Optional.of(WHISTLE_INITIAL);
            default -> Optional.empty();
        };
    }
}
