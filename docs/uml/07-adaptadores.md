# 7. Adaptadores: ensamblado, consola, interfaz gráfica y sonido

Los adaptadores traducen entre el mundo exterior (teclado, mouse, pantalla, parlantes, reloj del
sistema) y el núcleo. **Ninguna clase del núcleo aparece acá con una flecha que salga de ella hacia
un adaptador.** Las clases del núcleo están marcadas con `<<núcleo>>`.

`EnsambladorPartida` y `AplicacionAjedrez` son las raíces de composición: el único lugar donde se
eligen las implementaciones concretas y se arman las partes de la partida (inyección de
dependencias manual, sin contenedor).

## 7a. Ensamblado, consola e IA

```mermaid
classDiagram
    direction TB

    class Partida {
        <<núcleo>>
    }
    class EstrategiaDeJuego {
        <<núcleo interface>>
    }
    class FuenteDeTiempo {
        <<núcleo interface>>
    }

    class EnsambladorPartida {
        -sorteo Sorteo
        -fuente FuenteDeTiempo
        +prepararConfigurada(ConfiguracionPartida) PartidaConfigurada
        +crearRival(Partida, Bando, Dificultad) RivalArtificial
    }
    class ConfiguracionPartida {
        <<record>>
        +modo Modo
        +tiempo Tiempo
        +rival Rival
        +equipoJugador1 Equipo
        +equipoJugador2 Equipo
        +colores Colores
        +permitirDeshacer boolean
        +limiteDeJugadas int
        +dificultad Optional~Dificultad~
    }
    class PartidaConfigurada {
        <<record>>
        +partida Partida
        +equipoBlancas Equipo
        +equipoNegras Equipo
        +rival Optional~RivalArtificial~
    }
    class Sorteo {
        <<interface>>
        +jugador1JuegaConBlancas() boolean
    }
    class SorteoAleatorio
    class FuenteDeTiempoSistema
    class RivalArtificial {
        -partida Partida
        -bando Bando
        -estrategia EstrategiaDeJuego
        +pensar() Respuesta
        +jugar() Movimiento
        +responderTablas() boolean
        +deshacer()
    }

    class Main
    class JuegoConsola {
        -partida Partida
        -rival Optional~RivalArtificial~
    }
    class RenderizadorConsola
    class MensajesDeConsola
    class NotacionAlgebraica

    EnsambladorPartida ..> ConfiguracionPartida : lee
    EnsambladorPartida ..> PartidaConfigurada : crea
    EnsambladorPartida o-- Sorteo
    EnsambladorPartida o-- FuenteDeTiempo
    EnsambladorPartida ..> Partida : crea
    EnsambladorPartida ..> RivalArtificial : crea
    Sorteo <|.. SorteoAleatorio
    FuenteDeTiempo <|.. FuenteDeTiempoSistema
    PartidaConfigurada o-- Partida
    PartidaConfigurada o-- RivalArtificial
    RivalArtificial o-- EstrategiaDeJuego
    RivalArtificial --> Partida

    Main ..> EnsambladorPartida
    Main ..> JuegoConsola
    JuegoConsola --> Partida
    JuegoConsola o-- RivalArtificial
    JuegoConsola o-- RenderizadorConsola
    JuegoConsola o-- MensajesDeConsola
    JuegoConsola ..> NotacionAlgebraica
```

## 7b. Interfaz gráfica y sonido

```mermaid
classDiagram
    direction TB

    class Partida {
        <<núcleo>>
    }
    class ObservadorDePartida {
        <<núcleo interface>>
    }
    class EnsambladorPartida
    class RivalArtificial

    class AplicacionAjedrez {
        <<JavaFX Application>>
    }
    class VistaMenuPartida
    class VistaPartida
    class ControladorTablero {
        -partida Partida
        -rival Optional~RivalArtificial~
        +pulsar(Posicion)
        +escuchar(ObservadorDePartida)
    }
    class VistaTablero
    class PanelEstadoPartida
    class CartelDeGol
    class MensajesDeLaInterfaz
    class TurnoDeLaIa {
        -partida Partida
        -rival RivalArtificial
        -hiloDePensar Executor
    }
    class RepresentacionPiezas {
        <<interface>>
        +crear(Pieza, TemaTablero) Node
    }
    class RepresentacionConSprites
    class RepresentacionUnicode
    class PresentacionClub {
        <<record>>
    }

    class SonidosDePartida {
        -catalogo CatalogoDeSonidos
        -reproductor ReproductorDeAudio
        -guion GuionDeSonidos
    }
    class GuionDeSonidos
    class CatalogoDeSonidos
    class ReproductorDeAudio {
        <<interface>>
        +reproducir(URL, volumen)
        +enBucle(URL, volumen) Pista
    }
    class ReproductorJavaSound
    class SonidosDelMenu
    class ControlDeVolumen

    AplicacionAjedrez ..> EnsambladorPartida
    AplicacionAjedrez ..> VistaMenuPartida
    AplicacionAjedrez ..> VistaPartida
    AplicacionAjedrez ..> ControladorTablero
    AplicacionAjedrez ..> TurnoDeLaIa
    AplicacionAjedrez ..> SonidosDePartida
    AplicacionAjedrez ..> SonidosDelMenu

    VistaPartida o-- ControladorTablero
    VistaPartida *-- VistaTablero
    VistaPartida *-- PanelEstadoPartida
    VistaPartida *-- CartelDeGol
    VistaPartida o-- RepresentacionPiezas
    VistaPartida o-- MensajesDeLaInterfaz
    VistaPartida ..> PresentacionClub
    PanelEstadoPartida o-- ControladorTablero
    ControladorTablero --> Partida
    ControladorTablero o-- RivalArtificial
    TurnoDeLaIa --> Partida
    TurnoDeLaIa o-- RivalArtificial
    TurnoDeLaIa ..> ObservadorDePartida : escucha con una lambda
    RepresentacionPiezas <|.. RepresentacionConSprites
    RepresentacionPiezas <|.. RepresentacionUnicode
    RepresentacionConSprites o-- RepresentacionPiezas : respaldo

    ObservadorDePartida <|.. SonidosDePartida
    SonidosDePartida --> Partida : se suscribe
    SonidosDePartida *-- GuionDeSonidos
    SonidosDePartida o-- CatalogoDeSonidos
    SonidosDePartida o-- ReproductorDeAudio
    SonidosDePartida o-- ControlDeVolumen
    SonidosDelMenu o-- CatalogoDeSonidos
    SonidosDelMenu o-- ReproductorDeAudio
    SonidosDelMenu o-- ControlDeVolumen
    ReproductorDeAudio <|.. ReproductorJavaSound
```

## Qué hace cada grupo

| Grupo | Responsabilidad | Tecnología que encierra |
|---|---|---|
| Ensamblado (`EnsambladorPartida`, `configuracion`) | Traduce lo elegido en el menú a una `Partida` armada | ninguna (Java puro) |
| Consola (`Main`, `JuegoConsola`, `RenderizadorConsola`, `NotacionAlgebraica`) | Juego por texto con notación algebraica | `System.in` / `System.out` |
| Interfaz gráfica (`ui`) | Menú, tablero, panel, relojes, cartel de gol | JavaFX |
| Sonido (`audio`) | Traduce eventos de la partida a sonidos | Java Sound (`javax.sound`) |
| IA (`RivalArtificial`, `TurnoDeLaIa`) | Decide cuándo le toca a la IA y en qué hilo piensa | `Executor` / hilo de JavaFX |

`GuionDeSonidos` decide **qué** suena con cada evento; `CatalogoDeSonidos` decide **qué archivo** es
(con sonidos propios de cada club); `ReproductorDeAudio` decide **cómo** se reproduce. Cada uno se
puede cambiar o probar por separado.
