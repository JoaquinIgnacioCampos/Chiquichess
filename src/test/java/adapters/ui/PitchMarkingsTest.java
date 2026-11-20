package adapters.ui;

import core.board.Board;
import core.board.Position;
import core.side.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Dónde van las líneas de la cancha del modo fútbol (se prueba solo la lógica, sin dibujar nada). */
class PitchMarkingsTest {

    /** Tablero de 8 x 8 con los arcos en las columnas 3 y 4 de las dos filas de fondo. */
    private PitchMarkings pitch() {
        Board board = new Board(8, 8);
        for (int column : new int[]{3, 4}) {
            board.markNet(new Position(column, 0), Side.WHITE);
            board.markNet(new Position(column, 7), Side.BLACK);
        }
        return PitchMarkings.of(board);
    }

    @Test
    void aBoardWithoutNetsPutsTheGoalsInTheMiddleColumns() {
        PitchMarkings classic = PitchMarkings.of(new Board(8, 8));
        PitchMarkings football = pitch();
        for (int column = 0; column < 8; column++) {
            assertEquals(football.horizontal(1, column), classic.horizontal(1, column), "área chica, columna " + column);
            assertEquals(football.horizontal(2, column), classic.horizontal(2, column), "área grande, columna " + column);
        }
        assertTrue(classic.vertical(2, 0));
        assertTrue(classic.vertical(6, 0));
    }

    @Test
    void sidelinesAndEndLinesSurroundTheWholeBoard() {
        PitchMarkings pitch = pitch();
        for (int row = 0; row < 8; row++) {
            assertTrue(pitch.vertical(0, row), "costado izquierdo, fila " + row);
            assertTrue(pitch.vertical(8, row), "costado derecho, fila " + row);
        }
        for (int column = 0; column < 8; column++) {
            assertTrue(pitch.horizontal(0, column), "línea de fondo de abajo, columna " + column);
            assertTrue(pitch.horizontal(8, column), "línea de fondo de arriba, columna " + column);
        }
    }

    @Test
    void halfwayLineCrossesTheWholeBoard() {
        PitchMarkings pitch = pitch();
        for (int column = 0; column < 8; column++) {
            assertTrue(pitch.horizontal(4, column), "columna " + column);
        }
    }

    @Test
    void smallAreaIsOneColumnWiderThanTheNetOnEachSideAndOneRowDeep() {
        PitchMarkings pitch = pitch();
        for (int column = 0; column < 8; column++) {
            boolean inside = column >= 2 && column <= 5;
            assertEquals(inside, pitch.horizontal(1, column), "frente del área chica de abajo, columna " + column);
            assertEquals(inside, pitch.horizontal(7, column), "frente del área chica de arriba, columna " + column);
        }
        assertTrue(pitch.vertical(2, 0));
        assertTrue(pitch.vertical(6, 0));
        assertTrue(pitch.vertical(2, 7));
        assertTrue(pitch.vertical(6, 7));
        assertFalse(pitch.vertical(2, 3), "los lados del área no siguen más allá de su profundidad");
    }

    @Test
    void largeAreaIsTwoColumnsWiderThanTheNetAndTwoRowsDeep() {
        PitchMarkings pitch = pitch();
        for (int column = 0; column < 8; column++) {
            boolean inside = column >= 1 && column <= 6;
            assertEquals(inside, pitch.horizontal(2, column), "frente del área grande de abajo, columna " + column);
            assertEquals(inside, pitch.horizontal(6, column), "frente del área grande de arriba, columna " + column);
        }
        for (int row : new int[]{0, 1, 6, 7}) {
            assertTrue(pitch.vertical(1, row), "lado izquierdo del área grande, fila " + row);
            assertTrue(pitch.vertical(7, row), "lado derecho del área grande, fila " + row);
        }
        assertFalse(pitch.vertical(1, 2));
        assertFalse(pitch.vertical(7, 5));
    }

    @Test
    void noOtherBordersHaveLines() {
        PitchMarkings pitch = pitch();
        assertFalse(pitch.horizontal(3, 4));
        assertFalse(pitch.horizontal(5, 4));
        assertFalse(pitch.vertical(4, 3));
        assertFalse(pitch.vertical(3, 3));
    }
}
