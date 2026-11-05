package Nucleo.jugadas;

import Nucleo.bando.Bando;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.piezas.Caballo;
import Nucleo.piezas.Rey;
import Nucleo.piezas.Torre;
import Nucleo.tablero.Tablero;
import dobles.JugadasDePrueba;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

class HuellaDePosicionTest {

    @Test
    void lasMismasPiezasEnLasMismasCasillasDanLaMismaHuellaAunqueSeArmenDistinto() {
        // Arrange: el mismo dibujo, pero un tablero armado pieza por pieza en otro orden
        Tablero dibujado = TableroFake.desde(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R.C.");
        Tablero aMano = TableroFake.vacio();
        aMano.colocar(pos("g1"), new Caballo(Bando.BLANCO));
        aMano.colocar(pos("e8"), new Rey(Bando.NEGRO));
        aMano.colocar(pos("e1"), new Rey(Bando.BLANCO));

        // Act + Assert
        assertEquals(HuellaDePosicion.de(dibujado, Bando.BLANCO), HuellaDePosicion.de(aMano, Bando.BLANCO));
    }

    @Test
    void elTurnoCambiaLaHuella() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();

        // Act + Assert
        assertNotEquals(HuellaDePosicion.de(tablero, Bando.BLANCO), HuellaDePosicion.de(tablero, Bando.NEGRO));
    }

    @Test
    void laCasillaElTipoYElBandoDeLaPiezaCambianLaHuella() {
        // Act
        long torreBlancaEnA1 = HuellaDePosicion.de(TableroFake.conUnaPieza("a1", new Torre(Bando.BLANCO)), Bando.BLANCO);
        long torreBlancaEnA2 = HuellaDePosicion.de(TableroFake.conUnaPieza("a2", new Torre(Bando.BLANCO)), Bando.BLANCO);
        long torreNegraEnA1 = HuellaDePosicion.de(TableroFake.conUnaPieza("a1", new Torre(Bando.NEGRO)), Bando.BLANCO);
        long caballoBlancoEnA1 = HuellaDePosicion.de(TableroFake.conUnaPieza("a1", new Caballo(Bando.BLANCO)), Bando.BLANCO);

        // Assert
        assertEquals(4, List.of(torreBlancaEnA1, torreBlancaEnA2, torreNegraEnA1, caballoBlancoEnA1)
                .stream().distinct().count());
    }

    @Test
    void actualizarConCadaJugadaDaLoMismoQueCalcularDesdeCero() {
        // Arrange: apertura con capturas de los dos lados
        Tablero tablero = new DisposicionEstandar().crearTablero();
        long incremental = HuellaDePosicion.de(tablero, Bando.BLANCO);

        // Act: de a una jugada, para comparar en cada paso
        Bando turno = Bando.BLANCO;
        for (String texto : new String[]{"e2e4", "d7d5", "e4d5", "d8d5", "b1c3", "d5a5"}) {
            Jugada jugada = JugadasDePrueba.jugar(tablero, texto).get(0);
            incremental = HuellaDePosicion.tras(incremental, jugada);
            turno = turno.opuesto();
            // Assert
            assertEquals(HuellaDePosicion.de(tablero, turno), incremental, "Después de " + texto);
        }
    }

    @Test
    void irYVolverDevuelveExactamenteLaMismaHuella() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();
        long inicial = HuellaDePosicion.de(tablero, Bando.BLANCO);

        // Act
        long huella = inicial;
        for (Jugada jugada : JugadasDePrueba.jugar(tablero, "g1f3", "g8f6", "f3g1", "f6g8")) {
            huella = HuellaDePosicion.tras(huella, jugada);
        }

        // Assert
        assertEquals(inicial, huella);
    }

    @Test
    void laActualizacionFuncionaConJugadasDeVariosEfectos() {
        // Arrange: un enroque armado a mano (dos traslados)
        Tablero tablero = TableroFake.desde(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R..T");
        long antes = HuellaDePosicion.de(tablero, Bando.BLANCO);
        Jugada enroque = new Jugada(pos("e1"), pos("g1"), List.of(
                new Traslado(tablero.piezaEn(pos("e1")).orElseThrow(), pos("e1"), pos("g1")),
                new Traslado(tablero.piezaEn(pos("h1")).orElseThrow(), pos("h1"), pos("f1"))));

        // Act
        enroque.aplicar(tablero);

        // Assert
        assertEquals(HuellaDePosicion.de(tablero, Bando.NEGRO), HuellaDePosicion.tras(antes, enroque));
    }
}
