# Fase 4: condiciones de fin sin estado propio

## Qué

`ContextoDeJugada` ahora incluye la lista de jugadas ejecutadas en la partida. La pieza movida y si
hubo captura ya no se guardan aparte: `piezaMovida()`, `huboCaptura()` y `ultimaJugada()` se deducen
de la última jugada, así no pueden contradecir al historial.

- **TablasPorReglaDeLos50Movimientos** cuenta, desde la última jugada hacia atrás, cuántas jugadas
  seguidas hubo sin captura ni movimiento de peón. Ya no tiene un contador.
- **TablasPorRepeticion** toma una copia del tablero actual y la revierte jugada por jugada (cada
  `Jugada` sabe deshacerse) para reconstruir las posiciones anteriores y contar cuántas veces aparece
  la actual. Ya no tiene un mapa de apariciones. Solo retrocede hasta la última captura o movimiento
  de peón: las piezas capturadas no vuelven y los peones no retroceden, así que ninguna posición
  anterior a esa jugada puede repetirse.

`CondicionDeFinalizacion` documenta el nuevo contrato: el resultado depende solo del contexto.

## Por qué

- **Para poder deshacer.** Con contadores internos, deshacer una jugada dejaba a las condiciones
  desincronizadas: la repetición seguiría contando una posición que ya no existe. Ahora, si el
  historial pierde una jugada, las condiciones lo ven solas.
- **Funciones puras, más fáciles de probar.** Con el mismo contexto responden siempre lo mismo;
  los tests arman un historial y comparan, sin depender del orden de las llamadas anteriores.
- **Sin "una instancia por partida".** Ya no hay riesgo de que dos partidas compartan memoria por
  usar la misma condición.

## Cuándo se rompería

- El corte de la repetición en la última jugada irreversible supone que las piezas capturadas no
  vuelven al tablero y que los peones solo avanzan. Una variante que reingrese piezas (por ejemplo,
  "cambios" que devuelvan piezas capturadas) tendría que quitar ese corte.
- La identidad de una posición sigue sin incluir los derechos de enroque ni de captura al paso. Al
  agregar esas jugadas (fases 6 y 7), dos posiciones con las mismas piezas pero distintos derechos
  se van a considerar iguales. Es una simplificación aceptada.
- Si una condición futura necesitara información que no está en el historial (por ejemplo, el
  tiempo usado en cada jugada), habría que agregarla al contexto en lugar de guardarla en la condición.

## Verificación

- 322 tests sin fallos. Los tests de las dos condiciones se reescribieron para el contrato nuevo
  (antes acumulaban llamadas sucesivas; ahora reciben un historial): 7 de repetición y 8 de 50
  movimientos, incluidos "no guarda memoria entre evaluaciones", "no modifica el tablero" y una
  repetición con una captura en el historial.
- Los tests de integración (repetición y 50 movimientos en partidas reales) pasaron sin cambios.
- Prueba diferencial fuera del repo: 1000 partidas aleatorias con semilla fija, jugadas con la
  versión anterior y con la nueva, dieron exactamente la misma secuencia de estados en las
  437.531 jugadas, incluyendo 110 finales por repetición y 377 por la regla de 50 movimientos.
- Costo: la versión nueva tarda alrededor de un 20% más en esas 1000 partidas (centésimas de
  milisegundo por jugada), gracias al corte en la última jugada irreversible.
