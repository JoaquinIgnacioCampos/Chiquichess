# Panel marcador Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Ejecutar en esta sesión siguiendo el diseño visual aprobado.

**Goal:** Mostrar jugadores, turno, jaque y resultado del núcleo en un panel derecho del 35% del ancho de la ventana.

**Architecture:** Una consulta de jugador por color en Partida, delegaciones de lectura en el controlador, panel de presentación inyectado con tema. GridPane con filas para los jugadores y el marcador; texto LED dibujado con Canvas sin fuentes externas. VistaPartida conserva los clics y el redibujado existentes.

**Tech Stack:** Java 21, JavaFX 21.0.4, Maven, JUnit 6.0.3.

**Spec:** Maqueta de marcador aprobada en el chat y docs/javafx-paso-4-panel.md.

## Global Constraints

- Panel derecho 35% del ancho de la ventana; zona del tablero 65%, casillas cuadradas.
- Negras arriba, blancas abajo; estado y resultado siempre consultados al núcleo.
- Estética de marcador oscuro con texto LED y borde de jugador activo, colores por tema.
- Sin relojes visuales, botones, clubes reales, sprites nuevos ni materiales en este paso.
- Conservar la interacción y todos los tests. Sin JavaFX en el núcleo.

## Review Focus

- Rendición fuera de turno: ganador leído del resultado, no deducido del turno.
- Timeout rechazado durante un clic: el panel también muestra el final actualizado.
- Jaque resuelto: desaparece el aviso y cambia el jugador activo.
- Ventanas 900x620, 1280x820 y 1600x900: proporción estable y contenido legible.
- Partidas finalizadas: ninguna tarjeta debe seguir indicando un turno activo.

## Tareas

### 1 Consultas del modelo
Archivos: Partida.java, ControladorTablero.java, JugadorDePartidaTest.java.
Interfaz: Partida.jugador(Bando): Jugador inmutable; controlador delega jugador, turno, estado y resultado.
- [x] Tests primero del acceso por color, consistencia con turno y argumento nulo; ejecutar RED.
- [x] Agregar consulta mínima y delegaciones; suite GREEN.

### 2 Panel e integración
Archivos nuevos: TemaPanel.java, TextoLed.java, TarjetaJugador.java, PanelEstadoPartida.java.
Modificar: VistaPartida.java, AplicacionAjedrez.java y fixtures de tests UI existentes.
Interfaces: VistaPartida(controlador, temaTablero, representacion, temaPanel), PanelEstadoPartida.actualizar().
- [x] Tests primero de estado inicial, turnos por clic, jaque resuelto, mate, rendición,
  tablas con distintos motivos, timeout y proporciones; ejecutar RED.
- [x] Implementar presentación e inyección; suite GREEN.
- [x] Renderizar escenas de turno, jaque y final y revisar imágenes. Corregir defectos.

### 3 Cierre
- [x] Verificar consola, dependencias y diff; revisión independiente del cambio.
- [x] Documentar decisiones y evidencia; entregar sin commit salvo indicación del usuario.

## Evidencia

- Consulta de jugador: RED por método ausente, GREEN con 271 tests.
- Panel: RED por TemaPanel/constructor ausentes, GREEN con 282 tests.
- Verificación final tras ajustes tipográficos: mvn -o test, 282 sin fallos,
  errores ni omitidos. Log target/panel-verificacion-final.log.
- Cuatro escenas renderizadas e inspeccionadas: inicio, jaque, mate a 1280x820
  y ventana de 900x620 con nombres más largos. Sin advertencias de arranque.
- Revisión independiente sin hallazgos accionables; consola comprobada hasta mate.
- Único cambio del núcleo: consulta readonly jugador(Bando), con dos tests nuevos.
