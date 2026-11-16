package adaptadores.audio;

import adaptadores.configuracion.ConfiguracionPartida.Equipo;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Dónde está el archivo de cada sonido. Busca en este orden:
 * <ol>
 *   <li>el del club en la carpeta externa ({@code sonidos/clubes/boca/gol.wav}),</li>
 *   <li>el del club dentro del programa ({@code src/main/resources/sonidos/clubes/boca/gol.wav}),</li>
 *   <li>el común en la carpeta externa ({@code sonidos/comunes/gol.wav}),</li>
 *   <li>el común dentro del programa,</li>
 * </ol>
 * y si no hay ninguno, prueba con el {@link Sonido#alternativo() alternativo}. Si tampoco, ese sonido no
 * suena: la partida se juega igual.
 *
 * La carpeta externa permite cambiar sonidos sin tocar el código ni recompilar.
 */
public final class CatalogoDeSonidos {
    /** Formatos que entiende Java Sound sin bibliotecas extra. */
    private static final List<String> EXTENSIONES = List.of("wav", "aiff", "aif", "au");
    private static final String RECURSOS = "/sonidos/";

    private final Optional<Path> carpetaExterna;
    private final boolean usarRecursos;

    /** Catálogo real: la carpeta {@code sonidos} junto a donde se ejecuta el juego y los recursos del programa. */
    public CatalogoDeSonidos() {
        this(Optional.of(Path.of("sonidos")), true);
    }

    public CatalogoDeSonidos(Optional<Path> carpetaExterna, boolean usarRecursos) {
        this.carpetaExterna = Objects.requireNonNull(carpetaExterna, "carpetaExterna");
        this.usarRecursos = usarRecursos;
    }

    /** El archivo para ese sonido y ese club (vacío para los sonidos que son iguales para todos). */
    public Optional<URL> buscar(Sonido sonido, Optional<Equipo> equipo) {
        Objects.requireNonNull(sonido, "sonido");
        Objects.requireNonNull(equipo, "equipo");
        Optional<URL> encontrado = equipo.filter(e -> sonido.propioDeCadaEquipo())
                .flatMap(e -> buscarEn("clubes/" + e.name().toLowerCase(Locale.ROOT) + "/", sonido))
                .or(() -> buscarEn("comunes/", sonido));
        return encontrado.or(() -> sonido.alternativo().flatMap(otro -> buscar(otro, equipo)));
    }

    private Optional<URL> buscarEn(String subcarpeta, Sonido sonido) {
        for (String extension : EXTENSIONES) {
            String relativo = subcarpeta + sonido.archivo() + "." + extension;
            Optional<URL> externo = carpetaExterna.map(carpeta -> carpeta.resolve(relativo))
                    .filter(Files::isRegularFile)
                    .flatMap(CatalogoDeSonidos::comoUrl);
            if (externo.isPresent()) return externo;
        }
        if (usarRecursos) {
            for (String extension : EXTENSIONES) {
                URL recurso = CatalogoDeSonidos.class.getResource(RECURSOS + subcarpeta + sonido.archivo() + "." + extension);
                if (recurso != null) return Optional.of(recurso);
            }
        }
        return Optional.empty();
    }

    private static Optional<URL> comoUrl(Path archivo) {
        try {
            return Optional.of(archivo.toUri().toURL());
        } catch (MalformedURLException imposible) {
            return Optional.empty();
        }
    }
}
