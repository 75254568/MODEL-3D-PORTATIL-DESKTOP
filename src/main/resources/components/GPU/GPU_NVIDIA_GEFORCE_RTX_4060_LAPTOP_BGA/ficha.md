# ESPECIFICACIONES TÉCNICAS Y COTAS DE INGENIERÍA CAD
## TARJETA GRÁFICA DEDICADA PARA LAPTOP: NVIDIA GEFORCE RTX 4060 (BGA)
### Arquitectura Ada Lovelace (AD107 Mobile) • 8 GB GDDR6 • Encapsulado Soldado Direct-Die FC-BGA 1900+ • La GPU de Laptop Más Vendida del Mundo

---

## 1. DESCRIPCIÓN GENERAL Y EL ESTÁNDAR GLOBAL GAMING EN PORTÁTILES

La **NVIDIA GeForce RTX 4060 Laptop GPU** es el procesador gráfico dedicado para portátiles más vendido, popular y extendido del mundo, montado en millones de equipos (como Lenovo LOQ 15, ASUS TUF A15/F15, Acer Nitro 16, HP Victus y Dell G15). Basada en el chip **AD107**, destaca por ofrecer un rendimiento excepcional a 1080p y 1440p con un consumo eléctrico sumamente reducido.

### Innovaciones Clave de Ingeniería:
1. **Encapsulado Ultracompacto FC-BGA 1900+:**
   - Sustrato de resina BT de tan solo $35.00 \times 35.00\text{ mm}$ con más de 1,900 esferas de soldadura BGA.
   - Permite diseños de placa madre muy compactos, dejando espacio para baterías de mayor capacidad (60Wh - 99Wh).
2. **Memoria VRAM de 8 GB GDDR6 en 128 Bits:**
   - 4 chips de memoria SK Hynix o Samsung GDDR6 de 2 GB cada uno.
   - Ancho de banda de **256 GB/s** respaldado por 24 MB de caché L2 ultra-rápida integrada en el silicio, reduciendo la necesidad de accesos a la memoria principal.
3. **Pico de Rendimiento a Bajo Consumo:**
   - Gracias a las mejoras litográficas de TSMC 4N, alcanza el 95% de su rendimiento máximo con solo 90W-100W de consumo real en juegos.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS CAD (FC-BGA)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato BGA (X)** | **35.00 mm** | $\pm 0.10\text{ mm}$ | Formato ultracompacto para portátiles masivas. |
| **Largo del Sustrato BGA (Y)** | **35.00 mm** | $\pm 0.10\text{ mm}$ | Sustrato multicapa de 10 capas de interconexión. |
| **Espesor del Sustrato PCB** | **1.05 mm** | $\pm 0.05\text{ mm}$ | Sustrato de perfil bajo para chasis ligeros. |
| **Espesor Total con Silicio (Z)** | **1.75 mm** | $\pm 0.08\text{ mm}$ | Altura mínima optimizada para ensamblaje directo. |
| **Matriz del Silicio AD107** | **$16.00 \times 9.90\text{ mm}$** | $\pm 0.05\text{ mm}$ | Silicio rectificado a espejo con marcado láser numérico. |
| **Esferas de Soldadura BGA** | **1,900+ Esferas** | $\varnothing 0.40\text{ mm}$ | Aleación SAC305 con paso (*pitch*) de 0.78 mm. |
| **Chips VRAM GDDR6** | **4x Módulos 2GB** | $14.0 \times 12.0\text{ mm}$ | 8 GB de memoria distribuidos en 2 chips por flanco. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES MÓVILES

* **Silicio:** NVIDIA Ada Lovelace AD107 Mobile (TSMC 4N, $158.7\text{ mm}^2$).
* **Transistores:** **18,900 Millones** de compuertas lógicas integradas.
* **Núcleos de Cómputo:**
  * **3,072 Núcleos CUDA** con frecuencia boost de hasta 2,370 MHz.
  * **96 Núcleos Tensor de 4.ª Generación** con DLSS 3 Frame Generation.
  * **24 Núcleos RT de 3.ª Generación**.
* **Subsistema de Memoria:**
  * **Capacidad:** 8 GB GDDR6.
  * **Bus de Memoria:** 128-bit.
  * **Velocidad de Reloj:** 16 Gbps efectivos.
  * **Ancho de Banda Bruto:** **256 GB/s**.
* **Potencia y Eficiencia:** 115W TGP base ampliable dinámicamente hasta **140W con Dynamic Boost 2.0**.
* **Interfaz de Bus:** PCI Express 4.0 x8 nativo.