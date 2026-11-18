package adaptadores.audio;

import java.util.Objects;
import java.util.Optional;

/**
 * Sonido de la pantalla de selección: la risa del personaje la primera vez que se abre el juego y un
 * ambiente suave en bucle mientras se eligen las opciones. El ambiente se calla al empezar una partida
 * y vuelve al volver al menú. Sigue el volumen y el silencio del {@link ControlDeVolumen}, el mismo
 * de la partida.
 *
 * Como el resto del audio, si falta un archivo ese sonido no suena y el juego sigue igual.
 */
public final class SonidosDelMenu implements AutoCloseable {
    /** El ambiente acompaña: suena bastante más bajo que los efectos. */
    static final double VOLUMEN_AMBIENTE = 0.35;

    private final CatalogoDeSonidos catalogo;
    private final ReproductorDeAudio reproductor;
    private final ControlDeVolumen control;
    private final Runnable alCambiarVolumen = this::ajustarVolumen;
    private Optional<ReproductorDeAudio.Pista> ambiente = Optional.empty();
    private boolean seRio = false;

    public SonidosDelMenu(CatalogoDeSonidos catalogo, ReproductorDeAudio reproductor, ControlDeVolumen control) {
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.reproductor = Objects.requireNonNull(reproductor, "reproductor");
        this.control = Objects.requireNonNull(control, "control");
        control.alCambiar(alCambiarVolumen);
    }

    /** Al abrir el juego: la risa (solo la primera vez) y el ambiente del menú. */
    public void abrirJuego() {
        if (!seRio) {
            seRio = true;
            catalogo.buscar(Sonido.RISA, Optional.empty())
                    .ifPresent(archivo -> reproductor.reproducir(archivo, control.efectivo()));
        }
        entrarAlMenu();
    }

    /** Empieza el ambiente, si no estaba sonando. */
    public void entrarAlMenu() {
        if (ambiente.isPresent()) return;
        ambiente = catalogo.buscar(Sonido.AMBIENTE_MENU, Optional.empty())
                .map(archivo -> reproductor.enBucle(archivo, volumenDelAmbiente()));
    }

    /** Calla el ambiente: empieza una partida. */
    public void salirDelMenu() {
        ambiente.ifPresent(ReproductorDeAudio.Pista::detener);
        ambiente = Optional.empty();
    }

    private void ajustarVolumen() {
        ambiente.ifPresent(pista -> pista.volumen(volumenDelAmbiente()));
    }

    private double volumenDelAmbiente() {
        return control.efectivo() * VOLUMEN_AMBIENTE;
    }

    @Override
    public void close() {
        salirDelMenu();
        control.dejarDeEscuchar(alCambiarVolumen);
    }
}
