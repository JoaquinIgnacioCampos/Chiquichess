package adaptadores.audio;

import Nucleo.bando.Bando;
import Nucleo.juego.EventoDePartida;
import Nucleo.juego.ObservadorDePartida;
import Nucleo.juego.Partida;
import Nucleo.reglas.Marcador;
import adaptadores.audio.GuionDeSonidos.Toque;
import adaptadores.configuracion.ConfiguracionPartida.Equipo;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de sonido de una partida: escucha sus eventos (Observer), le pregunta al
 * {@link GuionDeSonidos} qué tiene que sonar, busca el archivo del club que corresponda en el
 * {@link CatalogoDeSonidos} y se lo pasa al {@link ReproductorDeAudio}.
 *
 * En el modo fútbol mantiene además las dos hinchadas sonando en bucle, con el volumen que da
 * {@link IntensidadDeHinchada} después de cada evento. Cuando la partida termina, las hinchadas se
 * callan y queda el sonido del final; si se deshace ese final, vuelven.
 */
public final class SonidosDePartida implements ObservadorDePartida, AutoCloseable {
    /** El clic de seleccionar una pieza es más suave que un movimiento. */
    static final double VOLUMEN_SELECCION = 0.6;
    /** Las hinchadas suenan más bajo que los efectos, para que no los tapen. */
    static final double VOLUMEN_HINCHADA = 0.6;

    private final Partida partida;
    private final CatalogoDeSonidos catalogo;
    private final ReproductorDeAudio reproductor;
    private final Map<Bando, Equipo> equipos;
    private final ControlDeVolumen control;
    private final GuionDeSonidos guion;
    private final boolean futbol;
    private final Map<Bando, ReproductorDeAudio.Pista> hinchadas = new EnumMap<>(Bando.class);
    private final Runnable alCambiarVolumen = this::actualizarHinchadas;
    private boolean cerrado = false;

    private SonidosDePartida(Partida partida, CatalogoDeSonidos catalogo, ReproductorDeAudio reproductor,
                             Map<Bando, Equipo> equipos, ControlDeVolumen control, boolean futbol) {
        this.partida = Objects.requireNonNull(partida, "partida");
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.reproductor = Objects.requireNonNull(reproductor, "reproductor");
        this.equipos = Map.copyOf(Objects.requireNonNull(equipos, "equipos"));
        this.control = Objects.requireNonNull(control, "control");
        this.futbol = futbol;
        this.guion = new GuionDeSonidos(futbol);
    }

    /**
     * Engancha el sonido a la partida: la escucha desde ahora, toca lo que suena al empezar y, en el
     * modo fútbol, pone a cantar a las hinchadas. Cerrarlo lo desengancha.
     *
     * @param equipos el club de cada bando, para los sonidos propios de cada uno
     */
    public static SonidosDePartida conectar(Partida partida, CatalogoDeSonidos catalogo, ReproductorDeAudio reproductor,
                                            Map<Bando, Equipo> equipos, ControlDeVolumen control, boolean futbol) {
        SonidosDePartida sonidos = new SonidosDePartida(partida, catalogo, reproductor, equipos, control, futbol);
        partida.agregarObservador(sonidos);
        control.alCambiar(sonidos.alCambiarVolumen);
        sonidos.guion.alEmpezar().forEach(sonidos::tocar);
        sonidos.actualizarHinchadas();
        return sonidos;
    }

    @Override
    public void notificar(EventoDePartida evento) {
        if (cerrado) return;
        guion.para(evento).forEach(this::tocar);
        actualizarHinchadas();
    }

    /**
     * La persona tocó una pieza suya para moverla. No es un evento de la partida (la partida no cambió):
     * lo avisa la vista.
     */
    public void piezaSeleccionada() {
        if (cerrado) return;
        catalogo.buscar(Sonido.SELECCION, Optional.empty())
                .ifPresent(archivo -> reproductor.reproducir(archivo, control.efectivo() * VOLUMEN_SELECCION));
    }

    private void tocar(Toque toque) {
        catalogo.buscar(toque.sonido(), toque.bando().map(equipos::get))
                .ifPresent(archivo -> reproductor.reproducir(archivo, control.efectivo()));
    }

    private void actualizarHinchadas() {
        if (!futbol || cerrado) return;
        if (partida.estado().estaFinalizada()) {
            detenerHinchadas();
            return;
        }
        Map<Bando, URL> archivos = new EnumMap<>(Bando.class);
        for (Bando bando : Bando.values()) {
            catalogo.buscar(Sonido.HINCHADA, Optional.ofNullable(equipos.get(bando)))
                    .ifPresent(archivo -> archivos.put(bando, archivo));
        }
        // Si los dos clubes usan el mismo archivo, suena una sola hinchada: dos copias iguales se pisan.
        boolean compartida = archivos.size() == 2 && archivos.get(Bando.BLANCO).toExternalForm()
                .equals(archivos.get(Bando.NEGRO).toExternalForm());
        if (compartida) archivos.remove(Bando.NEGRO);
        for (Bando bando : Bando.values()) {
            URL archivo = archivos.get(bando);
            if (archivo == null) {
                Optional.ofNullable(hinchadas.remove(bando)).ifPresent(ReproductorDeAudio.Pista::detener);
                continue;
            }
            double intensidad = compartida
                    ? Math.max(intensidad(Bando.BLANCO), intensidad(Bando.NEGRO))
                    : intensidad(bando);
            double volumen = control.efectivo() * VOLUMEN_HINCHADA * intensidad;
            ReproductorDeAudio.Pista pista = hinchadas.get(bando);
            if (pista == null) {
                hinchadas.put(bando, reproductor.enBucle(archivo, volumen));
            } else {
                pista.volumen(volumen);
            }
        }
    }

    private double intensidad(Bando bando) {
        return IntensidadDeHinchada.de(bando, partida.historial().size(), partida.limiteDeJugadas(),
                partida.marcador().orElse(new Marcador(0, 0)));
    }

    private void detenerHinchadas() {
        hinchadas.values().forEach(ReproductorDeAudio.Pista::detener);
        hinchadas.clear();
    }

    /** Desengancha el sonido de la partida y calla a las hinchadas. No cierra el reproductor, que es compartido. */
    @Override
    public void close() {
        if (cerrado) return;
        detenerHinchadas();
        cerrado = true;
        partida.quitarObservador(this);
        control.dejarDeEscuchar(alCambiarVolumen);
    }
}
