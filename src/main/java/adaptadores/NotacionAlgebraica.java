package adaptadores;


import Nucleo.juego.Posicion;

/** Traduce entre el texto "e2" y una Posicion del núcleo (a1 = columna 0, fila 0). */
public final class NotacionAlgebraica {

    private NotacionAlgebraica() {
    }

    public static Posicion parsear(String texto) {
        if (texto == null || texto.length() < 2) {
            throw new IllegalArgumentException("Casilla invalida: " + texto);
        }
        char letra = Character.toLowerCase(texto.charAt(0));
        if (letra < 'a' || letra > 'z') {
            throw new IllegalArgumentException("Casilla invalida: " + texto);
        }
        try {
            int fila = Integer.parseInt(texto.substring(1)) - 1;
            return new Posicion(letra - 'a', fila);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Casilla invalida: " + texto);
        }
    }

    public static String formatear(Posicion posicion) {
        return "" + (char) ('a' + posicion.columna()) + (posicion.fila() + 1);
    }
}
