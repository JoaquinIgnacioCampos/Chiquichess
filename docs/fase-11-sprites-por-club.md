# Fase 11: sprites de piezas por club

## Qué

Cada club tiene su propio juego de piezas, con una versión blanca y otra negra de cada una.

- `RepresentacionPiezas` pasó a ser una interfaz. El dibujo con símbolos siguió como `RepresentacionUnicode`.
- `CatalogoSprites` busca las imágenes de un club y las carga una sola vez.
- `RepresentacionConSprites` usa el catálogo del club de cada bando. Si no encuentra el archivo, delega en
  `RepresentacionUnicode`.
- `AplicacionAjedrez` arma esa representación con los equipos ya asignados por color, así que la revancha
  cambia los sprites junto con los colores.

## Archivos

Van en `src/main/resources/sprites/<club>/`, donde `<club>` es `boca`, `river`, `independiente` o `argentina` (el nombre de
su constante de `Equipo` en minúsculas).
Cada carpeta lleva estos 12 archivos:

| Pieza | Versión blanca | Versión negra |
|---|---|---|
| Rey | `king-white.png` | `king-black.png` |
| Reina | `queen-white.png` | `queen-black.png` |
| Torre | `rook-white.png` | `rook-black.png` |
| Alfil | `bishop-white.png` | `bishop-black.png` |
| Caballo | `knight-white.png` | `knight-black.png` |
| Peón | `pawn-white.png` | `pawn-black.png` |

El nombre dice el color de la pieza en la partida, no el del club, y va siempre en masculino.

Recomendaciones para las imágenes:

- PNG cuadrado de 256×256, con fondo transparente. Se dibuja al 85 % del lado de la casilla.
- La figura centrada y con la misma ocupación en todas las piezas, para que no cambie de escala entre sí.
- Las versiones blanca y negra de todos los clubes comparten los mismos elementos distintivos de color.
  Tienen que leerse sobre casillas claras y oscuras, y distinguirse también en River contra Independiente.

## Por qué

- **El núcleo no cambia.** Una `Pieza` no sabe de clubes: el equipo por color se conoce en el ensamblado.
- **Dos versiones por pieza.** Un club puede jugar con cualquier color, y el color tiene que seguir siendo
  evidente aunque los dos clubes compartan colores.
- **Respaldo Unicode.** Permite sumar los sprites de a poco, y una pieza sin imagen sigue siendo jugable.
- **Las imágenes se reutilizan.** `VistaTablero` reconstruye sus casillas en cada redibujado; cada una crea
  un `ImageView` nuevo sobre la misma `Image`.

## Cuándo se rompería

- Un archivo con el nombre mal escrito no da error: la pieza aparece con su símbolo Unicode.
- Una pieza nueva necesita su entrada en `CatalogoSprites`. Hasta entonces usa el respaldo.
- Un club nuevo necesita su carpeta, con el nombre de su constante de `Equipo` en minúsculas.
- Para elegir entre piezas clásicas y de club haría falta una opción en el menú.
