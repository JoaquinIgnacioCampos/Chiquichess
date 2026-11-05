# Paso 7: ofertas de tablas

«Ofrecer tablas» aparece junto a «Rendirse», abajo a la derecha de cada tarjeta. Ambos botones se ubican en una fila propia, con distribución compacta para ventanas pequeñas.

Solo se puede ofrecer durante el turno propio y sin otra oferta pendiente. Pulsar el botón abre una confirmación integrada en la tarjeta, con «Cancelar» y «Ofrecer tablas». Cancelar o pulsar Escape no envía órdenes. Mover antes de confirmar cierra la confirmación al cambiar el turno.

Después de confirmar, la tarjeta del oferente muestra «Esperando respuesta». La del rival presenta «Te ofrecieron tablas», con «Rechazar» y «Aceptar». Escape en este panel equivale a rechazar. La respuesta se admite aunque no sea el turno del receptor, según el contrato existente del núcleo.

Aceptar termina por acuerdo, sin ganador. Rechazar elimina la oferta y permite seguir jugando. El tablero permanece disponible y los relojes siguen corriendo. La oferta persiste si mueve el oferente; si mueve el rival, el núcleo la descarta. Un final por tiempo, rendición u otra causa elimina la oferta y cierra los paneles pendientes.

## Conexión

`ControladorTablero` delega `ofrecerTablas`, `aceptarTablas`, `rechazarTablas` y `ofertaTablasPendiente` a `Partida`; los rechazos conservan sus motivos tipados. No se modificaron reglas ni clases del núcleo en este paso.

`PanelEstadoPartida` presenta la oferta vigente del núcleo. `TarjetaJugador` conserva únicamente qué confirmación se está mostrando. `ConfirmacionAccionJavaFx` reemplaza el componente específico de rendición para reutilizar el estilo en los tres paneles. Las conexiones se hacen desde `AplicacionAjedrez`, mediante `VistaPartida.configurarRendicion()` y `configurarTablas()`.

## Verificación

Pasaron los 283 tests existentes, incluidos los contratos del núcleo para ofertas, movimientos y relojes. No se agregaron ni modificaron tests en este paso. Las comprobaciones nuevas de JavaFX usaron un programa temporal externo: botones contiguos, cancelación y Escape, confirmación previa a enviar, ambos colores, turno, espera, aceptación y rechazo, movimientos del oferente/rival, respuesta obsoleta, rendición con oferta, reloj y vencimiento, sin ventanas nuevas y capturas a tamaño normal y 900 × 620. El programa y sus clases se eliminan tras comprobarlos.

Para abrir la aplicación, ejecutar **ChiquiChess** en IntelliJ.
