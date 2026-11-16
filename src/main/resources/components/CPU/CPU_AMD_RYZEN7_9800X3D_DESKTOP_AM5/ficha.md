# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR AMD RYZEN 7 9800X3D (DESKTOP SOCKET AM5)
### Zócalo Oficial AMD Socket AM5 (LGA1718) • Arquitectura Zen 5 • 2da Gen 3D V-Cache (Caché Invertida)

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA DE CACHÉ 3D INVERTIDA

El **AMD Ryzen 7 9800X3D** es el procesador de 8 núcleos para juegos más avanzado y potente del mundo para computadoras de escritorio, basado en la arquitectura **Zen 5** y equipado con la **2da Generación de tecnología 3D V-Cache** de AMD, compatible con el zócalo **Socket AM5** (LGA1718).

### Innovaciones Clave de Ingeniería:
1. **Pila de Silicio Invertida (Inverted 3D V-Cache):**
   - En generaciones previas (como el 7800X3D), la memoria caché SRAM 3D de 64MB se apilaba *encima* de los núcleos Zen, actuando como una barrera térmica que dificultaba la disipación hacia el disipador metálico IHS.
   - En el **9800X3D**, AMD invirtió la arquitectura: la matriz de caché 3D V-Cache de 64MB se sitúa ahora **DEBAJO** del Core Complex Die (CCD) de cómputo.
   - Esto permite que los núcleos Zen 5 de alto calor hagan **contacto térmico directo con el IHS niquelado** a través de soldadura metálica STIM (oro-indio), logrando temperaturas hasta 15°C más bajas y permitiendo frecuencias sostenidas mucho más altas (hasta 5.20 GHz) y overclocking completo sin bloqueos térmicos.
2. **Disipador IHS Icónico "Octopus" (8 Patas):** Formato patentado AM5 con 8 recortes simétricos que exponen 16 condensadores cerámicos de desacoplo MLCC SMD 0402 en la cara superior.
3. **Zócalo LGA1718 (AM5):** 1,718 contactos planos dorados de alta resistencia con paso de pad de 0.85 mm.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (AMD AM5)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **40.00 mm** | $\pm 0.15\text{ mm}$ | Formato cuadrado simétrico estándar Socket AM5. |
| **Largo del Sustrato (Y)** | **40.00 mm** | $\pm 0.15\text{ mm}$ | Eje ortogonal perpendicular. |
| **Espesor del Sustrato PCB** | **1.48 mm** | $\pm 0.08\text{ mm}$ | Sustrato orgánico multicapa verde esmeralda de 12 capas. |
| **Altura Total con IHS (Z)** | **5.28 mm** | $\pm 0.10\text{ mm}$ | Z-Height exacto compatible con disipadores AM4/AM5. |
| **Domo Central del IHS** | **$26.00 \times 26.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Área de transferencia térmica de cobre electrolítico niquelado. |
| **Recortes Perimetrales (x8)** | **8 Recortes abiertos** | Exacto | Alojan los condensadores MLCC protegidos contra cortocircuito. |
| **Indicador Pin 1** | **$2.00 \times 2.00\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Cantidad de Contactos LGA** | **1,718 Lands** | Exacto | Baño electrolítico de oro duro ($30\,\mu\text{in}$). |

---

## 3. FICHA TÉCNICA Y RENDIMIENTO

* **Núcleos e Hilos:** 8 Núcleos Zen 5 / 16 Hilos.
* **Memoria Caché Total:** **104 MB** (8MB L2 + 32MB L3 Zen 5 + 64MB 3D V-Cache apilada).
* **Frecuencias:** Base 4.70 GHz | Boost hasta 5.20 GHz (Desbloqueado para Overclocking).
* **Consumo Energético (TDP):** 120W TDP Nominal | 162W PPT Máximo.
* **Proceso Litográfico:** TSMC 4nm FinFET (CCD) + TSMC 6nm (cIOD).
* **Soporte de Memoria:** Dual Channel DDR5-6000+ EXPO nativo.