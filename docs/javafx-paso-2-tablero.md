# Tablero estático

## Qué

VistaTablero dibuja una GridPane usando ITablero.ancho(), alto() y piezaEn().
Las filas visuales invierten la coordenada vertical: alto - 1 - fila. Así la fila
de origen del núcleo queda abajo, con las blancas en la orientación inicial.
La paridad se calcula sobre las coordenadas del núcleo; a1 es oscura, también
cuando cambian las dimensiones o su paridad. El tamaño y la proporción del tablero
se ajustan al espacio disponible, manteniendo casillas cuadradas y el tablero centrado.

TemaTablero contiene todos los colores: casillas, piezas, contorno y fondo. Se
construye en AplicacionAjedrez y se entrega a VistaPartida por constructor junto
con RepresentacionPiezas. La paleta provisional se puede reemplazar por la de un club.

RepresentacionPiezas concentra la traducción de tipo y color a un Node JavaFX.
Por ahora crea Text con los doce símbolos Unicode de ajedrez. Las vistas no
preguntan el tipo de pieza ni deciden su símbolo. Para piezas nuevas sin símbolo
estándar muestra su código como alternativa. La fuente provisional es Segoe UI
Symbol; JavaFX usa su sustitución habitual si esa fuente no está instalada.

## Por qué

La vista guarda una referencia de lectura al tablero real, no una matriz ni un
mapa de piezas propio. redibujar() reconstruye los nodos desde el estado actual
del núcleo. Los únicos objetos creados por la vista son componentes gráficos.
VistaPartida.redibujar() permite solicitar la actualización tras futuras órdenes.

Separar tema y representación permite cambiar colores o gráficos sin modificar
las reglas. RepresentacionPiezas devuelve Node para permitir una implementación
con sprites, normalizados al mismo tamaño visual base, sin cambiar la grilla.

## Cuándo se rompería

Si se permite cambiar el bando de observación durante el juego, habrá que hacer
configurable la orientación. La vista soporta dimensiones diferentes, pero la
disposición inicial estándar del ensamblador sigue siendo la del ajedrez clásico;
para otra variante se debe inyectar una partida con su disposición correspondiente.
El redibujado es explícito, no un observador ni un temporizador: las futuras
acciones de UI deberán pedirlo después de recibir una respuesta del núcleo.

## Verificación

Nueve tests nuevos cubren las doce representaciones, paleta inyectada, pieza nueva,
tablero rectangular 10x6, tablero de dimensiones impares, orientación, cambio de
tema, redibujado tras e2-e4 y casillas cuadradas al redimensionar. Suite completa:
248 pruebas sin fallos, errores ni omitidos. No se modificó el núcleo.
Se capturó la escena real de AplicacionAjedrez a 1000x720 mediante la API de
snapshot de JavaFX y se inspeccionó su imagen: tablero centrado, piezas visibles
y blancas abajo. La consola también conserva el mate del loco con victoria negra.
