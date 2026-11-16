# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR AMD RYZEN AI 9 HX 370 (LAPTOP BGA)
### Zócalo Mobile FP8 (BGA-2070) • Arquitectura Híbrida Zen 5/5c • NPU XDNA 2 (50 TOPS) • Gráficos RDNA 3.5

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA MOBILE DIRECT-DIE

El **AMD Ryzen AI 9 HX 370** ("Strix Point") es el procesador móvil insignia de AMD diseñado para laptops ultraligeras y de alto rendimiento que lidera la era de computadoras **Copilot+ PC**, integrando la NPU neuronal más veloz del mundo en un formato portátil.

### Innovaciones Clave de Ingeniería:
1. **Arquitectura Híbrida Zen 5 + Zen 5c:**
   - 4 Núcleos Zen 5 de máximo rendimiento y frecuencia para tareas monohilo exigentes.
   - 8 Núcleos Zen 5c de alta eficiencia y densidad compacta para multihilo masivo.
   - Total: 12 Núcleos y 24 Hilos de procesamiento simultáneo.
2. **NPU XDNA 2 con 50 TOPS de Cómputo Neuronal:**
   - Supera ampliamente el estándar de 40 TOPS exigido por Microsoft Copilot+.
   - Ejecuta modelos LLM locales de visión e inferencia sin conexión a la nube con precisión matemática Block FP16.
3. **Silicio Monolítico Direct-Die Slim (1.37 mm):**
   - A diferencia de los procesadores de escritorio basados en chiplets, el HX 370 utiliza una sola pastilla monolítica de silicio pulido a espejo de $21.20 \times 11.00\text{ mm}$ fabricada en proceso TSMC 4nm.
   - Hace contacto directo con la cámara de vapor de la laptop usando compuesto térmico de metal líquido o almohadillas de cambio de fase PTM7950.
4. **Gráficos Integrados RDNA 3.5 (Radeon 890M):**
   - 16 Unidades de Cómputo (1,024 stream processors) que alcanzan el rendimiento de GPUs discretas para gaming ligero a 1080p.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (SOCKET FP8)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **40.00 mm** | $\pm 0.15\text{ mm}$ | Formato rectangular optimizado para placas madre móviles. |
| **Largo del Sustrato (Y)** | **25.00 mm** | $\pm 0.15\text{ mm}$ | Anchura ultracompacta para notebooks delgadas. |
| **Espesor del Sustrato PCB** | **0.72 mm** | $\pm 0.06\text{ mm}$ | Sustrato verde HDI de alta densidad interconectada. |
| **Espesor Total Direct-Die (Z)**| **1.37 mm** | $\pm 0.08\text{ mm}$ | Altura ultra-slim sin tapa metálica IHS. |
| **Dimensiones Die Silicio** | **$21.20 \times 11.00\text{ mm}$** | $\pm 0.05\text{ mm}$ | Área de $233.2\text{ mm}^2$ con espejo de silicio expuesto. |
| **Altura Silicio sobre Sustrato**| **0.65 mm** | $\pm 0.04\text{ mm}$ | Altura de la pastilla con sellado perimetral de epoxi negro. |
| **Indicador Pin 1** | **$1.80 \times 1.80\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Matriz de Soldadura BGA** | **2,070 Esferas** | Exacto | Paso BGA de $0.50\text{ mm}$ con aleación libre de plomo SAC305. |

---

## 3. FICHA TÉCNICA Y RENDIMIENTO

* **Núcleos e Hilos:** 12 Núcleos (4 Zen 5 + 8 Zen 5c) • 24 Hilos.
* **Frecuencias:** Base 2.00 GHz | Boost hasta 5.10 GHz.
* **Potencia Térmica (TDP):** 28W Nominal (Rango configurable por fabricante de 15W a 54W).
* **NPU de IA:** AMD XDNA 2 con hasta 50 TOPS dedicados (80 TOPS en combinación con CPU y GPU).
* **Gráficos Integrados:** AMD Radeon 890M (16 CUs RDNA 3.5 a 2,900 MHz).
* **Memoria Soportada:** LPDDR5X-7500 / DDR5-5600 Dual Channel.