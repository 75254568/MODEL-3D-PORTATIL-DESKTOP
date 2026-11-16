# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR AMD EPYC 9654 "GENOA" (SERVER SOCKET SP5)
### Zócalo Oficial AMD Socket SP5 (LGA6096) • 96 Núcleos / 192 Hilos • El Mayor Encapsulado x86 de la Historia

---

## 1. DESCRIPCIÓN GENERAL Y EL GIGANTE DEL CENTRO DE DATOS

El **AMD EPYC 9654** ("Genoa") es el procesador para servidores de centros de datos y computación en la nube más avanzado y masivo jamás fabricado en la historia de la arquitectura x86, diseñado para placas madre con zócalo **Socket SP5** (LGA6096).

### Innovaciones Clave de Ingeniería:
1. **La Mayor Matriz LGA de la Industria (6,096 Contactos Dorados):**
   - El procesador mide unos asombrosos **72.00 mm de ancho por 75.40 mm de largo**, cubriendo un área de más de $5,400\text{ mm}^2$.
   - Incorpora 6,096 contactos dorados organizados en cuatro cuadrantes masivos con canales cruzados de desacoplo de condensadores cerámicos MLCC.
2. **Topología de 13 Matrices de Silicio (12 CCDs + 1 Giant Server IOD):**
   - 12 dados de cómputo Zen 4 de 8 núcleos cada uno (96 núcleos / 192 hilos).
   - Servidor I/O Die central que gestiona **12 canales de memoria DDR5 ECC** (hasta 6 TB de memoria por socket) y **128 líneas PCI Express 5.0**.
3. **Disipador IHS de Grado Centro de Datos:** Carcasa de cobre niquelado masiva soldada mediante STIM metálico de alto flujo térmico para soportar cargas continuas 24/7 a 360W-400W TDP.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (SOCKET SP5)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **72.00 mm** | $\pm 0.15\text{ mm}$ | El mayor ancho de encapsulado en computación x86. |
| **Largo del Sustrato (Y)** | **75.40 mm** | $\pm 0.15\text{ mm}$ | Longitud masiva para 13 matrices de silicio. |
| **Espesor del Sustrato PCB** | **1.70 mm** | $\pm 0.08\text{ mm}$ | Sustrato servidor de 16 capas de ultra-alta rigidez. |
| **Espesor Total con IHS (Z)** | **4.90 mm** | $\pm 0.10\text{ mm}$ | Altura Z estandarizada Socket SP5. |
| **Domo Central del IHS** | **$58.00 \times 61.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Superficie plana niquelada rectificada por CNC. |
| **Indicador Pin 1** | **$3.00 \times 3.00\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Cantidad de Contactos LGA** | **6,096 Lands** | Exacto | Baño electrolítico de oro duro ($30\,\mu\text{in}$) en 4 cuadrantes. |

---

## 3. FICHA TÉCNICA Y RENDIMIENTO

* **Núcleos e Hilos:** 96 Núcleos Zen 4 • 192 Hilos de Servidor.
* **Frecuencias de Reloj:** Base 2.40 GHz | Boost hasta 3.70 GHz All-Core.
* **Memoria Caché Total:** **416 MB** (384 MB L3 + 32 MB L2).
* **Potencia Térmica:** 360W TDP Base (Configurable hasta 400W cTDP).
* **Canales de Memoria:** 12 Canales DDR5-4800 ECC (Ancho de banda récord superior a 460 GB/s).
* **Líneas de Entrada/Salida:** 128 Líneas PCI Express 5.0 con soporte CXL 1.1+.