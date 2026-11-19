package Nucleo.ia;

import Nucleo.bando.Bando;
import Nucleo.juego.SituacionDePartida;
import Nucleo.jugadas.Jugada;
import Nucleo.movimientos.ReglaCompuesta;
import Nucleo.movimientos.ReglasBase;
import Nucleo.piezas.Peon;
import Nucleo.piezas.Pieza;
import Nucleo.piezas.Reina;
import Nucleo.piezas.Rey;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tablero.Tablero;
import dobles.FuenteDeTiempoFalsa;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El material lo declara cada pieza: una pieza que no existe en el ajedrez clásico cuenta para la IA
 * con su propio valor, sin tocar el evaluador.
 */
class EvaluadorDePosicionTest {

    /** Pieza inventada para la prueba: se mueve como el rey y vale 700. */
    private static final class Elefante extends Pieza {
        Elefante(Bando color) {
            super(color, new ReglaCompuesta(ReglasBase.ortogonales(1), ReglasBase.diagonales(1)));
        }

        @Override
        public String nombre() {
            return "Elefante";
        }

        @Override
        public char codigo() {
            return 'E';
        }

        @Override
        public int valor() {
            return 700;
        }
    }

    private static Tablero reyesEn(String blanco, String negro) {
        Tablero tablero = new Tablero(8, 8);
        tablero.colocar(pos(blanco), new Rey(Bando.BLANCO));
        tablero.colocar(pos(negro), new Rey(Bando.NEGRO));
        return tablero;
    }

    @Test
    void elMaterialEsElValorQueDeclaraCadaPieza() {
        // Arrange: solo los reyes (que no cuentan) y una pieza nueva, que vale más que una pieza menor
        Tablero tablero = reyesEn("a1", "h8");
        tablero.colocar(pos("d4"), new Elefante(Bando.BLANCO));

        // Act
        int paraBlancas = new EvaluadorDePosicion(tablero).evaluar(tablero, Bando.BLANCO, List.of());

        // Assert: 700 y nada de bonificación por el centro, que es solo para piezas menores y peones
        assertEquals(700, paraBlancas);
    }

    @Test
    void laIaPrefiereCapturarLaPiezaNuevaSiValeMas() {
        // Arrange: la dama negra puede comer un peón (d2) o el elefante (g5), los dos sin defensa
        Tablero tablero = reyesEn("a1", "h8");
        tablero.colocar(pos("d5"), new Reina(Bando.NEGRO));
        tablero.colocar(pos("d2"), new Peon(Bando.BLANCO));
        tablero.colocar(pos("g5"), new Elefante(Bando.BLANCO));
        SituacionDePartida situacion = new SituacionDePartida(tablero, Bando.NEGRO, List.of(),
                ReglasDePartida.clasicas());
        // Fácil mira una sola jugada: compara directamente lo que gana con cada captura. (Medio encuentra
        // algo mejor: Dama e5 da jaque y ataca al elefante a la vez.) El azar nunca lo distrae.
        Random sinDescuidos = new Random(1) {
            @Override
            public double nextDouble() {
                return 0.99;
            }
        };
        JugadorArtificial ia = new JugadorArtificial(Dificultad.FACIL, sinDescuidos, new FuenteDeTiempoFalsa());

        // Act
        Jugada elegida = ia.elegirJugada(situacion);

        // Assert
        assertEquals(pos("g5"), elegida.destino());
    }
}
