# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR AMD RYZEN 9 9950X (DESKTOP SOCKET AM5)
### Zócalo Oficial AMD Socket AM5 (LGA1718) &bull; Arquitectura Zen 5 &bull; Disipador IHS Icónico 'Octopus'

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA DESKTOP

El **AMD Ryzen 9 9950X** es el procesador insignia de 16 núcleos para computadoras de escritorio basado en la arquitectura **Zen 5** ("Granite Ridge"), compatible con placas madre con zócalo **Socket AM5** (chipsets serie AMD X870E, X870, B650E, B650).

Este procesador se distingue a nivel de ingeniería por dos innovaciones visuales y estructurales revolucionarias:
1. **Disipador IHS Icónico "Octopus" (8 Patas):** A diferencia de los difusores térmicos cuadrados tradicionales, el IHS de AM5 cuenta con **8 aberturas o recortes perimetrales** (dos por cada lado). Esto permite ubicar los condensadores cerámicos de desacoplo SMD (MLCC) en la cara superior del sustrato pero fuera del domo del IHS, logrando dos ventajas críticas:
   - Elimina la necesidad de crear cavidades complejas en la cara inferior LGA, aumentando la superficie de pines de tierra y alimentación.
   - Maximiza el área plana de cobre niquelado en contacto directo con los dies de silicio bajo soldadura de oro-indio (STIM).
2. **Zócalo Land Grid Array (LGA1718):** AMD reemplazó el tradicional diseño PGA (pines en el procesador propensos a doblarse) por el estándar LGA con **1,718 contactos planos dorados**, brindando mayor densidad de señal y soporte de memoria DDR5 de alta frecuencia.
3. **Compatibilidad Retroactiva de Altura Z (Z-Height):** A pesar de la transición a LGA, la altura total ($4.30\text{ mm}$) se mantuvo exactamente idéntica a la del zócalo AM4 anterior, permitiendo a los usuarios reutilizar todos los disipadores y bloques de refrigeración líquida existentes sin adaptadores adicionales.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (AMD AM5)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (Eje X)** | **40.00 mm** | $\pm 0.15\text{ mm}$ | Cuadrante simétrico de $40.00 \times 40.00\text{ mm}$. |
| **Longitud del Sustrato (Eje Y)**| **40.00 mm** | $\pm 0.15\text{ mm}$ | Formato estandarizado Socket AM5. |
| **Espesor del Sustrato PCB** | **1.20 mm** | $\pm 0.08\text{ mm}$ | Sustrato de fibra de vidrio verde esmeralda de 10 capas. |
| **Espesor Total con IHS (Z)** | **4.30 mm** | $\pm 0.10\text{ mm}$ | Z-Height exacto compatible con disipadores AM4 y AM5. |
| **Patas de Fijación del IHS** | **1.20 mm altura** | $\pm 0.05\text{ mm}$ | 8 patas de cobre que se adhieren con adhesivo epoxi al PCB. |
| **Domo Central del IHS** | **$28.00 \times 28.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Superficie plana niquelada de contacto con el bloque térmico. |
| **Muescas Perimetrales (x8)** | **8 Recortes abiertos** | Exacto | Alojan los condensadores MLCC con recubrimiento aislante. |
| **Muescas Guía de Inserción** | **2 Muescas laterales** | Exacto | Ranuras de polarización para calzar en el zócalo AM5. |
| **Indicador Pin 1** | **$2.00 \times 2.00\text{ mm}$** | Exacto | Triángulo dorado en el vértice superior izquierdo. |
| **Cantidad de Contactos LGA** | **1,718 Lands** | Exacto | Paso de pad de $0.85\text{ mm}$ con baño electrolítico de oro de $30\,\mu\text{in}$. |

---

## 3. ARQUITECTURA DE SILICIO Y RENDIMIENTO

* **Núcleos y Subprocesos:**
  * **16 Núcleos Zen 5** fabricados en litografía TSMC 4nm (2x CCDs de 8 núcleos cada uno).
  * **32 Hilos de procesamiento simultáneo (SMT)**.
* **Frecuencias:**
  * Frecuencia Max Boost: **Hasta 5.70 GHz**.
  * Frecuencia Base: 4.30 GHz.
* **Memoria Caché:**
  * **64 MB de Caché L3** (32 MB por CCD) + **16 MB de Caché L2** (1 MB por núcleo) = **80 MB en total**.
* **Líneas de Entrada/Salida:**
  * 28 líneas PCIe Gen 5.0 (24 utilizables para GPU PCIe 5.0 x16 y dos ranuras NVMe PCIe 5.0 x4).
* **Consumo Energético y Térmica:**
  * Potencia Térmica de Diseño (TDP): **170 W**.
  * Consumo Máximo de Paquete (Package Power Tracking - PPT): **230 W**.
  * Temperatura Máxima de Operación ($T_j\text{Max}$): $95^\circ\text{C}$.

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\CPU_AMD_RYZEN9_9950X_DESKTOP_AM5`

* [CPU_AMD_RYZEN9_9950X_DESKTOP_AM5.obj](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_9950X_DESKTOP_AM5/CPU_AMD_RYZEN9_9950X_DESKTOP_AM5.obj) &bull; Modelo CAD 3D paramétrico a escala real 1:1.
* [CPU_AMD_RYZEN9_9950X_DESKTOP_AM5.mtl](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_9950X_DESKTOP_AM5/CPU_AMD_RYZEN9_9950X_DESKTOP_AM5.mtl) &bull; Definición de materiales PBR (IHS níquel, condensadores MLCC, sustrato verde, oro LGA).
* [Visor_3D_AMD_Ryzen9_9950X_Desktop.html](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_9950X_DESKTOP_AM5/Visor_3D_AMD_Ryzen9_9950X_Desktop.html) &bull; Visor 3D interactivo offline con cotas 3D y ficha técnica.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_9950X_DESKTOP_AM5/01_render_isometrico_3d.png) &bull; Render isométrico fotorrealista 2K QHD.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_9950X_DESKTOP_AM5/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas de ingeniería sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_9950X_DESKTOP_AM5/03_render_perfil_lateral_espesor.png) &bull; Perfil ortogonal lateral (espesor AM5 con IHS 4.30 mm).
* [04_render_macro_silicio_ihs.png](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_9950X_DESKTOP_AM5/04_render_macro_silicio_ihs.png) &bull; Macro detalle de los recortes perimetrales con condensadores SMD.
* [05_render_posterior_pines_bga.png](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_9950X_DESKTOP_AM5/05_render_posterior_pines_bga.png) &bull; Vista posterior de los 1,718 contactos dorados LGA1718.
