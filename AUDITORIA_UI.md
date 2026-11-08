# Auditoría de la aplicación (fases A y B)

Alcance: pantallas Inicio, Comparador 3D y Catálogo, más la barra lateral y la barra de estado.
Método: lectura de FXML, CSS y controladores, capturas de la versión 1.0.39 y consulta de la base de datos.
No se ha modificado ningún archivo de la aplicación.
Lo marcado como "por verificar" se deduce del código y aún no se ha probado en ejecución.

## A. Auditoría visual

| # | Hallazgo | Evidencia |
|---|----------|-----------|
| A1 | Los botones usan corchetes ASCII (`[ Guardar ]`, `[ 3D ]`) y la navegación usa `[ / ]`, `[ | ]`, `[ + ]`. Parece una terminal, no una aplicación de Windows. Es el rasgo de "prototipo" más visible. | main-view.fxml, catalog-view.fxml, comparator-view.fxml |
| A2 | Consolas se usa en botones, etiquetas, pestañas y menús, no solo en datos. La jerarquía tipográfica es plana y hay mezcla de Segoe UI y Consolas sin criterio. | app.css |
| A3 | No hay iconos en ningún sitio. | Todas las pantallas |
| A4 | Inicio: los 17 "duelos recomendados" están en un único `HBox` y se aplastan hasta quedar ilegibles (columnas de 20 px con texto cortado). | captura inicio.png |
| A5 | Inicio: círculo "VS" con borde y color ámbar, sin función. 32 tarjetas de inventario con dos botones iguales cada una. | dashboard-view.fxml |
| A6 | Comparador: la barra de herramientas se parte en dos líneas a 1440 px y mezcla vista, modo, estado, despiece, tapa y sincronización sin agrupar. | captura comparador.png |
| A7 | Comparador: el panel de especificaciones está cerrado y reducido a una franja. Al abrirlo ocupa 300 px y reduce los visores. | comparator-view.fxml |
| A8 | Catálogo: la tabla corta columnas (CPU, GPU) y tiene scroll horizontal. El formulario lateral son 28 campos planos sin agrupar. Hay mucho espacio vacío bajo la tabla con 15 filas. | captura catalogo.png |
| A9 | Textos en español sin tildes ("Catalogo", "portatiles", "Bateria", "Diseno") y mezcla con inglés ("Ghost", "Slot", "VS", "Reset"). | Toda la interfaz |
| A10 | Color: en el CSS hay más de 40 valores hexadecimales repetidos a mano en vez de usar variables. El ámbar de "VS" y el amatista del lado B no tienen función clara fuera del comparador. | app.css |
| A11 | La barra de estado muestra "Java 21 / JavaFX 21.0.4+1 / Spring Boot / Memoria". Es información de desarrollador sin utilidad para quien usa la aplicación. | main-view.fxml |
| A12 | No hay menú, atajos de teclado ni menús contextuales. | Toda la aplicación |

## B. Auditoría funcional

### Errores confirmados

| # | Hallazgo | Evidencia |
|---|----------|-----------|
| B1 | Los duelos buscan por fragmento de nombre y toman la primera coincidencia. "MacBook Pro 16" devuelve el M5 Max (2026), no el M3 Max, en el duelo "Flagship 2024". "Zephyrus G16" devuelve el GU605MY (2024) aunque hay otro modelo de 2025. "Legion Pro 7i" devuelve la Gen 10 en un duelo pensado para la Gen 9. | Consulta a la base de datos y DashboardController |
| B2 | Si un duelo no encuentra un modelo, el botón no hace nada ni avisa. | DashboardController `go.setOnAction` |
| B3 | Dos duelos tienen el mismo título ("Gigantes de 18 pulgadas"). | DashboardController |
| B4 | El `DataSeeder` vuelve a insertar al arrancar cualquier modelo de la semilla que falte. Un modelo eliminado por el usuario reaparece al reiniciar. | DataSeeder.run |
| B5 | `PartInspector` escribe datos fijos que no vienen de ninguna fuente: "Soldado a la placa (BGA)" para toda CPU, "Polímero de litio" para toda batería, "2 ventiladores (ilustrativo)" y "La disposición interna es ilustrativa" aunque 9 modelos ya tienen interior reconstruido. El montaje de la GPU se decide por si el nombre contiene "Laptop 16". Es contenido presentado como dato. | PartInspector.java |
| B6 | La inspección de componentes no usa los datos reales de piezas (`parts.csv`, con confianza y fuente). No existe estado de verificación visible. | PartInspector, parts.csv |
| B7 | Ninguno de los 32 registros tiene imagen (`image_url` vacío en 32 de 32). El campo existe en el formulario y no se usa en ninguna pantalla. | Base de datos |

### Por verificar en ejecución

| # | Hallazgo | Motivo |
|---|----------|--------|
| B8 | Al editar un modelo que está en A o B, el comparador sigue mostrando la copia antigua. | `SelectionState` guarda la entidad, no el id |
| B9 | Tras buscar o cambiar de pantalla, la tabla pierde la selección pero el formulario conserva el id anterior. Guardar entonces edita un registro que ya no se ve seleccionado. | `cargarTabla` hace `setAll`, `mostrar(null)` no limpia |
| B10 | Guardar vacía el formulario y deselecciona. Tras editar un campo hay que volver a buscar el modelo. | `onGuardar` llama a `limpiar()` |
| B11 | Un laptop nuevo registrado por el usuario no tiene interior ni modelo 3D propio. Hay que ver qué muestra el visor. | LaptopModel (tabla INSIDE por nombre) |

### Funciones incompletas o ausentes

| # | Hallazgo |
|---|----------|
| B12 | La comparación solo admite dos modelos. Se pidió seleccionar varios. |
| B13 | Filtros: solo hay búsqueda de texto. No hay filtro por marca, categoría, GPU, RAM, pantalla ni peso. El orden de columnas solo ordena la página visible de 15. |
| B14 | La paginación de 15 filas para 32 registros obliga a pasar página sin necesidad. |
| B15 | "Asignar a A/B" no avisa si no hay fila seleccionada y en Inicio no marca qué modelo está ya en A o B. |
| B16 | "Rayos X", "Térmico" y "Carga 100%" no explican qué muestran, con qué datos ni qué es estimado. |
| B17 | No hay administración de componentes: nada identifica, edita ni relaciona piezas con laptops. |
| B18 | Eliminar tiene confirmación pero no se puede deshacer. No hay exportación de datos. |

### Lo que funciona y se conserva

- CRUD básico con validación (`@Valid`, mensajes en español), confirmación al eliminar y detección de duplicados marca+modelo.
- Persistencia en SQLite dentro de la carpeta del usuario.
- Búsqueda en vivo por marca, modelo, CPU, GPU y categoría.
- Importación de CSV con informe de incidencias.
- Comparador 3D con rotación, zoom, vistas, despiece y selección de piezas.

## C-D. Propuesta (pendiente de aprobación)

**Identidad.** Interfaz técnica, sobria, tipografía Segoe UI Variable para texto y Cascadia Mono solo para valores numéricos. Fondo grafito neutro, un único acento azul para la selección y los estados interactivos, y ámbar/verde/rojo solo para advertencia, correcto y error. Radios de 2 px, sin sombras ni gradientes. Los colores pasan a variables del CSS.

**Estructura.**
1. Barra lateral estrecha con iconos vectoriales propios (Catálogo, Comparar, Visor 3D) y etiquetas.
2. Catálogo como pantalla principal: tabla completa con scroll, filtros (marca, categoría, RAM, GPU, pantalla) y ficha agrupada por secciones (Procesador, Gráficos, Memoria, Pantalla, Chasis) en un panel derecho.
3. Comparación: seleccionar 2 a 4 modelos en el catálogo y ver una tabla de diferencias resaltadas. El visor 3D sigue siendo de dos modelos.
4. Visor 3D: barra de herramientas agrupada, panel lateral con lista de componentes, estado de verificación y fuente tomados de `parts.csv`.
5. Inicio eliminado como pantalla aparte; los duelos pasan a "Comparaciones sugeridas" con identificación por id.
6. Barra de estado solo con datos útiles (registros, selección, mensajes de operación).

**Texto.** Todo en español con tildes y el término "laptop" en toda la interfaz.

## E-H. Pendiente

Rediseño de pantallas, corrección de B1 a B18, pruebas funcionales y de resolución, y pulido. No se inicia hasta aprobar la propuesta.

## Resultado de la implementación (versión de desarrollo, sin empaquetar)

Hecho: rediseño de las cuatro pantallas (Catálogo, Comparar, Visor 3D, Ajustes), sistema visual en `app.css`, iconos vectoriales propios, textos en español con tildes y el término "laptop".
Corregidos: B1 y B3 (los duelos desaparecen con la pantalla Inicio), B4 (semilla ya no resucita modelos eliminados), B5 y B6 (el inspector solo muestra datos registrados y el estado real del modelo 3D), B8 (la selección se refresca al guardar), B9 y B10 (la ficha no deja ids obsoletos y conserva la selección tras guardar), B12 (comparación de 2 a 4), B13 y B14 (filtros y sin paginación), B15 (botones desactivados cuando no aplican), B17 en parte (estado de verificación visible por componente).
Pendiente por decisión del usuario: Nuevo laptop, Importar CSV y Exportar (el servicio de importación sigue en el código, sin botón).
Pendiente: B7 (imágenes de laptops), B11 (laptop nuevo sin interior propio, hoy irrelevante sin "Nuevo"), B16 (explicar Térmica y Carga con más detalle), comparaciones sugeridas dentro del catálogo, ventana menor de 1200 px.
Pruebas: `-Dstudio.func=true` ejecuta 31 comprobaciones sobre los controles reales y contra la base de datos (0 fallos). `-Dstudio.snapshot` genera capturas. `-Dstudio.interact=true` es la prueba de eventos del visor 3D; sus umbrales de píxeles dependen del tiempo y fallan de forma intermitente.
