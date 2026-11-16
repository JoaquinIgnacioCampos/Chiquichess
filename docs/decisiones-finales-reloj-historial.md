# Decisiones de finales de partida reloj e historial

## Situación inicial

Antes de este bloque, EstadoPartida representaba el fin mediante estaFinalizada().
Partida.ganador() solo contemplaba JAQUE_MATE y deducía el ganador como el color
opuesto al turno. El turno estaba en Partida.turno, inicialmente BLANCO.
Partida.mover() rechazaba partidas terminadas con MovimientoInvalidoException y
motivo PARTIDA_FINALIZADA. Estas consultas y ese rechazo se conservan.

La preparación creó la rama codex/finales-reloj-historial y fijó JUnit en 6.0.3.
Los 182 tests originales pasaron antes de ampliar el núcleo.

## Resultado y compatibilidad

**Qué:** ResultadoPartida es un record con Optional<Bando> ganador y
MotivoFinalizacion motivo. Partida.resultado() es vacío mientras la partida sigue,
incluido el jaque. Un resultado sin ganador representa tablas. Su constructor
rechaza motivos de tablas con ganador y victorias sin ganador.

**Por qué:** El ganador ya no puede deducirse siempre del turno: cualquiera puede
rendirse fuera de turno. Explicitarlo evita atribuir la victoria al color equivocado.
Se conservan estado(), ganador(), ResultadoMovimiento y todos los constructores
anteriores. Los finales se centralizan para borrar ofertas y detener el reloj.

**Cuándo se rompería:** EstadoPartida y MotivoFinalizacion siguen siendo enums
cerrados. Agregar un motivo obliga a ampliar esos tipos y los mensajes del
adaptador. Se acepta este límite para conservar los consumidores actuales. Si se
necesitan finales definidos por plugins, convendrá un contrato extensible y una
migración explícita de esta API. Las condiciones automáticas actuales se usan para
mate y las tres tablas originales, no para determinar ganadores de rendiciones.

## Órdenes de jugadores y ofertas

**Qué:** rendirse(Bando) admite cualquier bando mientras la partida siga.
ofrecerTablas(Bando) exige turno y ausencia de oferta. aceptarTablas(Bando) y
rechazarTablas(Bando) solo admiten al rival del oferente. Una jugada válida del
rival rechaza la oferta; las jugadas del oferente y los intentos inválidos la
conservan. Los errores de órdenes usan AccionInvalidaException con motivo tipado;
los movimientos conservan su excepción original.

**Por qué:** Estas son reglas del dominio, reutilizables desde UI, bot y pruebas.
Bando identifica uno de los dos bandos; la partida no depende de sesiones de UI.

**Cuándo se rompería:** Bando no autentica al usuario que envía la orden. Una
aplicación remota deberá identificarlo y autorizar el bando en la capa de aplicación.
Otra política de ofertas requeriría revisar estas reglas y sus pruebas.

## Puerto de tiempo y reloj opcional

**Qué:** FuenteDeTiempo responde ahoraEnNanos() con tiempo monotónico de origen
arbitrario. FuenteDeTiempoSistema usa System.nanoTime() en adaptadores. La fuente
falsa vive en tests y avanza a mano. RelojPartida es el contrato inyectado;
RelojAjedrez descuenta al bando activo y SinReloj implementa Null Object.
Las duraciones iniciales de cada color son positivas y pueden ser distintas.
Una instancia de RelojAjedrez pertenece a una sola partida y no puede reiniciarse.

**Por qué:** El núcleo no consulta el sistema ni ejecuta hilos o temporizadores.
Duration solo representa cantidades de tiempo. El tiempo restante se calcula al
consultar, sin valores negativos; las consultas no descuentan dos veces. SinReloj
evita controles de null y ramas por ausencia de reloj. El reloj empieza con blancas,
cambia después de movimientos válidos y se detiene en todo final, incluso inicial.

**Cuándo se rompería:** Pausas, incrementos, reinicio de partidas o controles por
etapas necesitarían ampliar el contrato. El puerto requiere una fuente monotónica;
no admite relojes que retrocedan. Las órdenes de una partida deben ejecutarse
secuencialmente: la aplicación debe serializarlas si recibe peticiones concurrentes.

## Vencimiento y prioridad de finales

**Qué:** verificarTiempo() finaliza por tiempo si el bando en turno agotó su saldo.
Después del final es idempotente. mover() comprueba el tiempo al entrar y nuevamente
antes de modificar el tablero, para cubrir tiempo consumido en validaciones. Las
otras órdenes comprueban tiempo al entrar. Un final ya registrado no se reemplaza.
Las lecturas son puras: observar cero no finaliza hasta verificar o enviar una orden.

**Por qué:** El usuario aprobó impedir jugadas tardías aunque la UI aún no haya
verificado el reloj. Aplicar la misma prioridad al resto de órdenes evita aceptar
tablas o una rendición después de detectar el vencimiento. Una jugada rechazada
con tiempo disponible no cambia el turno ni reinicia o detiene el reloj: el tiempo
del jugador sigue transcurriendo. Una jugada rechazada por vencimiento sí registra
ese final, sin modificar piezas ni historial.

**Cuándo se rompería:** Se simplifica la regla del ajedrez real: por tiempo siempre
gana el rival, incluso si no tiene material para dar mate. Incorporar esa excepción
requiere evaluar material. En juego remoto habría que definir el instante de
recepción de la orden y la política de latencia; este bloque es secuencial y local.

## Historial y persistencia futura

**Qué:** historial() devuelve List.copyOf de los Movimiento ejecutados, incluyendo
capturas y jugadas finales. Las copias son inmutables e independientes de jugadas
posteriores. Resultado, oferta y duraciones se exponen como valores inmutables.

**Por qué:** La UI y una futura capa de aplicación pueden consultar sin alterar
la partida. La base de datos se conectará por una interfaz de repositorio y su
adaptador; este bloque entrega los datos del núcleo y no agrega persistencia.

**Cuándo se rompería:** Para reproducir variantes o reconstruir posiciones
arbitrarias habrá que guardar también la disposición inicial y la configuración.
Un registro de eventos que incluya ofertas, tiempos por jugada y rendiciones es
otra necesidad y no se confunde con esta lista de movimientos. Promoción y enroque,
si se agregan, pueden requerir enriquecer Movimiento.

## Uso desde un adaptador

```java
FuenteDeTiempo fuente = new FuenteDeTiempoSistema();
RelojPartida reloj = new RelojAjedrez(Duration.ofMinutes(5), Duration.ofMinutes(5), fuente);
Partida partida = new Partida(tablero, blanco, negro, detector, condiciones, reloj);
// Los constructores anteriores crean una partida sin límite de tiempo.
partida.mover(movimiento);
partida.ofrecerTablas(partida.turno());
partida.aceptarTablas(partida.turno().opuesto());
Optional<ResultadoPartida> resultado = partida.resultado();
List<Movimiento> jugadas = partida.historial();
```

La UI llama verificarTiempo() periódicamente desde su infraestructura y consulta
tiempoRestante(color); Optional.empty indica sin límite. El núcleo no incorpora
JavaFX, acceso a bases de datos, temporizadores ni lectura del reloj del sistema.
El UML se omite por indicación del usuario.

## Verificación realizada

- 182 pruebas originales verificadas antes de empezar y conservadas sin modificaciones.
- Suite final: 234 pruebas (52 nuevas), sin fallos, errores ni omitidos con mvn -o test.
- Prueba de consola: mate del loco con victoria de negras.
- Núcleo sin referencias a JavaFX, SQL, hilos, temporizadores o reloj del sistema.
- Revisión independiente sin errores funcionales; se completó la cobertura del
  vencimiento durante validación. Esa prueba falla si se retira el segundo control
  de tiempo y pasa con la protección restaurada.
