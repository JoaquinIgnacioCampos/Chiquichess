package core.movements;

public class BaseRules {

    private static final int[][] ORTHOGONAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONALS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] JUMPS_KNIGHT = {
            {1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}};

    private BaseRules() {
    }

    public static MovementRule orthogonal(int maxSteps) {
        return slides(ORTHOGONAL, maxSteps);
    }

    public static MovementRule diagonals(int maxSteps) {
        return slides(DIAGONALS, maxSteps);
    }

    public static MovementRule knightJumps() {
        return new FixedJump(JUMPS_KNIGHT);
    }

    private static MovementRule slides(int[][] directions, int maxSteps) {
        MovementRule[] rules = new MovementRule[directions.length];
        for (int i = 0; i < directions.length; i++) {
            rules[i] = new Slide(directions[i][0], directions[i][1], maxSteps);
        }
        return new CompositeRule(rules);
    }
}