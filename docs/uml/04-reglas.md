# 4. Reglas: movimientos legales, reglas especiales y finales (Strategy)

Las reglas se agrupan en `GameRules`, que es lo que recibe `Game`. Hay dos puntos de extensión, y
los dos son el patrón Strategy:

- **`SpecialRule`** suma jugadas (enroque, captura al paso, pase) o transforma las que ya existen
  (promoción, gol).
- **`EndCondition`** decide si la partida terminó y quién ganó. También informa si la partida tiene
  un límite de jugadas y un marcador de goles. Así `Game` y los adaptadores no necesitan saber qué
  condiciones existen ni preguntar si hay pelota.

Las condiciones que necesitan memoria (repetición, 50 movimientos) implementan `StatefulCondition`
y devuelven un `Memento` en cada jugada, para poder deshacer.

```mermaid
classDiagram
    direction TB

    class GameRules {
        <<record>>
        +movementsLegal LegalMoves
        +conditions List~EndCondition~
        +allowsUndo boolean
        +classic()$ GameRules
        +football(moveLimit)$ GameRules
        +withUndo() GameRules
        +detector() ICheckDetector
    }

    class LegalMoves {
        -detector ICheckDetector
        -special List~SpecialRule~
        +movesPossible(Board, Position, history) List~Move~
        +movePossible(Board, origin, destination, history) Optional~Move~
        +destinations(Board, Position, history) Set~Position~
        +existsSome(Board, Side, history) boolean
        +leavesKingInCheck(Board, Move) boolean
    }

    class ICheckDetector {
        <<interface>>
        +isInCheck(IBoard, Side) boolean
    }
    class CheckDetector

    class SpecialRule {
        <<interface>>
        +moves(Board, Position, history, ICheckDetector) List~Move~
        +transform(Board, Move) List~Move~
    }
    class Castling
    class EnPassant
    class Promotion {
        -options List~Function~
    }
    class BallRule {
        -netColumns Set~Integer~
        -kickoffInitial Position
        +preparePitch(Board)
    }

    class EndCondition {
        <<interface>>
        +evaluate(MoveContext) Optional~GameState~
        +winner(GameState, MoveContext) Optional~Side~
        +moveLimit() Optional~Integer~
        +scoreboard(List~Move~) Optional~Scoreboard~
    }
    class StatefulCondition {
        <<interface>>
        +register(MoveContext) Memento
        +restore(Memento)
    }
    class Memento {
        <<interface>>
    }
    class Checkmate
    class Stalemate
    class GoalsVictory {
        -goalsToWin int
        -movesMaximum int
    }
    class RepetitionDraw
    class FiftyMoveRuleDraw

    class MoveContext {
        <<record>>
        +board IBoard
        +turn Side
        +moves List~Move~
        +inCheck boolean
        +hasMovementsLegal boolean
    }

    class Scoreboard {
        <<record>>
        +white int
        +black int
        +de(List~Move~)$ Scoreboard
        +leader() Optional~Side~
    }

    class GameState {
        <<enumeration>>
        IN_PROGRESS
        CHECK
        CHECK_MATE
        DRAW_STALEMATE
        DRAW_REPETITION
        DRAW_RULE_50_MOVEMENTS
        RESIGNATION
        TIME_EXHAUSTED
        DRAW_AGREEMENT
        VICTORY_BY_GOALS
        DRAW_TIED_ON_GOALS
    }
    class FinishReason {
        <<enumeration>>
    }

    GameRules *-- LegalMoves
    GameRules *-- "1..*" EndCondition
    LegalMoves o-- ICheckDetector
    LegalMoves o-- "0..*" SpecialRule
    ICheckDetector <|.. CheckDetector

    SpecialRule <|.. Castling
    SpecialRule <|.. EnPassant
    SpecialRule <|.. Promotion
    SpecialRule <|.. BallRule

    EndCondition <|.. Checkmate
    EndCondition <|.. Stalemate
    EndCondition <|.. GoalsVictory
    EndCondition <|-- StatefulCondition
    StatefulCondition <|.. RepetitionDraw
    StatefulCondition <|.. FiftyMoveRuleDraw
    StatefulCondition ..> Memento : crea y restaura
    EndCondition ..> MoveContext : evalúa
    EndCondition ..> GameState
    GameState --> FinishReason
    GoalsVictory ..> Scoreboard
```

## Las dos combinaciones que existen hoy

| Fábrica | Reglas especiales (en orden) | Condiciones de fin |
|---|---|---|
| `GameRules.classic()` | `Castling`, `EnPassant`, `Promotion` | `Checkmate`, `Stalemate`, `RepetitionDraw`, `FiftyMoveRuleDraw` |
| `GameRules.football(n)` | las clásicas + `BallRule` (después de `Promotion`) | `Checkmate`, `GoalsVictory(3, n)`, `Stalemate`, `RepetitionDraw`, `FiftyMoveRuleDraw` |

Una regla nueva es una clase nueva que se agrega a una de estas listas. No hace falta tocar `Game`
ni `LegalMoves`.
