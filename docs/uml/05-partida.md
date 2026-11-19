# 5. Partida, eventos y reloj

`Partida` es la fachada del núcleo: recibe las órdenes de los jugadores (mover, deshacer, rendirse,
ofrecer tablas), las valida con las reglas y avisa lo que pasó a sus observadores. Todas sus
dependencias llegan por constructor: el tablero, los jugadores, las reglas y el reloj.

## 5a. Partida y sus colaboradores

```mermaid
classDiagram
    direction LR

    class Partida {
        -tablero Tablero
        -reglas ReglasDePartida
        -reloj RelojPartida
        -historialDeJugadas HistorialDeJugadas
        -observadores List~ObservadorDePartida~
        -turno Bando
        -estado EstadoPartida
        -ofertaTablas Optional~Bando~
        +Partida(Tablero, Jugador, Jugador, ReglasDePartida, RelojPartida)
        +mover(Movimiento) ResultadoMovimiento
        +movimientosLegales(Posicion) Set~Posicion~
        +opcionesDePromocion(Posicion, Posicion) List~Pieza~
        +deshacer()
        +rehacer()
        +rendirse(Bando)
        +ofrecerTablas(Bando)
        +aceptarTablas(Bando)
        +rechazarTablas(Bando)
        +verificarTiempo()
        +agregarObservador(ObservadorDePartida)
        +tablero() ITablero
        +turno() Bando
        +estado() EstadoPartida
        +resultado() Optional~ResultadoPartida~
        +marcador() Optional~Marcador~
        +limiteDeJugadas() Optional~Integer~
        +historial() List~Movimiento~
        +situacion() SituacionDePartida
    }

    class HistorialDeJugadas {
        -hechas List~Entrada~
        -deshechas List~Jugada~
    }

    class Jugador {
        <<record>>
        +nombre String
        +color Bando
    }
    class Movimiento {
        <<record>>
        +origen Posicion
        +destino Posicion
        +promocion Optional~Character~
        +de(Jugada)$ Movimiento
    }
    class ResultadoMovimiento {
        <<record>>
        +piezaCapturada Optional~Pieza~
        +estado EstadoPartida
    }
    class ResultadoPartida {
        <<record>>
        +ganador Optional~Bando~
        +motivo MotivoFinalizacion
    }
    class SituacionDePartida {
        <<record>>
        +tablero Tablero
        +turno Bando
        +jugadas List~Jugada~
        +reglas ReglasDePartida
    }
    class AccionInvalidaException
    class MovimientoInvalidoException

    Partida *-- HistorialDeJugadas
    Partida o-- Tablero
    Partida o-- ReglasDePartida
    Partida o-- RelojPartida
    Partida o-- "2" Jugador
    Partida o-- "0..*" ObservadorDePartida
    Partida ..> Movimiento : recibe
    Partida ..> ResultadoMovimiento : devuelve
    Partida ..> ResultadoPartida
    Partida ..> SituacionDePartida : arma una copia para la IA
    Partida ..> CondicionDeFinalizacion : pregunta límite y marcador
    Partida ..> AccionInvalidaException : lanza
    Partida ..> MovimientoInvalidoException : lanza
    HistorialDeJugadas o-- "0..*" Jugada
```

## 5b. Eventos (Observer)

```mermaid
classDiagram
    direction LR

    class Partida {
        +agregarObservador(ObservadorDePartida)
        +quitarObservador(ObservadorDePartida)
    }
    class ObservadorDePartida {
        <<interface>>
        +notificar(EventoDePartida)
    }
    class EventoDePartida {
        <<sealed interface>>
    }
    class JugadaHecha {
        <<record>>
        +bando Bando
        +rasgos Set~Rasgo~
        +estado EstadoPartida
    }
    class JugadaDeshecha {
        <<record>>
        +estado EstadoPartida
    }
    class JugadaRehecha {
        <<record>>
        +bando Bando
        +rasgos Set~Rasgo~
        +estado EstadoPartida
    }
    class PartidaFinalizada {
        <<record>>
        +resultado ResultadoPartida
    }
    class Rasgo {
        <<enumeration>>
        CAPTURA
        ENROQUE
        PROMOCION
        PASE
        GOL
        +de(Jugada)$ Set~Rasgo~
    }

    Partida o-- "0..*" ObservadorDePartida : avisa
    Partida ..> EventoDePartida : publica
    ObservadorDePartida ..> EventoDePartida
    EventoDePartida <|.. JugadaHecha
    EventoDePartida <|.. JugadaDeshecha
    EventoDePartida <|.. JugadaRehecha
    EventoDePartida <|.. PartidaFinalizada
    JugadaHecha --> Rasgo
    JugadaRehecha --> Rasgo
```

Los observadores son adaptadores (`SonidosDePartida`, `TurnoDeLaIa`, `VistaPartida`,
`PanelEstadoPartida`): ver [diagrama 7](07-adaptadores.md).

## 5c. Reloj y disposición inicial

```mermaid
classDiagram
    direction TB

    class RelojPartida {
        <<interface>>
        +iniciar(Bando)
        +cambiarTurno()
        +detener()
        +tiempoRestante(Bando) Optional~Duration~
    }
    class RelojAjedrez {
        -fuente FuenteDeTiempo
        -restantes Map
    }
    class SinReloj
    class FuenteDeTiempo {
        <<interface>>
        +ahoraEnNanos() long
    }

    class DisposicionInicial {
        <<interface>>
        +crearTablero() Tablero
    }
    class DisposicionEstandar
    class DisposicionDeFutbol {
        -base DisposicionInicial
        -pelota ReglaDeLaPelota
    }

    Partida o-- RelojPartida
    DisposicionInicial ..> Tablero : crea
    RelojPartida <|.. RelojAjedrez
    RelojPartida <|.. SinReloj
    RelojAjedrez o-- FuenteDeTiempo

    DisposicionInicial <|.. DisposicionEstandar
    DisposicionInicial <|.. DisposicionDeFutbol
    DisposicionDeFutbol o-- DisposicionInicial : decora
```

## Patrones que se ven acá

- **Observer:** `Partida` publica `EventoDePartida` y no sabe quién escucha (el sonido, el panel, la
  IA). El evento dice qué pasó, no cómo se muestra.
- **Command + Memento** para deshacer: el historial guarda cada `Jugada` (que sabe revertirse)
  junto con los `Recuerdo` de las condiciones con memoria.
- **Null Object:** `SinReloj` evita preguntar "¿hay reloj?" en cada jugada.
- **Decorator:** `DisposicionDeFutbol` envuelve cualquier disposición y le agrega la cancha.
- **Puerto de salida:** `FuenteDeTiempo` es la única forma en que el núcleo conoce la hora.
