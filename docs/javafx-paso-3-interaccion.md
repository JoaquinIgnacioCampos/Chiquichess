# Selección movimientos y capturas

## Comportamiento acordado

Seleccionar una pieza propia muestra sus movimientos legales. Otra pieza propia
reemplaza la selección. Una casilla o pieza enemiga sin movimiento permitido borra
los indicadores. Un destino permitido envía la jugada al núcleo; si está ocupado,
la captura también la ejecuta el núcleo. Los destinos vacíos llevan un punto y
las piezas enemigas capturables un anillo superpuesto, según las referencias del
usuario. La casilla seleccionada se resalta. Se conservan los símbolos Unicode
y la paleta provisional del paso anterior.

## Diseño y secuencia

1. ControladorTablero, en el adaptador UI, conserva únicamente la posición
   seleccionada. Consulta Partida.movimientosLegales en lugar de calcular reglas
   o cachear un tablero. Los movimientos se envían a Partida.mover y sus rechazos
   se devuelven a la vista con el motivo tipado del núcleo.
2. La vista recibe ese controlador desde AplicacionAjedrez. VistaTablero traduce
   clics primarios a posiciones y dibuja selección, puntos y anillos recibidos
   como datos de presentación, sin decidir legalidad. Los círculos no interceptan clics.
3. TemaTablero contiene también los colores de selección y destinos. Se conserva
   su constructor previo para los consumidores del paso 2.
4. Tests primero del controlador; después tests de eventos y representación de
   JavaFX, implementación mínima y suite completa. Captura de escena para QA visual.

## Qué por qué y cuándo cambiaría

**Qué:** controlador de interacción separado del dibujo, con selección como único
estado propio, y métodos de redibujado explícitos.

**Por qué:** se prueba la conexión con turnos, capturas, jaque y tiempo usando la
partida real. Un destino resaltado no garantiza que una jugada se acepte después:
Partida.mover conserva la última palabra. El reloj puede vencer entre los clics.

**Cuándo cambiaría:** un bot o una conexión remota debe disparar un redibujado tras
actualizar la partida. El controlador limpia selecciones que ya no correspondan
al bando actual o a una partida activa. La detección periódica de tiempo, los
botones de partida y los mensajes de resultado completos quedan para sus pasos.

## Verificación realizada

- 269 tests completos pasan: 248 anteriores y 21 nuevos (12 del controlador y
  nueve de eventos y dibujo). El fixture de VistaTableroTest se adapta al
  controlador inyectado, conservando las mismas comprobaciones anteriores.
- El núcleo y sus tests no se modificaron. La consola conserva el mate del loco.
- Se construyó mediante ocho jugadas legales una posición con reina blanca en d3
  y reina negra en f5. Un clic JavaFX en d3 mostró el anillo sobre la reina negra;
  se capturó e inspeccionó la escena. El siguiente clic en f5 ejecutó la captura
  real, registró la novena jugada, dejó 31 piezas y eliminó los indicadores.
- Revisión independiente de solo lectura sin hallazgos accionables. La comparación
  visual y la ejecución de la escena las realizó el implementador por separado.
