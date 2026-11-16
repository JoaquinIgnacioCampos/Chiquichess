package adaptadores.audio;

import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CatalogoDeSonidosTest {

    @TempDir
    Path carpeta;

    private CatalogoDeSonidos catalogo() {
        return new CatalogoDeSonidos(Optional.of(carpeta), false);
    }

    private void crear(String relativo) throws IOException {
        Path archivo = carpeta.resolve(relativo);
        Files.createDirectories(archivo.getParent());
        Files.write(archivo, new byte[]{0});
    }

    private static String ruta(Optional<URL> url) {
        return url.map(u -> u.getPath().substring(u.getPath().indexOf("/comunes/") >= 0
                ? u.getPath().indexOf("/comunes/") + 1 : u.getPath().indexOf("/clubes/") + 1)).orElse("ninguno");
    }

    @Test
    void elGolDelClubTienePrioridadSobreElComun() throws IOException {
        // Arrange
        crear("comunes/gol.wav");
        crear("clubes/boca/gol.wav");

        // Act + Assert
        assertEquals("clubes/boca/gol.wav", ruta(catalogo().buscar(Sonido.GOL, Optional.of(Equipo.BOCA))));
        assertEquals("comunes/gol.wav", ruta(catalogo().buscar(Sonido.GOL, Optional.of(Equipo.RIVER))));
    }

    @Test
    void losSonidosComunesNoMiranLaCarpetaDelClub() throws IOException {
        // Arrange: aunque el club tenga un movimiento propio, el movimiento es igual para todos
        crear("comunes/movimiento.wav");
        crear("clubes/boca/movimiento.wav");

        // Act + Assert
        assertEquals("comunes/movimiento.wav", ruta(catalogo().buscar(Sonido.MOVIMIENTO, Optional.of(Equipo.BOCA))));
    }

    @Test
    void siFaltaUnSonidoSeUsaSuAlternativo() throws IOException {
        // Arrange
        crear("comunes/movimiento.wav");

        // Act + Assert
        assertEquals("comunes/movimiento.wav", ruta(catalogo().buscar(Sonido.ENROQUE, Optional.empty())));
    }

    @Test
    void siNoHayNingunArchivoNoSuena() {
        assertTrue(catalogo().buscar(Sonido.GOL, Optional.of(Equipo.BOCA)).isEmpty());
        assertTrue(catalogo().buscar(Sonido.CAPTURA, Optional.empty()).isEmpty());
    }

    @Test
    void aceptaAiffYPrefiereWav() throws IOException {
        // Arrange
        crear("comunes/jaque.aiff");
        crear("comunes/tablas.aiff");
        crear("comunes/tablas.wav");

        // Act + Assert
        assertEquals("comunes/jaque.aiff", ruta(catalogo().buscar(Sonido.JAQUE, Optional.empty())));
        assertEquals("comunes/tablas.wav", ruta(catalogo().buscar(Sonido.TABLAS, Optional.empty())));
    }
}
