package Nucleo.ia;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.Marcador;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Qué tan buena es una posición, en centésimos de peón, desde el punto de vista de un bando. Es lo que
 * Minimax usa cuando deja de mirar hacia adelante. No decide finales (mate, goles para ganar): eso lo
 * dicen las condiciones de fin de las reglas.
 *
 * <ul>
 *   <li><b>Material:</b> peón 100, caballo 320, alfil 330, torre 500, dama 900.</li>
 *   <li><b>Posición:</b> caballos, alfiles y peones valen un poco más cerca del centro; los peones,
 *       también a medida que avanzan.</li>
 *   <li><b>Fútbol</b> (si hay pelota): cada gol de diferencia vale {@link #VALOR_GOL}, más que una
 *       dama; tener la pelota suma, y más cuanto más cerca está del arco rival.</li>
 * </ul>
 */
public final class EvaluadorDePosicion {
    static final int VALOR_GOL = 1200;
    static final int POSESION = 40;
    static final int POR_CERCANIA_AL_ARCO = 12;

    /** Casillas de arco que defiende cada bando: ahí hace gol el rival. */
    private final Map<Bando, List<Posicion>> arcos = new EnumMap<>(Bando.class);

    /** Los arcos se leen una sola vez del tablero de la partida: no cambian durante el juego. */
    public EvaluadorDePosicion(ITablero tablero) {
        for (Bando bando : Bando.values()) arcos.put(bando, new ArrayList<>());
        for (int columna = 0; columna < tablero.ancho(); columna++) {
            for (int fila = 0; fila < tablero.alto(); fila++) {
                Posicion casilla = new Posicion(columna, fila);
                tablero.arcoEn(casilla).ifPresent(defensor -> arcos.get(defensor).add(casilla));
            }
        }
    }

    /** Puntaje de la posición para {@code bando}: positivo si le conviene, negativo si no. */
    public int evaluar(ITablero tablero, Bando bando, List<Jugada> jugadas) {
        int blancas = 0;
        for (Map.Entry<Posicion, Pieza> entrada : tablero.piezas().entrySet()) {
            Pieza pieza = entrada.getValue();
            int valor = valor(pieza) + posicional(pieza, entrada.getKey(), tablero);
            blancas += pieza.color() == Bando.BLANCO ? valor : -valor;
        }
        if (tablero.pelota().isPresent()) blancas += futbol(tablero, jugadas);
        return bando == Bando.BLANCO ? blancas : -blancas;
    }

    /** Valor material; el rey no se cuenta porque nunca sale del tablero. */
    public static int valor(Pieza pieza) {
        return switch (Character.toUpperCase(pieza.codigo())) {
            case 'P' -> 100;
            case 'C' -> 320;
            case 'A' -> 330;
            case 'T' -> 500;
            case 'D' -> 900;
            default -> 0;
        };
    }

    private static int posicional(Pieza pieza, Posicion casilla, ITablero tablero) {
        char codigo = Character.toUpperCase(pieza.codigo());
        if (codigo != 'P' && codigo != 'C' && codigo != 'A') return 0;
        int haciaElCentroColumnas = Math.min(casilla.columna(), tablero.ancho() - 1 - casilla.columna());
        int haciaElCentroFilas = Math.min(casilla.fila(), tablero.alto() - 1 - casilla.fila());
        int centro = 5 * (Math.min(haciaElCentroColumnas, 3) + Math.min(haciaElCentroFilas, 3));
        if (codigo != 'P') return centro;
        int avance = pieza.color() == Bando.BLANCO ? casilla.fila() - 1 : tablero.alto() - 2 - casilla.fila();
        return centro / 2 + 8 * Math.max(0, avance);
    }

    /** Goles y pelota, desde el punto de vista de las blancas. */
    private int futbol(ITablero tablero, List<Jugada> jugadas) {
        Marcador marcador = Marcador.de(jugadas);
        int puntaje = VALOR_GOL * (marcador.blancas() - marcador.negras());
        Posicion pelota = tablero.pelota().orElseThrow();
        Optional<Pieza> conPelota = tablero.piezaEn(pelota);
        if (conPelota.isPresent()) {
            Bando duenio = conPelota.get().color();
            // Ataca el arco que defiende el rival.
            int distancia = distanciaAlArco(pelota, arcos.get(duenio.opuesto()));
            int cercania = Math.max(0, tablero.alto() - distancia);
            int ventaja = POSESION + POR_CERCANIA_AL_ARCO * cercania;
            puntaje += duenio == Bando.BLANCO ? ventaja : -ventaja;
        }
        return puntaje;
    }

    private static int distanciaAlArco(Posicion pelota, List<Posicion> arco) {
        int distancia = Integer.MAX_VALUE;
        for (Posicion casilla : arco) {
            distancia = Math.min(distancia, Math.max(Math.abs(casilla.columna() - pelota.columna()),
                    Math.abs(casilla.fila() - pelota.fila())));
        }
        return arco.isEmpty() ? 0 : distancia;
    }
}
