# 2. Tablero, piezas y movimientos

**Composición sobre herencia:** una pieza es un color más una `MovementRule`. Las subclases de
`Piece` solo eligen qué regla componer, cómo se llaman y cuánto valen; no reescriben cómo se mueven.
Por ejemplo, la reina es `CompositeRule(orthogonal, diagonals)`, y el peón envuelve deslizamientos
con los decoradores `OnlyIfEmpty`, `OnlyIfCapture` y `OnlyIf`.

Una pieza nueva se agrega con una subclase de `Piece` que combine reglas existentes. No hace falta
modificar ninguna clase.

```mermaid
classDiagram
    direction TB

    class Side {
        <<enumeration>>
        WHITE
        BLACK
        +directionAdvance() int
        +opposite() Side
    }

    class Position {
        <<record>>
        +column int
        +row int
        +shifted(dc, dr) Position
    }

    class IBoard {
        <<interface>>
        +width() int
        +height() int
        +contains(Position) boolean
        +pieceIn(Position) Optional~Piece~
        +pieces() Map
        +ball() Optional~Position~
        +netIn(Position) Optional~Side~
    }

    class Board {
        -pieces Map
        -ball Position
        -nets Map
        +Board(width, height)
        +place(Position, Piece)
        +remove(Position) Optional~Piece~
        +placeBall(Position)
        +markNet(Position, Side)
        +copy() Board
    }

    class InitialLayout {
        <<interface>>
        +createBoard() Board
    }

    class Piece {
        <<abstract>>
        -color Side
        -regla MovementRule
        #Piece(Side, MovementRule)
        +destinationsPossible(IBoard, Position) Set~Position~
        +squaresAttacked(IBoard, Position) Set~Position~
        +isKing() boolean
        +isPawn() boolean
        +isRook() boolean
        +displayName()* String
        +code()* char
        +value()* int
    }

    class King
    class Queen
    class Rook
    class Bishop
    class Knight
    class Pawn

    class MovementRule {
        <<interface>>
        +destinationsPossible(IBoard, Position, Side) Set~Position~
        +squaresAttacked(IBoard, Position, Side) Set~Position~
    }

    class Slide {
        -deltaColumn int
        -deltaRow int
        -maxSteps int
    }
    class FixedJump {
        -offsets int[][]
    }
    class CompositeRule {
        -rules List~MovementRule~
    }
    class BaseRules {
        <<utility>>
        +orthogonal(maxSteps)$ MovementRule
        +diagonals(maxSteps)$ MovementRule
        +knightJumps()$ MovementRule
    }
    class OnlyIfEmpty {
        -regla MovementRule
    }
    class OnlyIfCapture {
        -regla MovementRule
    }
    class OnlyIf {
        -condition Condition
        -regla MovementRule
    }

    IBoard <|.. Board
    Board o-- "0..*" Piece : contiene
    Board ..> Position
    InitialLayout ..> Board : crea

    Piece <|-- King
    Piece <|-- Queen
    Piece <|-- Rook
    Piece <|-- Bishop
    Piece <|-- Knight
    Piece <|-- Pawn
    Piece --> Side
    Piece *-- "1" MovementRule : se mueve con
    Piece ..> IBoard : consulta

    MovementRule <|.. Slide
    MovementRule <|.. FixedJump
    MovementRule <|.. CompositeRule
    MovementRule <|.. OnlyIfEmpty
    MovementRule <|.. OnlyIfCapture
    MovementRule <|.. OnlyIf
    CompositeRule o-- "1..*" MovementRule
    OnlyIfEmpty o-- MovementRule : decora
    OnlyIfCapture o-- MovementRule : decora
    OnlyIf o-- MovementRule : decora
    BaseRules ..> Slide : arma
    BaseRules ..> FixedJump : arma
    BaseRules ..> CompositeRule : arma
```

## Qué arma cada pieza

| Pieza | Regla | `value()` |
|---|---|---|
| `Rook` | `orthogonal(WITHOUT_LIMIT)` | 500 |
| `Bishop` | `diagonals(WITHOUT_LIMIT)` | 330 |
| `Queen` | `CompositeRule(orthogonal, diagonals)` | 900 |
| `King` | `CompositeRule(orthogonal(1), diagonals(1))`; el enroque es una `SpecialRule` | 0 (nunca sale del tablero) |
| `Knight` | `knightJumps()` | 320 |
| `Pawn` | avance (`OnlyIfEmpty`) + avance doble (`OnlyIf` fila inicial) + capturas (`OnlyIfCapture`) | 100 |

`value()` es abstracto: una pieza nueva no compila hasta declarar cuánto vale. Así la IA no la
regala por creer que vale 0.

`InitialLayout` la implementan `StandardLayout` y `FootballLayout` (paquete `game`,
ver [diagrama 5](05-partida.md)). Un tablero de otro tamaño es otra `InitialLayout`.
