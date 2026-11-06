package Nucleo.jugadas;

import Nucleo.bando.Bando;
import Nucleo.juego.DisposicionDeFutbol;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.piezas.Torre;
import Nucleo.reglas.Marcador;
import Nucleo.reglas.ReglaDeLaPelota;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/** La pelota en el tablero, el efecto que la mueve, su huella y el marcador. */
class MoverPelotaTest {

    @Test
    void unTableroNuevoNoTienePelota() {
        // Assert
        assertTrue(TableroFake.vacio().pelota().isEmpty());
    }

    @Test
    void laPelotaNoSePuedePonerFueraDelTablero() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> TableroFake.vacio().colocarPelota(pos("i1")));
    }

    @Test
    void copiarElTableroCopiaLaPelotaYLosArcos() {
        // Arrange
        Tablero tablero = TableroFake.vacio();
        tablero.colocarPelota(pos("d5"));
        tablero.marcarArco(pos("d8"), Bando.NEGRO);

        // Act
        Tablero copia = tablero.copiar();
        copia.colocarPelota(pos("e4"));

        // Assert: la copia es independiente
        assertEquals(Optional.of(Bando.NEGRO), copia.arcoEn(pos("d8")));
        assertEquals(Optional.of(pos("d5")), tablero.pelota());
        assertEquals(Optional.of(pos("e4")), copia.pelota());
    }

    @Test
    void aplicarYRevertirMuevenLaPelota() {
        // Arrange
        Tablero tablero = TableroFake.vacio();
        tablero.colocarPelota(pos("d5"));
        MoverPelota efecto = new MoverPelota(pos("d5"), pos("f6"), false);

        // Act + Assert
        efecto.aplicar(tablero);
        assertEquals(Optional.of(pos("f6")), tablero.pelota());
        efecto.revertir(tablero);
        assertEquals(Optional.of(pos("d5")), tablero.pelota());
    }

    @Test
    void fallaSinTocarNadaSiLaPelotaNoEstaDondeSeEspera() {
        // Arrange
        Tablero tablero = TableroFake.vacio();
        tablero.colocarPelota(pos("e4"));

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> new MoverPelota(pos("d5"), pos("f6"), false).aplicar(tablero));
        assertThrows(IllegalStateException.class,
                () -> new MoverPelota(pos("d5"), pos("f6"), false).aplicar(TableroFake.vacio()));
        assertEquals(Optional.of(pos("e4")), tablero.pelota());
    }

    @Test
    void laDisposicionDeFutbolMarcaLaCanchaSinCambiarLasPiezas() {
        // Act
        Tablero tablero = new DisposicionDeFutbol(new DisposicionEstandar(), new ReglaDeLaPelota()).crearTablero();

        // Assert: pelota en d5, arcos en d/e de cada fila de fondo, y las mismas 32 piezas
        assertEquals(Optional.of(pos("d5")), tablero.pelota());
        assertEquals(32, tablero.piezas().size());
        assertEquals(Optional.of(Bando.BLANCO), tablero.arcoEn(pos("d1")));
        assertEquals(Optional.of(Bando.BLANCO), tablero.arcoEn(pos("e1")));
        assertEquals(Optional.of(Bando.NEGRO), tablero.arcoEn(pos("d8")));
        assertEquals(Optional.of(Bando.NEGRO), tablero.arcoEn(pos("e8")));
        assertTrue(tablero.arcoEn(pos("c8")).isEmpty());
        assertTrue(tablero.arcoEn(pos("d2")).isEmpty());
    }

    @Test
    void laCasillaDeLaPelotaCambiaLaHuella() {
        // Arrange
        Tablero sinPelota = new DisposicionEstandar().crearTablero();
        Tablero enD5 = new DisposicionDeFutbol(new DisposicionEstandar(), new ReglaDeLaPelota()).crearTablero();
        Tablero enD4 = new DisposicionDeFutbol(new DisposicionEstandar(), new ReglaDeLaPelota(
                java.util.Set.of(3, 4), pos("d4"), pos("d5"), pos("d4"))).crearTablero();

        // Act
        long huellaSin = HuellaDePosicion.de(sinPelota, Bando.BLANCO);
        long huellaD5 = HuellaDePosicion.de(enD5, Bando.BLANCO);
        long huellaD4 = HuellaDePosicion.de(enD4, Bando.BLANCO);

        // Assert
        assertNotEquals(huellaSin, huellaD5);
        assertNotEquals(huellaD5, huellaD4);
    }

    @Test
    void laHuellaActualizadaConUnGolCoincideConLaCalculadaDesdeCero() {
        // Arrange: torre blanca con la pelota en d2, a punto de hacer gol
        Tablero tablero = TableroFake.desde(
                ".......r",
                "........",
                "........",
                "........",
                "........",
                "........",
                "...T....",
                "....R...");
        new Nucleo.reglas.ReglaDeLaPelota().prepararCancha(tablero);
        tablero.colocarPelota(pos("d2"));
        long antes = HuellaDePosicion.de(tablero, Bando.BLANCO);
        Jugada gol = new ReglaDeLaPelota().transformar(tablero, Jugada.normal(tablero, pos("d2"), pos("d8"))).get(0);

        // Act
        gol.aplicar(tablero);

        // Assert
        assertTrue(gol.esGol());
        assertEquals(HuellaDePosicion.de(tablero, Bando.NEGRO), HuellaDePosicion.tras(antes, gol));
    }

    @Test
    void elMarcadorCuentaLosGolesDeCadaBando() {
        // Arrange
        Torre blanca = new Torre(Bando.BLANCO);
        Torre negra = new Torre(Bando.NEGRO);
        Jugada golBlanco = new Jugada(pos("d2"), pos("d8"), List.of(
                new Traslado(blanca, pos("d2"), pos("d8")), new MoverPelota(pos("d2"), pos("d5"), true)));
        Jugada golNegro = new Jugada(pos("d7"), pos("d1"), List.of(
                new Traslado(negra, pos("d7"), pos("d1")), new MoverPelota(pos("d7"), pos("d4"), true)));
        Jugada sinGol = new Jugada(pos("d5"), pos("d6"), List.of(
                new Traslado(blanca, pos("d5"), pos("d6")), new MoverPelota(pos("d5"), pos("d6"), false)));

        // Act
        Marcador marcador = Marcador.de(List.of(golBlanco, sinGol, golNegro, golBlanco));

        // Assert
        assertEquals(new Marcador(2, 1), marcador);
        assertEquals(2, marcador.goles(Bando.BLANCO));
        assertEquals(Optional.of(Bando.BLANCO), marcador.lider());
        assertEquals(Optional.empty(), Marcador.de(List.of(golBlanco, golNegro)).lider());
    }
}
