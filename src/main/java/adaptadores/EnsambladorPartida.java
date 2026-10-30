package adaptadores;

import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Jugador;
import Nucleo.juego.Partida;
import Nucleo.piezas.Color;
import Nucleo.reglas.Ahogado;
import Nucleo.reglas.DetectorDeJaque;
import Nucleo.reglas.JaqueMate;
import Nucleo.reglas.MovimientosLegales;
import Nucleo.reglas.TablasPorReglaDeLos50Movimientos;
import Nucleo.reglas.TablasPorRepeticion;
import Nucleo.tiempo.FuenteDeTiempo;
import Nucleo.tiempo.RelojAjedrez;
import Nucleo.tiempo.RelojPartida;
import Nucleo.tiempo.SinReloj;

import java.time.Duration;
import java.util.List;

/** Único lugar que construye el grafo del juego para los adaptadores de entrada. */
public final class EnsambladorPartida {
    public Partida crearConReloj(Duration tiempoPorJugador) {
        return crearConReloj(tiempoPorJugador, new FuenteDeTiempoSistema());
    }

    /** Permite sustituir únicamente la fuente externa, conservando el ensamblado real. */
    public Partida crearConReloj(Duration tiempoPorJugador, FuenteDeTiempo fuente) {
        return crear(new RelojAjedrez(tiempoPorJugador, tiempoPorJugador, fuente));
    }

    public Partida crearSinReloj() {
        return crear(new SinReloj());
    }

    private Partida crear(RelojPartida reloj) {
        DetectorDeJaque detector = new DetectorDeJaque();
        return new Partida(
                new DisposicionEstandar().crearTablero(),
                new Jugador("Jugador 1", Color.BLANCO),
                new Jugador("Jugador 2", Color.NEGRO),
                detector,
                new MovimientosLegales(detector),
                List.of(new JaqueMate(), new Ahogado(),
                        new TablasPorRepeticion(), new TablasPorReglaDeLos50Movimientos()),
                reloj);
    }
}
