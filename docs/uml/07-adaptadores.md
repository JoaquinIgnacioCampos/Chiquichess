# 7. Adaptadores: ensamblado, consola, interfaz gráfica y sonido

Los adaptadores traducen entre el mundo exterior (teclado, mouse, pantalla, parlantes, reloj del
sistema, archivo de preferencias) y el núcleo. **Ninguna clase del núcleo aparece acá con una flecha
que salga de ella hacia un adaptador.** Las clases del núcleo están marcadas con `<<núcleo>>`.

Los adaptadores hablan con la partida a través de los puertos de entrada del núcleo
(`GameInProgress` y `GameQuery`, ver [diagrama 5](05-partida.md)). La clase concreta `Game` solo la
conoce `GameAssembler`, que la construye.

`GameAssembler` y `ChessApplication` son las raíces de composición: el único lugar donde se eligen
las implementaciones concretas y se arman las partes de la partida (inyección de dependencias
manual, sin contenedor).

| Diagrama | Qué muestra |
|---|---|
| 7a | Ensamblado, configuración, preferencias, consola e IA |
| 7b | Interfaz gráfica: la aplicación, el menú y la partida |
| 7c | Interfaz gráfica: componentes visuales en pixel art |
| 7d | Sonido |

## 7a. Ensamblado, configuración, consola e IA

```mermaid
classDiagram
    direction TB

    class Game {
        <<núcleo>>
    }
    class GameInProgress {
        <<núcleo interface>>
    }
    class GameStrategy {
        <<núcleo interface>>
    }
    class TimeSource {
        <<núcleo interface>>
    }

    class GameAssembler {
        -draw ColorLottery
        -font TimeSource
        +prepareConfigured(GameConfiguration) ConfiguredGame
        +createConfigured(GameConfiguration) GameInProgress
        +createRival(GameInProgress, Side, Difficulty) ArtificialRival
    }
    class GameConfiguration {
        <<record>>
        +mode Mode
        +time Time
        +rival Rival
        +teamPlayer1 Team
        +teamPlayer2 Team
        +colors Colors
        +allowUndo boolean
        +moveLimit int
        +difficulty Optional~Difficulty~
    }
    class ConfiguredGame {
        <<record>>
        +game GameInProgress
        +whiteTeam Team
        +blackTeam Team
        +clock GameClock
        +rival Optional~ArtificialRival~
    }
    class MenuSelection {
        <<record>>
        +usual()$ MenuSelection
        +de(GameConfiguration)$ MenuSelection
    }
    class Preferences {
        <<record>>
        +selection MenuSelection
        +volume double
        +muted boolean
        +byDefault()$ Preferences
    }
    class SettingsFile {
        -file Path
        +read() Preferences
        +save(Preferences)
    }
    class ColorLottery {
        <<interface>>
        +player1PlaysWithWhite() boolean
    }
    class RandomColorLottery
    class SystemTimeSource
    class ArtificialRival {
        -game GameInProgress
        -side Side
        -strategy GameStrategy
        +think() Response
        +play() MoveRequest
        +respondDraw() boolean
        +undo()
    }

    class Main
    class ConsoleGame {
        -game GameInProgress
        -rival Optional~ArtificialRival~
        +play()
    }
    class ConsoleRenderer {
        +draw(IBoard) String
    }
    class ConsoleMessages
    class AlgebraicNotation {
        +parse(String)$ Position
        +format(Position)$ String
    }

    GameAssembler ..> GameConfiguration : lee
    GameAssembler ..> ConfiguredGame : crea
    GameAssembler o-- ColorLottery
    GameAssembler o-- TimeSource
    GameAssembler ..> Game : crea (único que la conoce)
    GameInProgress <|.. Game
    GameAssembler ..> ArtificialRival : crea
    ColorLottery <|.. RandomColorLottery
    TimeSource <|.. SystemTimeSource
    ConfiguredGame o-- GameInProgress
    ConfiguredGame o-- ArtificialRival
    MenuSelection ..> GameConfiguration : recuerda
    Preferences *-- MenuSelection
    SettingsFile ..> Preferences : lee y guarda
    ArtificialRival o-- GameStrategy
    ArtificialRival --> GameInProgress

    Main ..> GameAssembler
    Main ..> ConsoleGame
    ConsoleGame --> GameInProgress
    ConsoleGame o-- ArtificialRival
    ConsoleGame o-- ConsoleRenderer
    ConsoleGame o-- ConsoleMessages
    ConsoleGame ..> AlgebraicNotation
```

Las opciones del menú y el volumen se guardan entre sesiones: `SettingsFile` las lee y las escribe en
`configuracion.txt` (en la carpeta desde donde se ejecuta el juego), como una `Preferences`.

## 7b. Interfaz gráfica: aplicación, menú y partida

```mermaid
classDiagram
    direction LR

    class GameInProgress {
        <<núcleo interface>>
    }
    class GameObserver {
        <<núcleo interface>>
    }
    class GameAssembler
    class ArtificialRival
    class SettingsFile
    class GameSounds
    class MenuSounds
    class CrossfadedLoop

    class GraphicalMain
    class ChessApplication {
        <<JavaFX Application>>
        +start(Stage)
        +stop()
    }
    class IntroScreen
    class GameMenuView {
        +preselect(MenuSelection)
        +configureSound(VolumeControl)
    }
    class GameView {
        +redraw()
        +lookFrom(Side)
        +configureUndo()
        +configureSound(VolumeControl)
    }
    class BoardController {
        -game GameInProgress
        -rival Optional~ArtificialRival~
        +press(Position)
        +listen(GameObserver)
    }
    class BoardView {
        +redraw()
        +markKing(Optional~Side~, boolean)
        +lookFrom(Side)
    }
    class GameStatePanel {
        +update()
        +updateClocks()
    }
    class PlayerCard
    class ClockView
    class GoalBanner {
        +celebrate(String, Color, Optional~Image~) Animation
    }
    class PromotionSelector
    class GameNavigation
    class JavaFxActionConfirmation
    class JavaFxClockUpdater
    class InterfaceMessages
    class AiTurn {
        -game GameInProgress
        -rival ArtificialRival
        -thinkingThread Executor
    }
    class PieceRepresentation {
        <<interface>>
        +create(Piece, BoardTheme) Node
    }
    class SpriteRepresentation
    class UnicodeRepresentation
    class SpriteCatalog {
        +de(Team)$ SpriteCatalog
    }
    class ClubPresentation {
        <<record>>
    }
    class BoardTheme {
        <<record>>
    }
    class PanelTheme {
        <<record>>
    }
    class PitchMarkings
    class PixelMarkers

    GraphicalMain ..> ChessApplication
    ChessApplication ..> GameAssembler
    ChessApplication ..> SettingsFile
    ChessApplication ..> IntroScreen
    ChessApplication ..> GameMenuView
    ChessApplication ..> GameView
    ChessApplication ..> BoardController
    ChessApplication ..> AiTurn
    ChessApplication ..> JavaFxClockUpdater
    ChessApplication ..> GameSounds
    ChessApplication ..> MenuSounds
    ChessApplication ..> CrossfadedLoop : audio de fondo
    ChessApplication ..> SpriteRepresentation
    IntroScreen *-- CrossfadedLoop : audio de la intro

    GameView o-- BoardController
    GameView *-- BoardView
    GameView *-- GameStatePanel
    GameView *-- GoalBanner
    GameView *-- PromotionSelector
    GameView *-- GameNavigation
    GameView o-- PieceRepresentation
    GameView o-- InterfaceMessages
    GameView ..> ClubPresentation
    GameStatePanel o-- BoardController
    GameStatePanel *-- "2" PlayerCard
    PlayerCard *-- ClockView
    PlayerCard *-- JavaFxActionConfirmation
    GameNavigation o-- BoardController
    JavaFxClockUpdater o-- BoardController
    BoardView o-- PieceRepresentation
    BoardView o-- BoardTheme
    BoardView ..> PitchMarkings : líneas de la cancha
    BoardView ..> PixelMarkers : marcas y pelota
    BoardController --> GameInProgress
    BoardController o-- ArtificialRival
    BoardController ..> GameObserver : suscribe a la vista y al panel
    AiTurn --> GameInProgress
    AiTurn o-- ArtificialRival
    AiTurn ..> GameObserver : escucha con una lambda
    PieceRepresentation <|.. SpriteRepresentation
    PieceRepresentation <|.. UnicodeRepresentation
    SpriteRepresentation o-- SpriteCatalog
    SpriteRepresentation o-- PieceRepresentation : respaldo
    ClubPresentation o-- PanelTheme
```

## 7c. Interfaz gráfica: componentes visuales en pixel art

Piezas chicas y reutilizables que dan el estilo pixel art. Las usan casi todas las vistas de 7b; acá
se muestran solo las dependencias principales para no llenar el diagrama de flechas.

```mermaid
classDiagram
    direction LR

    class PixelStyle {
        <<utility>>
        +font(double, boolean)$ Font
        +button(Color, Color, boolean)$ String
        +card(Color, Color)$ String
    }
    class PixelIcon {
        <<utility>>
        +create(String[], Color, Color, Color)$ Node
    }
    class HeaderButton {
        <<utility>>
        +create(String, String, Color, Color)$ Button
    }
    class WhistleIcon {
        <<utility>>
        +create(boolean, Color, Color)$ Node
    }
    class SoundControls {
        <<utility>>
        +create(VolumeControl, String, String, Color, Color)$ List~Node~
    }
    class LedText {
        +setText(String)
        +setColor(Color)
        +setColorOff(Color)
    }
    class Logo {
        <<utility>>
        +create()$ ImageView
    }
    class FitToHeight
    class WindowsTitleBar {
        <<utility>>
        +paint(Stage, Color, Color)$
    }
    class VolumeControl

    class GameView
    class GameMenuView
    class GameStatePanel
    class IntroScreen
    class ChessApplication
    class ClockView
    class GoalBanner

    HeaderButton ..> PixelIcon
    HeaderButton ..> PixelStyle
    WhistleIcon ..> PixelIcon
    SoundControls ..> HeaderButton
    SoundControls ..> WhistleIcon
    SoundControls --> VolumeControl
    GameView ..> SoundControls
    GameView ..> Logo
    GameMenuView ..> SoundControls
    GameMenuView ..> Logo
    GameMenuView ..> FitToHeight
    IntroScreen ..> SoundControls
    ChessApplication ..> WindowsTitleBar : colores de la barra en Windows
    GameStatePanel ..> LedText
    GameMenuView ..> LedText
    ClockView ..> LedText
    GoalBanner ..> LedText
```

`WindowsTitleBar` pinta la barra de título de la ventana con los colores del juego usando JNA (una
biblioteca para llamar a funciones de Windows). En otros sistemas, o si algo falla, la barra queda como
estaba.

## 7d. Sonido

```mermaid
classDiagram
    direction TB

    class GameQuery {
        <<núcleo interface>>
    }
    class GameObserver {
        <<núcleo interface>>
    }

    class GameSounds {
        -game GameQuery
        -catalog SoundCatalog
        -player AudioPlayer
        -script SoundScript
        -control VolumeControl
        +notify(GameEvent)
        +selectedPiece()
    }
    class SoundScript {
        +onStart() List~Cue~
        +para(GameEvent) List~Cue~
    }
    class SoundCatalog {
        +search(Sound, Optional~Team~) Optional~URL~
    }
    class Sound {
        <<enumeration>>
        +file() String
        +teamSpecific() boolean
        +alternative() Optional~Sound~
    }
    class CrowdIntensity {
        <<utility>>
        +de(Side, int, Optional~Integer~, Scoreboard)$ double
    }
    class AudioPlayer {
        <<interface>>
        +play(URL, double)
        +inLoop(URL, double) Track
    }
    class JavaSoundPlayer
    class MenuSounds {
        +openGame()
        +chosenOption()
        +enterMenu()
        +leaveMenu()
    }
    class VolumeControl {
        +effective() double
        +changeVolume(double)
        +mute(boolean)
        +onChange(Runnable)
    }
    class CrossfadedLoop {
        +CrossfadedLoop(String, VolumeControl, double, String)
    }

    GameObserver <|.. GameSounds
    GameSounds --> GameQuery : solo consulta y se suscribe
    GameSounds *-- SoundScript
    GameSounds o-- SoundCatalog
    GameSounds o-- AudioPlayer
    GameSounds o-- VolumeControl
    GameSounds ..> CrowdIntensity : volumen de cada hinchada
    SoundScript ..> Sound
    SoundCatalog ..> Sound
    MenuSounds o-- SoundCatalog
    MenuSounds o-- AudioPlayer
    MenuSounds o-- VolumeControl
    AudioPlayer <|.. JavaSoundPlayer
    CrossfadedLoop o-- VolumeControl
```

- `SoundScript` decide **qué** suena con cada evento; `SoundCatalog` decide **qué archivo** es (con
  sonidos propios de cada club); `AudioPlayer` decide **cómo** se reproduce. Cada uno se puede
  cambiar o probar por separado.
- `CrossfadedLoop` (paquete `ui`) reproduce el audio de fondo de la partida y de la pantalla de intro
  con JavaFX Media, y funde el final de cada vuelta con el principio de la siguiente.

## Qué hace cada grupo

| Grupo | Responsabilidad | Tecnología que encierra |
|---|---|---|
| Ensamblado (`GameAssembler`, `configuration`) | Traduce lo elegido en el menú a una partida armada; es el único que construye `Game` | ninguna (Java puro) |
| Preferencias (`SettingsFile`, `Preferences`, `MenuSelection`) | Recuerda las opciones del menú y el volumen entre sesiones | un archivo de texto |
| Consola (`Main`, `ConsoleGame`, `ConsoleRenderer`, `AlgebraicNotation`) | Juego por texto con notación algebraica | `System.in` / `System.out` |
| Interfaz gráfica (`ui`) | Intro, menú, tablero, panel, relojes, cartel de gol, estilo pixel art | JavaFX, JavaFX Media, JNA |
| Sonido (`audio`) | Traduce eventos de la partida a sonidos | Java Sound (`javax.sound`) |
| IA (`ArtificialRival`, `AiTurn`) | Decide cuándo le toca a la IA y en qué hilo piensa | `Executor` / hilo de JavaFX |
