# ESPECIFICACIONES TÉCNICAS Y COTAS DE INGENIERÍA CAD
## TARJETA GRÁFICA DEDICADA PARA LAPTOP: NVIDIA GEFORCE RTX 4080 (BGA)
### Arquitectura Ada Lovelace (AD104 Mobile) • 12 GB GDDR6 • Encapsulado Soldado Direct-Die FC-BGA 2300+ • 175W Máximo Dinámico

---

## 1. DESCRIPCIÓN GENERAL Y GAMA ALTA PORTÁTIL

La **NVIDIA GeForce RTX 4080 Laptop GPU** es el procesador gráfico móvil diseñado para portátiles gaming entusiastas de 16 y 18 pulgadas y estaciones de trabajo de renderizado 3D portátil. Basada en el silicio **AD104**, adopta el formato de enfriamiento directo (**Direct-Die**), donde el silicio rectificado a espejo transfiere calor directamente a bloques térmicos con cámara de vapor o interfaz de metal líquido (*Liquid Metal*).

### Innovaciones Clave de Ingeniería:
1. **Encapsulado Flip-Chip BGA de Alta Densidad (FC-BGA 2300+):**
   - Sustrato de resina BT de $40.00 \times 40.00\text{ mm}$ con más de 2,300 esferas de soldadura SAC305 (Sn96.5/Ag3.0/Cu0.5).
   - Grosor nominal de perfil bajo de $1.80\text{ mm}$ que permite su integración en chasis compactos con refrigeración de doble turbina.
2. **Memoria VRAM de 12 GB GDDR6 en 192 Bits:**
   - 6 chips de memoria Micron GDDR6 de 2 GB cada uno, colocados en disposiciones simétricas superior e inferior a escasos milímetros del silicio principal para minimizar la inductancia de pista.
   - Ancho de banda de memoria de **432 GB/s** a 18 Gbps efectivos.
3. **Capacitores de Desacoplo de Ultra-Alta Frecuencia:**
   - 40 condensadores cerámicos multicapa (MLCC) perimétricos para filtrar oscilaciones de voltaje durante picos de boost de hasta 2,280 MHz.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS CAD (FC-BGA)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato BGA (X)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Formato cuadrado estándar para plataformas gaming entusiastas. |
| **Largo del Sustrato BGA (Y)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Sustrato multicapa de 14 capas de pistas de cobre. |
| **Espesor del Sustrato PCB** | **1.10 mm** | $\pm 0.05\text{ mm}$ | Sustrato de rigidez reforzada contra ciclos térmicos. |
| **Espesor Total con Silicio (Z)** | **1.80 mm** | $\pm 0.08\text{ mm}$ | Altura Z estandarizada Direct-Die para laptops. |
| **Matriz del Silicio AD104** | **$23.50 \times 12.50\text{ mm}$** | $\pm 0.05\text{ mm}$ | Silicio rectificado a espejo con marcado láser numérico. |
| **Esferas de Soldadura BGA** | **2,300+ Esferas** | $\varnothing 0.42\text{ mm}$ | Aleación SAC305 con paso (*pitch*) de 0.82 mm. |
| **Chips VRAM GDDR6** | **6x Módulos 2GB** | $14.0 \times 12.0\text{ mm}$ | 12 GB de memoria distribuidos en 3 chips por flanco. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES MÓVILES

* **Silicio:** NVIDIA Ada Lovelace AD104 Mobile (TSMC 4N, $294.5\text{ mm}^2$).
* **Transistores:** **35,800 Millones** de compuertas lógicas integradas.
* **Núcleos de Cómputo:**
  * **7,424 Núcleos CUDA** con frecuencia boost oficial de hasta 2,280 MHz.
  * **232 Núcleos Tensor de 4.ª Generación** con soporte DLSS 3 Frame Generation.
  * **58 Núcleos RT de 3.ª Generación**.
* **Subsistema de Memoria:**
  * **Capacidad:** 12 GB GDDR6.
  * **Bus de Memoria:** 192-bit.
  * **Velocidad de Reloj:** 18 Gbps efectivos.
  * **Ancho de Banda Bruto:** **432 GB/s**.
* **Potencia y Eficiencia:** 150W TGP base ampliable dinámicamente hasta **175W con Dynamic Boost 2.0**.
* **Interfaz de Bus:** PCI Express 4.0 x16 nativo.