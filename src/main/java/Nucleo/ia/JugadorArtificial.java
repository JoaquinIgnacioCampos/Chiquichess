package Nucleo.ia;

import Nucleo.jugadas.Jugada;
import Nucleo.tiempo.FuenteDeTiempo;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * La IA de ChiquiChess: busca con {@link BuscadorMinimax} según su {@link Dificultad} y elige.
 *
 * Para equivocarse de forma creíble, a cada jugada le suma un error al azar de hasta
 * {@code dificultad.ruido()} y se queda con la de mayor puntaje resultante: puede elegir una jugada un
 * poco peor, pero nunca una mucho peor que la mejor que vio. Entre jugadas iguales elige al azar, así
 * dos partidas no son idénticas.
 *
 * Con la probabilidad de {@code dificultad.descuido()} se distrae: juega cualquiera de sus jugadas al
 * azar, salvo las que le dan mate en 1 al rival. Es el error de un principiante: no regala la partida
 * en una jugada, pero sí material.
 *
 * Al decidir ({@link #decidir}) dice también cuánto tardaría una persona de su nivel en jugar eso (ver
 * {@link RitmoHumano}). El fácil y el medio calculan lo de siempre y el resto es espera; el difícil
 * calcula durante todo ese tiempo.
 *
 * El azar y el tiempo se reciben por constructor (como el sorteo y el reloj de la partida), así los
 * tests son repetibles.
 */
public final class JugadorArtificial implements EstrategiaDeJuego {
    /** Acepta tablas solo si cree que va perdiendo por al menos esto (tres peones). */
    static final int DESVENTAJA_PARA_ACEPTAR_TABLAS = 300;
    /** Margen que hace que la búsqueda dé el puntaje exacto de todas las jugadas. */
    private static final int MARGEN_TOTAL = Integer.MAX_VALUE / 4;
    /** Dos jugadas a menos de esto (un tercio de peón) son "casi igual de buenas": la posición hace dudar. */
    static final int CASI_IGUALES = 30;

    /** La jugada elegida y si había otra casi igual de buena. */
    private record Eleccion(Jugada jugada, boolean dudosa) {
    }

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
        return elegir(situacion, menor(tiempoMaximo, dificultad.tiempoDeCalculo()), dificultad.profundidadMaxima()).jugada();
    }

    /**
     * Elige la jugada y cuánto tardaría una persona de su nivel en jugarla. El difícil calcula todo ese
     * tiempo; el fácil y el medio, su tiempo de cálculo (y quien lo use espera el resto).
     *
     * @param tiempoRestante lo que le queda en el reloj; vacío si la partida no tiene tiempo
     * @throws IllegalStateException si no hay ninguna jugada legal
     */
    @Override
    public DecisionDeJuego decidir(SituacionDePartida situacion, Optional<Duration> tiempoRestante) {
        Objects.requireNonNull(situacion, "situacion");
        Objects.requireNonNull(tiempoRestante, "tiempoRestante");
        int legales = new BuscadorMinimax(situacion, reloj, false).cantidadDeJugadasLegales(situacion.turno());
        if (legales == 0) throw new IllegalStateException("No hay jugadas legales para elegir");
        boolean enJaque = situacion.reglas().detector().estaEnJaque(situacion.tablero(), situacion.turno());
        Duration respuesta = RitmoHumano.tiempo(dificultad, legales, enJaque, situacion.jugadas().size(),
                tiempoRestante, azar.nextDouble(), azar.nextDouble());
        Duration calculo = dificultad.calculaMientrasEspera() ? respuesta
                : RitmoHumano.limiteDelReloj(tiempoRestante).map(limite -> menor(limite, dificultad.tiempoDeCalculo()))
                .orElse(dificultad.tiempoDeCalculo());
        // Con una sola jugada posible no hay nada que pensar: se mira una jugada y listo.
        Eleccion eleccion = elegir(situacion, calculo, legales == 1 ? 1 : dificultad.profundidadMaxima());
        if (eleccion.dudosa() && !dificultad.calculaMientrasEspera() && legales > 1) {
            respuesta = RitmoHumano.alargarPorComplejidad(respuesta, dificultad, situacion.jugadas().size(),
                    tiempoRestante, azar.nextDouble());
        }
        return new DecisionDeJuego(eleccion.jugada(), respuesta);
    }

    private Eleccion elegir(SituacionDePartida situacion, Duration tiempo, int profundidad) {
        if (dificultad.descuido() > 0 && azar.nextDouble() < dificultad.descuido()) {
            List<Jugada> distraidas = jugadasQueNoPierdenEnseguida(situacion, tiempo);
            if (!distraidas.isEmpty()) return new Eleccion(distraidas.get(azar.nextInt(distraidas.size())), false);
        }
        int ruido = dificultad.ruido();
        List<BuscadorMinimax.JugadaEvaluada> evaluadas =
                new BuscadorMinimax(situacion, reloj, dificultad.conQuietud())
                        .buscar(situacion.turno(), profundidad, tiempo, 2 * ruido, azar);
        if (evaluadas.isEmpty()) throw new IllegalStateException("No hay jugadas legales para elegir");
        int mejor = evaluadas.getFirst().puntaje();
        boolean dudosa = evaluadas.size() > 1 && Math.abs(mejor) < BuscadorMinimax.UMBRAL_DE_MATE
                && evaluadas.get(1).puntaje() >= mejor - CASI_IGUALES;
        // Error al azar entre -ruido y +ruido; el medio punto extra desempata al azar entre iguales.
        Jugada elegida = evaluadas.stream()
                .max(Comparator.comparingDouble(evaluada -> evaluada.puntaje()
                        + (ruido == 0 ? 0 : azar.nextInt(-ruido, ruido + 1)) + azar.nextDouble() * 0.5))
                .orElseThrow()
                .jugada();
        return new Eleccion(elegida, dudosa);
    }

    private static Duration menor(Duration una, Duration otra) {
        return una.compareTo(otra) <= 0 ? una : otra;
    }

    /** Sus jugadas que no le dejan al rival un mate en 1 (mira dos jugadas con puntaje exacto). */
    private List<Jugada> jugadasQueNoPierdenEnseguida(SituacionDePartida situacion, Duration tiempo) {
        return new BuscadorMinimax(situacion, reloj, false)
                .buscar(situacion.turno(), 2, tiempo, MARGEN_TOTAL, azar).stream()
                .filter(evaluada -> evaluada.puntaje() > -BuscadorMinimax.UMBRAL_DE_MATE)
                .map(BuscadorMinimax.JugadaEvaluada::jugada)
                .toList();
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
