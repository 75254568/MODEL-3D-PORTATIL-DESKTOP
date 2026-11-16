# ESPECIFICACIONES TÉCNICAS Y COTAS DE INGENIERÍA CAD
## TARJETA GRÁFICA AMD RADEON RX 7900 XTX DESKTOP
### Arquitectura AMD RDNA 3 Chiplet (Navi 31) • 24 GB GDDR6 • Triple Ventilador Axial con Franjas Rojas Racing • 96 MB Infinity Cache

---

## 1. DESCRIPCIÓN GENERAL Y EL PRIMER DISEÑO DE CHIPLETS EN GPU

La **AMD Radeon RX 7900 XTX** es la tarjeta gráfica buque insignia de la arquitectura **RDNA 3**, marcando el primer hito de la industria en utilizar un diseño multi-chiplet desagregado en un procesador gráfico de consumo general. Al separar el núcleo central de cálculo en un proceso litográfico de 5nm de los controladores de memoria y caché de 6nm, logra una eficiencia de costes, densidad energética y rendimiento térmico sin precedentes.

### Innovaciones Clave de Ingeniería:
1. **Topología de Silicio Desagregada (1x GCD + 6x MCD):**
   - **GCD (Graphics Compute Die):** Silicio de cómputo central fabricado en TSMC 5nm con un área de $300\text{ mm}^2$.
   - **6x MCD (Memory Cache Dies):** 6 dados perimétricos fabricados en TSMC 6nm ($37\text{ mm}^2$ cada uno), que albergan 96 MB de **Infinity Cache de 2.ª Generación** y el bus de memoria de 384 bits.
   - Enlace inter-die ultra-rápido *Infinity Fabric* con un ancho de banda interno de hasta **5.3 TB/s**.
2. **Refrigeración Avanzada con Aletas Anguladas y Triple Fan:**
   - Disipador de cámara de vapor sellada al vacío que cubre el silicio y los módulos VRM.
   - 3 ventiladores axiales de 9 aspas con aletas de dirección de flujo optimizadas acústicamente y el icónico acento rojo de carreras (*AMD Red Racing Stripe*) sobre el disipador central.
3. **Alimentación Estándar Dual 8-Pin PCIe:**
   - Diseñada para una compatibilidad universal sin necesidad de adaptadores ni cables propietarios de 16 pines, utilizando dos conectores PCIe de 8 pines estándar para alimentar sus **355W TBP**.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS CAD (ATX / PCIe 4.0)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Longitud Total (X)** | **287.00 mm** | $\pm 0.40\text{ mm}$ | Formato compacto ideal para la mayoría de gabinetes ATX. |
| **Altura / Ancho (Y)** | **123.00 mm** | $\pm 0.30\text{ mm}$ | Altura estandarizada con clearance para cables PCIe. |
| **Grosor / Ranuras (Z)** | **51.00 mm** | $\pm 0.20\text{ mm}$ | Factor de forma de **2.5 Ranuras (Dual-and-a-Half Slot)**. |
| **Pines del Zócalo PCIe** | **164 Contactos** | Estándar CEM | Baño de oro electrolítico de $30\,\mu\text{in}$ PCIe 4.0 x16. |
| **Ventiladores Axiales** | **$3\times 90.00\text{ mm}$** | $\pm 0.20\text{ mm}$ | Rodamientos dinámicos de fluidos (FDB) con perfil silencioso. |
| **Conectores de Corriente** | **2x PCIe 8-Pin** | Estándar ATX | 150W por conector + 75W zócalo PCIe (hasta 375W teórico). |
| **Backplate Estructural** | **2.00 mm** | $\pm 0.10\text{ mm}$ | Placa de refuerzo de aluminio fundido anodizado negro. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES DE COMPUTACIÓN

* **Silicio:** AMD Navi 31 Chiplet (1x GCD en TSMC 5nm + 6x MCD en TSMC 6nm).
* **Transistores:** **57,700 Millones** de transistores totales combinados.
* **Unidades de Cómputo:**
  * **6,144 Stream Processors** (96 Compute Units RDNA 3).
  * **96 Ray Accelerators de 2.ª Generación** con mejoras de intersección de cajas BVH.
  * **192 AI Accelerators** dedicados a operaciones matriciales de inferencia.
* **Frecuencias de Reloj:** Frecuencia de Juego (Game Clock) 2,300 MHz / Frecuencia Boost hasta 2,500 MHz.
* **Subsistema de Memoria:**
  * **Capacidad:** 24 GB GDDR6.
  * **Bus de Memoria:** 384-bit.
  * **Infinity Cache:** 96 MB de ultra-alta velocidad.
  * **Ancho de Banda Efectivo:** Hasta **3,500 GB/s** (960 GB/s de ancho de banda físico a 20 Gbps).
* **Potencia Energética:** 355W TBP (Total Board Power) con fuente recomendada de 800W.
* **Salidas de Video:** 2x DisplayPort 2.1 (UHBR13.5), 1x HDMI 2.1a y 1x USB Type-C DisplayPort.