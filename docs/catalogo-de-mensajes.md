# Catálogo de mensajes en los adaptadores

## Qué

Los textos que se muestran para los códigos del núcleo (rechazos de movimientos, rechazos de
acciones y motivos de final) salieron de las vistas y quedaron en un catálogo por adaptador:

- `adaptadores.ui.MensajesDeLaInterfaz`: textos de la interfaz gráfica. Admite textos que dependen
  de la acción intentada (por ejemplo, "fuera de turno" al rendirse o al ofrecer tablas) y, si no hay
  uno específico, usa el general del motivo.
- `adaptadores.MensajesDeConsola`: textos de la consola, con el nombre del ganador en los finales.

`VistaPartida`, `PanelEstadoPartida` y `JuegoConsola` reciben su catálogo por constructor (los
constructores anteriores usan el catálogo por defecto) y ya no tienen `switch` sobre códigos del
núcleo. Los textos no cambiaron.

## Por qué

- **Las vistas no cambian al agregar un código.** Antes había cinco `switch` repartidos en tres
  clases; un motivo nuevo (como los de deshacer) obligaba a editar vistas que no tenían nada que ver,
  incluso un método llamado `mostrarRespuestaTablas`. Ahora se suma una línea en cada catálogo.
- **Los textos no van al núcleo.** El núcleo informa con códigos; cada adaptador elige sus palabras
  (la interfaz usa tildes y voseo, la consola no). Poner los textos en el núcleo mezclaría reglas con
  presentación.
- **Se conserva la alarma del compilador.** El `switch` exhaustivo no compilaba si faltaba un caso.
  Ahora `MensajesDeLaInterfazTest` y `MensajesDeConsolaTest` recorren todos los códigos del núcleo y
  fallan indicando cuál no tiene texto. Si igual faltara uno en ejecución, se muestra un texto
  genérico en lugar de romper la ventana.

## Cuándo se rompería

- Si se necesitaran varios idiomas, convendría mover los textos a archivos de recursos
  (`ResourceBundle`) en lugar de escribirlos en el código del catálogo.
- Si muchos textos empezaran a depender del contexto (no solo de la acción), el catálogo necesitaría
  recibir más información o usar plantillas con parámetros.

## Verificación

- 368 tests sin fallos, 10 nuevos: los de los dos catálogos y `JuegoConsolaTest`, el primer test de la
  consola completa con entrada simulada.
- La salida de la consola en una partida con rechazos y jaque mate quedó idéntica byte por byte.
- Los tests de la interfaz que verifican los textos de final siguen pasando sin cambios.
- Prueba de la alarma: al borrar un texto del catálogo, el test falla con "Falta el texto de ...".
