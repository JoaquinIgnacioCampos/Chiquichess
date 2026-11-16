package adaptadores.configuracion;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Selección completa del menú, independiente de JavaFX y de las reglas del ajedrez.
 *
 * @param permitirDeshacer si los jugadores pueden deshacer y rehacer jugadas; solo se admite sin tiempo
 * @param limiteDeJugadas  en el modo fútbol, jugadas de los dos bandos juntos a las que termina la
 *                         partida (entre {@link #LIMITE_MINIMO} y {@link #LIMITE_MAXIMO}); el
 *                         ajedrez clásico no lo usa
 */
public record ConfiguracionPartida(Modo modo, Tiempo tiempo, Rival rival,
                                   Equipo equipoJugador1, Equipo equipoJugador2, Colores colores,
                                   boolean permitirDeshacer, int limiteDeJugadas) {

    public static final int LIMITE_POR_DEFECTO = 90;
    public static final int LIMITE_MINIMO = 10;
    public static final int LIMITE_MAXIMO = 400;

    public ConfiguracionPartida {
        Objects.requireNonNull(modo, "modo");
        Objects.requireNonNull(tiempo, "tiempo");
        Objects.requireNonNull(rival, "rival");
        Objects.requireNonNull(equipoJugador1, "equipoJugador1");
        Objects.requireNonNull(equipoJugador2, "equipoJugador2");
        Objects.requireNonNull(colores, "colores");
        if (equipoJugador1 == equipoJugador2) throw new IllegalArgumentException("Los equipos deben ser distintos");
        if (rival == Rival.IA) throw new IllegalArgumentException("La IA todavía no está disponible");
        if (permitirDeshacer && tiempo.duracion().isPresent()) {
            throw new IllegalArgumentException("Solo se puede deshacer en partidas sin tiempo");
        }
        if (limiteDeJugadas < LIMITE_MINIMO || limiteDeJugadas > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("El límite de jugadas debe estar entre "
                    + LIMITE_MINIMO + " y " + LIMITE_MAXIMO);
        }
    }

    /** Configuración con el límite de jugadas por defecto (90). */
    public ConfiguracionPartida(Modo modo, Tiempo tiempo, Rival rival,
                                Equipo equipoJugador1, Equipo equipoJugador2, Colores colores,
                                boolean permitirDeshacer) {
        this(modo, tiempo, rival, equipoJugador1, equipoJugador2, colores, permitirDeshacer, LIMITE_POR_DEFECTO);
    }

    /** Configuración sin deshacer. */
    public ConfiguracionPartida(Modo modo, Tiempo tiempo, Rival rival,
                                Equipo equipoJugador1, Equipo equipoJugador2, Colores colores) {
        this(modo, tiempo, rival, equipoJugador1, equipoJugador2, colores, false);
    }

    /** CLASICO: ajedrez. FUTBOL: "Posesión y gol", ajedrez con pelota (3 goles o más goles al llegar al límite). */
    public enum Modo { CLASICO, FUTBOL }
    public enum Rival { AMIGO, IA }
    public enum Equipo {
        BOCA("Boca"), RIVER("River"), INDEPENDIENTE("Independiente");
        private final String nombre;
        Equipo(String nombre) { this.nombre = nombre; }
        public String nombre() { return nombre; }
    }
    public enum Colores { JUGADOR_1_BLANCAS, JUGADOR_2_BLANCAS, ALEATORIO }
    public enum Tiempo {
        TRES_MINUTOS("3 minutos", 3), CINCO_MINUTOS("5 minutos", 5),
        DIEZ_MINUTOS("10 minutos", 10), SIN_TIEMPO("Sin tiempo", 0);
        private final String texto;
        private final int minutos;
        Tiempo(String texto, int minutos) { this.texto = texto; this.minutos = minutos; }
        public String texto() { return texto; }
        public Optional<Duration> duracion() {
            return minutos == 0 ? Optional.empty() : Optional.of(Duration.ofMinutes(minutos));
        }
    }
}
