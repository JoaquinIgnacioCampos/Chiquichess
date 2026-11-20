# ChiquiChess

Ajedrez del fútbol argentino: ajedrez clásico y un modo "Posesión y gol", con relojes, deshacer y rehacer,
rival IA y equipos con sus escudos y sonidos. Trabajo práctico de la UADE (Java 21, JavaFX, Maven).

## Jugar rápido

Abrir [`ChiquiChess/ChiquiChess.exe`](ChiquiChess/ChiquiChess.exe) (Windows, sin instalar ni compilar nada).
Para compilar o ejecutar desde el código, ver [`docs/como-ejecutar.md`](docs/como-ejecutar.md).

## Qué se entrega

| Entregable | Dónde |
|---|---|
| Código fuente completo | [`src/main/java`](src/main/java): `core` (el juego, sin interfaz) y `adapters` (pantalla, sonido, configuración) |
| Diagrama de clases (UML) | [`docs/uml/`](docs/uml/README.md) (el completo está en `uml-completo.png`) |
| Tests unitarios del núcleo | [`src/test/java/core`](src/test/java/core) (se corren con `mvn clean verify`) |
| Justificación de decisiones de diseño (qué / por qué / cuándo se rompería) | [`docs/justificacion.md`](docs/justificacion.md) |

El resto de `docs/` es el detalle de cada fase del desarrollo.
