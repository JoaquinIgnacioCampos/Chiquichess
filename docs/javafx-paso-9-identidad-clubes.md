# Paso 9: identidad de los clubes

Las tarjetas muestran el escudo original de Boca, Independiente o River en el lugar de la insignia con peón. Debajo del nombre del jugador aparece el club, conservando la indicación de blancas/negras. Los fondos y acentos de las tarjetas representan los colores del club: azul/dorado para Boca y rojo/blanco para River e Independiente. El tablero y el marcador central mantienen su distribución y tema. El verde sigue identificando el turno y el reloj activo.

Los botones de selección de equipo del menú también incluyen los escudos. Las tres imágenes proporcionadas por el usuario se copiaron sin modificar a `src/main/resources/escudos` y se cargan por classpath, sin depender de Descargas ni de rutas absolutas del equipo.

## Ensamblado y presentación

`EnsambladorPartida.prepararConfigurada` resuelve la elección o sorteo una sola vez y devuelve `PartidaConfigurada`, que contiene partida y equipos por color. Esa asignación usa el mismo resultado que determina los nombres de los jugadores: el club sigue a Jugador 1/Jugador 2 aunque intercambien blancas/negras. `crearConfigurada` conserva su firma y devuelve la partida de ese resultado.

`PresentacionClub` contiene el catálogo de escudos y temas. `AplicacionAjedrez` construye las identidades e inyecta el mapa por color en `VistaPartida`, `PanelEstadoPartida` y `TarjetaJugador`. Las vistas no sortean ni deducen equipos a partir de nombres. Los constructores anteriores siguen disponibles para vistas sin identidad de club.

En tarjetas compactas se reducen el escudo y los textos para conservar el espacio de nombres, club, color, relojes y botones. Las confirmaciones existentes reutilizan el tema de la tarjeta.

## Verificación

Pasaron los 283 tests existentes. Esta iteración no modifica el núcleo ni agrega o modifica tests. Un programa temporal externo verificó 144 asignaciones entre equipos y colores fijos/sorteados, el arranque desde el menú real, gráficos en las opciones de equipo, imágenes y nombres de los tres clubes, ubicación a 900 × 620, ofertas/rechazo de tablas y final por rendición. Se revisaron capturas grandes y compactas. Los hashes SHA-256 de los recursos coinciden con los PNG originales. El programa temporal y sus clases se eliminan al finalizar.

Ejecutar la configuración **ChiquiChess** de IntelliJ para abrir el menú y elegir los equipos.
