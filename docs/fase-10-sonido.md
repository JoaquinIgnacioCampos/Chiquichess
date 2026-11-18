# Fase 10: sonido

La partida ahora suena: un efecto por cada jugada, silbatos y hinchadas en el modo fútbol, y gol y
victoria propios de cada club. El sonido es un adaptador más: el núcleo no sabe que existe.

## Cómo suena

- **Cada jugada suena una vez**, con lo más importante que tuvo: gol, jaque, promoción, enroque,
  captura, pase o movimiento común, en ese orden. El gol tapa al pase y el jaque a la captura.
- **El final**: la victoria del club que ganó, o un sonido de tablas. En el modo fútbol, antes, el
  silbato final.
- **Deshacer y rehacer** suenan como un rebobinado.
- **Modo fútbol**: silbato al empezar y las dos hinchadas cantando en bucle todo el partido.
  - Crecen a medida que se acerca el límite de jugadas.
  - Si el partido está parejo (a lo sumo un gol de diferencia), crecen más: un final apretado es lo
    que más se escucha.
  - La hinchada del que va ganando canta completa; la del que pierde, más bajo; con empate, las dos
    parecido.
  - El grito de gol es el pico: lo pone el sonido del gol, no la hinchada.
  - Al terminar la partida se callan. Si se deshace la jugada final, vuelven.
- **El clásico** tiene solo efectos, sin hinchada ni silbatos.
- **La consola** no tiene sonido.
- **Volumen**: en la cabecera de la partida hay un botón "Sonido: sí/no" y una barra de volumen. Lo
  elegido sigue valiendo en la revancha y en las partidas siguientes.

## Qué

### 1. Núcleo: eventos (Observer)

- `EventoDePartida` (sellado) en `Nucleo.juego`:
  - `JugadaHecha(bando, rasgos, estado)`
  - `JugadaDeshecha(estado)`
  - `JugadaRehecha(bando, rasgos, estado)`
  - `PartidaFinalizada(resultado)`
- `Rasgo` = `CAPTURA`, `ENROQUE`, `PROMOCION`, `PASE`, `GOL`, deducidos de los efectos de la
  `Jugada`. El enroque se reconoce porque es la única jugada que traslada dos piezas.
- `ObservadorDePartida`: puerto de salida con un solo método, `notificar(evento)`.
- `Partida.agregarObservador` / `quitarObservador`. Partida avisa **después** de cambiar, en:
  - mover: la jugada y, si la terminó, el final;
  - deshacer y rehacer;
  - rendirse, aceptar tablas y tiempo agotado: el final.
- Una jugada rechazada no avisa nada. El tiempo agotado se avisa una sola vez.

### 2. Adaptador de sonido (`adaptadores.audio`)

| Clase | Qué hace |
|---|---|
| `Sonido` | Cada cosa que suena, con su nombre de archivo, si cada club puede tener el suyo y cuál usar si falta. |
| `GuionDeSonidos` | Evento → qué suena. No sabe de archivos ni de parlantes. |
| `CatalogoDeSonidos` | Sonido + club → archivo. Busca primero el del club y después el común, en la carpeta externa `sonidos/` y en los recursos. |
| `IntensidadDeHinchada` | Función pura: jugadas, límite y marcador → volumen de cada hinchada. |
| `SonidosDePartida` | El observador: une todo lo anterior y mantiene las hinchadas. |
| `ReproductorDeAudio` | Interfaz con lo único que se necesita: tocar una vez, tocar en bucle, cambiar volumen. |
| `ReproductorJavaSound` | La implementación real, con `javax.sound.sampled` (viene con el JDK). |
| `ControlDeVolumen` | Volumen y silencio del usuario, compartido entre partidas. |

### 3. Interfaz

- **La vista también escucha los eventos.** `ControladorTablero.escuchar` suscribe a la vista y al
  panel a la partida:
  - `PanelEstadoPartida` anuncia "¡GOL!" porque el evento de la última jugada dice que fue gol. Antes
    lo deducía comparando el total de goles con el del refresco anterior. Deshacer saca el anuncio y
    rehacer un gol lo vuelve a mostrar.
  - `VistaPartida` se redibuja con cada evento, así sigue a la partida aunque el cambio no lo haya
    provocado ella. El caso de hoy es el final por tiempo: lo detecta el pulso del reloj, la partida
    avisa y la vista muestra el resultado.
  - `ActualizadorRelojJavaFx` ya no se encarga de mostrar el final: solo hace latir los relojes y se
    detiene cuando la partida terminó.
  - La vista sigue redibujándose sola después de sus propias acciones, porque hay cosas que no son
    eventos de la partida: la casilla seleccionada, el selector de promoción y las ofertas de tablas.
- `AplicacionAjedrez` crea un reproductor y un control de volumen para todo el programa. En cada
  partida conecta un `SonidosDePartida` con los clubes de cada bando, y lo cierra al salir de ella.
- `VistaPartida.configurarSonido` agrega el botón de silencio y la barra de volumen.

## Por qué

- **Eventos desde el núcleo y no deducidos en el adaptador.** El núcleo ya sabe qué pasó en cada
  jugada: si hubo gol, captura o jaque. Si el adaptador lo dedujera comparando antes y después (como
  hacía el panel con los goles), esa lógica quedaría repetida en cada adaptador. Ahora la usan el
  sonido y la vista. El evento dice
  *qué pasó* y nada de *cómo se muestra*, así que el núcleo sigue sin saber que hay sonido.
- **Observer como puerto de salida.** Es el mismo patrón de `FuenteDeTiempo` al revés: allá el núcleo
  pide algo al exterior por una interfaz suya; acá le avisa por una interfaz suya. Las dependencias
  siguen apuntando hacia el núcleo.
- **Evento sellado.** `GuionDeSonidos` hace un `switch` sobre los eventos: si mañana aparece uno nuevo,
  no compila hasta decidir cómo suena.
- **El guion separado del catálogo y del reproductor.** Cada pieza se cambia sola: otros sonidos (el
  catálogo), otra tecnología (el reproductor) u otras reglas de qué suena (el guion). Y cada una se
  prueba sin parlantes.
- **La hinchada como función del estado.** Si fuera un contador que sube con cada evento, habría que
  deshacerlo al deshacer. Calculada del estado, al deshacer baja sola, como el marcador.
- **Sonidos por convención de nombres.** Cambiar un sonido o darle uno propio a un club es dejar un
  archivo con el nombre correcto, sin tocar código. La carpeta externa permite hacerlo sin recompilar.
- **Java Sound y no JavaFX Media.** Java Sound viene con el JDK; JavaFX Media sería una dependencia
  nueva. El costo es que solo lee WAV, AIFF y AU.
- **Un hilo propio para el sonido.** Leer una hinchada de un minuto tarda: hacerlo en el hilo de la
  interfaz trabaría el tablero.
- **Si algo del sonido falla, el juego sigue.** Sin parlantes, con un archivo roto o con uno que falta,
  la partida se juega igual: el reproductor avisa una vez por la consola de errores.

## Cuándo se rompería

- **Un observador que lanza una excepción** cortaría la orden que la provocó. Partida no la ataja a
  propósito (no debe esconder errores); el reproductor real ataja los suyos.
- **Avisos fuera del hilo de JavaFX.** Hoy todas las órdenes llegan desde la interfaz o desde el
  `Timeline` del reloj, que corren en ese hilo. Si alguna vez una IA jugara desde otro hilo, el
  observador recibiría avisos ahí. `SonidosDePartida` no toca la interfaz, pero un observador visual sí.
- **Una jugada nueva que traslade dos piezas** se reconocería como enroque.
- **Archivos muy largos** ocupan memoria: cada uno queda cargado entero. Conviene recortar las
  hinchadas a 30 o 40 segundos.

## Pruebas

- `EventosDePartidaTest` (núcleo): cada orden avisa lo que corresponde y en orden (jugada antes que
  final). Las jugadas rechazadas no avisan, el tiempo agotado avisa una vez y un observador quitado no
  recibe más avisos.
- `GuionDeSonidosTest`: qué suena con cada evento y la prioridad entre rasgos. Comprueba también que
  todos los sonidos salen de algún evento.
- `CatalogoDeSonidosTest`: el del club gana al común, los comunes no miran la carpeta del club, los
  alternativos, y qué pasa si no hay archivos.
- `IntensidadDeHinchadaTest`: crece con el avance, el final parejo suena más, el que gana canta más,
  siempre entre 0 y 1, y depende solo del estado.
- `PanelEstadoPartidaTest`:
  - deshacer un gol saca el anuncio y rehacerlo lo vuelve a mostrar, aunque se haga desde la partida
    y no desde la vista;
  - el final por tiempo se muestra solo por el evento, sin redibujar a mano.
- `SonidosDePartidaTest`: partidas reales con un `ReproductorFalso` y archivos de prueba. Cubre:
  - el club ganador;
  - una o dos hinchadas;
  - subir al jugar y bajar al deshacer;
  - callarse al final;
  - el silencio;
  - desengancharse al cerrar.
- El reproductor real se probó a mano con WAV de 16 y de 24 bits, en un bucle, con cambio de volumen y
  con un archivo que no existe.

## Los sonidos (todos CC0)

Todos son de [Freesound](https://freesound.org) con licencia **CC0**: se pueden usar, cambiar y
recortar sin pedir permiso ni citar al autor. Igual se lo cita, por cortesía. La licencia de cada uno
se verificó en su página. Para bajar los originales de Freesound hace falta una cuenta (gratis).

Hay que bajarlos en el formato original (WAV o AIFF). Cada uno va en `src/main/resources/sonidos/` con
el nombre de la columna "Archivo".

### Comunes

| Archivo | Sonido | Autor | Duración | Notas |
|---|---|---|---|---|
| `comunes/movimiento.wav` | [chess_move_on_alabaster](https://freesound.org/people/mh2o/sounds/351518/) | mh2o | 0,07 s | |
| `comunes/captura.wav` | [Wooden Click](https://freesound.org/people/BenjaminNelan/sounds/321083/) | BenjaminNelan | 0,09 s | |
| `comunes/enroque.wav` | [chess pieces](https://freesound.org/people/simone_ds/sounds/366065/) | simone_ds | 23 s | Opcional: recortar dos toques seguidos. Si no está, suena el movimiento. |
| `comunes/promocion.wav` | [WinFantasia](https://freesound.org/people/Fupicat/sounds/521645/) | Fupicat | 3,8 s | |
| `comunes/jaque.wav` | [Timpani C2](https://freesound.org/people/Terry93D/sounds/369394/) | Terry93D | 4 s | |
| `comunes/pase.wav` | [Soccer Ball Kick](https://freesound.org/people/bittermelonheart/sounds/555042/) | bittermelonheart | 0,7 s | Otra opción: [Soccer Kick](https://freesound.org/people/musita182/sounds/261267/). |
| `comunes/gol.wav` | [Goal!](https://freesound.org/people/blaukreuz/sounds/189821/) | blaukreuz | 1:22 | Recortar a unos 8 s. |
| `comunes/victoria.wav` | [WinBrass](https://freesound.org/people/Fupicat/sounds/521639/) | Fupicat | 3,8 s | |
| `comunes/tablas.wav` | [Tibetan bowl center hit](https://freesound.org/people/dersinnsspace/sounds/421829/) | dersinnsspace | 19 s | Recortar a unos 4 s. |
| `comunes/silbato-inicial.wav` | [Referee whistle sound](https://freesound.org/people/Rosa-Orenes256/sounds/538422/) | Rosa-Orenes256 | 0,5 s | |
| `comunes/silbato-final.wav` | [Referee whistle blow, gymnasium](https://freesound.org/people/SpliceSound/sounds/218318/) | SpliceSound | 3,3 s | |
| `comunes/rebobinar.wav` | [Woosh_Medium_Short_01](https://freesound.org/people/moogy73/sounds/425706/) | moogy73 | 2,3 s | |
| `comunes/hinchada.aiff` | [noise#01 (Feyenoord, De Kuip)](https://freesound.org/people/huubjeroen/sounds/39733/) | huubjeroen | 5:25 | Recortar a 30–40 s. Es AIFF: se usa tal cual. |

### Propios de cada club

Grabaciones de hinchadas reales, distintas para cada club. **No** se usan cantos ni himnos de los
clubes: tienen derechos de autor.

| Club | Gol | Victoria | Hinchada |
|---|---|---|---|
| Boca | [Crowd screaming GOAL (México)](https://freesound.org/people/felix.blume/sounds/241630/), felix.blume, recortar | [Crowd Cheer](https://freesound.org/people/FoolBoyMedia/sounds/397434/), FoolBoyMedia | [Hinchas con bombos (Valparaíso)](https://freesound.org/people/felix.blume/sounds/500250/), felix.blume, recortar |
| River | [Crazy Soccer Crowd Cheering](https://freesound.org/people/Kinoton/sounds/348978/), Kinoton, recortar | [Crowd Cheer II](https://freesound.org/people/FoolBoyMedia/sounds/397435/), FoolBoyMedia | [60.000 personas en el Mineirão](https://freesound.org/people/felix.blume/sounds/612131/), felix.blume, recortar |
| Independiente | [goalloop](https://freesound.org/people/huubjeroen/sounds/113698/), huubjeroen | [RoaringCrowd](https://freesound.org/people/benfree/sounds/130568/), benfree | la común (Feyenoord) |
| San Lorenzo, Racing y Argentina | el común | el común | la común (Feyenoord) |

Los archivos van en `clubes/boca/gol.wav`, `clubes/boca/victoria.wav`, `clubes/boca/hinchada.wav`, y
lo mismo para `river`, `independiente`, `san_lorenzo`, `racing` y `argentina`.
