# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR INTEL CORE ULTRA 9 285K (DESKTOP LGA1851)
### Zócalo Oficial Intel Socket LGA1851 &bull; Arquitectura Arrow Lake-S &bull; Disipador IHS Asimétrico

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA DESKTOP

El **Intel Core Ultra 9 285K** es el procesador buque insignia de la familia de escritorio de última generación (Arrow Lake-S), diseñado para placas madre con zócalo **LGA1851** (chipsets serie Intel 800: Z890, B860).

A diferencia de los procesadores soldados de laptops:
1. **Zócalo Land Grid Array (LGA1851):** Cuenta con **1,851 puntos de contacto planos dorados** en la cara inferior del sustrato. Los pines con resorte residen en el zócalo de la placa madre y no en el procesador, eliminando el riesgo de doblar pines en la CPU.
2. **IHS Metálico Asimétrico de Cobre Niquelado:** Diseñado para soportar una enorme presión de apriete mecánico ($50\text{ kgf}$) ejercida por la palanca del mecanismo ILM (*Independent Loading Mechanism*) del zócalo. El IHS posee dos alas laterales extendidas que reciben los ganchos de retención.
3. **Cavidad Central Inferior de Condensadores:** En el centro de la cara inferior se ubica una cavidad rebajada donde residen más de 80 condensadores cerámicos multicapa (MLCC) para desacoplo de voltajes transitorios de alta frecuencia.
4. **Soldadura STIM de Indio:** Entre los tiles de silicio internos y la tapa metálica del IHS existe una soldadura metálica de indio que asegura la máxima conductividad térmica hacia bloques de refrigeración líquida (AIO) de $360\text{ mm}$ o disipadores de doble torre.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (INTEL LGA1851)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (Eje X)** | **45.00 mm** | $\pm 0.15\text{ mm}$ | Ancho total del sustrato FR-4 reforzado. |
| **Longitud del Sustrato (Eje Y)**| **37.50 mm** | $\pm 0.15\text{ mm}$ | Largo total del sustrato. |
| **Espesor del Sustrato PCB** | **1.15 mm** | $\pm 0.08\text{ mm}$ | Sustrato reforzado contra deformación por presión mecánica. |
| **Espesor Total con IHS (Z)** | **4.40 mm** | $\pm 0.10\text{ mm}$ | Cota Z-Height estandarizada para zócalo LGA1851. |
| **Brida Inferior del IHS** | **1.10 mm altura** | $\pm 0.05\text{ mm}$ | Reborde perimetral sellado con silicona de alta temperatura. |
| **Domo Central del IHS** | **$34.00 \times 28.50\text{ mm}$** | $\pm 0.10\text{ mm}$ | Superficie plana niquelada de contacto con el bloque disipador. |
| **Alas de Retención Laterales** | **$4.00\text{ mm}$ vuelo** | $\pm 0.08\text{ mm}$ | Alas que reciben los rodillos de presión del cerrojo ILM. |
| **Muescas Guía de Inserción** | **2 Muescas en bordes** | Exacto | Ranuras asimétricas que impiden la instalación invertida. |
| **Indicador Pin 1** | **$2.00 \times 2.00\text{ mm}$** | Exacto | Triángulo bañado en oro en la esquina superior izquierda. |
| **Cantidad de Contactos LGA** | **1,851 Lands** | Exacto | Paso de pad de $0.90\text{ mm}$ con baño de oro duro de $30\,\mu\text{in}$. |

---

## 3. ARQUITECTURA DE SILICIO Y RENDIMIENTO

* **Núcleos y Subprocesos:**
  * **24 Núcleos de Procesamiento:** 8 P-Cores ("Lion Cove") + 16 E-Cores ("Skymont").
  * **24 Hilos de ejecución** (Arquitectura optimizada sin Hyper-Threading para máxima eficiencia por vatio).
* **Frecuencias:**
  * Frecuencia Turbo Máxima: **Hasta 5.70 GHz** (Intel Thermal Velocity Boost).
  * Frecuencia Base: 3.20 GHz (P-Cores) / 2.60 GHz (E-Cores).
* **Memoria Caché:**
  * **36 MB de Intel Smart Cache (L3)** + **40 MB de Caché L2** (Total 76 MB de caché combinada).
* **Líneas de Entrada/Salida:**
  * 24 líneas PCIe totales (hasta 20 líneas PCIe Gen 5.0 directas de CPU para GPU y SSD NVMe).
* **Consumo Energético y Térmica:**
  * Potencia Base del Procesador (TDP): **125 W**.
  * Potencia Turbo Máxima (Maximum Turbo Power - MTP): **250 W**.
  * Temperatura Máxima de Operación: $105^\circ\text{C}$.

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851`

* [CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851.obj](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851/CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851.obj) &bull; Modelo CAD 3D paramétrico a escala real 1:1.
* [CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851.mtl](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851/CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851.mtl) &bull; Definición de materiales PBR (IHS níquel, contactos oro, grabado láser).
* [Visor_3D_Intel_Core_Ultra9_285K_Desktop.html](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851/Visor_3D_Intel_Core_Ultra9_285K_Desktop.html) &bull; Visor 3D interactivo offline con cotas 3D y ficha técnica.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851/01_render_isometrico_3d.png) &bull; Render isométrico fotorrealista 2K QHD.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas de ingeniería sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851/03_render_perfil_lateral_espesor.png) &bull; Perfil ortogonal lateral (espesor desktop con IHS 4.40 mm).
* [04_render_macro_silicio_ihs.png](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851/04_render_macro_silicio_ihs.png) &bull; Macro detalle de serigrafía láser sobre níquel satinado.
* [05_render_posterior_pines_bga.png](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_285K_DESKTOP_LGA1851/05_render_posterior_pines_bga.png) &bull; Vista posterior de los 1,851 contactos dorados LGA y cavidad MLCC.
