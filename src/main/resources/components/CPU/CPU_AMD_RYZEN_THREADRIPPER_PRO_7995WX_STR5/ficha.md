# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR AMD RYZEN THREADRIPPER PRO 7995WX (SOCKET sTR5)
### Zócalo Oficial AMD Socket sTR5 (LGA4844) • 96 Núcleos / 192 Hilos • Titán Workstation • 12x CCDs Zen 4

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA WORKSTATION GIGANTE

El **AMD Ryzen Threadripper PRO 7995WX** es el procesador para estaciones de trabajo y cómputo científico más poderoso del planeta, basado en la arquitectura **Zen 4** ("Storm Peak"), diseñado para placas madre con zócalo **Socket sTR5** (chipset AMD WRX90 / TRX50).

### Innovaciones Clave de Ingeniería:
1. **Escala y Dimensiones Gigantescas ($58.50 \times 75.40\text{ mm}$):**
   - Su área superficial es casi **3 veces mayor** que la de un procesador de escritorio convencional AM5.
   - Cuenta con una matriz inferior **LGA4844** con 4,844 contactos planos dorados organizados en dos bancos simétricos masivos con un canal central de desacoplo de alta frecuencia poblado por condensadores cerámicos SMD MLCC.
2. **Topología de 13 Matrices de Silicio (12 CCDs + 1 sIOD):**
   - 12 dados de cómputo Core Complex Dies (CCDs) fabricados por TSMC en 5nm (8 núcleos cada uno = 96 núcleos / 192 hilos).
   - 1 dado central Server I/O Die (sIOD) fabricado en 6nm que gestiona **128 líneas PCIe 5.0** y **8 canales de memoria DDR5 ECC Registered** (RDIMM) con hasta 2 TB de capacidad.
3. **Disipador IHS de Cobre Niquelado con Retención Torx T20:**
   - La cubierta metálica cubre más de $2,800\text{ mm}^2$ y está soldada a las 13 matrices con compuesto metálico STIM (oro-indio).
   - El sistema de anclaje utiliza 3 tornillos Torx T20 con apriete regulado de 1.5 Nm guiado por rieles laterales plásticos en el zócalo.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (SOCKET sTR5)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **58.50 mm** | $\pm 0.15\text{ mm}$ | Formato rectangular gigante sTR5. |
| **Largo del Sustrato (Y)** | **75.40 mm** | $\pm 0.15\text{ mm}$ | Longitud para alojar 13 matrices y 4,844 pads. |
| **Espesor del Sustrato PCB** | **1.60 mm** | $\pm 0.08\text{ mm}$ | Sustrato pesado verde de 14 capas de grado servidor. |
| **Espesor Total con IHS (Z)** | **4.80 mm** | $\pm 0.10\text{ mm}$ | Altura Z estandarizada para zócalo sTR5 / SP6. |
| **Domo Central del IHS** | **$46.00 \times 62.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Superficie masiva de cobre electrolítico niquelado. |
| **Orificios Guía Torx** | **4 Orificios en brida** | Exacto | Puntos de alineación pasante para el marco de retención. |
| **Indicador Pin 1** | **$2.50 \times 2.50\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Cantidad de Contactos LGA** | **4,844 Lands** | Exacto | Baño electrolítico de oro duro ($30\,\mu\text{in}$) en doble banco. |

---

## 3. FICHA TÉCNICA Y RENDIMIENTO

* **Núcleos e Hilos:** 96 Núcleos Zen 4 • 192 Hilos de procesamiento simultáneo.
* **Frecuencias de Reloj:** Base 2.50 GHz | Boost hasta 5.10 GHz.
* **Memoria Caché Total:** **480 MB** (384 MB L3 + 96 MB L2).
* **Potencia Térmica (TDP):** 350W TDP Base (Hasta 500W+ en overclocking desbloqueado).
* **Canales de Memoria:** 8 Canales DDR5-5200 ECC RDIMM (Ancho de banda superior a 300 GB/s).
* **Líneas de Entrada/Salida:** 128 Líneas PCI Express 5.0 nativas.