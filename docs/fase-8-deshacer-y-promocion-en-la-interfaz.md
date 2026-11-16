# Fase 8: deshacer y promoción en la interfaz y la consola

La fase conecta con los jugadores lo que el núcleo ya sabía hacer desde las fases 5 (deshacer y
rehacer) y 7b (promoción). Se hizo en tres commits.

## Qué

### 1. Opción "Permitir deshacer" en el menú

- `ConfiguracionPartida` suma `permitirDeshacer`. Lanza una excepción si se pide junto con un límite de
  tiempo, porque deshacer solo se admite en partidas sin reloj.
- En `VistaMenuPartida`, la sección "DESHACER JUGADAS" solo se habilita con "Sin tiempo". Al elegir
  otro control de tiempo se apaga.
- `EnsambladorPartida` arma las reglas con `conDeshacer()` cuando se pide. La revancha conserva la
  opción.

### 2. Botones y comandos de deshacer

- `VistaPartida.configurarDeshacer()` agrega "Deshacer" y "Rehacer" a la cabecera, junto a "Volver al
  menú", solo si la partida lo permite (`Partida.permiteDeshacer()`). Los botones se habilitan según
  `puedeDeshacer()` y `puedeRehacer()`.
- `BotonDeCabecera` tiene el estilo común de los botones de la cabecera, que antes estaba en
  `NavegacionPartida`.
- La consola acepta "deshacer" y "rehacer". Después de un mate o de unas tablas automáticas ofrece
  deshacer en vez de terminar. `Main` arma la partida de consola con deshacer.

### 3. Elegir la pieza al promover

- `Partida.opcionesDePromocion(origen, destino)` devuelve las piezas a las que puede promover ese
  movimiento, en el orden de las reglas. Devuelve una lista vacía si el movimiento no es una promoción
  permitida.
- `ControladorTablero`: cuando el destino elegido es una promoción, no mueve. Deja una **promoción
  pendiente** (`promocionPendiente()`), que se completa con `promover(codigo)` o se descarta con
  `cancelarPromocion()`. Pulsar otra casilla, deshacer o rehacer también la descartan.
- `SelectorPromocion`: una tarjeta sobre el tablero con un botón por pieza (ids `promover-D`, `-T`,
  `-A`, `-C`) y "Cancelar". La tecla Escape también cancela. Se crea recién la primera vez que un peón
  promueve.
- Consola: un tercer dato de una letra elige la pieza (`e7 e8 C`). Sin él, el peón promueve a dama.

## Por qué

- **Los botones van en la cabecera.** Los dos jugadores pueden deshacer, así que no corresponde ponerlos
  en la tarjeta de uno solo.
- **Ocultar los botones en vez de deshabilitarlos.** Si la partida no admite deshacer, unos botones
  grises que nunca se habilitan solo confunden.
- **Las opciones de promoción las da el núcleo.** La interfaz no sabe qué piezas existen: muestra las
  que devuelve `opcionesDePromocion`. Si una variante cambia las opciones en `Promocion`, el selector
  las muestra sin cambios.
- **La espera vive en el controlador, no en la Partida.** Para el núcleo, una jugada se hace entera o
  no se hace. Esperar la elección del jugador es un asunto de la interfaz, como la selección de una
  casilla.
- **Selector integrado, no una ventana.** Sigue el mismo criterio que la confirmación de "Volver al
  menú": sin ventanas emergentes, dentro de la escena.
- **Creación diferida del selector.** Los botones de JavaFX necesitan el toolkit iniciado. Crear el
  selector recién cuando se usa mantiene la vista liviana, y los tests de vista existentes no
  necesitaron cambios.

## Cuándo se rompería

- Las opciones se comparan por código de pieza. Dos piezas de promoción con el mismo código serían
  indistinguibles, igual que en el núcleo.
- En la consola, el tercer dato tiene que ser una sola letra. Si una variante usara códigos más
  largos, habría que cambiar el formato.
- Con reloj no hay deshacer. Si una variante lo quisiera, habría que decidir qué pasa con el tiempo
  consumido.

## Verificación

- Parte 1: tests del menú, del ensamblado y de la configuración.
- Parte 2: 437 tests, 10 nuevos: 4 de los botones con una partida real, 5 de la consola y 1 del
  catálogo de mensajes.
- Parte 3: 451 tests, 14 nuevos:
  - 2 de `opcionesDePromocion`.
  - 6 del controlador: espera la elección, elegir, cancelar, pulsar otra casilla, promover sin
    pendiente y una jugada común.
  - 3 del selector en la vista.
  - 3 de la consola: elegir la pieza, la dama por defecto y las elecciones inválidas.
