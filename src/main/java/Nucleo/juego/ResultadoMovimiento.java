package Nucleo.juego;

import Nucleo.piezas.Pieza;
import Nucleo.reglas.EstadoPartida;

import java.util.Optional;

public record ResultadoMovimiento(Optional<Pieza> piezaCapturada, EstadoPartida estado) {
}
