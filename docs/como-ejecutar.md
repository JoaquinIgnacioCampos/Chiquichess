# ChiquiChess: cómo ejecutarlo

## Ejecutable de Windows (no necesita instalar nada)

1. Descomprimir `ejecutable/ChiquiChess-windows.zip` en cualquier carpeta que permita escribir
   (por ejemplo el Escritorio; no en `Program Files`).
2. Abrir `ChiquiChess/ChiquiChess.exe`.

El ejecutable trae su propio Java adentro. Las opciones elegidas se guardan en `configuracion.txt`,
al lado del `.exe`.

## Desde el código (JDK 21 y Maven)

| Qué | Comando |
|---|---|
| Jugar con la interfaz gráfica | `mvn javafx:run` |
| Compilar y correr todos los tests | `mvn clean verify` |
| Jugar por consola | `mvn exec:java -Dexec.mainClass=adapters.Main` (argumentos opcionales: `futbol`, `ia`, `facil`/`medio`/`dificil`, un número como límite de jugadas) |
| Jar con todo adentro | `mvn -DskipTests package` y se genera `target/TPOAjedrez-1.0-SNAPSHOT-completo.jar` (se abre con `java -jar`) |

## Regenerar el ejecutable

Con JDK 21 (que incluye `jpackage`) y Maven en el PATH:

```powershell
.\empaquetar.ps1
```

Deja `dist\ChiquiChess\ChiquiChess.exe`. Para entregarlo, comprimir esa carpeta como
`ejecutable/ChiquiChess-windows.zip`. Solo sirve para Windows: el jar incluye las bibliotecas de JavaFX
de ese sistema.
