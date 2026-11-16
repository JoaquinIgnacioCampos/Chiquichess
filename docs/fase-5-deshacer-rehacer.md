# Fase 5: deshacer y rehacer jugadas

## Qué

En el núcleo (`Partida`):

- `deshacer()` revierte la última jugada: el tablero (la `Jugada` como Command), las condiciones
  con memoria (cada una restaura su `Recuerdo`, en orden inverso), el turno y el estado.
- `rehacer()` vuelve a ejecutar la última jugada deshecha. Hacer una jugada nueva borra lo que se
  podía rehacer.
- `puedeDeshacer()` y `puedeRehacer()` dicen si la orden se aceptaría, para que la interfaz
  habilite sus botones.
- `HistorialDeJugadas` (nuevo, en `Nucleo.juego`) guarda dos pilas: jugadas hechas, cada una con los
  recuerdos de las condiciones, y jugadas deshechas.
- `ReglasDePartida` suma `permiteDeshacer` (por defecto `false`) y `conDeshacer()`.
- `AccionInvalidaException.Motivo` suma `DESHACER_NO_PERMITIDO`, `NADA_PARA_DESHACER` y
  `NADA_PARA_REHACER`. `VistaPartida` agrega sus mensajes porque recorre todos los motivos.

Reglas acordadas:

| Situación | Resultado |
|---|---|
| Reglas sin deshacer, o partida con límite de tiempo | `DESHACER_NO_PERMITIDO` |
| Quién puede deshacer | Cualquiera de los dos jugadores |
| Partida terminada por jaque mate o tablas automáticas | Se puede deshacer: la partida se reanuda |
| Partida terminada por rendición, acuerdo o tiempo | No se puede: `PARTIDA_FINALIZADA` |
| Oferta de tablas pendiente | Se borra al deshacer y al rehacer |

La opción del menú y los botones son de la fase 8; esta fase solo agrega las reglas al núcleo.

## Por qué

- **Command + Memento.** Cada jugada sabe revertirse (fase 3) y cada condición con memoria sabe
  volver a su estado anterior (fase 4b). Deshacer es coordinar las dos cosas en orden inverso;
  no hace falta reconstruir la partida desde el principio.
- **La regla vive en el núcleo.** Igual que la rendición fuera de turno, que la interfaz deshabilita
  pero Partida igual rechaza, deshacer se valida en Partida: vale desde la interfaz, la consola,
  los tests o una futura IA.
- **Un final por una jugada se puede deshacer; una decisión de los jugadores no.** El mate o las tablas
  automáticas son consecuencia de una jugada: deshacer la jugada deshace el final. La rendición, el
  acuerdo y el tiempo agotado no dependen de una jugada. Partida distingue los dos casos según cómo
  terminó, sin depender de una lista de motivos: un final futuro decidido por una condición (por
  ejemplo, los goles del modo fútbol) se va a poder deshacer sin cambiar nada.
- **Ofertas simples.** Una oferta se hizo pensando en una posición; si la posición cambia por deshacer
  o rehacer, se descarta y quien quiera la vuelve a ofrecer.
- **Partida más chica.** El historial y las dos pilas pasaron a `HistorialDeJugadas`; Partida solo
  coordina.

## Cuándo se rompería

- **Con reloj.** Deshacer con tiempo permitiría ganar tiempo deshaciendo; por eso está prohibido. Si
  se quisiera, habría que decidir cómo se restaura el tiempo de cada jugador.
- **Deshacer sin acuerdo.** Cualquiera de los dos puede deshacer sin que el rival acepte. En una partida
  remota o competitiva habría que pedir confirmación al rival.
- **Ofertas.** Si se quisiera conservar o reconstruir la oferta al deshacer, habría que guardarla en el
  historial junto a cada jugada.
- **Orden de los recuerdos.** Partida asume que las condiciones con memoria registran y restauran en el
  mismo orden de `ReglasDePartida.condiciones()`. Si las reglas cambiaran durante la partida, ese
  orden dejaría de coincidir.

## Verificación

- 358 tests sin fallos: los 334 anteriores y 24 nuevos (6 de `HistorialDeJugadas` y 18 de deshacer y
  rehacer en Partida). Los de Partida usan el criterio del Command: después de deshacer, la partida
  (tablero, turno, estado, resultado e historial) es idéntica a la de antes de jugar.
- Casos cubiertos: capturas, varias jugadas, rehacer en orden, jugada nueva que borra rehacer, deshacer
  y rehacer un mate, rendición y acuerdo no deshacibles, sin habilitar, con reloj, ofertas, la
  repetición no se inventa al ir y volver, la regla de 50 movimientos recupera su cuenta, y cada
  condición recibe su propio recuerdo en orden inverso.
- Prueba aleatoria fuera del repo: 2000 partidas con deshacer y rehacer mezclados (715.416 jugadas,
  183.458 deshacer, 101 de ellos sobre partidas terminadas, 18.721 rehacer). Cada deshacer y rehacer
  dejó la partida idéntica a la esperada, y al final las 2000 coincidieron con una partida nueva que
  jugó la misma línea sin deshacer.
