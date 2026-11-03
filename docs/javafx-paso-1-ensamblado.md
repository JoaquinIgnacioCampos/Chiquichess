# Ensamblado de la interfaz

## Qué

EnsambladorPartida es el único punto de construcción del grafo del juego utilizado
por la consola y JavaFX. Construye tablero, jugadores, detector de jaque,
movimientos legales, condiciones de finalización y reloj. Se crea un grafo nuevo
por partida, incluyendo las condiciones que guardan memoria de posiciones.

AplicacionAjedrez solicita una partida con reloj real. Desde el paso 3 la inyecta
en ControladorTablero y entrega ese controlador por constructor a VistaPartida.
JavaFX instancia la clase Application mediante su ciclo de vida;
esa clase es el arranque, no una vista. VistaPartida recibe su dependencia y no
construye partidas, relojes ni reglas. Por ahora la vista permanece vacía.

El arranque gráfico usa cinco minutos por jugador, configurados provisionalmente
en TIEMPO_POR_JUGADOR. La consola conserva su funcionamiento sin límite de tiempo.

## Por qué

Es inyección de dependencias manual: el ensamblado elige las implementaciones y
las pasa a quienes las necesitan. La vista utiliza el controlador inyectado; este
consulta y envía las órdenes a Partida. El reloj mutable y la fuente del sistema
permanecen fuera de las vistas.

La Tarea 1 ya está implementada, por lo que se utilizan rendición, tablas y reloj
reales desde el principio. Las pruebas sustituyen la FuenteDeTiempo mediante el
puerto existente, sin sustituir las reglas. No se introduce un contenedor de DI.

## Cuándo se rompería

Al agregar el menú, nombres, equipos o tiempos distintos se deberá ampliar la
configuración del ensamblado. Si se reemplaza una implementación concreta de las
reglas o del reloj, su elección se cambia aquí; no en las vistas. Una futura
vista de nueva partida deberá pedir su creación al arranque o a un colaborador
inyectado, en lugar de ejecutar new Partida dentro de la vista.

## Comprobaciones

EnsambladorPartidaTest comprueba el tablero inicial, el uso de la fuente inyectada,
el final real por tiempo, el mate sin reloj, la independencia de las condiciones
de repetición y las operaciones reales de rendición y acuerdo.

La suite completa pasa con 239 tests (los 234 anteriores y cinco nuevos). Se
comprobó la consola hasta jaque mate y la creación de la ventana ChiquiChess con
el arranque nuevo; Windows informó una ventana válida y un proceso respondiendo.
El núcleo no tuvo modificaciones en este paso.
