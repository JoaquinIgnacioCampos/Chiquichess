# Fase 7: captura al paso

## Qué

`CapturaAlPaso` (`Nucleo.reglas`) es una segunda `ReglaEspecial`. Si la última jugada fue un peón rival
que avanzó dos casillas y quedó al lado de un peón propio, ese peón puede ir en diagonal a la casilla
que el rival "saltó" y capturarlo. La jugada es `[Retiro del peón rival en su casilla, Traslado del
peón propio]`: la pieza capturada no está en el destino. `ReglasDePartida.clasicas()` la incluye.

## Por qué

- **Sin cambios en el resto del núcleo.** Es una clase nueva que se suma a la lista de reglas
  especiales: no se tocó `Partida`, `Jugada`, `MovimientosLegales` ni la huella. Es el caso que
  justificó la fase 3: una captura en otra casilla que el destino es solo otra combinación de efectos.
- **"Solo en la jugada siguiente" sale del historial.** La regla mira únicamente la última jugada, así
  no hay un derecho de captura al paso que guardar ni que restaurar al deshacer.
- **La clavada horizontal se resuelve sola.** Si al sacar los dos peones de la fila el rey queda en
  jaque, la jugada se rechaza con `DEJA_AL_REY_EN_JAQUE`, porque `MovimientosLegales` simula la jugada
  completa, con el retiro incluido.

## Cuándo se rompería

- La regla reconoce el avance doble como "un peón que avanzó dos filas en la misma columna". Una
  variante con peones que avanzaran tres casillas necesitaría otra definición.
- La repetición no distingue el derecho de captura al paso (simplificación documentada en la fase 4b).
- Los atajos de Partida con detector y condiciones no incluyen reglas especiales; el juego real usa
  `ReglasDePartida.clasicas()`.

## Verificación

- 403 tests: 12 nuevos (8 de la regla y 4 en partidas reales: la captura, que se pierde si no se
  aprovecha enseguida, deshacerla, y la clavada horizontal).
- Prueba aleatoria de deshacer/rehacer fuera del repo: 2000 partidas con 22 capturas al paso y 170
  enroques, todas consistentes con partidas rejugadas sin deshacer.
- Siguen siendo 2 los ciclos entre paquetes.
