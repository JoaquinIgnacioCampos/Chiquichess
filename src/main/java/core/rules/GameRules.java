package core.rules;

import java.util.List;
import java.util.Objects;

/**
 * Reglas con las que se juega una partida: qué jugadas son legales, cuándo termina y si se permite
 * deshacer jugadas. Agrupa lo que Partida consulta en una sola dependencia, así cada regla nueva
 * (por ejemplo, jugadas especiales) se agrega acá sin sumar otro constructor a Partida.
 *
 * El detector de jaque no se recibe por separado: es el mismo que usan los movimientos legales,
 * de modo que no pueden quedar dos detectores distintos en una misma partida.
 *
 * Las condiciones con memoria (repetición, 50 movimientos) guardan estado, por eso una instancia
 * pertenece a una sola partida. {@link #clasicas()} crea instancias nuevas en cada llamada.
 *
 * @param condiciones     condiciones de fin, evaluadas en orden (la primera que se cumple gana).
 *                        Conviene poner primero jaque mate y ahogado.
 * @param permiteDeshacer si los jugadores pueden deshacer y rehacer jugadas. Aun habilitado, Partida
 *                        lo rechaza si la partida tiene límite de tiempo.
 */
public record GameRules(LegalMoves movementsLegal, List<EndCondition> conditions,
                              boolean allowsUndo) {

    public GameRules {
        Objects.requireNonNull(movementsLegal, "movementsLegal");
        conditions = List.copyOf(Objects.requireNonNull(conditions, "conditions"));
    }

    /** Reglas sin deshacer. */
    public GameRules(LegalMoves movementsLegal, List<EndCondition> conditions) {
        this(movementsLegal, conditions, false);
    }

    /** Arma los movimientos legales con el detector recibido, sin reglas especiales ni deshacer. */
    public GameRules(ICheckDetector detector, List<EndCondition> conditions) {
        this(new LegalMoves(detector), conditions);
    }

    /**
     * Ajedrez clásico: enroque, captura al paso y promoción; jaque mate, ahogado, triple repetición y
     * regla de los 50 movimientos; sin deshacer.
     */
    public static GameRules classic() {
        ICheckDetector detector = new CheckDetector();
        List<SpecialRule> special = List.of(new Castling(), new EnPassant(), new Promotion());
        return new GameRules(new LegalMoves(detector, special), List.of(
                new Checkmate(), new Stalemate(),
                new RepetitionDraw(), new FiftyMoveRuleDraw()));
    }

    /**
     * Modo fútbol "Posesión y gol" con el límite por defecto de 90 jugadas.
     */
    public static GameRules football() {
        return football(90);
    }

    /**
     * Modo fútbol "Posesión y gol": las reglas clásicas más la pelota. Gana quien llega a 3 goles o
     * tiene más goles al llegar al límite de jugadas; el jaque mate gana siempre y las tablas
     * clásicas siguen valiendo. La cancha (arcos y pelota) se marca en el tablero con
     * {@link Nucleo.juego.DisposicionDeFutbol}.
     *
     * @param limiteDeJugadas jugadas de los dos bandos juntos a las que termina la partida
     */
    public static GameRules football(int moveLimit) {
        ICheckDetector detector = new CheckDetector();
        List<SpecialRule> special = List.of(
                new Castling(), new EnPassant(), new Promotion(), new BallRule());
        return new GameRules(new LegalMoves(detector, special), List.of(
                new Checkmate(), new GoalsVictory(3, moveLimit), new Stalemate(),
                new RepetitionDraw(), new FiftyMoveRuleDraw()));
    }

    /** Las mismas reglas, permitiendo deshacer y rehacer jugadas. */
    public GameRules withUndo() {
        return new GameRules(movementsLegal, conditions, true);
    }

    public ICheckDetector detector() {
        return movementsLegal.detector();
    }
}
