package adaptadores;

import Nucleo.juego.Partida;
import java.io.IOException;

/** Entrada de consola: utiliza el mismo ensamblador que el arranque gráfico. */
public class Main {

    public static void main(String[] args) throws IOException {
        Partida partida = new EnsambladorPartida().crearSinReloj();

        new JuegoConsola(partida, new RenderizadorConsola(), System.in, System.out).jugar();
    }
}
