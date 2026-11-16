# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## MEMORIA RAM DDR5 8 GB SO-DIMM (5600 MHz) – EXCLUSIVO PARA LAPTOPS
### Estándar Oficial JEDEC MO-329 &bull; Factor de Forma Portátil (262 Pines) &bull; Single-Sided Slim

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA

Este módulo corresponde a la configuración de **8 Gigabytes (8,192 MB)** de memoria **DDR5 SO-DIMM** a **5600 MT/s**, diseñada específicamente para computadoras portátiles (laptops) y equipos ultracompactos.

A diferencia de los módulos de mayor densidad (como los de 32GB que son de doble cara), el módulo de **8 GB** se caracteriza por su **diseño Single-Sided (componentes montados exclusivamente en la cara frontal)**, lo que resulta en un perfil vertical ultra-delgado de solo **2.65 mm de espesor máximo**, ideal para los compartimentos de memoria más estrechos de portátiles modernos como Lenovo LOQ, Legion, ASUS ROG o Dell G-Series.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (JEDEC MO-329)

| Parámetro Geométrico | Cota Nominal | Tolerancia JEDEC | Descripción Técnica |
| :--- | :--- | :--- | :--- |
| **Longitud Total del PCB (Eje X)** | **69.60 mm** | $\pm 0.15\text{ mm}$ | Ancho estándar JEDEC MO-329 para zócalos de laptop. |
| **Altura Total del PCB (Eje Y)** | **30.00 mm** | $\pm 0.15\text{ mm}$ | Altura de bajo perfil compatible con disipadores térmicos de chasis. |
| **Espesor del Sustrato PCB** | **1.20 mm** | $\pm 0.10\text{ mm}$ | Circuito impreso multicapa de 8 capas FR-4 High-Tg. |
| **Espesor Total Máximo (Eje Z)** | **2.65 mm** | Máx. $2.80\text{ mm}$ | Perfil ultra-delgado debido al montaje en una sola cara (Single-Sided). |
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

* **Topología de Memoria:** Single-Rank **1Rx16** o **1Rx8**.
* **Chips DRAM (Capacidad 8 GB):**
  * **4x circuitos integrados BGA de $16\text{ Gigabits}$ ($2\text{ GB}$ cada uno)** para conformar los $8\text{ GB}$ totales.
  * Encapsulado: FBGA-102 de $10.20 \times 7.50 \times 0.80\text{ mm}$.
  * Montaje superficial BGA sobre la cara frontal ($Z > 0$).
  * En configuraciones 1Rx16 alternativas, pueden utilizarse solo **2 matrices de $32\text{ Gb}$ ($4\text{ GB}$ c/u)**, dejando 2 posiciones de pista libres.
* **On-Die ECC:** Cada chip DRAM de 8GB incluye motor autónomo de paridad y corrección de bits erróneos.
* **PMIC Onboard (Power Management IC):**
  * Encapsulado: QFN-32 ($4.00 \times 4.00 \times 0.85\text{ mm}$).
  * Entrada: $5.0\text{V}$ DC desde la batería/fuente del portátil.
  * Salidas locales: $\text{VDD} = 1.10\text{V}$, $\text{VDDQ} = 1.10\text{V}$, $\text{VPP} = 1.80\text{V}$.
* **Inductores de Potencia:** 3 bobinas apantalladas de ferrita moldeada ($2.50 \times 2.00 \times 1.10\text{ mm}$).
* **SPD Hub + Sensor Térmico:** DFN-8 ($2.50 \times 2.00 \times 0.75\text{ mm}$) con bus I3C de telemetría térmica continua.
* **Cara Posterior:** Completamente plana, sin chips de memoria DRAM montados, optimizando la altura Z para laptops delgadas.

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\DDR5_8GB_SODIMM_LAPTOP`

* [DDR5_8GB_SODIMM_LAPTOP.obj](file:///C:/Users/mathi/Documents/3D/DDR5_8GB_SODIMM_LAPTOP/DDR5_8GB_SODIMM_LAPTOP.obj) &bull; Modelo CAD 3D paramétrico a escala 1:1.
* [DDR5_8GB_SODIMM_LAPTOP.mtl](file:///C:/Users/mathi/Documents/3D/DDR5_8GB_SODIMM_LAPTOP/DDR5_8GB_SODIMM_LAPTOP.mtl) &bull; Definición de materiales PBR de ingeniería.
* [Visor_3D_DDR5_8GB_Laptop.html](file:///C:/Users/mathi/Documents/3D/DDR5_8GB_SODIMM_LAPTOP/Visor_3D_DDR5_8GB_Laptop.html) &bull; Visor 3D interactivo offline con medidor de cotas.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/DDR5_8GB_SODIMM_LAPTOP/01_render_isometrico_3d.png) &bull; Render isométrico 2K QHD.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/DDR5_8GB_SODIMM_LAPTOP/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas milimétricas sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/DDR5_8GB_SODIMM_LAPTOP/03_render_perfil_lateral_espesor.png) &bull; Perfil lateral (espesor slim de 2.65 mm).
* [04_render_macro_pmic_circuiteria.png](file:///C:/Users/mathi/Documents/3D/DDR5_8GB_SODIMM_LAPTOP/04_render_macro_pmic_circuiteria.png) &bull; Macro detalle del PMIC y bobinas de ferrita.
* [05_render_posterior_plano_masa.png](file:///C:/Users/mathi/Documents/3D/DDR5_8GB_SODIMM_LAPTOP/05_render_posterior_plano_masa.png) &bull; Cara trasera de apantallamiento plano.
