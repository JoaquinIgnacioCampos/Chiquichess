package adaptadores;

import Nucleo.bando.Bando;
import Nucleo.ia.DecisionDeJuego;
import Nucleo.ia.EstrategiaDeJuego;
import Nucleo.juego.SituacionDePartida;
import Nucleo.juego.Movimiento;
import Nucleo.juego.PartidaEnJuego;

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
    /**
     * Lo que decidió la IA: el movimiento y cuánto tardaría una persona en jugarlo, contando desde que
     * empezó a pensar. La consola y la ventana esperan hasta ese momento para mover.
     */
    public record Respuesta(Movimiento movimiento, Duration demora) {
    }

    private final PartidaEnJuego partida;
    private final Bando bando;
    private final EstrategiaDeJuego estrategia;

    public RivalArtificial(PartidaEnJuego partida, Bando bando, EstrategiaDeJuego estrategia) {
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
     * Elige la jugada para esa situación y cuánto tardaría una persona en jugarla. No toca la partida,
     * así que se puede llamar desde otro hilo con una situación tomada antes. Con reloj, nunca piensa ni
     * tarda más de una fracción de lo que le queda: no pierde por tiempo.
     *
     * @param tiempoRestante lo que le queda en el reloj; vacío si la partida no tiene tiempo
     */
    public Respuesta pensar(SituacionDePartida situacion, Optional<Duration> tiempoRestante) {
        DecisionDeJuego decision = estrategia.decidir(situacion, tiempoRestante);
        return new Respuesta(Movimiento.de(decision.jugada()), decision.tiempoDeRespuesta());
    }

    /** Piensa sobre la situación actual de la partida (en este mismo hilo). */
    public Respuesta pensar() {
        return pensar(partida.situacion(), partida.tiempoRestante(bando));
    }

    /** Piensa y juega enseguida, en este mismo hilo, sin esperar la demora. Devuelve lo que jugó. */
    public Movimiento jugar() {
        if (!leToca()) throw new IllegalStateException("No es el turno de la IA");
        Movimiento movimiento = pensar().movimiento();
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
