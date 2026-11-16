package adaptadores;

import Nucleo.juego.Partida;
import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.ConfiguracionPartida.Modo;

import java.io.IOException;

/** Entrada de consola: utiliza el mismo ensamblador que el arranque gráfico. */
public class Main {

    /**
     * Sin argumentos se juega al ajedrez clásico. Con el argumento "futbol", al modo "Posesión y gol";
     * un segundo argumento elige el límite de jugadas (por ejemplo "futbol 60"; por defecto, 90).
     */
    public static void main(String[] args) throws IOException {
        Modo modo = args.length > 0 && args[0].equalsIgnoreCase("futbol") ? Modo.FUTBOL : Modo.CLASICO;
        int limite = args.length > 1 ? Integer.parseInt(args[1]) : ConfiguracionPartida.LIMITE_POR_DEFECTO;
        if (limite < ConfiguracionPartida.LIMITE_MINIMO || limite > ConfiguracionPartida.LIMITE_MAXIMO) {
            System.out.println("El limite de jugadas debe estar entre " + ConfiguracionPartida.LIMITE_MINIMO
                    + " y " + ConfiguracionPartida.LIMITE_MAXIMO + ".");
            return;
        }
        // La consola juega sin tiempo, así que permite deshacer y rehacer.
        Partida partida = new EnsambladorPartida().crearSinRelojConDeshacer(modo, limite);

        new JuegoConsola(partida, new RenderizadorConsola(), new MensajesDeConsola(), System.in, System.out).jugar();
    }
}
