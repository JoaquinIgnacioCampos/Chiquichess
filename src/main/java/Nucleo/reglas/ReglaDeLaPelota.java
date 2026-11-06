package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Efecto;
import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.MoverPelota;
import Nucleo.jugadas.Pase;
import Nucleo.jugadas.Reemplazo;
import Nucleo.jugadas.Retiro;
import Nucleo.jugadas.Traslado;
import Nucleo.piezas.Peon;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Modo fútbol "Posesión y gol": la pelota se lleva y se mete en el arco rival.
 *
 * La pelota y los arcos son marcas del tablero, no piezas: la pelota no bloquea a nadie y la tiene
 * la pieza que está parada sobre ella. {@link #prepararCancha} pone esas marcas al empezar; después
 * la regla las lee del tablero, así el dibujo y la regla usan los mismos arcos.
 *
 * A cada jugada que toca la pelota le suma un {@link MoverPelota}, igual que la promoción le suma un
 * reemplazo. Además agrega los pases.
 * <ul>
 *   <li>Terminar el movimiento en la casilla de la pelota la toma; pasar por encima no.</li>
 *   <li>Quien lleva la pelota la arrastra en cada movimiento.</li>
 *   <li>Capturar a quien la lleva la gana (también en la captura al paso, donde el que captura
 *       termina en otra casilla).</li>
 *   <li>Llevarla a una casilla del arco rival es gol; el rey la puede llevar pero no hace gol. Un
 *       peón que promueve en el arco hace gol con la pieza nueva.</li>
 *   <li>Después del gol la pelota va al saque del bando que lo recibió, sin mover las piezas. Si
 *       hay una pieza parada en el saque, esa pieza se queda con la pelota.</li>
 *   <li>Pase: quien tiene la pelota se la puede dar a un compañero parado en una casilla que podría
 *       capturar si fuera rival. Las piezas que se deslizan necesitan el camino libre; el caballo
 *       la pasa por arriba; el rey, a una casilla de al lado; el peón, solo en diagonal hacia
 *       adelante. El pase gasta el turno. Si el que recibe está en el arco rival y no es el rey,
 *       es gol.</li>
 * </ul>
 *
 * Tiene que ir después de la promoción en la lista de reglas especiales, para ver la pieza nueva.
 * Si el tablero no tiene pelota, no cambia ninguna jugada.
 */
public class ReglaDeLaPelota implements ReglaEspecial {

    private final Set<Integer> columnasDelArco;
    private final Posicion saqueInicial;
    private final Posicion saqueTrasGolDeBlancas;
    private final Posicion saqueTrasGolDeNegras;

    /**
     * Tablero de 8x8: el arco son las columnas d y e de la fila de fondo de cada bando; la pelota
     * empieza en d5, vuelve a d5 (campo negro) tras un gol blanco y a d4 (campo blanco) tras un gol negro.
     */
    public ReglaDeLaPelota() {
        this(Set.of(3, 4), new Posicion(3, 4), new Posicion(3, 4), new Posicion(3, 3));
    }

    /**
     * @param columnasDelArco        columnas que forman el arco, en la fila de fondo de cada bando
     * @param saqueInicial           dónde empieza la pelota
     * @param saqueTrasGolDeBlancas  dónde vuelve la pelota cuando hacen gol las blancas
     * @param saqueTrasGolDeNegras   dónde vuelve la pelota cuando hacen gol las negras
     */
    public ReglaDeLaPelota(Set<Integer> columnasDelArco, Posicion saqueInicial,
                           Posicion saqueTrasGolDeBlancas, Posicion saqueTrasGolDeNegras) {
        this.columnasDelArco = Set.copyOf(Objects.requireNonNull(columnasDelArco, "columnasDelArco"));
        if (this.columnasDelArco.isEmpty()) {
            throw new IllegalArgumentException("El arco tiene que tener al menos una columna");
        }
        this.saqueInicial = Objects.requireNonNull(saqueInicial, "saqueInicial");
        this.saqueTrasGolDeBlancas = Objects.requireNonNull(saqueTrasGolDeBlancas, "saqueTrasGolDeBlancas");
        this.saqueTrasGolDeNegras = Objects.requireNonNull(saqueTrasGolDeNegras, "saqueTrasGolDeNegras");
    }

    /**
     * Marca la cancha en el tablero: el arco de cada bando en su fila de fondo y la pelota en el saque
     * inicial. Las piezas no se tocan.
     */
    public void prepararCancha(Tablero tablero) {
        for (int columna : columnasDelArco) {
            tablero.marcarArco(new Posicion(columna, 0), Bando.BLANCO);
            tablero.marcarArco(new Posicion(columna, tablero.alto() - 1), Bando.NEGRO);
        }
        tablero.colocarPelota(saqueInicial);
    }

    /** Los pases de la pieza que está en origen, si es la que tiene la pelota. */
    @Override
    public List<Jugada> jugadas(Tablero tablero, Posicion origen, List<Jugada> historial, IDetectorDeJaque detector) {
        Optional<Pieza> pasador = tablero.piezaEn(origen);
        if (pasador.isEmpty() || !tablero.pelota().filter(origen::equals).isPresent()) {
            return List.of();
        }
        List<Jugada> pases = new ArrayList<>();
        for (Map.Entry<Posicion, Pieza> entrada : tablero.piezas().entrySet()) {
            Posicion hacia = entrada.getKey();
            Pieza receptor = entrada.getValue();
            if (!hacia.equals(origen) && receptor.color() == pasador.get().color()
                    && alcanza(tablero, pasador.get(), origen, hacia)) {
                pases.add(pase(tablero, pasador.get(), origen, receptor, hacia));
            }
        }
        return pases;
    }

    /**
     * true si la pieza podría capturar en esa casilla si ahí hubiera un rival. Así cada pieza pasa
     * con su propio movimiento de captura: camino libre para las que se deslizan, salto para el
     * caballo, diagonal hacia adelante para el peón.
     */
    private static boolean alcanza(Tablero tablero, Pieza pasador, Posicion origen, Posicion hacia) {
        Tablero prueba = tablero.copiar();
        prueba.colocar(hacia, new Peon(pasador.color().opuesto()));
        return pasador.destinosPosibles(prueba, origen).contains(hacia);
    }

    /** Jugada de pase; si el que recibe está en el arco rival y no es el rey, la pelota va al saque. */
    private Jugada pase(Tablero tablero, Pieza pasador, Posicion origen, Pieza receptor, Posicion hacia) {
        Jugada pase = new Jugada(origen, hacia, List.of(new Pase(pasador, origen, receptor, hacia)));
        boolean gol = !receptor.esRey()
                && tablero.arcoEn(hacia).filter(defensor -> defensor != receptor.color()).isPresent();
        return gol ? pase.con(new MoverPelota(hacia, saqueTrasGolDe(receptor.color()), true)) : pase;
    }

    private Posicion saqueTrasGolDe(Bando goleador) {
        return goleador == Bando.BLANCO ? saqueTrasGolDeBlancas : saqueTrasGolDeNegras;
    }

    @Override
    public List<Jugada> transformar(Tablero tablero, Jugada jugada) {
        Optional<Posicion> pelota = tablero.pelota();
        if (pelota.isEmpty()) {
            return List.of(jugada);
        }
        Posicion desde = pelota.get();
        Posicion adonde = adondeVa(jugada, desde);
        Optional<Pieza> portador = portadorEn(jugada, adonde);
        boolean gol = portador
                .filter(pieza -> !pieza.esRey())
                .filter(pieza -> tablero.arcoEn(adonde).filter(defensor -> defensor != pieza.color()).isPresent())
                .isPresent();
        if (gol) {
            return List.of(jugada.con(new MoverPelota(desde, saqueTrasGolDe(portador.get().color()), true)));
        }
        if (adonde.equals(desde)) {
            return List.of(jugada);
        }
        return List.of(jugada.con(new MoverPelota(desde, adonde, false)));
    }

    /**
     * Casilla donde queda la pelota después de la jugada, antes de ver si es gol: la sigue a quien la
     * llevaba; si a ese lo capturaron, va a la casilla de quien capturó; si no, se queda donde está.
     */
    private static Posicion adondeVa(Jugada jugada, Posicion pelota) {
        for (Efecto efecto : jugada.efectos()) {
            if (efecto instanceof Traslado traslado && traslado.origen().equals(pelota)) {
                return traslado.destino();
            }
        }
        for (Efecto efecto : jugada.efectos()) {
            if (efecto instanceof Retiro retiro && retiro.posicion().equals(pelota)) {
                return jugada.destino();
            }
        }
        return pelota;
    }

    /**
     * Pieza de esta jugada que termina en la casilla (ya promovida, si promovió); vacío si ninguna
     * pieza de la jugada termina ahí, es decir, si la pelota quedó con una pieza que no se movió.
     */
    private static Optional<Pieza> portadorEn(Jugada jugada, Posicion casilla) {
        Optional<Pieza> portador = Optional.empty();
        for (Efecto efecto : jugada.efectos()) {
            if (efecto instanceof Traslado traslado && traslado.destino().equals(casilla)) {
                portador = Optional.of(traslado.pieza());
            } else if (efecto instanceof Reemplazo reemplazo && reemplazo.posicion().equals(casilla)) {
                portador = Optional.of(reemplazo.nueva());
            }
        }
        return portador;
    }
}
