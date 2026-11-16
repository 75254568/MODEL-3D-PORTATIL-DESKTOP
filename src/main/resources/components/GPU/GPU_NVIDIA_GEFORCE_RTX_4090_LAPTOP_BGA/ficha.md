# ESPECIFICACIONES TÉCNICAS Y COTAS DE INGENIERÍA CAD
## PROCESADOR GRÁFICO MÓVIL NVIDIA GEFORCE RTX 4090 LAPTOP (BGA)
### Arquitectura Ada Lovelace (AD103 Mobile) • 16 GB GDDR6 • Encapsulado Soldado Direct-Die FC-BGA 2500+ • 175W Máximo Dinámico

---

## 1. DESCRIPCIÓN GENERAL Y POTENCIA EN FORMATO MÓVIL DIRECT-DIE

La **NVIDIA GeForce RTX 4090 Laptop GPU** es el silicio gráfico más potente jamás integrado dentro de un factor de forma de computadora portátil para entusiastas y estaciones de trabajo móviles de alto rendimiento. Basada en el procesador **AD103**, abandona el disipador metálico convencional (IHS) para adoptar una arquitectura de contacto térmico directo (**Direct-Die**), donde el silicio desnudo rectificado a espejo transfiere calor de forma inmediata a cámaras de vapor o bloques de refrigeración líquida.

### Innovaciones Clave de Ingeniería:
1. **Encapsulado Flip-Chip BGA de Alta Densidad (FC-BGA 2500+):**
   - El sustrato de fibra de vidrio de alta temperatura de $40.00 \times 40.00\text{ mm}$ cuenta en su cara inferior con una matriz de más de **2,500 micro-esferas de estaño-plata SAC305**, distribuidas en cuadrículas de señal PCIe 4.0, bus de memoria de 256 bits y líneas de alimentación masiva Vcore/Vmem.
2. **Silicio Desnudo Espejado y Protección Epóxica Perimetral:**
   - La matriz de silicio mide $25.20 \times 15.00\text{ mm}$ con un grosor microscópico de solo 0.70 mm, sellada en sus bordes por una resina epóxica negra (*underfill*) para amortiguar vibraciones mecánicas y tensiones térmicas derivadas de los ciclos de calor de la laptop.
3. **Matriz de Condensadores de Desacoplo MLCC:**
   - Incorpora 48 condensadores cerámicos multicapa (MLCC) perimétricos montados en superficie para filtrar caídas transitorias de voltaje durante picos de boost de hasta 2,040 MHz.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS CAD (FC-BGA)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato BGA (X)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Dimensión cuadrada estándar para zócalos de laptop MXM/BGA. |
| **Largo del Sustrato BGA (Y)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Sustrato multicapa de resina BT de baja expansión térmica. |
| **Espesor del Sustrato PCB** | **1.10 mm** | $\pm 0.05\text{ mm}$ | PCB de 14 capas de pistas de cobre de ultra-alta velocidad. |
| **Espesor Total con Silicio (Z)** | **1.80 mm** | $\pm 0.08\text{ mm}$ | Perfil ultra-delgado para chasis delgados de 16" a 18". |
| **Matriz del Silicio AD103** | **$25.20 \times 15.00\text{ mm}$** | $\pm 0.05\text{ mm}$ | Silicio rectificado a espejo con grabado láser identificativo. |
| **Esferas de Soldadura BGA** | **2,500+ Esferas** | $\varnothing 0.45\text{ mm}$ | Aleación SAC305 (Sn96.5/Ag3.0/Cu0.5) con paso (*pitch*) de 0.65 mm. |
| **Chips VRAM Perimétricos** | **8x Módulos GDDR6** | $14.0 \times 12.0\text{ mm}$ | 16 GB de memoria colocados a menos de 5 mm del silicio central. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES MÓVILES

* **Silicio:** NVIDIA Ada Lovelace AD103 Mobile (TSMC 4N personalizado).
* **Transistores:** **45,900 Millones** de transistores en un área de $378.6\text{ mm}^2$.
* **Núcleos de Cómputo:**
  * **9,728 Núcleos CUDA** con frecuencia boost de 2,040 MHz.
  * **304 Núcleos Tensor de 4.ª Generación** con DLSS 3 Frame Generation.
  * **76 Núcleos RT de 3.ª Generación**.
* **Subsistema de Memoria:**
  * **Capacidad:** 16 GB GDDR6 de alto rendimiento.
  * **Bus de Memoria:** 256-bit (8 controladores de 32-bit).
  * **Velocidad de Reloj:** 18 Gbps efectivos.
  * **Ancho de Banda Bruto:** **576 GB/s**.
* **Potencia y Eficiencia:** 150W TGP base ampliable dinámicamente hasta **175W con Dynamic Boost 2.0**.
* **Interfaz del Sistema:** PCIe 4.0 x16 conectado directamente al procesador central.