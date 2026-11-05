# Ofertas de tablas: plan de implementación

**Objetivo:** confirmar la intención antes de ofrecer tablas y permitir al rival aceptarlas o rechazarlas desde su tarjeta.

**Arquitectura:** conservar las reglas existentes de Partida; ControladorTablero traduce órdenes y rechazos. Las tarjetas solo gestionan presentación transitoria. El ensamblado conecta las acciones; no hay ventanas modales ni relojes propios en las vistas.

**Tecnologías:** Java 21, JavaFX controles/gráficos, Maven.

- [x] Añadir delegados de consulta, oferta, aceptación y rechazo al controlador.
- [x] Reutilizar una confirmación integrada parametrizable para rendición, oferta y respuesta.
- [x] Reservar una fila para «Ofrecer tablas» y «Rendirse», con tamaño adaptable en cada tarjeta.
- [x] Presentar confirmación, espera y respuesta desde la oferta vigente del núcleo; cerrar estados obsoletos al mover o finalizar.
- [x] Conectar acciones desde AplicacionAjedrez, presentar rechazos y preservar el reloj y el tablero durante la decisión.
- [x] Ejecutar suite existente; comprobar JavaFX con un programa temporal externo y eliminarlo. No crear tests de UI/adaptadores. Conservar únicamente pruebas del núcleo si resulta necesario ampliar sus contratos.
- [x] Revisar cambios y documentar comportamiento final.
