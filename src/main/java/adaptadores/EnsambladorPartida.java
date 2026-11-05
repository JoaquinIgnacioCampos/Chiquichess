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
import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.PartidaConfigurada;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

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

    /** Resuelve el sorteo una sola vez y construye el juego elegido en el menú. */
    public Partida crearConfigurada(ConfiguracionPartida configuracion) {
        return prepararConfigurada(configuracion).partida();
    }

    public PartidaConfigurada prepararConfigurada(ConfiguracionPartida configuracion) {
        Objects.requireNonNull(configuracion, "configuracion");
        boolean jugador1Blancas = switch (configuracion.colores()) {
            case JUGADOR_1_BLANCAS -> true;
            case JUGADOR_2_BLANCAS -> false;
            case ALEATORIO -> ThreadLocalRandom.current().nextBoolean();
        };
        RelojPartida reloj = configuracion.tiempo().duracion()
                .<RelojPartida>map(duracion -> new RelojAjedrez(duracion, duracion, new FuenteDeTiempoSistema()))
                .orElseGet(SinReloj::new);
        Partida partida = crear(reloj, new Jugador(jugador1Blancas ? "Jugador 1" : "Jugador 2", Color.BLANCO),
                new Jugador(jugador1Blancas ? "Jugador 2" : "Jugador 1", Color.NEGRO));
        return new PartidaConfigurada(partida,
                jugador1Blancas ? configuracion.equipoJugador1() : configuracion.equipoJugador2(),
                jugador1Blancas ? configuracion.equipoJugador2() : configuracion.equipoJugador1(), reloj);
    }

    private Partida crear(RelojPartida reloj) {
        return crear(reloj, new Jugador("Jugador 1", Color.BLANCO), new Jugador("Jugador 2", Color.NEGRO));
    }

    private Partida crear(RelojPartida reloj, Jugador blancas, Jugador negras) {
        DetectorDeJaque detector = new DetectorDeJaque();
        return new Partida(
                new DisposicionEstandar().crearTablero(),
                blancas,
                negras,
                detector,
                new MovimientosLegales(detector),
                List.of(new JaqueMate(), new Ahogado(),
                        new TablasPorRepeticion(), new TablasPorReglaDeLos50Movimientos()),
                reloj);
    }
}
