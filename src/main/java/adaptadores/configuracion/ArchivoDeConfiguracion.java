package adaptadores.configuracion;

import Nucleo.ia.Dificultad;
import adaptadores.configuracion.ConfiguracionPartida.Colores;
import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import adaptadores.configuracion.ConfiguracionPartida.Modo;
import adaptadores.configuracion.ConfiguracionPartida.Rival;
import adaptadores.configuracion.ConfiguracionPartida.Tiempo;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Archivo de texto con las {@link Preferencias}, una por renglón como {@code clave=valor}. Se lee al abrir
 * el juego; si no existe, se crea con los valores por defecto. Un valor que falta o no se entiende toma su
 * valor por defecto, así un archivo editado a mano nunca impide abrir el juego.
 *
 * Si no se puede leer ni escribir, se avisa por la consola de errores y el juego sigue con lo que tenga.
 */
public final class ArchivoDeConfiguracion {
    private final Path archivo;

    public ArchivoDeConfiguracion(Path archivo) {
        this.archivo = Objects.requireNonNull(archivo, "archivo");
    }

    /** Las preferencias guardadas; si el archivo no existe, lo crea con las de por defecto. */
    public Preferencias leer() {
        if (!Files.exists(archivo)) {
            Preferencias porDefecto = Preferencias.porDefecto();
            guardar(porDefecto);
            return porDefecto;
        }
        Properties valores = new Properties();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            valores.load(lector);
        } catch (IOException | IllegalArgumentException ilegible) {
            System.err.println("ChiquiChess: no se pudo leer " + archivo + " (" + ilegible.getMessage()
                    + "); se usan las opciones por defecto");
            return Preferencias.porDefecto();
        }
        return desde(valores);
    }

    public void guardar(Preferencias preferencias) {
        Objects.requireNonNull(preferencias, "preferencias");
        SeleccionDelMenu s = preferencias.seleccion();
        List<String> renglones = List.of(
                "# Opciones de ChiquiChess. Se cargan al abrir el juego y se guardan al jugar y al salir.",
                "# Si se borra este archivo, se vuelve a crear con las opciones por defecto.",
                "modo=" + nombre(s.modo()),
                "tiempo=" + nombre(s.tiempo()),
                "rival=" + nombre(s.rival()),
                "dificultad=" + nombre(s.dificultad()),
                "equipoJugador1=" + nombre(s.equipoJugador1()),
                "equipoJugador2=" + nombre(s.equipoJugador2()),
                "colores=" + nombre(s.colores()),
                "permitirDeshacer=" + s.permitirDeshacer(),
                "limiteDeJugadas=" + s.limiteDeJugadas(),
                "volumen=" + String.format(Locale.ROOT, "%.2f", preferencias.volumen()),
                "silenciado=" + preferencias.silenciado());
        try {
            Files.write(archivo, renglones, StandardCharsets.UTF_8);
        } catch (IOException noSePudo) {
            System.err.println("ChiquiChess: no se pudo guardar " + archivo + " (" + noSePudo.getMessage() + ")");
        }
    }

    private static Preferencias desde(Properties valores) {
        SeleccionDelMenu habitual = SeleccionDelMenu.habitual();
        Optional<Equipo> equipo1 = constante(valores, "equipoJugador1", Equipo.class);
        // Los dos jugadores no pueden tener el mismo club: el segundo queda sin elegir.
        Optional<Equipo> equipo2 = constante(valores, "equipoJugador2", Equipo.class).filter(e -> !equipo1.equals(Optional.of(e)));
        SeleccionDelMenu seleccion = new SeleccionDelMenu(
                constante(valores, "modo", Modo.class).or(habitual::modo),
                constante(valores, "tiempo", Tiempo.class).or(habitual::tiempo),
                constante(valores, "rival", Rival.class).or(habitual::rival),
                equipo1,
                equipo2,
                constante(valores, "colores", Colores.class).or(habitual::colores),
                Boolean.parseBoolean(valores.getProperty("permitirDeshacer", "false").trim()),
                entero(valores, "limiteDeJugadas").filter(limite -> limite >= ConfiguracionPartida.LIMITE_MINIMO
                        && limite <= ConfiguracionPartida.LIMITE_MAXIMO).orElse(habitual.limiteDeJugadas()),
                constante(valores, "dificultad", Dificultad.class));
        double volumen = decimal(valores, "volumen").filter(v -> v > 0 && v <= 1)
                .orElse(Preferencias.VOLUMEN_POR_DEFECTO);
        boolean silenciado = Boolean.parseBoolean(valores.getProperty("silenciado", "false").trim());
        return new Preferencias(seleccion, volumen, silenciado);
    }

    private static String nombre(Optional<? extends Enum<?>> valor) {
        return valor.map(Enum::name).orElse("");
    }

    private static <E extends Enum<E>> Optional<E> constante(Properties valores, String clave, Class<E> tipo) {
        String texto = valores.getProperty(clave, "").trim().toUpperCase(Locale.ROOT);
        try {
            return texto.isEmpty() ? Optional.empty() : Optional.of(Enum.valueOf(tipo, texto));
        } catch (IllegalArgumentException desconocido) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> entero(Properties valores, String clave) {
        try {
            return Optional.of(Integer.parseInt(valores.getProperty(clave, "").trim()));
        } catch (NumberFormatException noEsUnNumero) {
            return Optional.empty();
        }
    }

    private static Optional<Double> decimal(Properties valores, String clave) {
        try {
            return Optional.of(Double.parseDouble(valores.getProperty(clave, "").trim()));
        } catch (NumberFormatException noEsUnNumero) {
            return Optional.empty();
        }
    }
}
