package adaptadores;

import Nucleo.juego.Partida;
import java.io.IOException;

/** Entrada de consola: utiliza el mismo ensamblador que el arranque gráfico. */
public class Main {

    public static void main(String[] args) throws IOException {
        // La consola juega sin tiempo, así que permite deshacer y rehacer.
        Partida partida = new EnsambladorPartida().crearSinRelojConDeshacer();

        new JuegoConsola(partida, new RenderizadorConsola(), new MensajesDeConsola(), System.in, System.out).jugar();
    }
}
