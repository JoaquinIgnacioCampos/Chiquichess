package adaptadores;

import Nucleo.juego.*;
import Nucleo.reglas.EstadoPartida;


import java.io.*;

/** Adaptador de entrada/salida por consola: lee jugadas como "e2 e4" y las envía a la Partida. */
public class JuegoConsola {

    private final Partida partida;
    private final RenderizadorConsola renderizador;
    private final BufferedReader entrada;
    private final PrintStream salida;

    public JuegoConsola(Partida partida, RenderizadorConsola renderizador, InputStream entrada, PrintStream salida) {
        this.partida = partida;
        this.renderizador = renderizador;
        this.entrada = new BufferedReader(new InputStreamReader(entrada));
        this.salida = salida;
    }

    public void jugar() throws IOException {
        salida.println("Escriba la jugada como 'origen destino' (ej: e2 e4) o 'salir'.");
        while (true) {
            salida.println();
            salida.println(renderizador.dibujar(partida.tablero()));
            if (partida.estado().estaFinalizada()) {
                salida.println(mensajeFinal());
                break;
            }
            if (partida.estado() == EstadoPartida.JAQUE) {
                salida.println("Jaque!");
            }
            salida.print("Turno de " + partida.jugadorActual().nombre()
                    + " (" + partida.turno() + "): ");
            salida.flush();

            String linea = entrada.readLine();
            if (linea == null || linea.trim().equalsIgnoreCase("salir")) {
                break;
            }
            procesar(linea.trim());
        }
    }

    private void procesar(String linea) {
        String[] partes = linea.split("\\s+");
        if (partes.length != 2) {
            salida.println("Formato invalido. Ejemplo: e2 e4");
            return;
        }
        try {
            Movimiento movimiento = new Movimiento(
                    NotacionAlgebraica.parsear(partes[0]),
                    NotacionAlgebraica.parsear(partes[1]));
            ResultadoMovimiento resultado = partida.mover(movimiento);
            resultado.piezaCapturada().ifPresent(p -> salida.println("Captura: " + p.nombre()));
        } catch (IllegalArgumentException e) {
            salida.println(e.getMessage());
        } catch (MovimientoInvalidoException e) {
            salida.println(mensaje(e.motivo()));
        }
    }

    private String mensaje(MovimientoInvalidoException.Motivo motivo) {
        return switch (motivo) {
            case SIN_PIEZA_EN_ORIGEN -> "No hay ninguna pieza en la casilla de origen.";
            case NO_ES_SU_TURNO -> "Esa pieza no es suya: es el turno del otro jugador.";
            case MOVIMIENTO_NO_PERMITIDO -> "Esa pieza no puede moverse ahi.";
            case DEJA_AL_REY_EN_JAQUE -> "Movimiento ilegal: deja a su rey en jaque.";
            case PARTIDA_FINALIZADA -> "La partida ya termino.";
        };
    }

    private String mensajeFinal() {
        return switch (partida.estado()) {
            case JAQUE_MATE -> "Jaque mate! Gana "
                    + partida.ganador().map(Jugador::nombre).orElse("?") + ".";
            case TABLAS_AHOGADO -> "Tablas por ahogado: el jugador no tiene jugadas pero no esta en jaque.";
            case TABLAS_REPETICION -> "Tablas por repeticion de posicion.";
            case TABLAS_REGLA_50_MOVIMIENTOS -> "Tablas por la regla de los 50 movimientos.";
            case EN_CURSO, JAQUE -> "";
        };
    }
}
