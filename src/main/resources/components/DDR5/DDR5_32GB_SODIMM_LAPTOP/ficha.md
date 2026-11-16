# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## MEMORIA RAM DDR5 32 GB SO-DIMM (5600 MHz) – EXCLUSIVO PARA LAPTOPS
### Estándar Oficial JEDEC MO-329 &bull; Factor de Forma Portátil (262 Pines) &bull; Dual-Rank Double-Sided

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA

Este módulo corresponde a la configuración de alta gama de **32 Gigabytes (32,768 MB)** de memoria **DDR5 SO-DIMM** a **5600 MT/s**, diseñada para estaciones de trabajo móviles (*mobile workstations*), laptops de creación de contenido y portátiles gaming extremos.

A diferencia de los módulos de 8GB y 16GB, el módulo de **32 GB** utiliza una arquitectura **Dual-Rank 2Rx8 con PCB Double-Sided (poblado en ambas caras)**. Alberga un total de **8 chips de memoria DRAM FBGA** (4 en la cara frontal y 4 en la cara posterior), duplicando los bancos accesibles y permitiendo entrelazado de rangos (*rank interleaving*), lo que incrementa sustancialmente el ancho de banda efectivo.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (JEDEC MO-329)

| Parámetro Geométrico | Cota Nominal | Tolerancia JEDEC | Descripción Técnica |
| :--- | :--- | :--- | :--- |
| **Longitud Total del PCB (Eje X)** | **69.60 mm** | $\pm 0.15\text{ mm}$ | Ancho estándar JEDEC MO-329 para zócalos de laptop. |
| **Altura Total del PCB (Eje Y)** | **30.00 mm** | $\pm 0.15\text{ mm}$ | Altura de bajo perfil para compatibilidad con chasis unibody. |
| **Espesor del Sustrato PCB** | **1.20 mm** | $\pm 0.10\text{ mm}$ | Circuito impreso de 10 capas FR-4 High-Tg para aislamiento de pistas. |
| **Espesor Total con Chips (Eje Z)** | **3.90 mm** | Máx. $4.00\text{ mm}$ | Módulo Double-Sided con chips DRAM en ambas caras ($Z > 0$ y $Z < 0$). |
| **Cantidad de Pines de Borde** | **262 Pines** | Exacto | 131 pines dorados frontales + 131 posteriores. |
| **Paso de Pin (Pitch)** | **0.50 mm** | $\pm 0.03\text{ mm}$ | Paso micrométrico entre centros de contactos contiguos. |
| **Ancho de Cada Pin Dorado** | **0.28 mm** | $\pm 0.02\text{ mm}$ | Baño electrolítico de oro duro ($30\,\mu\text{in}$) sobre níquel. |
| **Altura del Contacto Dorado** | **3.40 mm** | $\pm 0.10\text{ mm}$ | Superficie de frotamiento para terminales del zócalo. |
| **Bisel de Entrada (Chamfer)** | **$20^\circ$** | $\pm 5^\circ$ | Rampa de inserción a $30^\circ$ para no doblar las láminas del conector. |
| **Posición Muesca Clave (Key Notch)** | **41.25 mm** | $\pm 0.10\text{ mm}$ | Medida desde el borde izquierdo del PCB al eje de la muesca. |
| **Dimensiones de la Muesca Clave** | **$1.50 \times 4.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Ranura de polarización (no encaja en ranuras DDR4 de 30.85 mm). |
| **Muescas Laterales de Retención** | **$1.50\text{ mm}$ prof.** | $\pm 0.10\text{ mm}$ | A $Y = 20.50\text{ mm}$ para los pestillos mecánicos del zócalo. |
| **Chaflanes en Esquinas Superiores** | **$1.00 \times 1.00\text{ mm}$** | $45^\circ$ | Biseles de seguridad para inserción manual limpia. |

---

## 3. ARQUITECTURA DE CHIPS Y CIRCUITOS INTEGRADOS

* **Topología de Memoria:** Dual-Rank **2Rx8** (2 rangos físicos de 8 bits por subcanal).
* **Chips DRAM (Capacidad 32 GB):**
  * **8x circuitos integrados BGA de $32\text{ Gigabits}$ ($4\text{ GB}$ cada uno)** para conformar los $32\text{ GB}$ totales.
  * **Cara Frontal ($Z > 0$):** 4 chips BGA (U1 a U4) = $16\text{ GB}$.
  * **Cara Posterior ($Z < 0$):** 4 chips BGA (U5 a U8) = $16\text{ GB}$.
  * Encapsulado: FBGA-102 de $11.00 \times 7.80 \times 0.85\text{ mm}$.
  * Un total de **816 bolas BGA** soldadas con aleación SAC305.
* **On-Die ECC:** Protección integrada de integridad de datos en los 8 chips DRAM.
* **PMIC Onboard High-Current (Power Management IC):**
  * Encapsulado: QFN-32 ($4.00 \times 4.00 \times 0.85\text{ mm}$) de mayor amperaje para alimentar simultáneamente los 8 chips.
  * Entrada: $5.0\text{V}$ DC desde la placa madre.
  * Salidas locales: $\text{VDD} = 1.10\text{V}$, $\text{VDDQ} = 1.10\text{V}$, $\text{VPP} = 1.80\text{V}$.
* **Inductores de Potencia:** 3 bobinas apantalladas de ferrita moldeada ($2.50 \times 2.00 \times 1.10\text{ mm}$).
* **SPD Hub + Sensor Térmico:** DFN-8 con bus I3C de telemetría térmica continua.
* **Espesor Z:** $3.90\text{ mm}$, requiriendo zócalos SO-DIMM estándar para laptop con tolerancia vertical JEDEC de hasta $4.00\text{ mm}$.

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\DDR5_32GB_SODIMM_LAPTOP`

* [DDR5_32GB_SODIMM_LAPTOP.obj](file:///C:/Users/mathi/Documents/3D/DDR5_32GB_SODIMM_LAPTOP/DDR5_32GB_SODIMM_LAPTOP.obj) &bull; Modelo CAD 3D paramétrico a escala 1:1 (Double-Sided).
* [DDR5_32GB_SODIMM_LAPTOP.mtl](file:///C:/Users/mathi/Documents/3D/DDR5_32GB_SODIMM_LAPTOP/DDR5_32GB_SODIMM_LAPTOP.mtl) &bull; Definición de materiales PBR de ingeniería.
* [Visor_3D_DDR5_32GB_Laptop.html](file:///C:/Users/mathi/Documents/3D/DDR5_32GB_SODIMM_LAPTOP/Visor_3D_DDR5_32GB_Laptop.html) &bull; Visor 3D interactivo offline con medidor de cotas.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/DDR5_32GB_SODIMM_LAPTOP/01_render_isometrico_3d.png) &bull; Render isométrico 2K QHD (8 chips DRAM BGA).
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/DDR5_32GB_SODIMM_LAPTOP/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas milimétricas sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/DDR5_32GB_SODIMM_LAPTOP/03_render_perfil_lateral_espesor.png) &bull; Perfil lateral (espesor Double-Sided de 3.90 mm).
* [04_render_macro_pmic_circuiteria.png](file:///C:/Users/mathi/Documents/3D/DDR5_32GB_SODIMM_LAPTOP/04_render_macro_pmic_circuiteria.png) &bull; Macro detalle del PMIC y bobinas de ferrita.
* [05_render_posterior_plano_masa.png](file:///C:/Users/mathi/Documents/3D/DDR5_32GB_SODIMM_LAPTOP/05_render_posterior_plano_masa.png) &bull; Vista posterior con los 4 chips DRAM traseros (U5 a U8).
