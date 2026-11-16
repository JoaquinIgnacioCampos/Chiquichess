package Nucleo.reglas;

import java.util.List;
import java.util.Objects;

/**
 * Reglas con las que se juega una partida: qué jugadas son legales, cuándo termina y si se permite
 * deshacer jugadas. Agrupa lo que Partida consulta en una sola dependencia, así cada regla nueva
 * (por ejemplo, jugadas especiales) se agrega acá sin sumar otro constructor a Partida.
 *
 * El detector de jaque no se recibe por separado: es el mismo que usan los movimientos legales,
 * de modo que no pueden quedar dos detectores distintos en una misma partida.
 *
 * Las condiciones con memoria (repetición, 50 movimientos) guardan estado, por eso una instancia
 * pertenece a una sola partida. {@link #clasicas()} crea instancias nuevas en cada llamada.
 *
 * @param condiciones     condiciones de fin, evaluadas en orden (la primera que se cumple gana).
 *                        Conviene poner primero jaque mate y ahogado.
 * @param permiteDeshacer si los jugadores pueden deshacer y rehacer jugadas. Aun habilitado, Partida
 *                        lo rechaza si la partida tiene límite de tiempo.
 */
public record ReglasDePartida(MovimientosLegales movimientosLegales, List<CondicionDeFinalizacion> condiciones,
                              boolean permiteDeshacer) {

    public ReglasDePartida {
        Objects.requireNonNull(movimientosLegales, "movimientosLegales");
        condiciones = List.copyOf(Objects.requireNonNull(condiciones, "condiciones"));
    }

    /** Reglas sin deshacer. */
    public ReglasDePartida(MovimientosLegales movimientosLegales, List<CondicionDeFinalizacion> condiciones) {
        this(movimientosLegales, condiciones, false);
    }

    /** Arma los movimientos legales con el detector recibido, sin reglas especiales ni deshacer. */
    public ReglasDePartida(IDetectorDeJaque detector, List<CondicionDeFinalizacion> condiciones) {
        this(new MovimientosLegales(detector), condiciones);
    }

    /**
     * Ajedrez clásico: enroque, captura al paso y promoción; jaque mate, ahogado, triple repetición y
     * regla de los 50 movimientos; sin deshacer.
     */
    public static ReglasDePartida clasicas() {
        IDetectorDeJaque detector = new DetectorDeJaque();
        List<ReglaEspecial> especiales = List.of(new Enroque(), new CapturaAlPaso(), new Promocion());
        return new ReglasDePartida(new MovimientosLegales(detector, especiales), List.of(
                new JaqueMate(), new Ahogado(),
                new TablasPorRepeticion(), new TablasPorReglaDeLos50Movimientos()));
    }

    /**
     * Modo fútbol "Posesión y gol" con el límite por defecto de 90 jugadas.
     */
    public static ReglasDePartida futbol() {
        return futbol(90);
    }

    /**
     * Modo fútbol "Posesión y gol": las reglas clásicas más la pelota. Gana quien llega a 3 goles o
     * tiene más goles al llegar al límite de jugadas; el jaque mate gana siempre y las tablas
     * clásicas siguen valiendo. La cancha (arcos y pelota) se marca en el tablero con
     * {@link Nucleo.juego.DisposicionDeFutbol}.
     *
     * @param limiteDeJugadas jugadas de los dos bandos juntos a las que termina la partida
     */
    public static ReglasDePartida futbol(int limiteDeJugadas) {
        IDetectorDeJaque detector = new DetectorDeJaque();
        List<ReglaEspecial> especiales = List.of(
                new Enroque(), new CapturaAlPaso(), new Promocion(), new ReglaDeLaPelota());
        return new ReglasDePartida(new MovimientosLegales(detector, especiales), List.of(
                new JaqueMate(), new VictoriaPorGoles(3, limiteDeJugadas), new Ahogado(),
                new TablasPorRepeticion(), new TablasPorReglaDeLos50Movimientos()));
    }

    /** Las mismas reglas, permitiendo deshacer y rehacer jugadas. */
    public ReglasDePartida conDeshacer() {
        return new ReglasDePartida(movimientosLegales, condiciones, true);
    }

    public IDetectorDeJaque detector() {
        return movimientosLegales.detector();
    }
}
