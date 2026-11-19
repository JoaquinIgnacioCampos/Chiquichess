# 4. Reglas: movimientos legales, reglas especiales y finales (Strategy)

Las reglas se agrupan en `ReglasDePartida`, que es lo que recibe `Partida`. Hay dos puntos de
extensión, y los dos son el patrón Strategy:

- **`ReglaEspecial`** suma jugadas (enroque, captura al paso, pase) o transforma las que ya existen
  (promoción, gol).
- **`CondicionDeFinalizacion`** decide si la partida terminó y quién ganó.

Las condiciones que necesitan memoria (repetición, 50 movimientos) implementan `CondicionConMemoria`
y devuelven un `Recuerdo` en cada jugada (Memento), para poder deshacer.

```mermaid
classDiagram
    direction TB

    class ReglasDePartida {
        <<record>>
        +movimientosLegales MovimientosLegales
        +condiciones List~CondicionDeFinalizacion~
        +permiteDeshacer boolean
        +clasicas()$ ReglasDePartida
        +futbol(limiteDeJugadas)$ ReglasDePartida
        +conDeshacer() ReglasDePartida
        +detector() IDetectorDeJaque
    }

    class MovimientosLegales {
        -detector IDetectorDeJaque
        -especiales List~ReglaEspecial~
        +jugadasPosibles(Tablero, Posicion, historial) List~Jugada~
        +jugadaPosible(Tablero, origen, destino, historial) Optional~Jugada~
        +destinos(Tablero, Posicion, historial) Set~Posicion~
        +existeAlguno(Tablero, Bando, historial) boolean
        +dejaAlReyEnJaque(Tablero, Jugada) boolean
    }

    class IDetectorDeJaque {
        <<interface>>
        +estaEnJaque(ITablero, Bando) boolean
    }
    class DetectorDeJaque

    class ReglaEspecial {
        <<interface>>
        +jugadas(Tablero, Posicion, historial, IDetectorDeJaque) List~Jugada~
        +transformar(Tablero, Jugada) List~Jugada~
    }
    class Enroque
    class CapturaAlPaso
    class Promocion {
        -opciones List~Function~
    }
    class ReglaDeLaPelota {
        -columnasDelArco Set~Integer~
        -saqueInicial Posicion
        +prepararCancha(Tablero)
    }

    class CondicionDeFinalizacion {
        <<interface>>
        +evaluar(ContextoDeJugada) Optional~EstadoPartida~
        +ganador(EstadoPartida, ContextoDeJugada) Optional~Bando~
        +limiteDeJugadas() Optional~Integer~
    }
    class CondicionConMemoria {
        <<interface>>
        +registrar(ContextoDeJugada) Recuerdo
        +restaurar(Recuerdo)
    }
    class Recuerdo {
        <<interface>>
    }
    class JaqueMate
    class Ahogado
    class VictoriaPorGoles
    class TablasPorRepeticion
    class TablasPorReglaDeLos50Movimientos

    class ContextoDeJugada {
        <<record>>
        +tablero ITablero
        +turno Bando
        +jugadas List~Jugada~
        +enJaque boolean
        +tieneMovimientosLegales boolean
    }

    class Marcador {
        <<record>>
        +blancas int
        +negras int
        +de(List~Jugada~)$ Marcador
    }

    class EstadoPartida {
        <<enumeration>>
        EN_CURSO
        JAQUE
        JAQUE_MATE
        TABLAS_AHOGADO
        TABLAS_REPETICION
        TABLAS_REGLA_50_MOVIMIENTOS
        RENDICION
        TIEMPO_AGOTADO
        TABLAS_ACUERDO
        VICTORIA_POR_GOLES
        TABLAS_EMPATE_EN_GOLES
    }
    class MotivoFinalizacion {
        <<enumeration>>
    }

    ReglasDePartida *-- MovimientosLegales
    ReglasDePartida *-- "1..*" CondicionDeFinalizacion
    MovimientosLegales o-- IDetectorDeJaque
    MovimientosLegales o-- "0..*" ReglaEspecial
    IDetectorDeJaque <|.. DetectorDeJaque

    ReglaEspecial <|.. Enroque
    ReglaEspecial <|.. CapturaAlPaso
    ReglaEspecial <|.. Promocion
    ReglaEspecial <|.. ReglaDeLaPelota

    CondicionDeFinalizacion <|.. JaqueMate
    CondicionDeFinalizacion <|.. Ahogado
    CondicionDeFinalizacion <|.. VictoriaPorGoles
    CondicionDeFinalizacion <|-- CondicionConMemoria
    CondicionConMemoria <|.. TablasPorRepeticion
    CondicionConMemoria <|.. TablasPorReglaDeLos50Movimientos
    CondicionConMemoria ..> Recuerdo : crea y restaura
    CondicionDeFinalizacion ..> ContextoDeJugada : evalúa
    CondicionDeFinalizacion ..> EstadoPartida
    EstadoPartida --> MotivoFinalizacion
    VictoriaPorGoles ..> Marcador
```

## Las dos combinaciones que existen hoy

| Fábrica | Reglas especiales (en orden) | Condiciones de fin |
|---|---|---|
| `ReglasDePartida.clasicas()` | `Enroque`, `CapturaAlPaso`, `Promocion` | `JaqueMate`, `Ahogado`, `TablasPorRepeticion`, `TablasPorReglaDeLos50Movimientos` |
| `ReglasDePartida.futbol(n)` | las clásicas + `ReglaDeLaPelota` (después de `Promocion`) | `JaqueMate`, `VictoriaPorGoles(3, n)`, `Ahogado`, `TablasPorRepeticion`, `TablasPorReglaDeLos50Movimientos` |

Una regla nueva es una clase nueva que se agrega a una de estas listas. No hace falta tocar
`Partida` ni `MovimientosLegales`.
