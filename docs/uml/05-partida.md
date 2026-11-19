# 5. Partida, eventos y reloj

`Partida` es la fachada del núcleo: recibe las órdenes de los jugadores (mover, deshacer, rendirse,
ofrecer tablas), las valida con las reglas y avisa lo que pasó a sus observadores. Todas sus
dependencias llegan por constructor (uno solo): el tablero, los jugadores, las reglas y el reloj.

`Partida` coordina, y delega lo demás en cuatro clases privadas del paquete:

| Clase | Responsabilidad |
|---|---|
| `ArbitroDeFinales` | Decide si la partida terminó después de cada jugada y quién ganó; coordina la memoria de las condiciones (Memento) |
| `HistorialDeJugadas` | Las jugadas hechas y deshechas, con los recuerdos de cada una |
| `OfertaDeTablas` | La oferta pendiente y sus reglas: quién puede ofrecer y quién responder |
| `AvisosDePartida` | Los observadores y el envío de cada evento |

## 5a. Partida y sus colaboradores

```mermaid
classDiagram
    direction LR

    class Partida {
        -tablero Tablero
        -reglas ReglasDePartida
        -reloj RelojPartida
        -arbitro ArbitroDeFinales
        -historialDeJugadas HistorialDeJugadas
        -ofertaDeTablas OfertaDeTablas
        -avisos AvisosDePartida
        -turno Bando
        -estado EstadoPartida
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

    class ArbitroDeFinales {
        -reglas ReglasDePartida
        -condicionesConMemoria List~CondicionConMemoria~
        ~registrarYEvaluar(Tablero, Bando, List~Jugada~) Registro
        ~evaluar(Tablero, Bando, List~Jugada~) Veredicto
        ~restaurar(List~Recuerdo~)
        ~limiteDeJugadas() Optional~Integer~
        ~marcador(List~Jugada~) Optional~Marcador~
    }
    class HistorialDeJugadas {
        -hechas List~Entrada~
        -deshechas List~Jugada~
    }
    class OfertaDeTablas {
        -oferente Optional~Bando~
        ~ofrecer(Bando, Bando turno)
        ~validarRespuesta(Bando)
        ~alMover(Bando)
        ~descartar()
    }
    class AvisosDePartida {
        -observadores List~ObservadorDePartida~
        ~agregar(ObservadorDePartida)
        ~quitar(ObservadorDePartida)
        ~avisar(EventoDePartida)
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

    Partida *-- ArbitroDeFinales
    Partida *-- HistorialDeJugadas
    Partida *-- OfertaDeTablas
    Partida *-- AvisosDePartida
    Partida o-- Tablero
    Partida o-- ReglasDePartida
    Partida o-- RelojPartida
    Partida o-- "2" Jugador
    Partida ..> Movimiento : recibe
    Partida ..> ResultadoMovimiento : devuelve
    Partida ..> ResultadoPartida
    Partida ..> SituacionDePartida : arma una copia para la IA
    Partida ..> AccionInvalidaException : lanza
    Partida ..> MovimientoInvalidoException : lanza
    ArbitroDeFinales o-- ReglasDePartida
    ArbitroDeFinales ..> CondicionDeFinalizacion : consulta en orden
    ArbitroDeFinales ..> Recuerdo : guarda y restaura
    HistorialDeJugadas o-- "0..*" Jugada
    OfertaDeTablas ..> AccionInvalidaException : lanza
    AvisosDePartida o-- "0..*" ObservadorDePartida
```

## 5b. Eventos (Observer)

```mermaid
classDiagram
    direction LR

    class Partida {
        +agregarObservador(ObservadorDePartida)
        +quitarObservador(ObservadorDePartida)
    }
    class AvisosDePartida
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

    Partida *-- AvisosDePartida
    AvisosDePartida o-- "0..*" ObservadorDePartida : avisa
    Partida ..> EventoDePartida : crea
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
  junto con los `Recuerdo` que devuelve el árbitro. `Partida` es el cuidador: los guarda y los
  devuelve sin saber qué contienen.
- **Null Object:** `SinReloj` evita preguntar "¿hay reloj?" en cada jugada.
- **Decorator:** `DisposicionDeFutbol` envuelve cualquier disposición y le agrega la cancha.
- **Puerto de salida:** `FuenteDeTiempo` es la única forma en que el núcleo conoce la hora.
