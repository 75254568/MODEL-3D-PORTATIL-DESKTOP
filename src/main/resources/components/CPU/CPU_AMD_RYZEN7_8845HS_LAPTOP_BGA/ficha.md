# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR APU MÓVIL AMD RYZEN 7 8845HS (LAPTOP BGA)
### Encapsulado Soldado FP7/FP8 • 8 Núcleos Zen 4 / 16 Hilos • Gráficos RDNA 3 Radeon 780M • Acelerador NPU Ryzen AI 38 TOPS

---

## 1. DESCRIPCIÓN GENERAL Y LA COMPUTADORA PORTÁTIL CON INTELIGENCIA ARTIFICIAL

El **AMD Ryzen 7 8845HS** ("Hawk Point") es la unidad de procesamiento acelerado (APU) insignia para computadoras portátiles equilibradas, notebooks delgadas de alto rendimiento y equipos ultraligeros de última generación preparados para **Copilot+ PC**. Combina en un único silicio monolítico de 4nm núcleos de cómputo Zen 4 de alta frecuencia, la potente GPU integrada Radeon 780M y un motor de red neuronal (**NPU XDNA**) dedicado a tareas de inferencia local de IA.

### Innovaciones Clave de Ingeniería:
1. **Silicio Monolítico Integral "Hawk Point":**
   - Una única matriz de silicio de $18.50 \times 11.20\text{ mm}$ fabricada en el nodo de 4nm FinFET de TSMC, optimizada para ofrecer una autonomía de batería sobresaliente junto a un rendimiento sostenido de primer nivel.
2. **Motor Neural AMD Ryzen AI XDNA:**
   - NPU integrada con capacidad de **16 TOPS dedicados** (hasta **38 TOPS totales del sistema** combinando CPU, GPU y NPU), acelerando modelos de lenguaje local, efectos de cámara de Windows Studio y eliminación de ruido por IA sin penalizar el consumo de batería.
3. **Gráficos Integrados Radeon 780M (RDNA 3):**
   - 12 unidades de cómputo (768 sombreadores) con trazado de rayos por hardware capaces de ejecutar títulos modernos en resolución 1080p sin necesidad de gráfica dedicada.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (SOCKET FP7 / FP8)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato BGA (X)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Formato rectangular estándar de las plataformas FP7/FP8. |
| **Largo del Sustrato BGA (Y)** | **28.00 mm** | $\pm 0.10\text{ mm}$ | Sustrato ultracompacto para placas madre delgadas de laptop. |
| **Espesor del Sustrato PCB** | **1.05 mm** | $\pm 0.05\text{ mm}$ | PCB de 12 capas de interconexión de alta densidad (HDI). |
| **Espesor Total con Silicio (Z)** | **1.70 mm** | $\pm 0.08\text{ mm}$ | Altura mínima optimizada para chasis de 14" y 16". |
| **Matriz del Silicio Hawk Point** | **$18.50 \times 11.20\text{ mm}$** | $\pm 0.05\text{ mm}$ | Silicio monolítico Direct-Die rectificado a espejo. |
| **Esferas de Soldadura BGA** | **1,500+ Contactos** | $\varnothing 0.38\text{ mm}$ | Paso de esferas de 0.82 mm con aleación libre de plomo SAC305. |
| **Condensadores Perimétricos** | **18x MLCC Cerámicos** | Tipo SMD 0402 | Filtrado de voltaje para raíles de núcleo, GPU y NPU. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES DE COMPUTACIÓN

* **Silicio:** AMD Hawk Point Monolítico (TSMC 4nm FinFET, $\approx 178\text{ mm}^2$).
* **Núcleos e Hilos:** 8 Núcleos Zen 4 • 16 Hilos de procesamiento simultáneo.
* **Frecuencias de Reloj:** Frecuencia Base de 3.80 GHz / Frecuencia Boost Máxima de **5.10 GHz**.
* **Memoria Caché:** 16 MB Caché L3 unificada + 8 MB Caché L2 (24 MB Total).
* **Unidad de IA:** AMD Ryzen AI (Arquitectura NPU XDNA) con **16 TOPS** dedicados.
* **Gráficos Integrados:** AMD Radeon 780M (12 Compute Units RDNA 3 a 2,700 MHz).
* **Potencia Térmica:** 35W a 54W TDP Configurable (cTDP) para diseños delgados y silenciosos.
* **Soporte de Memoria:** DDR5-5600 y LPDDR5X-7500 en cuádruple canal de 32 bits.