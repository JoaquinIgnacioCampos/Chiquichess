# 3. Jugadas y efectos (Command)

Toda jugada es una lista de **efectos**, y cada efecto sabe aplicarse y revertirse. Así, todas las
jugadas tienen la misma forma:

- una captura común es `[Retiro, Traslado]`;
- un enroque son dos `Traslado`;
- la captura al paso es un `Retiro` en otra casilla que el destino;
- la promoción agrega un `Reemplazo`;
- en el modo fútbol se suman `MoverPelota` y `Pase`.

Por eso deshacer, la validación de jaque y la IA (que aplica y deshace jugadas imaginadas) funcionan
igual para todas.

```mermaid
classDiagram
    direction LR

    class Jugada {
        <<record>>
        +origen Posicion
        +destino Posicion
        +efectos List~Efecto~
        +normal(ITablero, Posicion, Posicion)$ Jugada
        +piezaMovida() Pieza
        +capturada() Optional~Pieza~
        +promocion() Optional~Pieza~
        +esPase() boolean
        +esGol() boolean
        +con(Efecto) Jugada
        +aplicar(Tablero)
        +revertir(Tablero)
    }

    class Efecto {
        <<sealed interface>>
        +aplicar(Tablero)
        +revertir(Tablero)
    }

    class Traslado {
        <<record>>
        +pieza Pieza
        +origen Posicion
        +destino Posicion
    }
    class Retiro {
        <<record>>
        +pieza Pieza
        +posicion Posicion
    }
    class Reemplazo {
        <<record>>
        +anterior Pieza
        +nueva Pieza
        +posicion Posicion
    }
    class MoverPelota {
        <<record>>
        +desde Posicion
        +hacia Posicion
        +gol boolean
    }
    class Pase {
        <<record>>
        +pasador Pieza
        +desde Posicion
        +receptor Pieza
        +hacia Posicion
    }

    class HuellaDePosicion {
        <<utility>>
        +de(ITablero, Bando)$ long
        +tras(long, Jugada)$ long
    }

    Jugada *-- "1..*" Efecto
    Efecto <|.. Traslado
    Efecto <|.. Retiro
    Efecto <|.. Reemplazo
    Efecto <|.. MoverPelota
    Efecto <|.. Pase
    Efecto ..> Tablero : modifica
    HuellaDePosicion ..> Jugada : recorre sus efectos
```

`HuellaDePosicion` calcula un número que identifica una posición (técnica de Zobrist) y lo actualiza
jugada a jugada. La usan las tablas por repetición y la IA.

`Efecto` es **sellada** a propósito: si se agrega un efecto nuevo, el compilador obliga a decidir cómo
lo tratan la huella y los rasgos de los eventos. Esa es la excepción consciente a "agregar sin
modificar" (ver [la decisión 5 del documento de justificación](../justificacion.md#5-la-jugada-como-lista-de-efectos-command)).
