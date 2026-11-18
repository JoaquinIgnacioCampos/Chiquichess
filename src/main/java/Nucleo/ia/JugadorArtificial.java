package Nucleo.ia;

import Nucleo.jugadas.Jugada;
import Nucleo.tiempo.FuenteDeTiempo;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * La IA de ChiquiChess: busca con {@link BuscadorMinimax} según su {@link Dificultad} y elige.
 *
 * Para equivocarse de forma creíble, a cada jugada le suma un error al azar de hasta
 * {@code dificultad.ruido()} y se queda con la de mayor puntaje resultante: puede elegir una jugada un
 * poco peor, pero nunca una mucho peor que la mejor que vio. Entre jugadas iguales elige al azar, así
 * dos partidas no son idénticas.
 *
 * El azar y el tiempo se reciben por constructor (como el sorteo y el reloj de la partida), así los
 * tests son repetibles.
 */
public final class JugadorArtificial implements EstrategiaDeJuego {
    /** Acepta tablas solo si cree que va perdiendo por al menos esto (tres peones). */
    static final int DESVENTAJA_PARA_ACEPTAR_TABLAS = 300;

    private final Dificultad dificultad;
    private final Random azar;
    private final FuenteDeTiempo reloj;

    public JugadorArtificial(Dificultad dificultad, Random azar, FuenteDeTiempo reloj) {
        this.dificultad = Objects.requireNonNull(dificultad, "dificultad");
        this.azar = Objects.requireNonNull(azar, "azar");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    public Dificultad dificultad() {
        return dificultad;
    }

    /** Piensa lo que le permite su dificultad, sin pasarse de {@code tiempoMaximo}. */
    @Override
    public Jugada elegirJugada(SituacionDePartida situacion, Duration tiempoMaximo) {
        Objects.requireNonNull(situacion, "situacion");
        Objects.requireNonNull(tiempoMaximo, "tiempoMaximo");
        Duration tiempo = tiempoMaximo.compareTo(dificultad.tiempoDeCalculo()) < 0
                ? tiempoMaximo : dificultad.tiempoDeCalculo();
        int ruido = dificultad.ruido();
        List<BuscadorMinimax.JugadaEvaluada> evaluadas =
                new BuscadorMinimax(situacion, reloj, dificultad.conQuietud())
                        .buscar(situacion.turno(), dificultad.profundidadMaxima(), tiempo, 2 * ruido, azar);
        if (evaluadas.isEmpty()) throw new IllegalStateException("No hay jugadas legales para elegir");
        // Error al azar entre -ruido y +ruido; el medio punto extra desempata al azar entre iguales.
        return evaluadas.stream()
                .max(Comparator.comparingDouble(evaluada -> evaluada.puntaje()
                        + (ruido == 0 ? 0 : azar.nextInt(-ruido, ruido + 1)) + azar.nextDouble() * 0.5))
                .orElseThrow()
                .jugada();
    }

    /** Elige con todo el tiempo de su dificultad. */
    public Jugada elegirJugada(SituacionDePartida situacion) {
        return elegirJugada(situacion, dificultad.tiempoDeCalculo());
    }

    /** Acepta solo si va claramente perdiendo; si no, sigue jugando. Nunca se rinde. */
    @Override
    public boolean aceptaTablas(SituacionDePartida situacion) {
        Objects.requireNonNull(situacion, "situacion");
        int paraQuienOfrece = new EvaluadorDePosicion(situacion.tablero())
                .evaluar(situacion.tablero(), situacion.turno(), situacion.jugadas());
        return paraQuienOfrece >= DESVENTAJA_PARA_ACEPTAR_TABLAS;
    }
}
