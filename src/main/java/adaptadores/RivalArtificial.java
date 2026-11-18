package adaptadores;

import Nucleo.bando.Bando;
import Nucleo.ia.EstrategiaDeJuego;
import Nucleo.ia.SituacionDePartida;
import Nucleo.juego.Movimiento;
import Nucleo.juego.Partida;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * La IA sentada a la mesa: un jugador más de la partida, del lado de los adaptadores. Le pide jugadas a
 * su {@link EstrategiaDeJuego} y las juega con {@code Partida.mover}, igual que una persona; por eso el
 * sonido, el panel y los eventos funcionan sin saber que es una IA. No depende de JavaFX: la usan la
 * consola (que la espera) y la ventana (que la hace pensar en otro hilo).
 *
 * También decide lo que en una partida entre personas decide el otro jugador: si acepta las tablas. Y
 * como deshacer contra la IA tiene que devolverle el turno a la persona, deshace de a dos jugadas.
 */
public final class RivalArtificial {
    /** Con reloj, piensa como mucho esta fracción de lo que le queda: nunca pierde por tiempo. */
    static final int FRACCION_DEL_TIEMPO_RESTANTE = 30;
    /** Sin reloj, el único límite es el de su dificultad. */
    private static final Duration SIN_LIMITE = Duration.ofMinutes(10);

    private final Partida partida;
    private final Bando bando;
    private final EstrategiaDeJuego estrategia;

    public RivalArtificial(Partida partida, Bando bando, EstrategiaDeJuego estrategia) {
        this.partida = Objects.requireNonNull(partida, "partida");
        this.bando = Objects.requireNonNull(bando, "bando");
        this.estrategia = Objects.requireNonNull(estrategia, "estrategia");
    }

    /** Con qué color juega la IA. */
    public Bando bando() {
        return bando;
    }

    /** true si la partida sigue y es el turno de la IA. */
    public boolean leToca() {
        return !partida.estado().estaFinalizada() && partida.turno() == bando;
    }

    /**
     * Elige la jugada para esa situación. No toca la partida, así que se puede llamar desde otro hilo
     * con una situación tomada antes.
     *
     * @param tiempoRestante lo que le queda en el reloj; vacío si la partida no tiene tiempo
     */
    public Movimiento pensar(SituacionDePartida situacion, Optional<Duration> tiempoRestante) {
        Duration maximo = tiempoRestante.map(restante -> restante.dividedBy(FRACCION_DEL_TIEMPO_RESTANTE))
                .orElse(SIN_LIMITE);
        return Movimiento.de(estrategia.elegirJugada(situacion, maximo));
    }

    /** Piensa sobre la situación actual de la partida (en este mismo hilo). */
    public Movimiento pensar() {
        return pensar(partida.situacion(), partida.tiempoRestante(bando));
    }

    /** Piensa y juega, en este mismo hilo. Devuelve lo que jugó. */
    public Movimiento jugar() {
        if (!leToca()) throw new IllegalStateException("No es el turno de la IA");
        Movimiento movimiento = pensar();
        partida.mover(movimiento);
        return movimiento;
    }

    /**
     * Si el rival le ofreció tablas, las acepta o las rechaza según su estrategia; si no hay oferta del
     * rival, no hace nada. Devuelve true si las aceptó.
     */
    public boolean responderTablas() {
        if (partida.ofertaTablasPendiente().filter(oferente -> oferente != bando).isEmpty()) return false;
        if (estrategia.aceptaTablas(partida.situacion())) {
            partida.aceptarTablas(bando);
            return true;
        }
        partida.rechazarTablas(bando);
        return false;
    }

    /**
     * Deshace hasta que le toque a la persona: la última jugada de la IA y la de la persona. Si la IA
     * todavía no había respondido, deshace solo la de la persona.
     *
     * @throws Nucleo.juego.AccionInvalidaException si la partida no deja deshacer
     */
    public void deshacer() {
        partida.deshacer();
        if (partida.turno() == bando && partida.puedeDeshacer()) partida.deshacer();
    }

    /** Rehace la jugada de la persona y la respuesta de la IA, si la había. */
    public void rehacer() {
        partida.rehacer();
        if (partida.turno() == bando && partida.puedeRehacer()) partida.rehacer();
    }
}
