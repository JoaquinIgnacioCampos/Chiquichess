# Fase 1: reorganización de paquetes del núcleo

## Qué

Se movieron cuatro clases y se renombró una, sin cambiar comportamiento:

| Clase | Antes | Después |
|---|---|---|
| `Posicion` | `Nucleo.juego` | `Nucleo.tablero` |
| `Color` → `Bando` | `Nucleo.piezas` | `Nucleo.bando` (paquete nuevo) |
| `ReglasBase` | `Nucleo.reglas` | `Nucleo.movimientos` |
| `MotivoFinalizacion` | `Nucleo.juego` | `Nucleo.reglas` |

Sus tests acompañan el movimiento: `PosicionTest` pasa a `tablero` y `ColorTest` a `bando/BandoTest`.
Se actualizaron todos los imports, los nombres calificados (`Nucleo.piezas.Color` en la UI) y las
menciones del tipo en `docs/`. Los nombres de variables y métodos (`color`, `pieza.color()`,
`jugador.color()`) se conservaron para no mezclar este refactor con un cambio de API.

## Por qué

Los paquetes del núcleo formaban 7 ciclos de dependencia. La causa principal era que tipos básicos
vivían en paquetes de alto nivel:

- `Posicion` es una coordenada del tablero (el propio Tablero valida sus límites y la usa como clave),
  pero estaba en `juego`, el paquete que coordina la partida. Tablero, piezas, movimientos y reglas
  tenían que depender de `juego`, que a su vez depende de todos ellos.
- `Color` representa un bando, no algo propio de las piezas: lo usan el reloj, los jugadores, el turno
  y las reglas. El reloj dependía de `piezas` solo para saber de qué bando es el tiempo, y
  `piezas` y `movimientos.peon` se usaban mutuamente. El nombre `Bando` además evita la colisión con
  `javafx.scene.paint.Color`, que obligaba a escribir el nombre calificado en la UI.
- `ReglasBase` es el catálogo de movimientos (ortogonales, diagonales, saltos de caballo) y se
  construye con clases de `movimientos`; en `reglas` creaba el ciclo `piezas` ↔ `reglas`.
- `MotivoFinalizacion` lo usa `EstadoPartida` (en `reglas`); estando en `juego` creaba el ciclo
  `juego` ↔ `reglas`.

Resultado: de 7 a 2 ciclos y de 23 a 20 dependencias entre paquetes. Las dependencias ahora van en
un solo sentido: `adaptadores → juego → reglas → piezas/movimientos/tablero → bando`.

## Cuándo se rompería

Quedan dos ciclos que son propios del dominio y se aceptan:

- `piezas` ↔ `tablero`: el tablero guarda piezas y cada pieza consulta el tablero para moverse.
- `piezas` ↔ `movimientos`: una pieza se define por sus reglas de movimiento, y las reglas miran
  qué pieza ocupa cada casilla para frenar o capturar.

Romperlos exigiría unir esos paquetes o introducir interfaces solo para cortar el ciclo, lo que
agregaría complejidad sin beneficio real. Si una variante necesitara otro concepto de bando (más de
dos jugadores, por ejemplo), `Bando` dejaría de ser un enum de dos valores y habría que revisar
`opuesto()` y la dirección de avance.

## Verificación

- 283 tests antes y después del cambio, sin fallos (lanzador de JUnit Platform 6, JDK 25 con `--release 21`).
- Compilan núcleo, consola, interfaz JavaFX y tests.
- Sin referencias restantes a `Nucleo.piezas.Color`, `Nucleo.juego.Posicion`,
  `Nucleo.reglas.ReglasBase` ni `Nucleo.juego.MotivoFinalizacion`; el `Color` de JavaFX quedó intacto.
- Análisis de dependencias entre paquetes recalculado sobre el código: 2 ciclos.
