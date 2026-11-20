package core.board;

/**
 * Coordenada en el tablero (value object inmutable). Es la clave del Map del Tablero,
 * por eso al ser un record ya trae equals/hashCode correctos.
 * No valida límites: eso le corresponde al Tablero, que es quien conoce su tamaño.
 */
public record Position(int column, int row) {

    public Position shifted(int deltaColumn, int deltaRow) {
        return new Position(column + deltaColumn, row + deltaRow);
    }
}
