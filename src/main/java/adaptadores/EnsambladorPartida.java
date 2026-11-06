package adaptadores;

import Nucleo.bando.Bando;
import Nucleo.juego.DisposicionConPelota;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Jugador;
import Nucleo.juego.Partida;
import Nucleo.reglas.ReglaDeLaPelota;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tablero.DisposicionInicial;
import Nucleo.tiempo.FuenteDeTiempo;
import Nucleo.tiempo.RelojAjedrez;
import Nucleo.tiempo.RelojPartida;
import Nucleo.tiempo.SinReloj;
import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.ConfiguracionPartida.Modo;
import adaptadores.configuracion.PartidaConfigurada;
import adaptadores.configuracion.Sorteo;

import java.time.Duration;
import java.util.Objects;

/**
 * Único lugar que construye el grafo del juego para los adaptadores de entrada.
 * Recibe por constructor lo que depende del mundo exterior (el azar del sorteo y el tiempo),
 * así todo el ensamblado real se puede probar con dobles.
 */
public final class EnsambladorPartida {
    private final Sorteo sorteo;
    private final FuenteDeTiempo fuente;

    /** Ensamblado real: sorteo al azar y tiempo del sistema. */
    public EnsambladorPartida() {
        this(new SorteoAleatorio(), new FuenteDeTiempoSistema());
    }

    public EnsambladorPartida(Sorteo sorteo, FuenteDeTiempo fuente) {
        this.sorteo = Objects.requireNonNull(sorteo, "sorteo");
        this.fuente = Objects.requireNonNull(fuente, "fuente");
    }

    public Partida crearConReloj(Duration tiempoPorJugador) {
        return crearConReloj(tiempoPorJugador, fuente);
    }

    /** Permite sustituir únicamente la fuente externa, conservando el ensamblado real. */
    public Partida crearConReloj(Duration tiempoPorJugador, FuenteDeTiempo fuente) {
        return crear(new RelojAjedrez(tiempoPorJugador, tiempoPorJugador, fuente));
    }

    public Partida crearSinReloj() {
        return crear(new SinReloj());
    }

    /** Partida sin tiempo en la que se puede deshacer y rehacer (la usa la consola). */
    public Partida crearSinRelojConDeshacer() {
        return crearSinRelojConDeshacer(Modo.CLASICO);
    }

    /** Partida sin tiempo, con deshacer, del modo elegido (la consola puede jugar al fútbol). */
    public Partida crearSinRelojConDeshacer(Modo modo) {
        return crear(new SinReloj(), new Jugador("Jugador 1", Bando.BLANCO), new Jugador("Jugador 2", Bando.NEGRO),
                true, modo);
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
            case ALEATORIO -> sorteo.jugador1JuegaConBlancas();
        };
        RelojPartida reloj = configuracion.tiempo().duracion()
                .<RelojPartida>map(duracion -> new RelojAjedrez(duracion, duracion, fuente))
                .orElseGet(SinReloj::new);
        Partida partida = crear(reloj, new Jugador(jugador1Blancas ? "Jugador 1" : "Jugador 2", Bando.BLANCO),
                new Jugador(jugador1Blancas ? "Jugador 2" : "Jugador 1", Bando.NEGRO),
                configuracion.permitirDeshacer(), configuracion.modo());
        return new PartidaConfigurada(partida,
                jugador1Blancas ? configuracion.equipoJugador1() : configuracion.equipoJugador2(),
                jugador1Blancas ? configuracion.equipoJugador2() : configuracion.equipoJugador1(), reloj);
    }

    private Partida crear(RelojPartida reloj) {
        return crear(reloj, new Jugador("Jugador 1", Bando.BLANCO), new Jugador("Jugador 2", Bando.NEGRO), false,
                Modo.CLASICO);
    }

    /**
     * Cada partida recibe reglas nuevas: las condiciones de repetición y 50 movimientos guardan memoria.
     * El modo elige las reglas y la disposición: el fútbol suma la pelota en su saque inicial.
     */
    private Partida crear(RelojPartida reloj, Jugador blancas, Jugador negras, boolean permitirDeshacer, Modo modo) {
        ReglasDePartida reglas = switch (modo) {
            case CLASICO -> ReglasDePartida.clasicas();
            case FUTBOL -> ReglasDePartida.futbol();
        };
        DisposicionInicial disposicion = switch (modo) {
            case CLASICO -> new DisposicionEstandar();
            case FUTBOL -> new DisposicionConPelota(new DisposicionEstandar(), new ReglaDeLaPelota().saqueInicial());
        };
        return new Partida(disposicion.crearTablero(), blancas, negras,
                permitirDeshacer ? reglas.conDeshacer() : reglas, reloj);
    }
}
