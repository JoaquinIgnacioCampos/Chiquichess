# Fase 1: reorganización de paquetes del núcleo

## Qué

Se movieron cuatro clases y se renombró una, sin cambiar comportamiento:

| Clase | Antes | Después |
|---|---|---|
| `Position` | `core.game` | `core.board` |
| `Color` → `Side` | `core.pieces` | `core.side` (paquete nuevo) |
| `BaseRules` | `core.rules` | `core.movements` |
| `FinishReason` | `core.game` | `core.rules` |

Sus tests acompañan el movimiento: `PositionTest` pasa a `board` y `ColorTest` a `side/SideTest`.
Se actualizaron todos los imports, los nombres calificados (`core.pieces.Color` en la UI) y las
menciones del tipo en `docs/`. Los nombres de variables y métodos (`color`, `piece.color()`,
`player.color()`) se conservaron para no mezclar este refactor con un cambio de API.

## Por qué

Los paquetes del núcleo formaban 7 ciclos de dependencia. La causa principal era que tipos básicos
vivían en paquetes de alto nivel:

- `Position` es una coordenada del tablero (el propio Board valida sus límites y la usa como clave),
  pero estaba en `game`, el paquete que coordina la partida. Tablero, piezas, movimientos y reglas
  tenían que depender de `game`, que a su vez depende de todos ellos.
- `Color` representa un bando, no algo propio de las piezas: lo usan el reloj, los jugadores, el turno
  y las reglas. El reloj dependía de `pieces` solo para saber de qué bando es el tiempo, y
  `pieces` y `movements.pawn` se usaban mutuamente. El nombre `Side` además evita la colisión con
  `javafx.scene.paint.Color`, que obligaba a escribir el nombre calificado en la UI.
- `BaseRules` es el catálogo de movimientos (ortogonales, diagonales, saltos de caballo) y se
  construye con clases de `movements`; en `rules` creaba el ciclo `pieces` ↔ `rules`.
- `FinishReason` lo usa `GameState` (en `rules`); estando en `game` creaba el ciclo
  `game` ↔ `rules`.

Resultado: de 7 a 2 ciclos y de 23 a 20 dependencias entre paquetes. Las dependencias ahora van en
un solo sentido: `adapters → game → rules → pieces/movements/board → side`.

## Cuándo se rompería

Quedan dos ciclos que son propios del dominio y se aceptan:

- `pieces` ↔ `board`: el tablero guarda piezas y cada pieza consulta el tablero para moverse.
- `pieces` ↔ `movements`: una pieza se define por sus reglas de movimiento, y las reglas miran
  qué pieza ocupa cada casilla para frenar o capturar.

Romperlos exigiría unir esos paquetes o introducir interfaces solo para cortar el ciclo, lo que
agregaría complejidad sin beneficio real. Si una variante necesitara otro concepto de bando (más de
dos jugadores, por ejemplo), `Side` dejaría de ser un enum de dos valores y habría que revisar
`opposite()` y la dirección de avance.

## Verificación

- 283 tests antes y después del cambio, sin fallos (lanzador de JUnit Platform 6, JDK 25 con `--release 21`).
- Compilan núcleo, consola, interfaz JavaFX y tests.
- Sin referencias restantes a `core.pieces.Color`, `core.game.Position`,
  `core.rules.BaseRules` ni `core.game.FinishReason`; el `Color` de JavaFX quedó intacto.
- Análisis de dependencias entre paquetes recalculado sobre el código: 2 ciclos.
