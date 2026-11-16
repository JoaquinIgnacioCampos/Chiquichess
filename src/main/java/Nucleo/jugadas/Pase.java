package Nucleo.jugadas;

import Nucleo.piezas.Pieza;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.Objects;

/**
 * Pase del modo fútbol: la pelota va de la pieza que la tiene a un compañero. Las piezas no se
 * mueven; al revertir, la pelota vuelve a quien pasó.
 *
 * Verifica que las dos piezas sigan en su lugar, así un pase no se puede aplicar sobre otra
 * posición. Si el pase termina en gol, la jugada le suma después un {@link MoverPelota} al saque.
 */
public record Pase(Pieza pasador, Posicion desde, Pieza receptor, Posicion hacia) implements Efecto {

    public Pase {
        Objects.requireNonNull(pasador, "pasador");
        Objects.requireNonNull(desde, "desde");
        Objects.requireNonNull(receptor, "receptor");
        Objects.requireNonNull(hacia, "hacia");
        if (desde.equals(hacia)) {
            throw new IllegalArgumentException("Un pase tiene que ir a otra casilla");
        }
        if (pasador.color() != receptor.color()) {
            throw new IllegalArgumentException("Solo se le puede pasar la pelota a un compañero");
        }
    }

    @Override
    public void aplicar(Tablero tablero) {
        pasar(tablero, desde, hacia);
    }

    @Override
    public void revertir(Tablero tablero) {
        pasar(tablero, hacia, desde);
    }

    private void pasar(Tablero tablero, Posicion de, Posicion a) {
        if (tablero.piezaEn(desde).orElse(null) != pasador || tablero.piezaEn(hacia).orElse(null) != receptor) {
            throw new IllegalStateException("Las piezas del pase no están en " + desde + " y " + hacia);
        }
        if (!tablero.pelota().filter(de::equals).isPresent()) {
            throw new IllegalStateException("La pelota no está en " + de);
        }
        tablero.colocarPelota(a);
    }
}
