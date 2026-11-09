package adaptadores.audio;

import Nucleo.bando.Bando;
import Nucleo.reglas.Marcador;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IntensidadDeHinchadaTest {

    private static double intensidad(Bando hinchada, int jugadas, int blancas, int negras) {
        return IntensidadDeHinchada.de(hinchada, jugadas, Optional.of(90), new Marcador(blancas, negras));
    }

    @Test
    void creceAMedidaQueAvanzaElPartido() {
        double alPrincipio = intensidad(Bando.BLANCO, 0, 0, 0);
        double aLaMitad = intensidad(Bando.BLANCO, 45, 0, 0);
        double alFinal = intensidad(Bando.BLANCO, 89, 0, 0);

        assertTrue(alPrincipio < aLaMitad && aLaMitad < alFinal);
    }

    @Test
    void unFinalParejoSuenaMasQueUnaGoleada() {
        // Con 3 goles termina el partido, así que 2 a 0 ya es una goleada
        assertTrue(intensidad(Bando.BLANCO, 80, 1, 0) > intensidad(Bando.BLANCO, 80, 2, 0));
    }

    @Test
    void laHinchadaDelQueGanaCantaMasQueLaDelQuePierde() {
        assertTrue(intensidad(Bando.NEGRO, 40, 0, 1) > intensidad(Bando.BLANCO, 40, 0, 1));
        assertEquals(intensidad(Bando.BLANCO, 40, 1, 1), intensidad(Bando.NEGRO, 40, 1, 1));
    }

    @Test
    void quedaEntreCeroYUno() {
        for (int jugadas = -5; jugadas <= 200; jugadas += 5) {
            for (Bando bando : Bando.values()) {
                double valor = intensidad(bando, jugadas, 1, 0);
                assertTrue(valor >= 0 && valor <= 1, "Fuera de rango: " + valor);
            }
        }
    }

    @Test
    void sinLimiteDeJugadasNoCrece() {
        Marcador empate = new Marcador(0, 0);
        assertEquals(IntensidadDeHinchada.de(Bando.BLANCO, 0, Optional.empty(), empate),
                IntensidadDeHinchada.de(Bando.BLANCO, 300, Optional.empty(), empate));
    }

    @Test
    void deshacerLaBajaPorqueDependeSoloDelEstado() {
        // Volver a la misma situación da el mismo valor: no hay nada acumulado
        assertEquals(intensidad(Bando.BLANCO, 30, 0, 0), intensidad(Bando.BLANCO, 30, 0, 0));
        assertTrue(intensidad(Bando.BLANCO, 29, 0, 0) < intensidad(Bando.BLANCO, 30, 0, 0));
    }
}
