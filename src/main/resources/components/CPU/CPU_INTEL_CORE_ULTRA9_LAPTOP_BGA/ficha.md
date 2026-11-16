# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR INTEL CORE ULTRA 9 (LAPTOP GAMING BGA)
### Encapsulado Oficial BGA-2833 &bull; Arquitectura Foveros 3D Direct-Die &bull; Sin Tapa Metálica (IHS)

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA MOBILE

Este modelo representa el procesador móvil insignia de Intel para laptops para videojuegos y estaciones móviles de máxima potencia (como Lenovo Legion Pro 7, ASUS ROG Strix SCAR 16/18, Alienware m16/m18 y MSI Raider).

A diferencia de los procesadores de escritorio, los procesadores para portátiles de gama alta se montan en formato **Direct-Die BGA (Ball Grid Array)**:
1. **Ausencia de IHS (Integrated Heat Spreader):** El silicio de los núcleos de cómputo y gráficos se encuentra expuesto al aire libre y pulido a espejo.
2. **Espesor Ultra-Slim de 1.48 mm:** Permite que las cámaras de vapor de cobre (*vapor chambers*) y los heatpipes ultra-delgados hagan contacto físico directo con la matriz de silicio utilizando compuesto térmico de **Metal Líquido (Liquid Metal)** o almohadillas de cambio de fase (PTM7950).
3. **Sustrato HDI de 0.80 mm:** Circuito impreso de resina orgánica multicapa (12 capas) color cerceta/azul con matriz inferior de 2,833 esferas de soldadura BGA de aleación SAC305.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (INTEL BGA-2833)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (Eje X)** | **45.00 mm** | $\pm 0.15\text{ mm}$ | Ancho total del encapsulado orgánico HDI. |
| **Longitud del Sustrato (Eje Y)**| **37.50 mm** | $\pm 0.15\text{ mm}$ | Largo total del encapsulado. |
| **Espesor del Sustrato** | **0.80 mm** | $\pm 0.08\text{ mm}$ | Sustrato orgánico multicapa de alta densidad de interconexión. |
| **Espesor Total (Z-Height)** | **1.48 mm** | Máx. $1.52\text{ mm}$ | Altura máxima desde la base de soldadura hasta la cresta del silicio. |
| **Dimensiones Matriz Silicio** | **$26.50 \times 12.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Matriz Foveros 3D unificada (Compute, Graphics, SoC, I/O Tiles). |
| **Altura del Silicio sobre Sustrato**| **0.68 mm** | $\pm 0.03\text{ mm}$ | Espesor del silicio pulido con tolerancia de planitud de $15\,\mu\text{m}$. |
| **Bisel y Sellado de Borde** | **Filete Epoxi 0.70 mm** | $45^\circ$ | Resina negra (*underfill*) para absorción de dilatación térmica. |
| **Indicador Pin 1** | **$2.00 \times 2.00\text{ mm}$** | Exacto | Triángulo bañado en oro duro en el vértice superior izquierdo. |
| **Cantidad de Puntos BGA** | **2,833 Esferas** | Exacto | Matriz BGA de paso $0.45\text{ mm}$ soldada a $245^\circ\text{C}$ a la motherboard. |
| **Desacoplo Superior** | **40+ Condensadores MLCC** | SMD 0201/0402 | Filtrado transitorio para rieles de bajo voltaje (0.7V - 1.25V). |

---

## 3. ARQUITECTURA DE SILICIO Y RENDIMIENTO

* **Núcleos y Subprocesos:**
  * **24 Núcleos en total:** 8 P-Cores (Lion Cove / Raptor Cove) de alto rendimiento + 16 E-Cores (Skymont / Gracemont) de alta eficiencia energética.
  * **24 a 32 Hilos de procesamiento simultáneo**.
* **Frecuencias:**
  * Frecuencia Turbo Máxima: **Hasta 5.40 GHz - 5.80 GHz**.
  * Frecuencia Base: 2.20 GHz (P-Cores) / 1.60 GHz (E-Cores).
* **Gráficos Integrados:**
  * Intel Arc GPU Tile con arquitectura Xe-LPG de hasta 8 núcleos Xe con soporte completo DirectX 12 Ultimate y ray tracing por hardware.
* **NPU de Inteligencia Artificial:**
  * Motor neuronal integrado para aceleración de modelos locales de IA en Windows Copilot+ PC.
* **Gestión Térmica y Potencia:**
  * Potencia Base (TDP PL1): **55 W**.
  * Potencia Turbo Máxima (PL2): **157 W a 175 W**.
  * Temperatura de Unión Máxima ($T_j\text{Max}$): $100^\circ\text{C} - 105^\circ\text{C}$.

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA`

* [CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA.obj](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA/CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA.obj) &bull; Modelo CAD 3D paramétrico a escala real 1:1.
* [CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA.mtl](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA/CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA.mtl) &bull; Definición de materiales PBR (Silicio espejado, epoxi, sustrato teal, BGA).
* [Visor_3D_Intel_Core_Ultra9_Laptop.html](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA/Visor_3D_Intel_Core_Ultra9_Laptop.html) &bull; Visor 3D interactivo offline con cotas 3D y ficha técnica.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA/01_render_isometrico_3d.png) &bull; Render isométrico fotorrealista 2K QHD.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas de ingeniería sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA/03_render_perfil_lateral_espesor.png) &bull; Perfil ortogonal lateral (espesor slim 1.48 mm).
* [04_render_macro_silicio_ihs.png](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA/04_render_macro_silicio_ihs.png) &bull; Macro detalle de la matriz de silicio Foveros 3D y condensadores MLCC.
* [05_render_posterior_pines_bga.png](file:///C:/Users/mathi/Documents/3D/CPU_INTEL_CORE_ULTRA9_LAPTOP_BGA/05_render_posterior_pines_bga.png) &bull; Vista posterior de la matriz de soldadura BGA-2833.
