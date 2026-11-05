# Navegación y revancha

**Objetivo:** botón blanco «Volver al menú» junto al título y «Revancha» al terminar.

**Comportamiento acordado:** durante la partida, volver pide confirmación integrada; cancelar conserva estado y tiempo en marcha; confirmar descarta sin registrar rendición y abre menú sin selecciones. Después del final, volver es directo. Revancha conserva jugadores, clubes, modo, rival y duración inicial; invierte los colores realmente jugados y reinicia todo el estado.

**Arquitectura:** navegación de presentación recibe callbacks del ensamblado. AplicacionAjedrez conserva la partida preparada actual, cierra actualización visual y reloj real antes de cambiar pantalla. La partida preparada expone cierre mediante el puerto RelojPartida, sin nuevas reglas en el núcleo.

- [x] Añadir cierre idempotente del reloj a PartidaConfigurada y centralizar limpieza en AplicacionAjedrez.
- [x] Crear adaptador visual de navegación con botón blanco, confirmación integrada, cancelación y estado final.
- [x] Integrar botón junto a marca y confirmación sobre tablero; revancha en marcador lateral al finalizar.
- [x] Construir cada revancha desde configuración anterior con asignación de colores invertida; limpiar menú en cada regreso.
- [x] Ejecutar suite existente, revisar código y verificar ida/vuelta, cancelación, final, reloj, sorteo, varias revanchas, sin tiempo y ventana pequeña con comprobador temporal fuera del proyecto. No agregar tests de UI/adaptadores ni modificar núcleo.
