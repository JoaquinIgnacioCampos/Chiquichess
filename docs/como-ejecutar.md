# ChiquiChess: cómo ejecutarlo

## Ejecutable de Windows (no necesita instalar nada)

Abrir `ChiquiChess/ChiquiChess.exe` (carpeta `ChiquiChess/` de la raíz del repositorio). Trae su propio
Java adentro, así que no hace falta compilar. Si se copia a otro lugar, hay que llevar la carpeta entera.

Las opciones elegidas se guardan en `configuracion.txt`, al lado del `.exe`; por eso la carpeta debe
estar en un lugar donde se pueda escribir (no en `Program Files`).

## Desde el código (JDK 21 y Maven)

| Qué | Comando |
|---|---|
| Jugar con la interfaz gráfica | `mvn javafx:run` |
| Compilar y correr todos los tests | `mvn clean verify` |
| Jugar por consola | `java -cp target/classes adapters.Main` después de compilar (argumentos opcionales: `futbol`, `ia`, `facil`/`medio`/`dificil`, un número como límite de jugadas) |
| Jar con todo adentro | `mvn -DskipTests package` genera `target/TPOAjedrez-1.0-SNAPSHOT-completo.jar` (se abre con `java -jar`) |

## Regenerar el ejecutable

Con JDK 21 (incluye `jpackage`) y Maven en el PATH:

```powershell
.\empaquetar.ps1
```

Reemplaza la carpeta `ChiquiChess/`. Solo sirve para Windows: el jar incluye las bibliotecas de JavaFX
de ese sistema.
