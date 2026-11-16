# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR INTEL CORE ULTRA 7 265K (DESKTOP SOCKET LGA1851)
### Zócalo Oficial Intel Socket LGA1851 • 20 Núcleos (8P + 12E) • Arrow Lake-S Foveros 3D • 5.50 GHz

---

## 1. DESCRIPCIÓN GENERAL Y EL EQUILIBRIO PERFECTO DE ARROW LAKE

El **Intel Core Ultra 7 265K** es el procesador de 20 núcleos de gama alta de la nueva generación **Arrow Lake-S**, diseñado para placas madre con zócalo **Socket LGA1851** (chipsets Intel serie 800: Z890, B860).

### Innovaciones Clave de Ingeniería:
1. **Eficiencia Energética Histórica en Desktop:**
   - Reduce el consumo de energía en hasta **100 W** en comparación con su antecesor directo (el Core i7-14700K), operando con temperaturas hasta 15°C más bajas.
2. **Empaquetado Foveros 3D Multi-Tile en Litografía TSMC 3nm:**
   - **Compute Tile (TSMC N3B 3nm):** 8 P-Cores "Lion Cove" y 12 E-Cores "Skymont".
   - **Graphics Tile (TSMC N4P 4nm):** Gráficos integrados Intel Xe-LPG.
   - **SoC Tile (TSMC N6):** NPU de inteligencia artificial integrada (13 TOPS) y controlador de memoria.
3. **Disipador IHS Asimétrico con Alas Laterales ILM:**
   - Carcasa protectora de cobre niquelado de $37.50 \times 45.00\text{ mm}$ diseñada para soportar hasta 50 kgf de apriete uniforme del nuevo zócalo LGA1851.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (LGA1851)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **45.00 mm** | $\pm 0.15\text{ mm}$ | Formato rectangular optimizado LGA1851. |
| **Largo del Sustrato (Y)** | **37.50 mm** | $\pm 0.15\text{ mm}$ | Eje ortogonal. |
| **Espesor del Sustrato PCB** | **1.15 mm** | $\pm 0.08\text{ mm}$ | Sustrato multicapa Intel Teal reforzado. |
| **Espesor Total con IHS (Z)** | **4.40 mm** | $\pm 0.10\text{ mm}$ | Altura Z estandarizada para zócalo LGA1851. |
| **Domo Central del IHS** | **$34.00 \times 28.50\text{ mm}$** | $\pm 0.10\text{ mm}$ | Superficie plana niquelada de contacto con bloque líquido. |
| **Alas Laterales ILM (x2)** | **$1.60\text{ mm}$ espesor** | $\pm 0.05\text{ mm}$ | Puntos de contacto donde asientan las levas del ILM. |
| **Muescas de Guía (x2)** | **2 Muescas laterales** | Exacto | Evitan inserción incorrecta con giro de 180°. |
| **Indicador Pin 1** | **$2.00 \times 2.00\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Cantidad de Contactos LGA** | **1,851 Lands** | Exacto | Baño electrolítico de oro duro ($30\,\mu\text{in}$). |

---

## 3. FICHA TÉCNICA Y RENDIMIENTO

* **Núcleos e Hilos:** 20 Núcleos (8 Lion Cove P-Cores + 12 Skymont E-Cores) • 20 Hilos.
* **Frecuencias de Reloj:** Base 3.90 GHz (P-Core) / 3.30 GHz (E-Core) | P-Core Turbo hasta 5.50 GHz.
* **Consumo Energético:** 125W Processor Base Power (PBP) | 250W Maximum Turbo Power (MTP).
* **Caché Inteligente Intel:** 30 MB Intel Smart Cache (L3) + 36 MB L2 (Total: 66 MB).
* **NPU de Inteligencia Artificial:** Intel AI Boost (hasta 13 TOPS dedicados).
* **Memoria:** Dual Channel DDR5-6400 nativo (Hasta 192 GB).