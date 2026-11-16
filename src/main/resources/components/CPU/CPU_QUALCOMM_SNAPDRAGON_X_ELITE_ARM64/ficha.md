# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR QUALCOMM SNAPDRAGON X ELITE (ARM64 COPILOT+ PC)
### Zócalo Mobile FCBGA-1840 • 12 Núcleos Qualcomm Oryon ARM64 • NPU Hexagon 45 TOPS • Litografía TSMC 4nm

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA ARM64 PARA WINDOWS

El **Qualcomm Snapdragon X Elite (X1E-84-100)** es el procesador basado en arquitectura **ARM64** que redefinió el estándar de portátiles con sistema operativo Windows 11, encabezando la categoría **Copilot+ PC**.

### Innovaciones Clave de Ingeniería:
1. **Núcleos Personalizados Qualcomm Oryon:**
   - 12 Núcleos de cómputo de 64 bits organizados en 3 clusters con 42 MB de memoria caché total.
   - Frecuencia All-Core de 3.80 GHz con Dual-Core Boost de hasta 4.20 GHz.
2. **NPU Hexagon de 45 TOPS:**
   - Permite la ejecución ininterrumpida de modelos de visión artificial, transcripción en tiempo real y agentes autónomos locales con un consumo de batería mínimo.
3. **Encapsulado FCBGA-1840 Ultra-Compacto ($35.00 \times 35.00\text{ mm}$):**
   - Incorpora una cubierta de dispersión térmica metálica de cobre niquelado integrada de tan solo $1.45\text{ mm}$ de altura total, permitiendo diseños de laptops sin ventilador o con disipadores de un solo ventilador ultrafino.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (FCBGA)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **35.00 mm** | $\pm 0.15\text{ mm}$ | Formato cuadrado ultracompacto para placas base móviles. |
| **Largo del Sustrato (Y)** | **35.00 mm** | $\pm 0.15\text{ mm}$ | Eje ortogonal perpendicular. |
| **Espesor del Sustrato PCB** | **0.85 mm** | $\pm 0.06\text{ mm}$ | Sustrato orgánico azul oscuro/negro de alta densidad. |
| **Espesor Total con Tapa (Z)**| **1.45 mm** | $\pm 0.08\text{ mm}$ | Perfil ultra-delgado para chasis de menos de 12 mm. |
| **Domo Central del Difusor** | **$28.00 \times 28.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Superficie plana niquelada de contacto térmico. |
| **Indicador Pin 1** | **$2.00 \times 2.00\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Matriz de Soldadura BGA** | **1,840 Esferas** | Exacto | Paso BGA de $0.50\text{ mm}$ con aleación SAC305 libre de plomo. |

---

## 3. FICHA TÉCNICA Y RENDIMIENTO

* **Núcleos e Hilos:** 12 Núcleos Qualcomm Oryon ARMv8.7-A • 12 Hilos.
* **Frecuencias de Reloj:** Base 3.80 GHz | Boost Dual-Core hasta 4.20 GHz.
* **NPU de IA:** Qualcomm Hexagon con **45 TOPS** dedicados.
* **Gráficos Integrados:** Qualcomm Adreno GPU (hasta 4.6 TFLOPS FP32).
* **Consumo Energético:** 23W Nominal (Rango configurable de 12W a 80W en boost).
* **Memoria:** LPDDR5X-8448 de 8 canales (136 GB/s de ancho de banda).