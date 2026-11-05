# Menú inicial: primera iteración

**Objetivo:** mostrar al iniciar un menú basado en el boceto aprobado: portada 40 %, configuración 60 %, aspecto oscuro del marcador.

**Alcance:** ninguna selección inicial; modo Clásico obligatorio; tiempos 3, 5, 10 minutos o sin tiempo; amigo en misma pantalla e IA deshabilitada; equipos independientes para Jugador 1 y 2 sin repetición; elección de quién juega blancas o sorteo. Personaje y escudos reales pendientes del usuario.

**Arquitectura:** la vista recibe tema y callback. ConfiguracionPartida, sin JavaFX, valida los datos antes de entregarlos. AplicacionAjedrez ensambla partida y reloj únicamente al pulsar Jugar con configuración completa. La partida respeta tiempo y color/nombre. Los equipos quedan en la configuración para la siguiente iteración visual. El núcleo y la consola conservan sus contratos.

**Fuera de alcance:** temas de clubes durante la partida, revancha, botón de volver al menú. Estos puntos siguen en las iteraciones acordadas.

- [x] Crear configuración inmutable con opciones válidas y equipos distintos.
- [x] Implementar VistaMenuPartida con botones, selección excluyente y validación reactiva. Reservar imagen del personaje. Mantener accesibilidad por teclado y desplazamiento vertical en ventanas pequeñas.
- [x] Extender ensamblado manual para nombres/colores y tiempo elegidos; conservar arranques existentes.
- [x] Mostrar primero el menú y crear partida/reloj solo después de confirmar la configuración.
- [x] Ejecutar suite existente y verificación JavaFX temporal fuera del proyecto. No crear ni modificar tests de UI/adaptadores en esta iteración; no se cambia el núcleo.
- [x] Revisar código y capturas y eliminar herramientas temporales.
