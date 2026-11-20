package adapters;



import core.board.Position;

/** Traduce entre el texto "e2" y una Posicion del núcleo (a1 = columna 0, fila 0). */
public final class AlgebraicNotation {

    private AlgebraicNotation() {
    }

    public static Position parse(String texto) {
        if (texto == null || texto.length() < 2) {
            throw new IllegalArgumentException("Invalid square: " + texto);
        }
        char letter = Character.toLowerCase(texto.charAt(0));
        if (letter < 'a' || letter > 'z') {
            throw new IllegalArgumentException("Invalid square: " + texto);
        }
        try {
            int row = Integer.parseInt(texto.substring(1)) - 1;
            return new Position(letter - 'a', row);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid square: " + texto);
        }
    }

    public static String format(Position position) {
        return "" + (char) ('a' + position.column()) + (position.row() + 1);
    }
}
