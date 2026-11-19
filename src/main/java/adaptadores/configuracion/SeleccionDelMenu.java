package adaptadores.configuracion;

import Nucleo.ia.Dificultad;
import adaptadores.configuracion.ConfiguracionPartida.Colores;
import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import adaptadores.configuracion.ConfiguracionPartida.Modo;
import adaptadores.configuracion.ConfiguracionPartida.Rival;
import adaptadores.configuracion.ConfiguracionPartida.Tiempo;

import java.util.Objects;
import java.util.Optional;

/**
 * Opciones que el menú muestra elegidas al abrirse. A diferencia de {@link ConfiguracionPartida}, puede
 * estar incompleta: por ejemplo, la selección habitual no elige equipos.
 */
public record SeleccionDelMenu(Optional<Modo> modo, Optional<Tiempo> tiempo, Optional<Rival> rival,
                               Optional<Equipo> equipoJugador1, Optional<Equipo> equipoJugador2,
                               Optional<Colores> colores, boolean permitirDeshacer, int limiteDeJugadas,
                               Optional<Dificultad> dificultad) {

    public SeleccionDelMenu {
        Objects.requireNonNull(modo, "modo");
        Objects.requireNonNull(tiempo, "tiempo");
        Objects.requireNonNull(rival, "rival");
        Objects.requireNonNull(equipoJugador1, "equipoJugador1");
        Objects.requireNonNull(equipoJugador2, "equipoJugador2");
        Objects.requireNonNull(colores, "colores");
        Objects.requireNonNull(dificultad, "dificultad");
    }

    /** La partida más común: clásico, 10 minutos, contra un amigo y colores al azar; los equipos los elige cada uno. */
    public static SeleccionDelMenu habitual() {
        return new SeleccionDelMenu(Optional.of(Modo.CLASICO), Optional.of(Tiempo.DIEZ_MINUTOS),
                Optional.of(Rival.AMIGO), Optional.empty(), Optional.empty(), Optional.of(Colores.ALEATORIO),
                false, ConfiguracionPartida.LIMITE_POR_DEFECTO, Optional.empty());
    }

    /** Lo que se eligió para la última partida, para volver a mostrarlo al regresar al menú. */
    public static SeleccionDelMenu de(ConfiguracionPartida configuracion) {
        Objects.requireNonNull(configuracion, "configuracion");
        return new SeleccionDelMenu(Optional.of(configuracion.modo()), Optional.of(configuracion.tiempo()),
                Optional.of(configuracion.rival()), Optional.of(configuracion.equipoJugador1()),
                Optional.of(configuracion.equipoJugador2()), Optional.of(configuracion.colores()),
                configuracion.permitirDeshacer(), configuracion.limiteDeJugadas(), configuracion.dificultad());
    }
}
