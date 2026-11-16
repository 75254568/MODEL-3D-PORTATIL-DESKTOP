# ESPECIFICACIONES TÉCNICAS Y COTAS DE INGENIERÍA CAD
## TARJETA GRÁFICA DEDICADA PARA LAPTOP: NVIDIA GEFORCE RTX 4070 (BGA)
### Arquitectura Ada Lovelace (AD106 Mobile) • 8 GB GDDR6 • Encapsulado Soldado Direct-Die FC-BGA 2100+ • 140W Máximo Dinámico

---

## 1. DESCRIPCIÓN GENERAL Y EL PUNTO DULCE ENTUSIASTA EN PORTÁTILES

La **NVIDIA GeForce RTX 4070 Laptop GPU** es el procesador gráfico para computadoras portátiles equilibradas de alta gama (como Lenovo Legion Pro 5, ASUS ROG Strix G16 y HP OMEN 16), combinando una alta eficiencia por vatio con rendimiento fluido en resoluciones 1440p (QHD). Basada en el procesador **AD106**, opera en formato de contacto directo **Direct-Die**.

### Innovaciones Clave de Ingeniería:
1. **Encapsulado Compacto FC-BGA 2100+:**
   - Sustrato de resina BT de $37.50 \times 37.50\text{ mm}$ con más de 2,100 micro-esferas de soldadura BGA.
   - Perfil térmico optimizado para chasis de 15.6" y 16" con disipadores de 3 o 4 caloductos de cobre.
2. **Memoria VRAM de 8 GB GDDR6 en 128 Bits:**
   - 4 chips de memoria Samsung o Micron GDDR6 de 2 GB cada uno, situados a los laterales de la matriz central.
   - Ancho de banda de **256 GB/s** a 16 Gbps efectivos con asistencia de 32 MB de caché L2 interna en el silicio.
3. **Eficiencia Energética Superior:**
   - Alcanza su punto óptimo de curva de frecuencia/voltaje a partir de los 100W-115W, permitiendo portátiles más delgadas y silenciosas.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS CAD (FC-BGA)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato BGA (X)** | **37.50 mm** | $\pm 0.10\text{ mm}$ | Formato cuadrado normalizado para placas madre móviles. |
| **Largo del Sustrato BGA (Y)** | **37.50 mm** | $\pm 0.10\text{ mm}$ | Sustrato multicapa de 12 capas de interconexión. |
| **Espesor del Sustrato PCB** | **1.10 mm** | $\pm 0.05\text{ mm}$ | Sustrato reforzado contra deformación térmica. |
| **Espesor Total con Silicio (Z)** | **1.80 mm** | $\pm 0.08\text{ mm}$ | Altura estandarizada Direct-Die para laptops. |
| **Matriz del Silicio AD106** | **$18.50 \times 10.20\text{ mm}$** | $\pm 0.05\text{ mm}$ | Silicio rectificado a espejo con marcado láser numérico. |
| **Esferas de Soldadura BGA** | **2,100+ Esferas** | $\varnothing 0.42\text{ mm}$ | Aleación SAC305 con paso (*pitch*) de 0.80 mm. |
| **Chips VRAM GDDR6** | **4x Módulos 2GB** | $14.0 \times 12.0\text{ mm}$ | 8 GB de memoria distribuidos en 2 chips por flanco. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES MÓVILES

* **Silicio:** NVIDIA Ada Lovelace AD106 Mobile (TSMC 4N, $187.8\text{ mm}^2$).
* **Transistores:** **22,900 Millones** de compuertas lógicas integradas.
* **Núcleos de Cómputo:**
  * **4,608 Núcleos CUDA** con frecuencia boost de hasta 2,175 MHz.
  * **144 Núcleos Tensor de 4.ª Generación** con DLSS 3 Frame Generation.
  * **36 Núcleos RT de 3.ª Generación**.
* **Subsistema de Memoria:**
  * **Capacidad:** 8 GB GDDR6.
  * **Bus de Memoria:** 128-bit.
  * **Velocidad de Reloj:** 16 Gbps efectivos.
  * **Ancho de Banda Bruto:** **256 GB/s**.
* **Potencia y Eficiencia:** 115W TGP base ampliable hasta **140W con Dynamic Boost 2.0**.
* **Interfaz de Bus:** PCI Express 4.0 x8 nativo.