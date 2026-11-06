package adaptadores;

import Nucleo.juego.Partida;
import adaptadores.configuracion.ConfiguracionPartida.Modo;

import java.io.IOException;

/** Entrada de consola: utiliza el mismo ensamblador que el arranque gráfico. */
public class Main {

    /** Sin argumentos se juega al ajedrez clásico; con el argumento "futbol", al modo "Posesión y gol". */
    public static void main(String[] args) throws IOException {
        Modo modo = args.length > 0 && args[0].equalsIgnoreCase("futbol") ? Modo.FUTBOL : Modo.CLASICO;
        // La consola juega sin tiempo, así que permite deshacer y rehacer.
        Partida partida = new EnsambladorPartida().crearSinRelojConDeshacer(modo);

        new JuegoConsola(partida, new RenderizadorConsola(), new MensajesDeConsola(), System.in, System.out).jugar();
    }
}
