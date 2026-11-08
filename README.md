# Laptop Studio 3D

Aplicación de escritorio (Java 21, JavaFX 21, Spring Boot, SQLite) para registrar, comparar y visualizar en 3D laptops.

## Pantallas
- **Catálogo**: tabla con búsqueda y filtros, ficha por secciones, guardar y eliminar. Se marcan de 2 a 4 laptops para comparar.
- **Comparar**: tabla de diferencias entre los laptops marcados.
- **Visor 3D**: dos visores, despiece, tapa, capas, lista de componentes y ficha de cada pieza.
- **Ajustes**: ubicación de la base de datos y versiones.

## Compilar y ejecutar
Requiere JDK 21 y Maven.

    mvn compile
    mvn spring-boot:run

`empaquetar.cmd` genera el instalador de Windows (necesita jpackage y WiX 3.11). `mvn.cmd` usa rutas locales de una máquina concreta: ajústalas o usa tu Maven.

## Pruebas automáticas de la interfaz
- `-Dstudio.func=true`: 31 comprobaciones sobre los controles reales y la base de datos (usar con `-Duser.home` temporal, porque modifica datos).
- `-Dstudio.snapshot=<carpeta>`: guarda capturas de cada pantalla.
- `-Dstudio.interact=true`: eventos del visor 3D (umbrales de píxeles sensibles al tiempo).

## Estado y pendientes
Ver `AUDITORIA_UI.md` (rediseño) e `INFORME_INTERIOR_FASE_D.md` (interiores 3D).
Interiores reconstruidos pieza por pieza: Legion Pro 7i Gen 10, LOQ 15, SCAR 18, TUF A18, Helios 16 AI, Helios 18, OMEN MAX 16, ZBook Fury 16, Victus 16. El resto es aproximado.
Pendiente: botones Nuevo, Importar y Exportar, imágenes de los laptops, comparaciones sugeridas.
