# Finales de partida reloj e historial

## Alcance acordado

Implementar la orden del arquitecto en su secuencia: preparación, resultado,
rendición, ofertas de tablas, reloj opcional, consultas e historial y verificación.
Se omite el UML por instrucción del usuario. No incluye UI, bot ni persistencia.
La conexión futura a base de datos será por un puerto y un adaptador coordinados
desde la capa de aplicación; Partida expone únicamente lecturas del historial.

## Estado previo y respuestas requeridas

1. El fin se representa con EstadoPartida.estaFinalizada(). El ganador se deduce
   en Partida.ganador() únicamente para JAQUE_MATE: gana el opuesto al turno.
2. Partida guarda el turno en un campo Color, inicialmente BLANCO, y lo alterna
   después de una jugada válida.
3. Partida.mover() verifica primero el estado final y lanza
   MovimientoInvalidoException con motivo PARTIDA_FINALIZADA.

## Enfoque y compatibilidad

Reemplazar EstadoPartida rompería sus consumidores y tests. Se conserva como API
existente y se amplía con RENDICION, TIEMPO_AGOTADO y TABLAS_ACUERDO. Un nuevo
ResultadoPartida inmutable separa Optional<Color> ganador y MotivoFinalizacion.
Partida.resultado() es vacío mientras se juega, también durante el jaque.
Los finales automáticos conservan sus reglas, prioridad y ganador actuales.
Se centraliza el cierre para registrar resultado, borrar ofertas y detener reloj.
El enum sigue siendo cerrado: agregar motivos exige actualizar estados y mensajes.

## Órdenes y consultas

- rendirse(Color): cualquiera de los dos colores, sin exigir turno, solo en curso.
- ofrecerTablas(Color): únicamente el color en turno, sin otra oferta pendiente.
- aceptarTablas(Color) / rechazarTablas(Color): solo el rival del oferente.
- Una jugada válida del rival borra la oferta; una inválida la conserva.
- verificarTiempo(): idempotente tras finalizar; antes del final, pierde por tiempo
  el color en turno si tiene cero restante. mover() también comprueba el tiempo.
- El resto de órdenes también verifica tiempo antes de actuar para conservar un
  único criterio de prioridad; un final ya registrado nunca se reemplaza.
- Errores de órdenes: AccionInvalidaException con motivos tipados; se conserva la
  excepción existente para movimientos. Color es la identidad de bando del dominio,
  no un mecanismo de autenticación.
- ofertaTablasPendiente(): Optional<Color>; historial(): copia inmutable de los
  Movimiento realmente ejecutados; tiempoRestante(Color): Optional<Duration>,
  vacío significa partida sin límite. Las lecturas no finalizan la partida.

## Reloj

Puerto FuenteDeTiempo.ahoraEnNanos(), con contrato monotónico. El adaptador real usa
System.nanoTime(); el falso de tests se avanza manualmente. Duration expresa
duraciones, sin acceder al sistema desde el núcleo.
RelojPartida define iniciar, cambiarTurno, detener, tiempoRestante y tiempoAgotado.
RelojAjedrez recibe duraciones positivas por color y la fuente. SinReloj aplica
Null Object. Se conserva cada constructor existente y se agregan sobrecargas
con reloj inyectado. Cada instancia de reloj pertenece a una sola partida.
La partida inicia el reloj con blancas, lo alterna únicamente tras jugadas válidas
y lo detiene en todos los finales, incluso si el tablero inicial ya es terminal.
No hay hilos, sleeps, pausas ni incrementos. El tiempo nunca se muestra negativo.
Se simplifica el vencimiento: siempre gana el rival, sin evaluar material de mate.

## Verificación

Los 182 tests originales deben pasar antes y después de los cambios. Cada bloque
lleva un ciclo de tests primero y ejecución antes y después de implementarlo.
Cubrir ambos colores, acciones fuera de turno, órdenes tras finalizar, ofertas
propias, intentos inválidos, exacto cero, reloj detenido y lecturas inmutables.
Inspeccionar imports y llamadas del núcleo al final. Documentar cada decisión en
formato qué / por qué / cuándo se rompería.
