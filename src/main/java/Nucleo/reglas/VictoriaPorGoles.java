package Nucleo.reglas;

import Nucleo.bando.Bando;

import java.util.Optional;

/**
 * Final del modo fútbol: gana quien llega a 3 goles. Si se llega a la jugada 90 (contando las de los
 * dos bandos) sin que nadie llegue, gana quien tenga más goles, y con los goles iguales es empate.
 *
 * No guarda estado: cuenta los goles en las jugadas del contexto (ver {@link Marcador}), así deshacer
 * un gol funciona solo. Como al llegar a la jugada 90 puede ganar el bando que tiene el turno, esta
 * condición dice quién gana en lugar de dejar que se asuma que ganó el que acaba de mover.
 *
 * Conviene ponerla después del jaque mate, que gana siempre.
 */
public class VictoriaPorGoles implements CondicionDeFinalizacion {

    private final int golesParaGanar;
    private final int jugadasMaximas;

    /** 3 goles o 90 jugadas en total. */
    public VictoriaPorGoles() {
        this(3, 90);
    }

    public VictoriaPorGoles(int golesParaGanar, int jugadasMaximas) {
        if (golesParaGanar < 1 || jugadasMaximas < 1) {
            throw new IllegalArgumentException("Los goles para ganar y las jugadas máximas deben ser positivos");
        }
        this.golesParaGanar = golesParaGanar;
        this.jugadasMaximas = jugadasMaximas;
    }

    @Override
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        Marcador marcador = Marcador.de(contexto.jugadas());
        if (marcador.blancas() >= golesParaGanar || marcador.negras() >= golesParaGanar) {
            return Optional.of(EstadoPartida.VICTORIA_POR_GOLES);
        }
        if (contexto.jugadas().size() >= jugadasMaximas) {
            return Optional.of(marcador.lider().isPresent()
                    ? EstadoPartida.VICTORIA_POR_GOLES
                    : EstadoPartida.TABLAS_EMPATE_EN_GOLES);
        }
        return Optional.empty();
    }

    /** Gana quien tiene más goles, haya movido o no. */
    @Override
    public Optional<Bando> ganador(EstadoPartida estado, ContextoDeJugada contexto) {
        return estado.esTablas() ? Optional.empty() : Marcador.de(contexto.jugadas()).lider();
    }
}
