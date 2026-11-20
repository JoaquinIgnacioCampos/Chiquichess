# 5. Partida, eventos y reloj

`Game` es la fachada del núcleo: recibe las órdenes de los jugadores (mover, deshacer, rendirse,
ofrecer tablas), las valida con las reglas y avisa lo que pasó a sus observadores. Todas sus
dependencias llegan por constructor (uno solo): el tablero, los jugadores, las reglas y el reloj.

`Game` coordina, y delega lo demás en cuatro clases privadas del paquete:

| Clase | Responsabilidad |
|---|---|
| `EndingReferee` | Decide si la partida terminó después de cada jugada y quién ganó; coordina la memoria de las condiciones (Memento) |
| `MoveHistory` | Las jugadas hechas y deshechas, con los recuerdos de cada una |
| `DrawOffer` | La oferta pendiente y sus reglas: quién puede ofrecer y quién responder |
| `GameNotices` | Los observadores y el envío de cada evento |

## 5a. Game y sus colaboradores

```mermaid
classDiagram
    direction LR

    class Game {
        -board Board
        -rules GameRules
        -clock GameClock
        -referee EndingReferee
        -moveHistory MoveHistory
        -drawOffer DrawOffer
        -notices GameNotices
        -turn Side
        -state GameState
        +Game(Board, Player, Player, GameRules, GameClock)
        +move(MoveRequest) MoveResult
        +movementsLegal(Position) Set~Position~
        +promotionOptions(Position, Position) List~Piece~
        +undo()
        +redo()
        +resign(Side)
        +offerDraw(Side)
        +acceptDraw(Side)
        +rejectDraw(Side)
        +verifyTime()
        +addObserver(GameObserver)
        +board() IBoard
        +turn() Side
        +state() GameState
        +result() Optional~GameResult~
        +scoreboard() Optional~Scoreboard~
        +moveLimit() Optional~Integer~
        +history() List~MoveRequest~
        +situation() GameSituation
    }

    class EndingReferee {
        -rules GameRules
        -conditionsWithMemory List~StatefulCondition~
        ~registerAndEvaluate(Board, Side, List~Move~) Record
        ~evaluate(Board, Side, List~Move~) Verdict
        ~restore(List~Memento~)
        ~moveLimit() Optional~Integer~
        ~scoreboard(List~Move~) Optional~Scoreboard~
    }
    class MoveHistory {
        -made List~Entrada~
        -undone List~Move~
    }
    class DrawOffer {
        -offerer Optional~Side~
        ~offer(Side, Side turn)
        ~validateResponse(Side)
        ~onMove(Side)
        ~discard()
    }
    class GameNotices {
        -observers List~GameObserver~
        ~add(GameObserver)
        ~remove(GameObserver)
        ~inform(GameEvent)
    }

    class Player {
        <<record>>
        +displayName String
        +color Side
    }
    class MoveRequest {
        <<record>>
        +origin Position
        +destination Position
        +promotion Optional~Character~
        +de(Move)$ MoveRequest
    }
    class MoveResult {
        <<record>>
        +capturedPiece Optional~Piece~
        +state GameState
    }
    class GameResult {
        <<record>>
        +winner Optional~Side~
        +reason FinishReason
    }
    class GameSituation {
        <<record>>
        +board Board
        +turn Side
        +moves List~Move~
        +rules GameRules
    }
    class InvalidActionException
    class InvalidMoveException

    Game *-- EndingReferee
    Game *-- MoveHistory
    Game *-- DrawOffer
    Game *-- GameNotices
    Game o-- Board
    Game o-- GameRules
    Game o-- GameClock
    Game o-- "2" Player
    Game ..> MoveRequest : recibe
    Game ..> MoveResult : devuelve
    Game ..> GameResult
    Game ..> GameSituation : arma una copia para la IA
    Game ..> InvalidActionException : lanza
    Game ..> InvalidMoveException : lanza
    EndingReferee o-- GameRules
    EndingReferee ..> EndCondition : consulta en orden
    EndingReferee ..> Memento : guarda y restaura
    MoveHistory o-- "0..*" Move
    DrawOffer ..> InvalidActionException : lanza
    GameNotices o-- "0..*" GameObserver
```

## 5b. Puertos de entrada: lo que ven los adaptadores

Los adaptadores no conocen la clase `Game`: reciben una de estas dos interfaces del núcleo. Solo
`GameAssembler` construye la `Game` concreta.

```mermaid
classDiagram
    direction TB

    class GameQuery {
        <<interface>>
        +board() IBoard
        +turn() Side
        +player(Side) Player
        +state() GameState
        +result() Optional~GameResult~
        +scoreboard() Optional~Scoreboard~
        +moveLimit() Optional~Integer~
        +history() List~MoveRequest~
        +situation() GameSituation
        +timeRemaining(Side) Optional~Duration~
        +movementsLegal(Position) Set~Position~
        +promotionOptions(Position, Position) List~Piece~
        +canUndo() boolean
        +addObserver(GameObserver)
    }
    class GameInProgress {
        <<interface>>
        +move(MoveRequest) MoveResult
        +undo()
        +redo()
        +resign(Side)
        +offerDraw(Side)
        +acceptDraw(Side)
        +rejectDraw(Side)
        +verifyTime()
    }
    class Game

    GameQuery <|-- GameInProgress
    GameInProgress <|.. Game
```

- **`GameQuery`** es de solo lectura, más la suscripción a los eventos. La recibe el sonido: por
  tipo, no puede mover piezas ni terminar la partida.
- **`GameInProgress`** suma las órdenes de los jugadores. La reciben la consola, el controlador del
  tablero, el rival artificial y el turno de la IA.

(En la interfaz de consulta se muestran los métodos principales; también tiene `currentPlayer`,
`winner`, `offerDrawPending`, `allowsUndo`, `canRedo` y `removeObserver`.)

## 5c. Eventos (Observer)

```mermaid
classDiagram
    direction LR

    class Game {
        +addObserver(GameObserver)
        +removeObserver(GameObserver)
    }
    class GameNotices
    class GameObserver {
        <<interface>>
        +notify(GameEvent)
    }
    class GameEvent {
        <<sealed interface>>
    }
    class MoveMade {
        <<record>>
        +side Side
        +traits Set~Trait~
        +state GameState
    }
    class MoveUndone {
        <<record>>
        +state GameState
    }
    class MoveRedone {
        <<record>>
        +side Side
        +traits Set~Trait~
        +state GameState
    }
    class FinishedGame {
        <<record>>
        +result GameResult
    }
    class Trait {
        <<enumeration>>
        CAPTURE
        CASTLING
        PROMOTION
        PASS
        GOAL
        +de(Move)$ Set~Trait~
    }

    Game *-- GameNotices
    GameNotices o-- "0..*" GameObserver : avisa
    Game ..> GameEvent : crea
    GameObserver ..> GameEvent
    GameEvent <|.. MoveMade
    GameEvent <|.. MoveUndone
    GameEvent <|.. MoveRedone
    GameEvent <|.. FinishedGame
    MoveMade --> Trait
    MoveRedone --> Trait
```

Los observadores son adaptadores (`GameSounds`, `AiTurn`, `GameView`, `GameStatePanel`): ver
[diagrama 7](07-adaptadores.md).

## 5d. Reloj y disposición inicial

```mermaid
classDiagram
    direction TB

    class GameClock {
        <<interface>>
        +start(Side)
        +changeTurn()
        +stop()
        +timeRemaining(Side) Optional~Duration~
        +timeExhausted(Side) boolean
    }
    class ChessClock {
        -font TimeSource
        -remainingTimes Map
    }
    class NoClock
    class TimeSource {
        <<interface>>
        +nowInNanos() long
    }

    class InitialLayout {
        <<interface>>
        +createBoard() Board
    }
    class StandardLayout
    class FootballLayout {
        -base InitialLayout
        -ball BallRule
    }

    Game o-- GameClock
    InitialLayout ..> Board : crea
    GameClock <|.. ChessClock
    GameClock <|.. NoClock
    ChessClock o-- TimeSource

    InitialLayout <|.. StandardLayout
    InitialLayout <|.. FootballLayout
    FootballLayout o-- InitialLayout : decora
```

## Patrones que se ven acá

- **Observer:** `Game` publica `GameEvent` y no sabe quién escucha (el sonido, el panel, la IA). El
  evento dice qué pasó, no cómo se muestra.
- **Command + Memento** para deshacer: el historial guarda cada `Move` (que sabe revertirse) junto con
  los `Memento` que devuelve el árbitro. `Game` es el cuidador: los guarda y los devuelve sin saber
  qué contienen.
- **Null Object:** `NoClock` evita preguntar "¿hay reloj?" en cada jugada.
- **Decorator:** `FootballLayout` envuelve cualquier disposición y le agrega la cancha.
- **Puerto de salida:** `TimeSource` es la única forma en que el núcleo conoce la hora.
- **Puertos de entrada con segregación de interfaces:** `GameQuery` y `GameInProgress`. Cada
  adaptador recibe solo lo que necesita (la I de SOLID) y depende de una abstracción del núcleo, no de
  la clase concreta (la D de SOLID).
