# 2. Tablero, piezas y movimientos

**Composición sobre herencia:** una pieza es un color más una `ReglaMovimiento`. Las subclases de
`Pieza` solo eligen qué regla componer y cómo se llaman; no reescriben cómo se mueven. Por ejemplo,
la reina es `ReglaCompuesta(ortogonales, diagonales)`, y el peón envuelve deslizamientos con los
decoradores `SoloSiVacio`, `SoloSiCaptura` y `SoloSi`.

Una pieza nueva se agrega con una subclase de `Pieza` que combine reglas existentes. No hace falta
modificar ninguna clase.

```mermaid
classDiagram
    direction TB

    class Bando {
        <<enumeration>>
        BLANCO
        NEGRO
        +direccionAvance() int
        +opuesto() Bando
    }

    class Posicion {
        <<record>>
        +columna int
        +fila int
        +desplazada(dc, df) Posicion
    }

    class ITablero {
        <<interface>>
        +ancho() int
        +alto() int
        +contiene(Posicion) boolean
        +piezaEn(Posicion) Optional~Pieza~
        +piezas() Map
        +pelota() Optional~Posicion~
        +arcoEn(Posicion) Optional~Bando~
    }

    class Tablero {
        -piezas Map
        -pelota Posicion
        -arcos Map
        +Tablero(ancho, alto)
        +colocar(Posicion, Pieza)
        +quitar(Posicion) Optional~Pieza~
        +colocarPelota(Posicion)
        +marcarArco(Posicion, Bando)
        +copiar() Tablero
    }

    class DisposicionInicial {
        <<interface>>
        +crearTablero() Tablero
    }

    class Pieza {
        <<abstract>>
        -color Bando
        -regla ReglaMovimiento
        #Pieza(Bando, ReglaMovimiento)
        +destinosPosibles(ITablero, Posicion) Set~Posicion~
        +casillasAtacadas(ITablero, Posicion) Set~Posicion~
        +esRey() boolean
        +esPeon() boolean
        +esTorre() boolean
        +nombre()* String
        +codigo()* char
    }

    class Rey
    class Reina
    class Torre
    class Alfil
    class Caballo
    class Peon

    class ReglaMovimiento {
        <<interface>>
        +destinosPosibles(ITablero, Posicion, Bando) Set~Posicion~
        +casillasAtacadas(ITablero, Posicion, Bando) Set~Posicion~
    }

    class Deslizamiento {
        -deltaColumna int
        -deltaFila int
        -maxPasos int
    }
    class SaltoFijo {
        -desplazamientos int[][]
    }
    class ReglaCompuesta {
        -reglas List~ReglaMovimiento~
    }
    class ReglasBase {
        <<utility>>
        +ortogonales(maxPasos)$ ReglaMovimiento
        +diagonales(maxPasos)$ ReglaMovimiento
        +saltosDeCaballo()$ ReglaMovimiento
    }
    class SoloSiVacio {
        -regla ReglaMovimiento
    }
    class SoloSiCaptura {
        -regla ReglaMovimiento
    }
    class SoloSi {
        -condicion Condicion
        -regla ReglaMovimiento
    }

    ITablero <|.. Tablero
    Tablero o-- "0..*" Pieza : contiene
    Tablero ..> Posicion
    DisposicionInicial ..> Tablero : crea

    Pieza <|-- Rey
    Pieza <|-- Reina
    Pieza <|-- Torre
    Pieza <|-- Alfil
    Pieza <|-- Caballo
    Pieza <|-- Peon
    Pieza --> Bando
    Pieza *-- "1" ReglaMovimiento : se mueve con
    Pieza ..> ITablero : consulta

    ReglaMovimiento <|.. Deslizamiento
    ReglaMovimiento <|.. SaltoFijo
    ReglaMovimiento <|.. ReglaCompuesta
    ReglaMovimiento <|.. SoloSiVacio
    ReglaMovimiento <|.. SoloSiCaptura
    ReglaMovimiento <|.. SoloSi
    ReglaCompuesta o-- "1..*" ReglaMovimiento
    SoloSiVacio o-- ReglaMovimiento : decora
    SoloSiCaptura o-- ReglaMovimiento : decora
    SoloSi o-- ReglaMovimiento : decora
    ReglasBase ..> Deslizamiento : arma
    ReglasBase ..> SaltoFijo : arma
    ReglasBase ..> ReglaCompuesta : arma
```

## Qué arma cada pieza

| Pieza | Regla |
|---|---|
| Torre | `ortogonales(SIN_LIMITE)` |
| Alfil | `diagonales(SIN_LIMITE)` |
| Reina | `ReglaCompuesta(ortogonales, diagonales)` |
| Rey | `ReglaCompuesta(ortogonales(1), diagonales(1))`; el enroque es una `ReglaEspecial` |
| Caballo | `saltosDeCaballo()` |
| Peón | avance (`SoloSiVacio`) + avance doble (`SoloSi` fila inicial) + capturas (`SoloSiCaptura`) |

`DisposicionInicial` la implementan `DisposicionEstandar` y `DisposicionDeFutbol` (paquete `juego`,
ver [diagrama 5](05-partida.md)). Un tablero de otro tamaño es otra `DisposicionInicial`.
