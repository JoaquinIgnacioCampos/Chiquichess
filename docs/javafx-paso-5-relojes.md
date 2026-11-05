# Paso 5: relojes del panel

Cada tarjeta de jugador muestra el tiempo restante en formato `mm:ss`, con el reloj del turno actual en verde. La configuración inicial sigue siendo de cinco minutos por jugador y se define en `AplicacionAjedrez`.

`VistaReloj` presenta la duración que recibe del núcleo. Redondea hacia arriba los segundos únicamente para mostrarlos y usa «Sin límite» cuando la partida no tiene reloj.

`ActualizadorRelojJavaFx` es el adaptador de actualización visual. Un `Timeline` de JavaFX consulta cada 100 ms a través de `ControladorTablero`: primero pide a `Partida` verificar el vencimiento y después actualiza los relojes. No descuenta tiempo ni decide ganadores. El cálculo sigue en `RelojAjedrez` con la fuente monotónica `FuenteDeTiempoSistema`.

Los pulsos habituales actualizan solo los relojes: conservan los nodos del tablero y la selección. Al finalizar, el adaptador redibuja el estado final y se detiene. También se detiene al ocultar o cerrar la ventana. Las partidas sin límite no inician actualizaciones periódicas.

El ensamblado y las conexiones del ciclo de vida están en `AplicacionAjedrez`; las vistas reciben sus dependencias y no construyen partidas ni relojes.

## Verificación

Los 282 tests existentes pasan. Por indicación del usuario, este paso no incorpora archivos de tests ni modifica los existentes. La comprobación adicional se ejecutó con un programa temporal fuera del proyecto: pulso real de JavaFX con fuente de tiempo controlada, cambio de turno, conservación de selección y nodos, vencimiento, ganador, detención, partidas sin límite, formato y capturas en dos tamaños de ventana.

Para abrir la aplicación, ejecutar la configuración compartida **ChiquiChess** de IntelliJ (`javafx:run` con Java 21).
