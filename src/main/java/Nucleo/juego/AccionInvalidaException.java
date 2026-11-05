package Nucleo.juego;

/** Rechazo de una orden que no es un movimiento de pieza. */
public class AccionInvalidaException extends RuntimeException {
    public enum Motivo {
        PARTIDA_FINALIZADA,
        NO_ES_SU_TURNO,
        OFERTA_PENDIENTE,
        SIN_OFERTA,
        ES_SU_PROPIA_OFERTA
    }

    private static final long serialVersionUID = 1L;
    private final Motivo motivo;

    public AccionInvalidaException(Motivo motivo) {
        super(motivo.name());
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
