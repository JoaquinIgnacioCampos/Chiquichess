package Nucleo.tiempo;

import Nucleo.bando.Bando;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Descuenta tiempo por consultas y cambios de turno, sin ejecutar tareas propias. */
public class RelojAjedrez implements RelojPartida {
    private final FuenteDeTiempo fuente;
    private final Map<Bando, Duration> restantes = new EnumMap<>(Bando.class);
    private boolean iniciado;
    private Bando activo;
    private long inicioDelTurno;

    public RelojAjedrez(Duration tiempoBlanco, Duration tiempoNegro, FuenteDeTiempo fuente) {
        this.fuente = Objects.requireNonNull(fuente, "fuente");
        restantes.put(Bando.BLANCO, validarDuracion(tiempoBlanco));
        restantes.put(Bando.NEGRO, validarDuracion(tiempoNegro));
    }

    private static Duration validarDuracion(Duration duracion) {
        Objects.requireNonNull(duracion, "duracion");
        if (duracion.isNegative() || duracion.isZero()) {
            throw new IllegalArgumentException("El tiempo inicial debe ser positivo");
        }
        return duracion;
    }

    @Override
    public void iniciar(Bando color) {
        Objects.requireNonNull(color, "color");
        if (iniciado) {
            throw new IllegalStateException("El reloj ya pertenece a una partida iniciada");
        }
        inicioDelTurno = fuente.ahoraEnNanos();
        activo = color;
        iniciado = true;
    }

    @Override
    public void cambiarTurno() {
        if (activo == null) {
            throw new IllegalStateException("El reloj no está corriendo");
        }
        long ahora = fuente.ahoraEnNanos();
        restantes.put(activo, restanteActivo(ahora));
        activo = activo.opuesto();
        inicioDelTurno = ahora;
    }

    @Override
    public void detener() {
        if (activo != null) {
            restantes.put(activo, restanteActivo(fuente.ahoraEnNanos()));
            activo = null;
        }
    }

    @Override
    public Optional<Duration> tiempoRestante(Bando color) {
        Objects.requireNonNull(color, "color");
        return Optional.of(color == activo
                ? restanteActivo(fuente.ahoraEnNanos())
                : restantes.get(color));
    }

    private Duration restanteActivo(long ahora) {
        Duration restante = restantes.get(activo).minusNanos(ahora - inicioDelTurno);
        return restante.isNegative() ? Duration.ZERO : restante;
    }
}
