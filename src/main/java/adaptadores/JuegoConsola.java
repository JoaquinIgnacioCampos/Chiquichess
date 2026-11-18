package adaptadores;

import Nucleo.bando.Bando;
import Nucleo.juego.*;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.Marcador;
import Nucleo.tablero.Posicion;

import java.io.*;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Adaptador de entrada/salida por consola: lee jugadas como "e2 e4" y las envía a la Partida. */
public class JuegoConsola {

    private final Partida partida;
    private final RenderizadorConsola renderizador;
    private final MensajesDeConsola mensajes;
    private final BufferedReader entrada;
    private final PrintStream salida;
    /** La IA, si se juega contra ella: cuando le toca, juega sola y se muestra qué hizo. */
    private final Optional<RivalArtificial> rival;
    /** Cómo espera mientras la IA "piensa" como una persona; en los tests, no espera. */
    private final Consumer<Duration> esperar;

    public JuegoConsola(Partida partida, RenderizadorConsola renderizador, InputStream entrada, PrintStream salida) {
        this(partida, renderizador, new MensajesDeConsola(), entrada, salida);
    }

    public JuegoConsola(Partida partida, RenderizadorConsola renderizador, MensajesDeConsola mensajes,
                        InputStream entrada, PrintStream salida) {
        this(partida, renderizador, mensajes, entrada, salida, Optional.empty());
    }

    public JuegoConsola(Partida partida, RenderizadorConsola renderizador, MensajesDeConsola mensajes,
                        InputStream entrada, PrintStream salida, Optional<RivalArtificial> rival) {
        this(partida, renderizador, mensajes, entrada, salida, rival, JuegoConsola::dormir);
    }

    public JuegoConsola(Partida partida, RenderizadorConsola renderizador, MensajesDeConsola mensajes,
                        InputStream entrada, PrintStream salida, Optional<RivalArtificial> rival,
                        Consumer<Duration> esperar) {
        this.rival = Objects.requireNonNull(rival, "rival");
        this.esperar = Objects.requireNonNull(esperar, "esperar");
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
        salida.println("Para promover un peon, agregue la pieza: D (dama), T (torre), A (alfil) o C (caballo). Ej: e7 e8 C");
        if (conPelota()) {
            salida.println("Modo futbol: lleve la pelota (o; un * marca a quien la tiene) al arco rival (#).");
            salida.println("Para pasarla, mueva a quien la tiene hacia un companero que podria capturar (ej: d4 d7).");
            salida.println("Gana quien llega a 3 goles o tiene mas goles en la jugada "
                    + partida.limiteDeJugadas().map(String::valueOf).orElse("final") + ". El jaque mate gana siempre.");
        }
        while (true) {
            salida.println();
            salida.println(renderizador.dibujar(partida.tablero()));
            if (conPelota()) {
                salida.println(marcador());
            }
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
                if (rival.filter(RivalArtificial::leToca).isPresent()) {
                    jugarLaIa(rival.get());
                    continue;
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
                // Contra la IA se deshace de a dos jugadas, para que vuelva a tocarle a la persona.
                if (linea.equalsIgnoreCase("deshacer")) {
                    if (rival.isPresent()) rival.get().deshacer(); else partida.deshacer();
                } else {
                    if (rival.isPresent()) rival.get().rehacer(); else partida.rehacer();
                }
            } catch (AccionInvalidaException e) {
                salida.println(mensajes.rechazoDeAccion(e.motivo()));
            }
            return;
        }
        String[] partes = linea.split("\\s+");
        // Un tercer dato, de una letra, elige la pieza al promover: "e7 e8 C". Sin él, se promueve a dama.
        boolean conPromocion = partes.length == 3 && partes[2].length() == 1;
        if (partes.length != 2 && !conPromocion) {
            salida.println("Formato invalido. Ejemplo: e2 e4 (para promover: e7 e8 C)");
            return;
        }
        try {
            Posicion origen = NotacionAlgebraica.parsear(partes[0]);
            Posicion destino = NotacionAlgebraica.parsear(partes[1]);
            Movimiento movimiento = conPromocion
                    ? Movimiento.conPromocion(origen, destino, Character.toUpperCase(partes[2].charAt(0)))
                    : new Movimiento(origen, destino);
            Marcador antes = partida.marcador();
            boolean pase = partida.tablero().piezaEn(destino)
                    .filter(pieza -> pieza.color() == partida.turno()).isPresent();
            ResultadoMovimiento resultado = partida.mover(movimiento);
            if (pase) {
                salida.println("Pase!");
            }
            resultado.piezaCapturada().ifPresent(p -> salida.println("Captura: " + p.nombre()));
            if (!partida.marcador().equals(antes)) {
                salida.println("GOL!");
            }
        } catch (IllegalArgumentException e) {
            salida.println(e.getMessage());
        } catch (MovimientoInvalidoException e) {
            salida.println(mensajes.rechazoDeMovimiento(e.motivo()));
        }
    }

    /** La IA piensa (la consola espera) y juega; se muestra la jugada como la escribiría una persona. */
    private void jugarLaIa(RivalArtificial ia) {
        salida.println(partida.jugadorActual().nombre() + " esta pensando...");
        Marcador antes = partida.marcador();
        long inicio = System.nanoTime();
        RivalArtificial.Respuesta respuesta = ia.pensar();
        // Mueve recién cuando pasó lo que tardaría una persona de su nivel.
        Duration falta = respuesta.demora().minusNanos(System.nanoTime() - inicio);
        if (!falta.isNegative() && !falta.isZero()) esperar.accept(falta);
        Movimiento jugada = respuesta.movimiento();
        partida.mover(jugada);
        salida.println(partida.jugador(ia.bando()).nombre() + " juega " + NotacionAlgebraica.formatear(jugada.origen())
                + " " + NotacionAlgebraica.formatear(jugada.destino())
                + jugada.promocion().map(codigo -> " " + codigo).orElse(""));
        if (!partida.marcador().equals(antes)) {
            salida.println("GOL!");
        }
    }

    private static void dormir(Duration tiempo) {
        try {
            Thread.sleep(tiempo);
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean conPelota() {
        return partida.tablero().pelota().isPresent();
    }

    private String marcador() {
        Marcador marcador = partida.marcador();
        return "Marcador: " + partida.jugador(Bando.BLANCO).nombre() + " " + marcador.blancas()
                + " - " + marcador.negras() + " " + partida.jugador(Bando.NEGRO).nombre()
                + "   (jugada " + partida.historial().size()
                + partida.limiteDeJugadas().map(limite -> " de " + limite).orElse("") + ")";
    }

    private String mensajeFinal() {
        return partida.resultado()
                .map(resultado -> mensajes.finDePartida(resultado.motivo(),
                        partida.ganador().map(Jugador::nombre)))
                .orElse("");
    }
}
