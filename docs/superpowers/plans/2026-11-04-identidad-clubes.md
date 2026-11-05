# Identidad de clubes en menú y partida

**Objetivo:** usar los PNG del usuario (Boca, Independiente, River), reemplazar el peón de las tarjetas por el escudo elegido, mostrar el nombre del club y aplicar sus colores sin alterar tablero/marcador central.

**Arquitectura:** el ensamblado resuelve una sola vez el color de los jugadores y entrega la partida junto a los equipos por color. Los recursos y temas son responsabilidad de la UI. Se mantienen constructores anteriores para vistas sin identidad de club. Sin modificaciones al núcleo ni nuevos tests de UI/adaptadores.

- [x] Copiar los tres PNG originales a recursos de aplicación y cargar por classpath.
- [x] Exponer resultado de ensamblado con equipos por color y conservar crearConfigurada para compatibilidad.
- [x] Crear catálogo visual de clubes con imágenes y temas de tarjeta.
- [x] Inyectar identidades desde arranque a panel y tarjetas; conservar nombres y color de piezas, sumar club y escudo.
- [x] Mostrar escudos en los botones de equipos del menú.
- [x] Verificar suite existente, asociación fija/sorteada, recursos empaquetados, acciones, legibilidad y ventana pequeña con comprobador temporal externo; eliminarlo y documentar.
