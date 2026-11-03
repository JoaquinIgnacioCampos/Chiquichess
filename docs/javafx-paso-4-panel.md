# Panel de estado con estilo de marcador

## Diseño aprobado

El tablero ocupa la zona izquierda del 65% de la ventana y el panel derecho el
35%. Dentro del panel: visitante con negras arriba, estado de partida en el
centro y local con blancas abajo. Fondo oscuro, textura de puntos, texto LED,
acento ámbar y borde verde para el jugador activo, como en la maqueta aprobada.
El tablero conserva sus piezas Unicode y los colores del tema actual.

## Qué por qué y cuándo cambiaría

**Qué:** consulta de solo lectura Partida.jugador(Color), panel con datos del
controlador, tema de presentación independiente y dibujo de letras LED en Canvas.
La zona del tablero contiene también el título y los mensajes de rechazo.

**Por qué:** ambos nombres, turno, jaque, ganador y motivo vienen del núcleo.
La UI no guarda un resultado propio ni deduce el ganador a partir del turno.
Las proporciones se aplican al ancho de la ventana completa, sin un ancho fijo
para el panel. Las letras se ajustan al espacio sin descargar tipografías.

**Cuándo cambiaría:** equipos, escudos y relojes se incorporarán en sus pasos.
Local/visitante es solo presentación de blancas/negras. El panel se actualiza
al redibujar tras una interacción o cambio externo de la partida. La verificación
periódica del tiempo sigue perteneciendo al futuro paso del reloj visual.

## Comprobaciones

282 tests pasan (269 anteriores, dos nuevos de la consulta de jugadores y once
del panel). Cubren cambios de turno por clic, cancelación sin cambio de turno,
jaque y resolución, mate, rendición fuera de turno, los motivos de tablas, rechazo
por tiempo y relación 35/65 a tres tamaños. Los fixtures UI previos se adaptaron
al tema del panel inyectado, conservando sus comprobaciones.

Se renderizaron e inspeccionaron las escenas de inicio, jaque y final a 1280x820,
además de una escena de 900x620 con nombres de jugadores de mayor longitud. Se
ajustó la tipografía secundaria y se centró el motivo del resultado. La consola
conserva el mate del loco. Revisión independiente sin hallazgos accionables.

La consulta jugador(Color) es el único cambio del núcleo; no cambia reglas ni
incorpora dependencias gráficas. Los nombres se ajustan a varias líneas; un límite
o truncado de entradas arbitrariamente largas deberá definirse con el futuro menú.
