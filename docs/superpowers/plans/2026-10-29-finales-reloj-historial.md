# Finales reloj e historial Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Ejecutar en esta sesión en el orden del arquitecto.

**Goal:** Agregar resultado, rendición, tablas por acuerdo, reloj opcional e historial preservando los 182 tests.

**Architecture:** Mantener las APIs actuales. Resultado inmutable separado del estado; reloj por interfaz y Null Object; tiempo externo por puerto monotónico. Un único cierre limpia ofertas y detiene el reloj.

**Tech Stack:** Java 21, Maven, JUnit 6.0.3.

**Spec:** docs/superpowers/specs/2026-10-29-finales-reloj-historial-design.md

## Global Constraints

- Sin JavaFX, bases de datos, hilos, temporizadores ni reloj del sistema en el núcleo.
- Cambios mínimos; tests nuevos antes de código nuevo; conservar tests originales.
- mover verifica tiempo; solo el jugador en turno ofrece tablas.
- Historial solo de movimientos ejecutados. UML omitido. Persistencia fuera del bloque.

## Review Focus

- Tiempo vencido antes de mover: no modifica tablero, turno ni historial.
- Movimiento inválido con oferta: conserva oferta y reloj activo.
- Todos los finales detienen el reloj y borran ofertas, incluido final inicial.
- Ganador de rendición fuera de turno no depende del turno.
- Copias del historial y resultado no permiten alterar la partida.

## Tareas en orden

### 1 Preparación
- [x] Crear codex/finales-reloj-historial en el checkout solicitado.
- [x] Fijar JUnit 6.0.3 y ejecutar mvn -o test: 182 sin fallos.
- [x] Responder por escrito las tres preguntas en el diseño.

### 2 Resultado y rendición
Archivos: Nucleo/juego/{MotivoFinalizacion,ResultadoPartida,AccionInvalidaException,Partida}.java,
Nucleo/reglas/EstadoPartida.java, adaptadores/JuegoConsola.java.
Interfaces nuevas: resultado(): Optional<ResultadoPartida>, rendirse(Bando): void.
- [x] Escribir y ejecutar ResultadoPartidaTest y RendicionTest antes de implementar.
- [x] Implementar resultado compatible y rendición de ambos colores, incluso fuera de turno.
- [x] Ejecutar suite completa, incluidos rechazos posteriores a finales.

### 3 Ofertas de tablas
Archivos: Partida.java y OfertasTablasTest.java.
Interfaces: ofrecerTablas(Bando), aceptarTablas(Bando), rechazarTablas(Bando), ofertaTablasPendiente().
- [x] Tests primero: acuerdo, rechazo, falta de oferta, respuesta propia, doble oferta,
  oferta fuera de turno, movimiento válido e inválido, final limpia oferta.
- [x] Ejecutar RED, implementar reglas y ejecutar suite GREEN.

### 4 Reloj e integración
Archivos: Nucleo/tiempo/{FuenteDeTiempo,RelojPartida,RelojAjedrez,SinReloj}.java,
adaptadores/FuenteDeTiempoSistema.java, dobles/FuenteDeTiempoFalsa.java y Partida.java.
Interfaces: fuente ahoraEnNanos(): long; reloj iniciar(Bando), cambiarTurno(), detener(),
tiempoRestante(Bando): Optional<Duration>, tiempoAgotado(Bando): boolean;
Partida.verificarTiempo(), tiempoRestante(Bando) y constructores con reloj.
- [x] Tests primero del reloj con 30 segundos, alternancia, exacto cero y detención.
- [x] Ejecutar RED, implementar reloj, ejecutar GREEN.
- [x] Tests primero de integración: movimiento inválido, vencimiento antes de mover,
  verificación sin efecto, todos los finales y partida sin reloj.
- [x] Ejecutar RED, integrar por constructor y ejecutar suite GREEN.

### 5 Historial y cierre
Archivos: Partida.java, HistorialTest.java y docs/decisiones-finales-reloj-historial.md.
Interfaz: historial(): List<Movimiento>, copia inmutable en orden de ejecución.
- [x] Tests primero de orden, capturas, intentos inválidos y copias independientes.
- [x] Ejecutar RED, implementar y ejecutar suite completa GREEN.
- [x] Inspeccionar núcleo, diff y consola; documentar decisiones qué/por qué/cuándo.
- [x] Revisión final independiente y corregir hallazgos relevantes con tests.

## Registro de ejecución

- Preparación: rama creada; mvn -o test, 182 tests, 0 fallos/errores/omitidos.
- Ejecución autorizada por el usuario: «Perfecto, empezá a trabajar» tras aclaraciones.
- Se conserva el checkout y el cambio previo de .idea/misc.xml del usuario.
- Resultado y rendición: tests nuevos detectaron APIs ausentes; implementación y suite 193/193.
- Ofertas: tests nuevos detectaron APIs ausentes; implementación y suite 206/206.
- Reloj aislado: tests nuevos detectaron APIs ausentes; implementación y suite 214/214.
- Reloj integrado: tests nuevos detectaron APIs ausentes; implementación y suite 227/227.
- Historial: tests nuevos detectaron API ausente; implementación y suite 233/233.
- Todas las suites con mvn -o test; logs RED y GREEN conservados en target (ignorado por Git).
- Lectura del núcleo sin referencias prohibidas; tests originales sin modificaciones;
  git diff --check limpio y consola comprobada con el mate del loco.
- Ruling: se conserva EstadoPartida como API compatible y se agrega resultado inmutable;
  el costo es mantener enums cerrados al agregar motivos. Está documentado.
- Ruling: las demás órdenes también comprueban tiempo al entrar; evita aceptar acuerdos
  tras vencimiento detectado. El costo es exigir esta prioridad en futuros adaptadores.
- Revisión independiente: sin hallazgos críticos ni errores funcionales; una brecha de
  cobertura en vencimiento durante validación. Se trata como relevante porque protege
  el rechazo de jugadas tardías. Se agregó un test con fuente avanzada en el validador;
  al quitar el segundo control falla (1 fallo esperado). Se restauró la protección.
- Ámbitos excluidos de la revisión: UI, persistencia, bot, concurrencia y latencia remota;
  siguen fuera del bloque. Material insuficiente por tiempo sigue simplificado según
  consigna. El costo de extender esos ámbitos es introducir sus políticas y adaptadores.
- Verificación final: mvn -o test, 234/234 (182 originales + 52 nuevos), sin fallos,
  errores ni omitidos. Log: target/verificacion-final.log. Diff sin errores de formato,
  tests originales intactos y núcleo sin referencias prohibidas.
- Entrega en la rama de trabajo con cambios sin commit, sin merge ni push.
