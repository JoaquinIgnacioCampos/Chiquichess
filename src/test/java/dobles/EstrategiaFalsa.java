package dobles;

import Nucleo.ia.EstrategiaDeJuego;
import Nucleo.juego.SituacionDePartida;
import Nucleo.jugadas.Jugada;
import Nucleo.tablero.Posicion;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * IA de prueba: juega la primera jugada legal (recorriendo las casillas en orden) y acepta o rechaza
 * tablas según se le diga. Anota cuánto tiempo le dieron para pensar.
 */
public class EstrategiaFalsa implements EstrategiaDeJuego {
    private final boolean aceptaTablas;
    public final List<Duration> tiemposRecibidos = new ArrayList<>();

    public EstrategiaFalsa(boolean aceptaTablas) {
        this.aceptaTablas = aceptaTablas;
    }

    @Override
    public Jugada elegirJugada(SituacionDePartida situacion, Duration tiempoMaximo) {
        tiemposRecibidos.add(tiempoMaximo);
        var legales = situacion.reglas().movimientosLegales();
        List<Posicion> propias = situacion.tablero().piezas().entrySet().stream()
                .filter(entrada -> entrada.getValue().color() == situacion.turno())
                .map(java.util.Map.Entry::getKey)
                .sorted(Comparator.comparingInt(Posicion::fila).thenComparingInt(Posicion::columna))
                .toList();
        for (Posicion origen : propias) {
            for (Jugada jugada : legales.jugadasPosibles(situacion.tablero(), origen, situacion.jugadas())) {
                if (!legales.dejaAlReyEnJaque(situacion.tablero(), jugada)) return jugada;
            }
        }
        throw new IllegalStateException("No hay jugadas legales");
    }

    @Override
    public boolean aceptaTablas(SituacionDePartida situacion) {
        return aceptaTablas;
    }
}
