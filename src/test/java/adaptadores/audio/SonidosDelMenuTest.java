package adaptadores.audio;

import dobles.ReproductorFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** La risa al abrir el juego y el ambiente del menú, con un reproductor que no hace ruido. */
class SonidosDelMenuTest {

    @TempDir
    Path carpeta;
    private final ReproductorFalso reproductor = new ReproductorFalso();
    private final ControlDeVolumen control = new ControlDeVolumen();
    private SonidosDelMenu sonidos;

    @BeforeEach
    void preparar() throws IOException {
        for (String nombre : List.of("risa", "menu")) {
            Path archivo = carpeta.resolve("sonidos/comunes/" + nombre + ".wav");
            Files.createDirectories(archivo.getParent());
            Files.write(archivo, new byte[]{0});
        }
        sonidos = new SonidosDelMenu(new CatalogoDeSonidos(Optional.of(carpeta.resolve("sonidos")), false),
                reproductor, control);
    }

    @Test
    void alAbrirElJuegoSeRieUnaVezYEmpiezaElAmbiente() {
        // Act
        sonidos.abrirJuego();

        // Assert
        assertEquals(List.of("comunes/risa.wav"), reproductor.tocados);
        assertEquals(List.of("comunes/menu.wav"), reproductor.sonando().stream().map(p -> p.archivo).toList());
    }

    @Test
    void laRisaNoSeRepiteAlVolverAlMenu() {
        // Act: se abre, se juega una partida y se vuelve al menú dos veces
        sonidos.abrirJuego();
        sonidos.salirDelMenu();
        sonidos.entrarAlMenu();
        sonidos.salirDelMenu();
        sonidos.abrirJuego();

        // Assert
        assertEquals(1, reproductor.tocados.size());
        assertEquals(1, reproductor.sonando().size());
    }

    @Test
    void alEmpezarUnaPartidaElAmbienteSeCallaYAlVolverSigue() {
        // Arrange
        sonidos.abrirJuego();

        // Act + Assert
        sonidos.salirDelMenu();
        assertTrue(reproductor.sonando().isEmpty());
        sonidos.entrarAlMenu();
        assertEquals(1, reproductor.sonando().size());
    }

    @Test
    void elAmbienteSuenaBajoYSigueElSilencio() {
        // Arrange
        sonidos.abrirJuego();
        ReproductorFalso.PistaFalsa ambiente = reproductor.sonando().getFirst();

        // Act + Assert
        assertEquals(ControlDeVolumen.VOLUMEN_POR_DEFECTO * SonidosDelMenu.VOLUMEN_AMBIENTE, ambiente.volumen, 1e-9);
        control.silenciar(true);
        assertEquals(0.0, ambiente.volumen);
        control.silenciar(false);
        assertTrue(ambiente.volumen > 0);
    }

    @Test
    void sinArchivosNoSuenaNadaYNoFalla() {
        // Arrange
        SonidosDelMenu sinArchivos = new SonidosDelMenu(new CatalogoDeSonidos(Optional.empty(), false),
                reproductor, control);

        // Act
        sinArchivos.abrirJuego();

        // Assert
        assertTrue(reproductor.tocados.isEmpty());
        assertTrue(reproductor.pistas.isEmpty());
    }
}
