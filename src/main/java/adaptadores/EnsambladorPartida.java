package adaptadores;

import Nucleo.bando.Bando;
import Nucleo.ia.Dificultad;
import Nucleo.ia.JugadorArtificial;
import Nucleo.juego.DisposicionDeFutbol;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Jugador;
import Nucleo.juego.Partida;
import Nucleo.juego.PartidaEnJuego;
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
import java.util.Optional;
import java.util.Random;

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

    public PartidaEnJuego crearConReloj(Duration tiempoPorJugador) {
        return crearConReloj(tiempoPorJugador, fuente);
    }

    /** Permite sustituir únicamente la fuente externa, conservando el ensamblado real. */
    public PartidaEnJuego crearConReloj(Duration tiempoPorJugador, FuenteDeTiempo fuente) {
        return crear(new RelojAjedrez(tiempoPorJugador, tiempoPorJugador, fuente));
    }

    public PartidaEnJuego crearSinReloj() {
        return crear(new SinReloj());
    }

    /** Partida sin tiempo en la que se puede deshacer y rehacer (la usa la consola). */
    public PartidaEnJuego crearSinRelojConDeshacer() {
        return crearSinRelojConDeshacer(Modo.CLASICO);
    }

    /** Partida sin tiempo, con deshacer, del modo elegido y el límite de jugadas por defecto. */
    public PartidaEnJuego crearSinRelojConDeshacer(Modo modo) {
        return crearSinRelojConDeshacer(modo, ConfiguracionPartida.LIMITE_POR_DEFECTO);
    }

    /** Partida sin tiempo, con deshacer, del modo elegido (la consola puede jugar al fútbol). */
    public PartidaEnJuego crearSinRelojConDeshacer(Modo modo, int limiteDeJugadas) {
        return crear(new SinReloj(), new Jugador("Jugador 1", Bando.BLANCO), new Jugador("Jugador 2", Bando.NEGRO),
                true, modo, limiteDeJugadas);
    }

    /**
     * Partida de consola contra la IA: sin tiempo y con deshacer. La IA juega con negras.
     */
    public PartidaContraLaIa crearSinRelojContraLaIa(Modo modo, int limiteDeJugadas, Dificultad dificultad) {
        Partida partida = crear(new SinReloj(), new Jugador("Jugador 1", Bando.BLANCO),
                new Jugador(nombreDeLaIa(dificultad), Bando.NEGRO), true, modo, limiteDeJugadas);
        return new PartidaContraLaIa(partida, crearRival(partida, Bando.NEGRO, dificultad));
    }

    /** Una partida y la IA que juega en ella. */
    public record PartidaContraLaIa(PartidaEnJuego partida, RivalArtificial rival) {
    }

    /** La IA de esa dificultad, con azar real y el tiempo del sistema. */
    public RivalArtificial crearRival(PartidaEnJuego partida, Bando bando, Dificultad dificultad) {
        return new RivalArtificial(partida, bando, new JugadorArtificial(dificultad, new Random(), fuente));
    }

    private static String nombreDeLaIa(Dificultad dificultad) {
        return "IA (" + switch (dificultad) {
            case FACIL -> "fácil";
            case MEDIO -> "medio";
            case DIFICIL -> "difícil";
        } + ")";
    }

    /** Resuelve el sorteo una sola vez y construye el juego elegido en el menú. */
    public PartidaEnJuego crearConfigurada(ConfiguracionPartida configuracion) {
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
        // La IA es el Jugador 2: juega con su equipo y con el color que le tocó.
        String jugador2 = configuracion.dificultad().map(EnsambladorPartida::nombreDeLaIa).orElse("Jugador 2");
        Partida partida = crear(reloj, new Jugador(jugador1Blancas ? "Jugador 1" : jugador2, Bando.BLANCO),
                new Jugador(jugador1Blancas ? jugador2 : "Jugador 1", Bando.NEGRO),
                configuracion.permitirDeshacer(), configuracion.modo(), configuracion.limiteDeJugadas());
        Bando bandoDelJugador2 = jugador1Blancas ? Bando.NEGRO : Bando.BLANCO;
        Optional<RivalArtificial> rival = configuracion.dificultad()
                .map(dificultad -> crearRival(partida, bandoDelJugador2, dificultad));
        return new PartidaConfigurada(partida,
                jugador1Blancas ? configuracion.equipoJugador1() : configuracion.equipoJugador2(),
                jugador1Blancas ? configuracion.equipoJugador2() : configuracion.equipoJugador1(), reloj, rival);
    }

    private Partida crear(RelojPartida reloj) {
        return crear(reloj, new Jugador("Jugador 1", Bando.BLANCO), new Jugador("Jugador 2", Bando.NEGRO), false,
                Modo.CLASICO, ConfiguracionPartida.LIMITE_POR_DEFECTO);
    }

    /**
     * Cada partida recibe reglas nuevas: las condiciones de repetición y 50 movimientos guardan memoria.
     * El modo elige las reglas y la disposición: el fútbol marca la cancha (arcos y pelota) y termina
     * en el límite de jugadas elegido.
     */
    private Partida crear(RelojPartida reloj, Jugador blancas, Jugador negras, boolean permitirDeshacer, Modo modo,
                          int limiteDeJugadas) {
        ReglasDePartida reglas = switch (modo) {
            case CLASICO -> ReglasDePartida.clasicas();
            case FUTBOL -> ReglasDePartida.futbol(limiteDeJugadas);
        };
        DisposicionInicial disposicion = switch (modo) {
            case CLASICO -> new DisposicionEstandar();
            case FUTBOL -> new DisposicionDeFutbol(new DisposicionEstandar(), new ReglaDeLaPelota());
        };
        return new Partida(disposicion.crearTablero(), blancas, negras,
                permitirDeshacer ? reglas.conDeshacer() : reglas, reloj);
    }
}
