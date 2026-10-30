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

Para ejecutar directamente desde IntelliJ, abrir MainGrafico.java y pulsar Run
junto a main(). Si las dependencias aún no aparecen, recargar el proyecto Maven.
Este lanzador no hereda de Application: evita el error «JavaFX runtime components
are missing» al ejecutar con las dependencias Maven en el classpath del IDE.
AplicacionAjedrez mantiene el ensamblado y el ciclo de vida gráfico; MainGrafico
solo inicia esa aplicación. La consola conserva adaptadores.Main como entrada.

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
