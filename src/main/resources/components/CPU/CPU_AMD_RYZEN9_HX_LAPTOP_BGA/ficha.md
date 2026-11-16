# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR AMD RYZEN 9 HX (LAPTOP GAMING MULTI-CHIPLET BGA)
### Encapsulado Oficial Socket FL1 / FP8 &bull; Arquitectura Multi-Chiplet Direct-Die &bull; Marco Protector Stiffener

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA MOBILE

Este modelo representa el procesador móvil tope de línea de AMD para portátiles gaming entusiastas (como ASUS ROG Zephyrus G16, ROG Strix SCAR 17 X3D, Lenovo Legion Pro 5/7 y Razer Blade 16).

En portátiles de máxima densidad, AMD adopta una arquitectura modular **Multi-Chiplet Direct-Die**:
1. **Tres Chips de Silicio Expuestos:** Dos dados de cómputo CCD (*Core Complex Dies*) con núcleos Zen 5/Zen 4 y memoria caché 3D V-Cache apilada verticalmente, más un dado central de entrada/salida cIOD (*Client I/O Die*).
2. **Marco Metálico de Refuerzo (*Stiffener Ring*):** Un anillo perimetral de aluminio anodizado de $0.62\text{ mm}$ de altura que rodea los dies de silicio. Este componente es fundamental en portátiles: absorbe mecánicamente el apriete de los 4 o 6 tornillos de resorte de la cámara de vapor de la notebook, impidiendo que el bloque térmico astille las esquinas del silicio expuesto.
3. **Sustrato Verde Esmeralda de 0.85 mm:** Circuito impreso multicapa de fibra de vidrio de alta pureza con 2,592 esferas de soldadura BGA inferiores.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (AMD FL1 / FP8)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (Eje X)** | **40.00 mm** | $\pm 0.15\text{ mm}$ | Cuadrante simétrico de $40.00 \times 40.00\text{ mm}$. |
| **Longitud del Sustrato (Eje Y)**| **40.00 mm** | $\pm 0.15\text{ mm}$ | Formato estándar para zócalos soldados de laptop. |
| **Espesor del Sustrato** | **0.85 mm** | $\pm 0.08\text{ mm}$ | Sustrato orgánico de fibra de vidrio verde con blindaje EMI. |
| **Espesor Total con Chips (Z)** | **1.55 mm** | Máx. $1.60\text{ mm}$ | Altura total desde las esferas BGA hasta la cresta de los CCDs. |
| **Dimensiones de Cada CCD (x2)** | **$8.40 \times 8.00\text{ mm}$** | $\pm 0.08\text{ mm}$ | Matrices de cómputo fabricadas en litografía TSMC 4nm/5nm. |
| **Dimensiones del cIOD (x1)** | **$13.00 \times 10.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Controlador de memoria, PCIe y USB fabricado en TSMC 6nm. |
| **Altura de Silicio sobre Sustrato**| **0.68 mm** | $\pm 0.03\text{ mm}$ | Sobresale $0.06\text{ mm}$ por encima del marco para contacto térmico. |
| **Marco Protector (Stiffener)** | **$36.00 \times 36.00\text{ mm}$** | Altura 0.62 mm | Marco perimetral de aleación ligera que absorbe esfuerzos mecánicos. |
| **Indicador Pin 1** | **$2.00 \times 2.00\text{ mm}$** | Exacto | Triángulo bañado en oro en la esquina superior izquierda. |
| **Cantidad de Puntos BGA** | **2,592 Esferas** | Exacto | Conexión superficial BGA soldada directamente a la placa madre. |

---

## 3. ARQUITECTURA DE SILICIO Y RENDIMIENTO

* **Núcleos y Subprocesos:**
  * **16 Núcleos Zen 5 / Zen 4 de Alto Rendimiento**.
  * **32 Hilos de procesamiento simultáneo (SMT)**.
* **Memoria Caché Masiva:**
  * Hasta **64 MB a 128 MB de Caché L3** (con tecnología 3D V-Cache apilada verticalmente mediante unión híbrida TSV).
* **Frecuencias:**
  * Frecuencia Max Boost: **Hasta 5.40 GHz**.
  * Frecuencia Base: 2.50 GHz a 2.70 GHz.
* **Interconexión:**
  * Bus AMD Infinity Fabric de ultra-baja latencia enlazando los dos CCDs con el cIOD a más de $2.0\text{ GHz}$.
* **Gestión Térmica y Potencia:**
  * Potencia Térmica Base (TDP): **55 W**.
  * Potencia Turbo Máxima (Package Power Tracking): **Hasta 140 W**.
  * Temperatura de Operación Máxima: $95^\circ\text{C}$ a $100^\circ\text{C}$.

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\CPU_AMD_RYZEN9_HX_LAPTOP_BGA`

* [CPU_AMD_RYZEN9_HX_LAPTOP_BGA.obj](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_HX_LAPTOP_BGA/CPU_AMD_RYZEN9_HX_LAPTOP_BGA.obj) &bull; Modelo CAD 3D paramétrico a escala real 1:1.
* [CPU_AMD_RYZEN9_HX_LAPTOP_BGA.mtl](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_HX_LAPTOP_BGA/CPU_AMD_RYZEN9_HX_LAPTOP_BGA.mtl) &bull; Definición de materiales PBR (Silicio, marco stiffener, sustrato verde, BGA).
* [Visor_3D_AMD_Ryzen9_HX_Laptop.html](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_HX_LAPTOP_BGA/Visor_3D_AMD_Ryzen9_HX_Laptop.html) &bull; Visor 3D interactivo offline con cotas 3D y ficha técnica.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_HX_LAPTOP_BGA/01_render_isometrico_3d.png) &bull; Render isométrico fotorrealista 2K QHD.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_HX_LAPTOP_BGA/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas de ingeniería sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_HX_LAPTOP_BGA/03_render_perfil_lateral_espesor.png) &bull; Perfil ortogonal lateral (espesor con marco 1.55 mm).
* [04_render_macro_silicio_ihs.png](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_HX_LAPTOP_BGA/04_render_macro_silicio_ihs.png) &bull; Macro detalle de los 3 chiplets CCD/IOD y marco protector.
* [05_render_posterior_pines_bga.png](file:///C:/Users/mathi/Documents/3D/CPU_AMD_RYZEN9_HX_LAPTOP_BGA/05_render_posterior_pines_bga.png) &bull; Vista posterior de la matriz de soldadura BGA-2592.
