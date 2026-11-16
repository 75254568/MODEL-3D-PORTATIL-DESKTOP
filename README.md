# Laptop Explorer

Aplicación de escritorio (Java 21, JavaFX 21, Spring Boot, SQLite) para registrar, comparar y visualizar en 3D laptops.

## Pantallas
- **Catálogo**: tabla con búsqueda y filtros, ficha por secciones, guardar y eliminar. Se marcan de 2 a 4 laptops para comparar.
- **Comparar**: tabla de diferencias entre los laptops marcados.
- **Visor 3D**: dos visores, despiece, tapa, capas, lista de componentes y ficha de cada pieza.
- **Componentes 3D**: biblioteca de 45 procesadores, gráficas, memorias y SSD modelados a escala. Visor con vistas de cámara, cotas (ancho, fondo y alto medidos en el modelo), despiece por capas, ventiladores en movimiento y resaltado de piezas por material; al lado, la ficha técnica de cada componente.
- **Ajustes**: ubicación de la base de datos y versiones.

## Funciones añadidas
- **Buscar**: asistente con autocompletado en cada campo (las sugerencias se acotan según lo ya elegido), resultados en vivo, alternativas cercanas y similares.
- **Moneda**: selector USD/PEN en la barra superior. Los precios se guardan en USD; el tipo de cambio y su origen se definen en Ajustes.
- **Comparar**: pantalla dividida, una columna por laptop, con el mejor valor de cada fila resaltado ("Ver como tabla" conserva la vista densa).
- **Catálogo**: vista de tabla o de tarjetas; botón "Modelo 3D…" para importar un .obj (archivo o enlace directo), que se guarda en `~/.laptop-comparator/models3d/<id>/`.
- **Consulta en internet** (única función con red): usa la API de Claude con búsqueda web. Necesita una clave en Ajustes o en `ANTHROPIC_API_KEY`.

## Compilar y ejecutar
En este equipo: doble clic en `iniciar.cmd` (usa JDK 25 con las opciones que Lombok necesita).

Requiere JDK 21 y Maven.

    mvn compile
    mvn spring-boot:run

`empaquetar.cmd` genera el instalador de Windows (necesita jpackage y WiX 3.11). `mvn.cmd` usa rutas locales de una máquina concreta: ajústalas o usa tu Maven.

## Pruebas automáticas de la interfaz
- `-Dstudio.func=true`: 45 comprobaciones sobre los controles reales y la base de datos (usar con `-Duser.home` temporal, porque modifica datos).
- `-Dstudio.snapshot=<carpeta>`: guarda capturas de cada pantalla. Para la de Componentes 3D se puede fijar el estado: `-Dstudio.component=<texto del nombre>`, `-Dstudio.explode=<0..100>` y `-Dstudio.sheet=<0..1>` (desplazamiento de la ficha).
- `-Dstudio.interact=true`: eventos del visor 3D (umbrales de píxeles sensibles al tiempo).

## Datos y registro
- Todo se guarda en `~/.laptop-comparator/`: la base `laptops.db`, `config.properties`, los modelos 3D importados y el registro `logs/laptop-explorer.log` (ahí queda el detalle de cualquier error).
- El esquema de la base lo crean y lo cambian las migraciones de `src/main/resources/db/migration` (Flyway); Hibernate solo comprueba al arrancar que coincide con las entidades. Para cambiar una tabla se añade una migración nueva (`V4__...`), no se edita una ya aplicada.
- Los modelos 3D por enlace solo se descargan por `https` y de servidores públicos.

## Biblioteca de componentes 3D
Está en `src/main/resources/components/`. Cada componente tiene una carpeta `<grupo>/<id>/` con tres archivos: `<id>.obj` y `<id>.mtl` (modelo a escala real en milímetros, con +Z hacia arriba) y `ficha.md` (ficha técnica en Markdown).
- `index.json` lista los componentes que se muestran: `id`, `dir` (carpeta de grupo), `category` (CPU, GPU, RAM o SSD), `name` y `segment`. Para añadir uno: copiar su carpeta y añadir su entrada.
- `materials.json` da el nombre en español de cada material del modelo; un material que no esté ahí se muestra con su identificador.
- La ficha se limpia al leerla (`SpecMarkdown`): las fórmulas pasan a texto con sus unidades, y se omiten la lista de archivos de la carpeta original y los dibujos hechos con caracteres. Si a un texto le faltan cifras, se muestra tal cual con el aviso «cifra incompleta en la ficha original»; no se completa con suposiciones.

## Estado y pendientes
Ver `AUDITORIA_UI.md` (rediseño) e `INFORME_INTERIOR_FASE_D.md` (interiores 3D).
Interiores reconstruidos pieza por pieza: Legion Pro 7i Gen 10, LOQ 15, SCAR 18, TUF A18, Helios 16 AI, Helios 18, OMEN MAX 16, ZBook Fury 16, Victus 16. El resto es aproximado.
Pendiente: botones Nuevo, Importar y Exportar, imágenes de los laptops, comparaciones sugeridas.
