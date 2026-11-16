# ESPECIFICACIONES TÉCNICAS Y COTAS DE INGENIERÍA CAD
## TARJETA GRÁFICA NVIDIA GEFORCE RTX 4090 FOUNDERS EDITION
### Arquitectura NVIDIA Ada Lovelace (AD102-300) • 24 GB GDDR6X Micron • Chasis de Reloj de Arena en Aluminio Champán • 450W TGP

---

## 1. DESCRIPCIÓN GENERAL Y EL EMBLEMA FOUNDERS EDITION

La **NVIDIA GeForce RTX 4090 Founders Edition** es la tarjeta gráfica de escritorio de referencia más célebre de la era Ada Lovelace. Caracterizada por su icónico bastidor unibody perimetral en forma de **reloj de arena** acabado en aluminio anodizado tono champán-plateado, sentó el precedente absoluto para el diseño térmico push-pull de flujo pasante continuo (*Dual Axis Flow-Through*).

### Innovaciones Clave de Ingeniería:
1. **Chasis Monolítico en Forma de Reloj de Arena:**
   - La estructura exterior de aluminio mecanizado por CNC actúa simultáneamente como soporte rígido antitorsión de la placa base (PCB corta en forma de V) y marco de anclaje para el bloque disipador segmentado.
2. **Refrigeración Biaxial Push-Pull de 120 mm:**
   - Dos ventiladores gigantescos de 120 mm: el inferior empuja aire fresco hacia los componentes de potencia y la cámara de vapor, mientras el superior extrae el aire caliente directamente a través del paquete de aletas pasantes hacia la zona superior del chasis de la PC.
3. **Memoria GDDR6X con Señalización PAM4:**
   - Equipada con 12 módulos Micron GDDR6X de 2 GB cada uno, utilizando modulación por amplitud de pulsos de cuatro niveles (**PAM4**) para duplicar el rendimiento por ciclo de reloj, alcanzando **1,008 GB/s** de ancho de banda físico.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS CAD (ATX / PCIe 4.0)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Longitud Total (X)** | **304.00 mm** | $\pm 0.50\text{ mm}$ | Longitud total estandarizada Founders Edition. |
| **Altura / Profundidad (Y)** | **137.00 mm** | $\pm 0.30\text{ mm}$ | Altura vertical con espacio amplio para el adaptador 12VHPWR. |
| **Grosor / Ranuras (Z)** | **61.00 mm** | $\pm 0.25\text{ mm}$ | Formato masivo de **3.0 Ranuras (Triple Slot)**. |
| **Pines del Zócalo PCIe** | **164 Contactos** | Estándar CEM | Conector bañado en oro electrolítico de $30\,\mu\text{in}$ PCIe 4.0 x16. |
| **Ventiladores Axiales Biaxiales**| **$2\times 120.00\text{ mm}$** | $\pm 0.20\text{ mm}$ | Ventiladores contrarrotativos con cojinetes hidráulicos sellados. |
| **Conector de Alimentación** | **16-Pin 12VHPWR** | Estándar ATX 3.0 | Capacidad nominal de corriente continua hasta 450W / 600W. |
| **Logotipo Iluminado** | **GeForce RTX LED** | Blanco Frío | Retroiluminación LED perimetral blanco brillante con difusor mate. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES DE COMPUTACIÓN

* **Silicio:** NVIDIA Ada Lovelace AD102-300-A1 fabricado por TSMC en proceso personalizado 4N.
* **Transistores:** **76,300 Millones** de compuertas lógicas en una superficie de $608.5\text{ mm}^2$.
* **Núcleos de Cómputo:**
  * **16,384 Núcleos CUDA** a una frecuencia Boost oficial de 2,520 MHz (superando habitualmente los 2,750 MHz de fábrica).
  * **512 Núcleos Tensor de 4.ª Generación** con soporte de precisión mixta FP8 / FP16 / BF16.
  * **128 Núcleos RT de 3.ª Generación** con motores de ejecución de sombreado reordenado (SER).
* **Subsistema de Memoria:**
  * **Capacidad:** 24 GB GDDR6X Micron de alta densidad.
  * **Bus de Memoria:** 384-bit.
  * **Velocidad de Reloj:** 21 Gbps efectivos.
  * **Ancho de Banda Bruto:** **1,008 GB/s (1.01 TB/s)**.
* **Consumo Energético:** 450W TGP (con BIOS configurable por el usuario hasta 600W de límite de potencia).
* **Salidas de Video:** 3x DisplayPort 1.4a con DSC + 1x HDMI 2.1a (hasta 4K @ 240Hz u 8K @ 60Hz con HDR).