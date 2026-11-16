# ESPECIFICACIONES TÉCNICAS Y COTAS DE INGENIERÍA CAD
## TARJETA GRÁFICA DEDICADA PARA LAPTOP: AMD RADEON RX 7900M (BGA)
### Arquitectura AMD RDNA 3 Chiplet (Navi 31 Mobile) • 16 GB GDDR6 • Encapsulado Soldado Direct-Die FC-BGA 2500+ • 180W SmartShift Máximo

---

## 1. DESCRIPCIÓN GENERAL Y EL BUQUE INSIGNIA MÓVIL DE AMD

La **AMD Radeon RX 7900M** es la tarjeta gráfica para portátiles más potente jamás desarrollada por AMD, creada para competir de tú a tú en la cúspide del mercado móvil (montada exclusivamente en la edición insignia del Alienware m18 R1). Es la primera y única GPU móvil de la historia en implementar un diseño desagregado en **multi-chiplet** dentro de un chasis portátil.

### Innovaciones Clave de Ingeniería:
1. **Topología de Silicio Multi-Chiplet Móvil:**
   - **GCD (Graphics Compute Die):** Núcleo central de cálculo fabricado en el nodo de **5nm de TSMC** ($17.5 \times 17.5\text{ mm}$) con 4,608 Stream Processors y 72 Compute Units.
   - **4x MCD (Memory Cache Dies):** 4 chips perimétricos fabricados en **6nm de TSMC** que integran **64 MB de Infinity Cache de 2.ª Gen** y el bus de memoria de 256 bits.
2. **Memoria VRAM de 16 GB GDDR6 en 256 Bits:**
   - 8 chips de memoria GDDR6 a 18 Gbps que entregan un ancho de banda físico de **576 GB/s**, amplificado por el enlace *Infinity Fabric* hasta superar los **2.0 TB/s** de ancho de banda efectivo.
3. **Tecnología AMD SmartShift Max de hasta 180W:**
   - Asignación dinámica de energía bidireccional entre el procesador Ryzen y la gráfica Radeon, alcanzando hasta **180W de consumo continuo** mediante refrigeración por cámara de vapor de área completa.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS CAD (FC-BGA)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato BGA (X)** | **45.00 mm** | $\pm 0.10\text{ mm}$ | El mayor sustrato BGA móvil para GPUs de consumo. |
| **Largo del Sustrato BGA (Y)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Sustrato multicapa de 16 capas de ultra-alta rigidez. |
| **Espesor del Sustrato PCB** | **1.15 mm** | $\pm 0.05\text{ mm}$ | Sustrato reforzado contra estrés térmico por chiplets. |
| **Espesor Total con Silicio (Z)** | **1.85 mm** | $\pm 0.08\text{ mm}$ | Altura Z adaptada para chasis entusiastas de 18". |
| **Matriz GCD Central (5nm)** | **$17.50 \times 17.50\text{ mm}$** | $\pm 0.05\text{ mm}$ | Silicio central rectificado a espejo. |
| **Matrices MCD (x4, 6nm)** | **$5.20 \times 5.20\text{ mm}$** | $\pm 0.05\text{ mm}$ | 4 chips satélite de memoria y caché. |
| **Esferas de Soldadura BGA** | **2,500+ Esferas** | $\varnothing 0.42\text{ mm}$ | Aleación SAC305 con paso (*pitch*) de 0.82 mm. |
| **Chips VRAM GDDR6** | **8x Módulos 2GB** | $14.0 \times 12.0\text{ mm}$ | 16 GB de memoria distribuidos perimétricamente. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES MÓVILES

* **Silicio:** AMD Navi 31 Mobile Chiplet (1x GCD en 5nm + 4x MCD en 6nm).
* **Transistores:** Más de **42,000 Millones** de compuertas lógicas integradas.
* **Unidades de Cómputo:**
  * **4,608 Stream Processors** (72 Compute Units RDNA 3).
  * **72 Ray Accelerators de 2.ª Generación**.
  * **144 AI Accelerators**.
* **Frecuencias de Reloj:** Frecuencia de Juego 1,825 MHz / Frecuencia Boost hasta **2,090 MHz**.
* **Subsistema de Memoria:**
  * **Capacidad:** 16 GB GDDR6.
  * **Bus de Memoria:** 256-bit.
  * **Infinity Cache:** 64 MB de ultra-alta velocidad.
  * **Ancho de Banda Físico:** **576 GB/s** (hasta 2,000 GB/s efectivo).
* **Potencia y SmartShift:** 160W TGP base configurable hasta **180W SmartShift Max**.
* **Interfaz de Bus:** PCI Express 4.0 x16 nativo.