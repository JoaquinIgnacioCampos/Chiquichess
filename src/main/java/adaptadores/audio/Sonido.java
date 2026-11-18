package adaptadores.audio;

import java.util.Optional;

/**
 * Cada cosa que puede sonar. El nombre de archivo es la convención para reemplazar sonidos: basta con
 * dejar, por ejemplo, {@code gol.wav} en la carpeta de un club (ver {@link CatalogoDeSonidos}).
 */
public enum Sonido {
    MOVIMIENTO("movimiento", false),
    CAPTURA("captura", false),
    ENROQUE("enroque", false),
    PROMOCION("promocion", false),
    JAQUE("jaque", false),
    PASE("pase", false),
    GOL("gol", true),
    VICTORIA("victoria", true),
    TABLAS("tablas", false),
    SILBATO_INICIAL("silbato-inicial", false),
    SILBATO_FINAL("silbato-final", false),
    REBOBINAR("rebobinar", false),
    /** Ambiente del modo fútbol: suena en bucle y su volumen sigue a la partida (ver {@link IntensidadDeHinchada}). */
    HINCHADA("hinchada", true),
    /** Al tocar una pieza propia para moverla: un clic corto y bajo (no es un evento de la partida). */
    SELECCION("seleccion", false),
    /** Fuera de la partida: la risa del personaje al abrir el juego (ver {@link SonidosDelMenu}). */
    RISA("risa", false),
    /** Fuera de la partida: ambiente suave en bucle mientras se eligen las opciones. */
    AMBIENTE_MENU("menu", false);

    private final String archivo;
    private final boolean propioDeCadaEquipo;

    Sonido(String archivo, boolean propioDeCadaEquipo) {
        this.archivo = archivo;
        this.propioDeCadaEquipo = propioDeCadaEquipo;
    }

    /** Nombre del archivo sin extensión. */
    public String archivo() {
        return archivo;
    }

    /** true si cada club puede tener el suyo; si un club no lo tiene, se usa el común. */
    public boolean propioDeCadaEquipo() {
        return propioDeCadaEquipo;
    }

    /** Qué sonido usar si falta el archivo de este: así alcanza con unos pocos archivos para empezar. */
    public Optional<Sonido> alternativo() {
        return switch (this) {
            case CAPTURA, ENROQUE, PROMOCION, PASE -> Optional.of(MOVIMIENTO);
            case SILBATO_FINAL -> Optional.of(SILBATO_INICIAL);
            default -> Optional.empty();
        };
    }
}
