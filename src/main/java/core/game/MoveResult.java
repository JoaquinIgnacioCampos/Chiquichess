package core.game;

import core.pieces.Piece;
import core.rules.GameState;

import java.util.Optional;

public record MoveResult(Optional<Piece> capturedPiece, GameState state) {
}
