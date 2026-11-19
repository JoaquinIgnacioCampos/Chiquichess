package adaptadores.audio;

import Nucleo.bando.Bando;
import Nucleo.juego.PartidaEnJuego;
import adaptadores.EnsambladorPartida;
import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import adaptadores.configuracion.ConfiguracionPartida.Modo;
import dobles.ReproductorFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

/** El adaptador de sonido enganchado a partidas reales, con un reproductor que no hace ruido. */
class SonidosDePartidaTest {

    @TempDir
    Path carpeta;
    private final ReproductorFalso reproductor = new ReproductorFalso();
    private final ControlDeVolumen control = new ControlDeVolumen();
    private static final Map<Bando, Equipo> EQUIPOS = Map.of(Bando.BLANCO, Equipo.BOCA, Bando.NEGRO, Equipo.RIVER);

    @BeforeEach
    void sonidosComunes() throws IOException {
        for (Sonido sonido : Sonido.values()) {
            crear("comunes/" + sonido.archivo() + ".wav");
        }
    }

    private void crear(String relativo) throws IOException {
        Path archivo = carpeta.resolve("sonidos").resolve(relativo);
        Files.createDirectories(archivo.getParent());
        Files.write(archivo, new byte[]{0});
    }

    private SonidosDePartida conectar(PartidaEnJuego partida, boolean futbol) {
        return SonidosDePartida.conectar(partida, new CatalogoDeSonidos(Optional.of(carpeta.resolve("sonidos")), false), reproductor,
                EQUIPOS, control, futbol);
    }

    private static PartidaEnJuego clasica() {
        return new EnsambladorPartida().crearSinRelojConDeshacer(Modo.CLASICO);
    }

    private static PartidaEnJuego futbol() {
        return new EnsambladorPartida().crearSinRelojConDeshacer(Modo.FUTBOL, 10);
    }

    private static void jugar(PartidaEnJuego partida, String... jugadas) {
        for (String jugada : jugadas) {
            partida.mover(mov(jugada.substring(0, 2), jugada.substring(2, 4)));
        }
    }

    @Test
    void cadaJugadaTocaSuSonido() {
        // Arrange
        PartidaEnJuego partida = clasica();
        conectar(partida, false);

        // Act: mate del loco
        jugar(partida, "f2f3", "e7e5", "g2g4", "d8h4");

        // Assert: tres movimientos, el que da mate y la victoria de las negras (sin archivo propio de River)
        assertEquals(List.of("comunes/movimiento.wav", "comunes/movimiento.wav", "comunes/movimiento.wav",
                "comunes/movimiento.wav", "comunes/victoria.wav"), reproductor.tocados);
    }

    @Test
    void laVictoriaSuenaConElArchivoDelClubGanador() throws IOException {
        // Arrange
        crear("clubes/river/victoria.wav");
        crear("clubes/boca/victoria.wav");
        PartidaEnJuego partida = clasica();
        conectar(partida, false);

        // Act: ganan las negras, que son River
        jugar(partida, "f2f3", "e7e5", "g2g4", "d8h4");

        // Assert
        assertEquals("clubes/river/victoria.wav", reproductor.tocados.getLast());
    }

    @Test
    void elClasicoNoTieneHinchadaNiSilbatos() {
        // Act
        conectar(clasica(), false);

        // Assert
        assertTrue(reproductor.pistas.isEmpty());
        assertTrue(reproductor.tocados.isEmpty());
    }

    @Test
    void elFutbolArrancaConSilbatoYUnaHinchadaSiLosDosClubesComparten() {
        // Act
        conectar(futbol(), true);

        // Assert: una sola pista, porque ninguno tiene hinchada propia
        assertEquals(List.of("comunes/silbato-inicial.wav"), reproductor.tocados);
        assertEquals(1, reproductor.sonando().size());
    }

    @Test
    void cadaClubConHinchadaPropiaTieneSuPista() throws IOException {
        // Arrange
        crear("clubes/boca/hinchada.wav");
        crear("clubes/river/hinchada.wav");

        // Act
        conectar(futbol(), true);

        // Assert
        assertEquals(List.of("clubes/boca/hinchada.wav", "clubes/river/hinchada.wav"),
                reproductor.sonando().stream().map(p -> p.archivo).toList());
    }

    @Test
    void laHinchadaSubeConLasJugadasYBajaAlDeshacer() {
        // Arrange
        PartidaEnJuego partida = futbol();
        conectar(partida, true);
        ReproductorFalso.PistaFalsa hinchada = reproductor.sonando().getFirst();
        double alEmpezar = hinchada.volumen;

        // Act + Assert
        jugar(partida, "e2e4", "e7e5", "g1f3");
        double despues = hinchada.volumen;
        assertTrue(despues > alEmpezar);
        partida.deshacer();
        assertTrue(hinchada.volumen < despues);
    }

    @Test
    void alTerminarSeCallaLaHinchadaYAlCerrarSeDesengancha() {
        // Arrange
        PartidaEnJuego partida = futbol();
        SonidosDePartida sonidos = conectar(partida, true);

        // Act
        partida.rendirse(Bando.BLANCO);

        // Assert: silbato final y victoria de River; la hinchada se detuvo
        assertEquals(List.of("comunes/silbato-inicial.wav", "comunes/silbato-final.wav", "comunes/victoria.wav"),
                reproductor.tocados);
        assertTrue(reproductor.sonando().isEmpty());
        sonidos.close();
        int antes = reproductor.tocados.size();
        assertThrows(RuntimeException.class, () -> partida.mover(mov("e2", "e4")));
        assertEquals(antes, reproductor.tocados.size());
    }

    @Test
    void elSilencioLlevaTodoACeroYSeRecuperaConElVolumenElegido() {
        // Arrange
        PartidaEnJuego partida = futbol();
        conectar(partida, true);
        ReproductorFalso.PistaFalsa hinchada = reproductor.sonando().getFirst();
        control.cambiarVolumen(0.5);
        double conVolumen = hinchada.volumen;

        // Act
        control.silenciar(true);
        jugar(partida, "e2e4");

        // Assert
        assertEquals(0.0, hinchada.volumen);
        assertEquals(0.0, reproductor.volumenes.getLast());
        control.silenciar(false);
        assertTrue(hinchada.volumen > 0 && conVolumen > 0);
    }

    @Test
    void cerrarDejaDeEscucharElVolumen() {
        // Arrange
        SonidosDePartida sonidos = conectar(futbol(), true);
        ReproductorFalso.PistaFalsa hinchada = reproductor.pistas.getFirst();

        // Act
        sonidos.close();
        double alCerrar = hinchada.volumen;
        control.cambiarVolumen(0.1);

        // Assert
        assertTrue(hinchada.detenida);
        assertEquals(alCerrar, hinchada.volumen);
    }

    @Test
    void elegirUnaPiezaSuenaUnClicMasSuaveQueUnMovimiento() {
        // Arrange
        SonidosDePartida sonidos = conectar(clasica(), false);

        // Act
        sonidos.piezaSeleccionada();

        // Assert
        assertEquals(List.of("comunes/seleccion.wav"), reproductor.tocados);
        assertEquals(control.efectivo() * SonidosDePartida.VOLUMEN_SELECCION, reproductor.volumenes.getFirst(), 1e-9);
    }
}
