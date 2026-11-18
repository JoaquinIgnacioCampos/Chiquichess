package adaptadores;

import Nucleo.ia.Dificultad;
import Nucleo.juego.Partida;
import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.ConfiguracionPartida.Modo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Entrada de consola: utiliza el mismo ensamblador que el arranque gráfico. */
public class Main {

    /**
     * Sin argumentos se juega al ajedrez clásico entre dos personas. Argumentos (en cualquier orden):
     * <ul>
     *   <li>"futbol": modo "Posesión y gol"; un número elige el límite de jugadas (por defecto, 90).</li>
     *   <li>"ia": contra la IA, que juega con negras; después puede ir "facil", "medio" o "dificil"
     *       (por defecto, medio).</li>
     * </ul>
     * Ejemplo: {@code futbol 60 ia dificil}.
     */
    public static void main(String[] args) throws IOException {
        List<String> argumentos = Arrays.stream(args).map(arg -> arg.toLowerCase(Locale.ROOT)).toList();
        Modo modo = argumentos.contains("futbol") ? Modo.FUTBOL : Modo.CLASICO;
        int limite = argumentos.stream().filter(arg -> arg.matches("[0-9]+")).map(Integer::parseInt).findFirst()
                .orElse(ConfiguracionPartida.LIMITE_POR_DEFECTO);
        if (limite < ConfiguracionPartida.LIMITE_MINIMO || limite > ConfiguracionPartida.LIMITE_MAXIMO) {
            System.out.println("El limite de jugadas debe estar entre " + ConfiguracionPartida.LIMITE_MINIMO
                    + " y " + ConfiguracionPartida.LIMITE_MAXIMO + ".");
            return;
        }
        EnsambladorPartida ensamblador = new EnsambladorPartida();
        // La consola juega sin tiempo, así que permite deshacer y rehacer.
        if (argumentos.contains("ia")) {
            Dificultad dificultad = argumentos.contains("facil") ? Dificultad.FACIL
                    : argumentos.contains("dificil") ? Dificultad.DIFICIL : Dificultad.MEDIO;
            var contraLaIa = ensamblador.crearSinRelojContraLaIa(modo, limite, dificultad);
            new JuegoConsola(contraLaIa.partida(), new RenderizadorConsola(), new MensajesDeConsola(), System.in,
                    System.out, Optional.of(contraLaIa.rival())).jugar();
            return;
        }
        Partida partida = ensamblador.crearSinRelojConDeshacer(modo, limite);
        new JuegoConsola(partida, new RenderizadorConsola(), new MensajesDeConsola(), System.in, System.out).jugar();
    }
}
