# Paso 6: rendición desde las tarjetas

Cada tarjeta tiene un botón «Rendirse» en su esquina inferior derecha. Solo está habilitado el del jugador que tiene el turno. `Partida.rendirse(color)` también valida esta regla y rechaza al otro jugador con `NO_ES_SU_TURNO`.

El ensamblado en `AplicacionAjedrez` conecta las acciones mediante `VistaPartida.configurarRendicion()`. `ConfirmacionAccionJavaFx` es el panel integrado reutilizable que ofrece «Cancelar» y «Rendirse», sin abrir ventanas. Cancelar y Escape cierran la confirmación sin enviar órdenes. Al abrirla, el foco está en Cancelar. Este componente también se usa para las ofertas de tablas del paso 7.

La vista deriva el clic a `ControladorTablero.rendirse(color)` después de confirmar. El controlador pide verificar el tiempo y llama a `Partida.rendirse(color)`. El núcleo valida turno y estado, determina el ganador y detiene el reloj. La UI presenta la regla, pero no reemplaza su validación en el núcleo.

El reloj sigue corriendo durante la confirmación y el tablero permanece disponible. Si se mueve y cambia el turno, se cierra la confirmación anterior. Si vence el tiempo, se cierra y se conserva el resultado por tiempo. Tras finalizar se actualizan el marcador, el motivo, los relojes y la selección, y ambos botones quedan deshabilitados. El actualizador existente deja de emitir pulsos al detectar el final.

## Verificación

Los 283 tests pasan. Se actualizaron las pruebas del núcleo para la nueva regla y se adecuaron los escenarios existentes que antes rendían fuera de turno. No se agregaron archivos de tests de UI o adaptadores. La comprobación visual adicional usó un programa temporal externo al proyecto: confirmación integrada sin ventanas nuevas, cancelar, Escape, rendición de ambos colores, botones según turno, cierre al mover, reloj activo y vencimiento durante la confirmación, además de capturas en ventanas grandes y de 900 × 620. Se elimina el programa temporal después de comprobarlo.

Ejecutar la configuración **ChiquiChess** de IntelliJ para abrir la aplicación.
