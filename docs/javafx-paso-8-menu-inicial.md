# Paso 8: menú inicial, primera iteración

Al ejecutar ChiquiChess se presenta el menú en la misma ventana, con portada a la izquierda (40 %) y configuración a la derecha (60 %). El lugar del personaje usa un símbolo de ajedrez provisional hasta recibir la imagen. Se conserva el tema oscuro del marcador. En ventanas pequeñas, la configuración permite desplazarse verticalmente.

No hay selecciones iniciales. Para habilitar «Jugar» se deben elegir:

- Modo Clásico.
- Tiempo de 3, 5 o 10 minutos por jugador, sin incremento, o sin tiempo.
- Rival amigo en la misma pantalla. IA figura deshabilitada como «Próximamente».
- Boca, River o Independiente para cada jugador. El equipo seleccionado se deshabilita en la selección del otro jugador; cambiarlo o quitar la selección libera la opción anterior.
- Jugador 1 con blancas, Jugador 2 con blancas o Aleatorio.

`VistaMenuPartida` recibe tema y callback; no construye partidas. Cuando la selección está completa emite `ConfiguracionPartida`, un record independiente de JavaFX que también rechaza equipos repetidos y la IA no disponible.

`AplicacionAjedrez` ensambla la partida y el actualizador solamente después de «Jugar», y cambia la raíz de la misma escena. `EnsambladorPartida.crearConfigurada` respeta el tiempo y asigna los nombres fijos Jugador 1/Jugador 2 a los colores elegidos. El sorteo se realiza una sola vez al crear la partida. Se conservan las vías de ensamblado anteriores, incluida la consola. No se modificó el núcleo.

En esta primera iteración los equipos se eligen y validan en el menú. Su presentación y los escudos se incorporaron después en el paso 9; la revancha y volver al menú, en el paso 10. La imagen real del personaje sigue pendiente. El tablero conserva el diseño existente.

## Verificación

Pasaron los 283 tests existentes. No se agregaron ni modificaron archivos de tests en esta iteración. La comprobación adicional usó un programa temporal externo al proyecto: arranque real en menú, ausencia de selecciones, bloqueo de Jugar, IA no disponible, exclusión/liberación de equipos, configuración emitida, nombres y colores, cuatro tiempos, sorteo, validación del record, proporción 40/60 y acceso a Jugar a 900 × 620. Se revisaron capturas del menú vacío, completo y compacto. El programa y sus clases se eliminan al terminar.

Para verificarlo, ejecutar la configuración **ChiquiChess** en IntelliJ.
