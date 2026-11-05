# Paso 10: volver al menú y revancha

El botón claro «Volver al menú» aparece junto a ChiquiChess, en la cabecera del tablero. Durante una partida abre una confirmación integrada sobre el tablero; no crea ventanas. Cancelar o Escape cierran la confirmación y conservan tablero, selección y ofertas. El reloj sigue corriendo mientras se decide. Escape funciona aunque el foco haya salido del cuadro. Si la partida finaliza con la confirmación abierta, se cierra y se presenta el resultado.

Confirmar la salida descarta la partida sin registrar una rendición. El ensamblado detiene su actualizador visual y su reloj real y cambia a una instancia nueva del menú, sin selecciones. Tras el final, volver al menú es directo.

## Revancha

«Revancha» se muestra en el marcador lateral únicamente al finalizar. Crea otra partida desde el ensamblado, con los mismos nombres de jugadores, equipos, modo, rival y tiempo inicial. Intercambia los colores realmente jugados: si la partida original se sorteó, no vuelve a sortear. Cada revancha posterior invierte la asignación de la anterior.

La partida nueva empieza con tablero inicial, turno de blancas, historial vacío, sin selección ni oferta pendiente y con un reloj nuevo (o sin límite si así se había elegido). La partida anterior se cierra antes de ensamblar la siguiente. Los callbacks comprueban que aún pertenecen a la partida actual.

## Responsabilidades

`NavegacionPartida` presenta botones y confirmación y consulta el estado del núcleo para habilitar la revancha. `VistaPartida` integra la cabecera, el cuadro y el manejo de Escape. `PanelEstadoPartida` aloja el botón de revancha.

`AplicacionAjedrez` recibe las solicitudes, crea pantallas y partidas y centraliza `cerrarPartidaActual`. `PartidaConfigurada` conserva el puerto `RelojPartida` usado durante el ensamblado e implementa `AutoCloseable`: su cierre detiene ese reloj sin inventar un resultado de ajedrez. Cerrar la ventana o detener la aplicación también libera ambos relojes/actualizadores de forma idempotente. No se modificó el núcleo.

## Verificación

Pasaron los 283 tests existentes. No se agregaron ni modificaron tests en esta iteración. El comprobador temporal externo verificó confirmación/cancelación/Escape desde fuera del cuadro, reloj en marcha mientras se decide y congelado al salir, Timeline detenido, descarte sin resultado, menú limpio, revanchas múltiples con colores fijos y sorteados, asociación de clubes, sin tiempo, tiempo inicial restaurado, estado nuevo, ubicación a 900 × 620 y cierre repetido de la aplicación. Se revisaron capturas de partida, confirmación y final. Se elimina el programa temporal y sus clases tras verificar.

Ejecutar la configuración **ChiquiChess** en IntelliJ para probarlo.
