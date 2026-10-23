package adaptadores;

import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Jugador;
import Nucleo.juego.Partida;
import Nucleo.piezas.Color;
import Nucleo.reglas.*;


import java.io.IOException;
import java.util.List;

/** Punto de arranque: acá se "cablean" las dependencias concretas. */
public class Main {

    public static void main(String[] args) throws IOException {
        Partida partida = new Partida(
                new DisposicionEstandar().crearTablero(),
                new Jugador("Jugador 1", Color.BLANCO),
                new Jugador("Jugador 2", Color.NEGRO),
                new DetectorDeJaque(),
                List.of(new JaqueMate(),
                        new Ahogado(),
                        new TablasPorRepeticion(),
                        new TablasPorReglaDeLos50Movimientos()));

        new JuegoConsola(partida, new RenderizadorConsola(), System.in, System.out).jugar();
    }
}
