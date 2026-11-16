package dobles;

import adaptadores.audio.ReproductorDeAudio;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** Reproductor que no hace ruido: anota qué se pidió tocar y cómo están las pistas en bucle. */
public class ReproductorFalso implements ReproductorDeAudio {

    /** Una pista en bucle con su último volumen. */
    public static final class PistaFalsa implements Pista {
        public final String archivo;
        public double volumen;
        public boolean detenida;

        PistaFalsa(String archivo, double volumen) {
            this.archivo = archivo;
            this.volumen = volumen;
        }

        @Override
        public void volumen(double volumen) {
            this.volumen = volumen;
        }

        @Override
        public void detener() {
            detenida = true;
        }
    }

    /** Archivos tocados una vez, en orden, como "carpeta/archivo.wav" relativos a la raíz de los sonidos. */
    public final List<String> tocados = new ArrayList<>();
    public final List<Double> volumenes = new ArrayList<>();
    public final List<PistaFalsa> pistas = new ArrayList<>();
    public boolean cerrado;

    private static String nombre(URL archivo) {
        String ruta = archivo.getPath();
        int sonidos = ruta.lastIndexOf("/sonidos/");
        return sonidos >= 0 ? ruta.substring(sonidos + "/sonidos/".length()) : ruta;
    }

    @Override
    public void reproducir(URL archivo, double volumen) {
        tocados.add(nombre(archivo));
        volumenes.add(volumen);
    }

    @Override
    public Pista enBucle(URL archivo, double volumen) {
        PistaFalsa pista = new PistaFalsa(nombre(archivo), volumen);
        pistas.add(pista);
        return pista;
    }

    /** Las pistas en bucle que siguen sonando. */
    public List<PistaFalsa> sonando() {
        return pistas.stream().filter(p -> !p.detenida).toList();
    }

    @Override
    public void close() {
        cerrado = true;
    }
}
