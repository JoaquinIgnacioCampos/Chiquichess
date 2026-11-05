package adaptadores;

import Nucleo.juego.*;
import Nucleo.reglas.EstadoPartida;


import java.io.*;

/** Adaptador de entrada/salida por consola: lee jugadas como "e2 e4" y las envía a la Partida. */
public class JuegoConsola {

    private final Partida partida;
    private final RenderizadorConsola renderizador;
    private final MensajesDeConsola mensajes;
    private final BufferedReader entrada;
    private final PrintStream salida;

    public JuegoConsola(Partida partida, RenderizadorConsola renderizador, InputStream entrada, PrintStream salida) {
        this(partida, renderizador, new MensajesDeConsola(), entrada, salida);
    }

    public JuegoConsola(Partida partida, RenderizadorConsola renderizador, MensajesDeConsola mensajes,
                        InputStream entrada, PrintStream salida) {
        this.partida = partida;
        this.renderizador = renderizador;
        this.mensajes = mensajes;
        this.entrada = new BufferedReader(new InputStreamReader(entrada));
        this.salida = salida;
    }

    public void jugar() throws IOException {
        salida.println(partida.permiteDeshacer()
                ? "Escriba la jugada como 'origen destino' (ej: e2 e4), 'deshacer', 'rehacer' o 'salir'."
                : "Escriba la jugada como 'origen destino' (ej: e2 e4) o 'salir'.");
        while (true) {
            salida.println();
            salida.println(renderizador.dibujar(partida.tablero()));
            if (partida.estado().estaFinalizada()) {
                salida.println(mensajeFinal());
                // Un mate o unas tablas automáticas se pueden deshacer: se ofrece volver atrás.
                if (!partida.puedeDeshacer()) {
                    break;
                }
                salida.print("Escriba 'deshacer' para volver atras o 'salir': ");
            } else {
                if (partida.estado() == EstadoPartida.JAQUE) {
                    salida.println("Jaque!");
                }
                salida.print("Turno de " + partida.jugadorActual().nombre()
                        + " (" + partida.turno() + "): ");
            }
            salida.flush();

            String linea = entrada.readLine();
            if (linea == null || linea.trim().equalsIgnoreCase("salir")) {
                break;
            }
            procesar(linea.trim());
        }
    }

    private void procesar(String linea) {
        if (linea.equalsIgnoreCase("deshacer") || linea.equalsIgnoreCase("rehacer")) {
            try {
                if (linea.equalsIgnoreCase("deshacer")) {
                    partida.deshacer();
                } else {
                    partida.rehacer();
                }
            } catch (AccionInvalidaException e) {
                salida.println(mensajes.rechazoDeAccion(e.motivo()));
            }
            return;
        }
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
            salida.println(mensajes.rechazoDeMovimiento(e.motivo()));
        }
    }

    private String mensajeFinal() {
        return partida.resultado()
                .map(resultado -> mensajes.finDePartida(resultado.motivo(),
                        partida.ganador().map(Jugador::nombre)))
                .orElse("");
    }
}
