package adapters.audio;

import java.util.Optional;

/**
 * Cada cosa que puede sonar. El nombre de archivo es la convención para reemplazar sonidos: basta con
 * dejar, por ejemplo, {@code gol.wav} en la carpeta de un club (ver {@link CatalogoDeSonidos}).
 */
public enum Sound {
    MOVEMENT("movement", false),
    CAPTURE("capture", false),
    CASTLING("castling", false),
    PROMOTION("promotion", false),
    CHECK("check", false),
    PASS("pass", false),
    GOAL("goal", true),
    VICTORIA("victory", true),
    DRAW("draw", false),
    WHISTLE_INITIAL("start-whistle", false),
    WHISTLE_FINAL("final-whistle", false),
    REWIND("rewind", false),
    /** Ambiente del modo fútbol: suena en bucle y su volumen sigue a la partida (ver {@link IntensidadDeHinchada}). */
    CROWD("crowd", true),
    /** Al tocar una pieza propia para moverla: un clic corto y bajo (no es un evento de la partida). */
    SELECTION("selection", false),
    /** Fuera de la partida: la risa del personaje al abrir el juego (ver {@link SonidosDelMenu}). */
    LAUGH("laugh", false),
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
