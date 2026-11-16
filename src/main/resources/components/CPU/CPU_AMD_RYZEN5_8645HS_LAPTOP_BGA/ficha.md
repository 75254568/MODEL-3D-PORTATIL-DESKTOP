# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR APU MÓVIL AMD RYZEN 5 8645HS (LAPTOP BGA)
### Encapsulado Soldado FP7/FP8 • 6 Núcleos Zen 4 / 12 Hilos • Gráficos RDNA 3 Radeon 760M • Acelerador NPU Ryzen AI 31 TOPS

---

## 1. DESCRIPCIÓN GENERAL Y LA ERA DE LAS LAPTOPS CON INTELIGENCIA ARTIFICIAL

El **AMD Ryzen 5 8645HS** ("Hawk Point") es la unidad de procesamiento acelerado (APU) orientada a la democratización de la inteligencia artificial y el rendimiento gaming eficiente en computadoras portátiles modernas. Integra en un único chip de 4nm núcleos de cómputo Zen 4 de alta frecuencia, gráficos integrados Radeon 760M y la unidad de procesamiento neuronal (**NPU XDNA**).

### Innovaciones Clave de Ingeniería:
1. **Silicio Monolítico Integral "Hawk Point":**
   - Matriz de silicio de $18.50 \times 11.20\text{ mm}$ en 4nm FinFET de TSMC, balanceando un rendimiento ágil con una eficiencia de batería superior a 10 horas de uso típico.
2. **Motor Neural AMD Ryzen AI XDNA:**
   - NPU integrada capaz de **16 TOPS dedicados** (hasta **31 TOPS totales del sistema** combinando CPU, GPU y NPU), habilitando compatibilidad total con efectos de Windows Studio, edición por IA y modelos LLM ligeros locales.
3. **Gráficos Integrados Radeon 760M (RDNA 3):**
   - 8 unidades de cómputo (512 sombreadores) con aceleración de trazado de rayos por hardware, ideal para juegos competitivos y tareas multimedia.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (SOCKET FP7 / FP8)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato BGA (X)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Formato rectangular estándar de las plataformas FP7/FP8. |
| **Largo del Sustrato BGA (Y)** | **28.00 mm** | $\pm 0.10\text{ mm}$ | Sustrato compacto para placas madre de portátiles delgadas. |
| **Espesor del Sustrato PCB** | **1.05 mm** | $\pm 0.05\text{ mm}$ | PCB de 12 capas de interconexión de alta densidad (HDI). |
| **Espesor Total con Silicio (Z)** | **1.70 mm** | $\pm 0.08\text{ mm}$ | Altura mínima optimizada para chasis de 14" a 16". |
| **Matriz del Silicio Hawk Point** | **$18.50 \times 11.20\text{ mm}$** | $\pm 0.05\text{ mm}$ | Silicio monolítico Direct-Die rectificado a espejo. |
| **Esferas de Soldadura BGA** | **1,500+ Contactos** | $\varnothing 0.38\text{ mm}$ | Paso de esferas de 0.82 mm con aleación libre de plomo SAC305. |
| **Condensadores Perimétricos** | **16x MLCC Cerámicos** | Tipo SMD 0402 | Filtrado de rizado para núcleos, GPU y NPU. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES DE COMPUTACIÓN

* **Silicio:** AMD Hawk Point Monolítico (TSMC 4nm FinFET, $\approx 178\text{ mm}^2$).
* **Núcleos e Hilos:** 6 Núcleos Zen 4 • 12 Hilos de procesamiento simultáneo.
* **Frecuencias de Reloj:** Frecuencia Base de 4.30 GHz / Frecuencia Boost Máxima de **5.00 GHz**.
* **Memoria Caché:** 16 MB Caché L3 unificada + 6 MB Caché L2 (22 MB Total).
* **Unidad de IA:** AMD Ryzen AI (Arquitectura NPU XDNA) con **16 TOPS** dedicados.
* **Gráficos Integrados:** AMD Radeon 760M (8 Compute Units RDNA 3 a 2,600 MHz).
* **Potencia Térmica:** 35W a 54W TDP Configurable (cTDP) para diseños delgados y silenciosos.
* **Soporte de Memoria:** DDR5-5600 y LPDDR5X-7500 en cuádruple canal de 32 bits.