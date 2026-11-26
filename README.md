# Laptop Explorer

Aplicación de escritorio (Java 21, JavaFX 21, Spring Boot, SQLite) para registrar, comparar y visualizar en 3D laptops.

## Pantallas
- **Catálogo**: tarjetas de producto (por defecto) o tabla, con búsqueda y filtros, ficha por secciones, guardar y eliminar. Se marcan de 2 a 4 laptops para comparar.
- **Comparar**: tabla de diferencias entre los laptops marcados.
- **Visor 3D**: dos visores, despiece, tapa, capas, lista de componentes y ficha de cada pieza.
- **Componentes 3D**: biblioteca de 45 procesadores, gráficas, memorias y SSD modelados a escala. Visor con vistas de cámara, cotas (ancho, fondo y alto medidos en el modelo), despiece por capas, ventiladores en movimiento y resaltado de piezas por material; al lado, la ficha técnica de cada componente.
- **Ajustes**: ubicación de la base de datos y versiones.

## Funciones añadidas
- **Buscar**: asistente con autocompletado en cada campo (las sugerencias se acotan según lo ya elegido), resultados en vivo, alternativas cercanas y similares.
- **Moneda**: selector USD/PEN en la barra superior. Los precios se guardan en USD; el tipo de cambio y su origen se definen en Ajustes.
- **Comparar**: pantalla dividida, una columna por laptop, con el mejor valor de cada fila resaltado ("Ver como tabla" conserva la vista densa).
- **Catálogo**: tarjetas al estilo de una tienda: la imagen es el propio modelo 3D del laptop, con pantalla, RAM, SSD, procesador, núcleos y gráficos en etiquetas; debajo marca, modelo, precio, de dónde sale su 3D y las acciones. Un clic en la tarjeta abre su ficha a la derecha. La vista elegida (tarjetas o tabla) se recuerda. Botón "Modelo 3D…" para importar un .obj (archivo o enlace directo), que se guarda en `~/.laptop-explorer/models3d/<id>/`.
- **Diseño 3D de cada laptop**: los que tienen fotos propias se dibujan con ellas. Los demás toman los rasgos de su serie de `src/main/resources/models/designs.json` (colores de tapa, cubierta y teclas, retroiluminación, rótulo de la tapa, muesca, TrackPoint, barra de luz, repisa trasera). Si la ficha no trae ancho y fondo, se estiman a partir de la pantalla. Cada tarjeta y la ficha indican el origen: "fotos reales" (29), "fotos de su familia" (5, con el nombre del equipo del que son) o "diseño de la serie" (21, aproximación). Un modelo nuevo solo usa las fotos de otro si se decide a mano porque comparte chasis: hoy el MacBook Air 15 (M5) usa las del M3 y el Titan 18 HX AI las del Titan 18 HX, con las mismas medidas en la ficha (tabla `SAME_CHASSIS` en `AMJLaptopModel`). Las imágenes de las tarjetas se guardan en `~/.laptop-explorer/cache/laptops/` y solo se dibujan de nuevo si cambia la ficha.
- **Consulta en internet** (única función con red): usa la API de Claude con búsqueda web. Necesita una clave en Ajustes o en `ANTHROPIC_API_KEY`.

## Convención de nombres (equipo AMJ)
Equipo: Anthony, Mathias y Julio (AMJ). Las clases empiezan con `AMJ` (`AMJLaptopService`), los métodos con `amj` (`amjListar`) y, en los scripts de Python, las funciones con `amj_`. Los controles de las vistas usan prefijo por tipo (`btn`, `tgl`, `lbl`, `txt`, `cmb`, `chk`, `lst`, `tbl`, `sld`, `scr`, `pnl`, `img`). Quedan sin prefijo los nombres que exige una librería (`main`, `initialize`, `start`, métodos sobrescritos, consultas derivadas de Spring Data y la migración `V2__Precio_y_lanzamiento`).

## Compilar y ejecutar
Doble clic en `iniciar.cmd`. Usa las variables de entorno `JAVA_HOME` (JDK 21) y `MAVEN_HOME` (Apache Maven).

Requiere JDK 21 y Maven.

    mvn compile
    mvn spring-boot:run

`empaquetar.cmd` genera la versión portable y el instalador de Windows (necesita jpackage y WiX 3.11, que se busca en `WIX_HOME`, `WIX` o el `PATH`). `mvn.cmd` ejecuta Maven con el JDK de `JAVA_HOME`.

## Pruebas automáticas de la interfaz
- `-Dexplorer.func=true`: 50 comprobaciones sobre los controles reales y la base de datos (usar con `-Duser.home` temporal, porque modifica datos).
- `-Dexplorer.snapshot=<carpeta>`: guarda capturas de cada pantalla. Para la de Componentes 3D se puede fijar el estado: `-Dexplorer.component=<texto del nombre>`, `-Dexplorer.explode=<0..100>` y `-Dexplorer.sheet=<0..1>` (desplazamiento de la ficha).
- `-Dexplorer.interact=true`: eventos del visor 3D (umbrales de píxeles sensibles al tiempo).

## Datos y registro
- Todo se guarda en `~/.laptop-explorer/`: la base `laptops.db`, `config.properties`, los modelos 3D importados y el registro `logs/laptop-explorer.log` (ahí queda el detalle de cualquier error).
- El esquema de la base lo crean y lo cambian las migraciones de `src/main/resources/db/migration` (Flyway); Hibernate solo comprueba al arrancar que coincide con las entidades. Para cambiar una tabla se añade una migración nueva (`V4__...`), no se edita una ya aplicada.
- Los modelos 3D por enlace solo se descargan por `https` y de servidores públicos.

## Biblioteca de componentes 3D
Está en `src/main/resources/components/`. Cada componente tiene una carpeta `<grupo>/<id>/` con tres archivos: `<id>.obj` y `<id>.mtl` (modelo a escala real en milímetros, con +Z hacia arriba) y `ficha.md` (ficha técnica en Markdown).
- `index.json` lista los componentes que se muestran: `id`, `dir` (carpeta de grupo), `category` (CPU, GPU, RAM o SSD), `name` y `segment`. Para añadir uno: copiar su carpeta y añadir su entrada.
- `materials.json` da el nombre en español de cada material del modelo; un material que no esté ahí se muestra con su identificador.
- La ficha se limpia al leerla (`AMJSpecMarkdown`): las fórmulas pasan a texto con sus unidades, y se omiten la lista de archivos de la carpeta original y los dibujos hechos con caracteres. Si a un texto le faltan cifras, se muestra tal cual con el aviso «cifra incompleta en la ficha original»; no se completa con suposiciones.

## Fichas verificadas y modelos de Blender
- `src/main/resources/specs/verified/*.json`: una ficha por laptop, con sus fuentes, lo que no se pudo confirmar y las discrepancias. Al arrancar se aplica una sola vez, y solo a los campos vacíos o que conservan el valor de la semilla: lo editado a mano no se toca.
- `blender/build_laptops.py` genera con esas fichas los 55 modelos de `src/main/resources/laptops3d/`, que el visor enlaza por marca y modelo: `blender --background --factory-startup --python blender/build_laptops.py -- [--only texto] [--blend]`.
- Un puerto sin lado confirmado no se dibuja. Lo que ninguna fuente publica (grosor de tapa, radios, marcos) se calcula con una regla común y queda anotado como supuesto en el `meta.json` de cada modelo y en la ficha técnica de la aplicación.
- Los 55 laptops tienen diseño propio. `blender/photo_designs.py`, `photo_catalog.py` y `photo_models.py` contienen los 35 modelados sobre fotos del equipo. `blender/spec_designs.py` contiene los otros 20, de los que el proyecto no guarda fotos: se construyen a partir de su ficha, de reseñas y de galerías consultadas. `blender/spec_render.py` les da textura con un render de Cycles de su propio modelo (oclusión y aristas, no fotografías) y genera su pantalla: panel de tamaño real, marcos y un fondo original, también para los Framework Laptop 16, que no tienen foto de pantalla. Su `meta.json` lleva `modelBasis: descripciones` y anota lo estimado (posición de piezas, medidas sin publicar, logotipos simplificados). Los archivos editables quedan en `blender/modelos/*.blend`.
- `blender/bridge_server.py` y `blender/bridge_send.py` permiten trabajar sobre una ventana de Blender abierta; el puente solo escucha en `127.0.0.1` y exige un token.
- `blender/report_specs.py` regenera `docs/VERIFICACION_LAPTOPS.md`, el informe por modelo de lo verificado, lo supuesto y lo no encontrado.

## Estado y pendientes
Ver `AUDITORIA_UI.md` (rediseño) e `INFORME_INTERIOR_FASE_D.md` (interiores 3D).
Interiores reconstruidos pieza por pieza: Legion Pro 7i Gen 10, LOQ 15, SCAR 18, TUF A18, Helios 16 AI, Helios 18, OMEN MAX 16, ZBook Fury 16, Victus 16. El resto es aproximado.
Pendiente: botones Nuevo, Importar y Exportar, fotos propias de los 20 modelos construidos a partir de descripciones, comparaciones sugeridas.
