# Preparación de JavaFX

Requisitos: JDK 21 o compatible y Maven. Se conserva el proyecto no modular.
JavaFX 21.0.4 incorpora controles y gráficos; javafx.base llega como dependencia
necesaria de esos módulos. No se agrega FXML, Web ni Media.

## Abrir la ventana

Desde la raíz del proyecto:

```text
mvn javafx:run
```

El punto de entrada es adaptadores.ui.MainGrafico. En el paso 0 abría una ventana
vacía; en los pasos posteriores muestra el tablero. También puede ejecutarse desde la ventana Maven
de IntelliJ: Plugins → javafx → javafx:run. El plugin configura la ruta de módulos
y las bibliotecas nativas correspondientes al sistema operativo.

En IntelliJ, seleccionar la configuración compartida **ChiquiChess** de la barra
superior y pulsar Run. Está guardada en .run/ChiquiChess.run.xml y utiliza Maven
con el SDK temurin-21. Si todavía no aparece, recargar el proyecto Maven o volver
a abrir el proyecto. El SDK debe estar registrado en IntelliJ con ese nombre y
apuntar a una instalación JDK 21; este equipo ya lo tiene instalado y registrado.

El plugin utiliza el Java del proceso Maven mediante ${java.home}/bin/java para
que su JVM hija no tome otro Java del PATH. En terminal, JAVA_HOME debe apuntar
al JDK 21. El SDK del proyecto también está configurado en Java 21.

MainGrafico sigue siendo el punto de entrada y permite arrancar con dependencias
en el classpath, pero ejecutarlo directamente con el botón de su método main
puede crear otra configuración que cargue JavaFX fuera del module-path y vuelva
a mostrar el aviso de unnamed module. Para la ejecución configurada sin esos
avisos se utiliza **ChiquiChess**. AplicacionAjedrez mantiene el ensamblado y el
ciclo de vida gráfico; MainGrafico solo inicia esa aplicación. La consola conserva
adaptadores.Main como entrada.

## Consola y tests

```text
mvn test
java -cp target/classes adaptadores.Main
```

La consola conserva su entrada y comportamiento anteriores. El núcleo no depende
del paquete de UI ni importa JavaFX. Esta preparación solo configura dependencias
y abre la ventana; los componentes de la partida corresponden a los pasos siguientes.

## Comprobaciones del paso

- mvn -o test: 234 pruebas, sin fallos, errores ni omitidos.
- Consola ejecutada hasta el mate del loco, con victoria de negras.
- Ventana ChiquiChess confirmada en Windows con identificador de ventana y proceso
  respondiendo, al ejecutar directamente el punto de entrada con los módulos JavaFX.
- Maven javafx:run también se ejecutó sin errores. La herramienta de captura de
  pantalla no pudo iniciarse; la comprobación de apertura se realizó por los datos
  de la ventana del proceso, sin inspección de una captura.
- Núcleo y tests existentes sin modificaciones ni dependencias de UI.

## Ajuste del entorno de ejecución

Se comprobó el arranque de Maven con JDK 21 sin las advertencias de JavaFX,
acceso nativo ni sun.misc.Unsafe. Una comprobación temporal abrió la escena real,
la renderizó y confirmó Java 21.0.6 y el módulo javafx.graphics antes de cerrar
su propia ventana, sin emitir advertencias. La suite completa actual pasa con
269 tests. Se mantiene JavaFX 21.0.4 y no se silencian registros ni se cambian
las reglas del núcleo.
