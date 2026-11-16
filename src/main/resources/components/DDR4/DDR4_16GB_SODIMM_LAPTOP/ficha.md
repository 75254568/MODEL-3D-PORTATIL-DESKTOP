# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## MEMORIA RAM DDR4 16 GB SO-DIMM (3200 MHz) – EXCLUSIVO PARA LAPTOPS
### Estándar Oficial JEDEC MO-310 &bull; Factor de Forma Portátil (260 Pines) &bull; Double-Sided Profile

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA

Este módulo corresponde a la especificación estándar de **16 Gigabytes (16,384 MB)** de memoria **DDR4 SO-DIMM** a **3200 MT/s** (PC4-25600), diseñada para computadoras portátiles (laptops) de alto rendimiento para gaming, ingeniería y edición multitarea.

El módulo de **16 GB** utiliza una topología **Double-Sided (componentes montados en ambas caras del PCB: 4 chips en la cara frontal y 4 chips en la cara trasera)**. Esto optimiza la superficie del circuito impreso estándar JEDEC MO-310 ($69.60 \times 30.00\text{ mm}$) permitiendo un espesor total de **3.80 mm máximo**, perfectamente compatible con las holguras térmicas de ranuras SO-DIMM apiladas o adyacentes de notebooks.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (JEDEC MO-310)

| Parámetro Geométrico | Cota Nominal | Tolerancia JEDEC | Descripción Técnica |
| :--- | :--- | :--- | :--- |
| **Longitud Total del PCB (Eje X)** | **69.60 mm** | $\pm 0.15\text{ mm}$ | Ancho estándar JEDEC MO-310 para zócalos de notebook. |
| **Altura Total del PCB (Eje Y)** | **30.00 mm** | $\pm 0.15\text{ mm}$ | Altura estándar para instalación horizontal en laptops. |
| **Espesor del Sustrato PCB** | **1.20 mm** | $\pm 0.10\text{ mm}$ | Circuito impreso multicapa de 8 capas FR-4 High-Tg. |
| **Espesor Total Máximo (Eje Z)** | **3.80 mm** | Máx. $3.85\text{ mm}$ | Perfil de doble cara (DRAM en cara frontal y trasera). |
| **Cantidad de Pines de Borde** | **260 Pines** | Exacto | 130 contactos dorados frontales + 130 posteriores. |
| **Paso de Pin (Pitch)** | **0.50 mm** | $\pm 0.03\text{ mm}$ | Distancia entre centros de pines contiguos. |
| **Ancho de Cada Pin Dorado** | **0.28 mm** | $\pm 0.02\text{ mm}$ | Baño electrolítico de oro de $30\,\mu\text{in}$ sobre barrera de níquel. |
| **Altura del Contacto Dorado** | **3.40 mm** | $\pm 0.10\text{ mm}$ | Longitud de inserción para terminales de resorte. |
| **Bisel de Entrada (Chamfer)** | **$20^\circ$** | $\pm 5^\circ$ | Rampa de inserción frontal y trasera para evitar atascos. |
| **Posición Muesca Clave (Key Notch)** | **30.85 mm** | $\pm 0.10\text{ mm}$ | Medida desde el borde izquierdo (Pin 1) hasta el centro de la muesca. |
| **Distribución de Pines por Muesca** | **58 / 72** | Exacto | 58 pines en el bloque izquierdo y 72 pines en el bloque derecho. |
| **Dimensiones de la Muesca Clave** | **$1.50 \times 4.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Ranura de polarización mecánica (impide conexión errónea). |
| **Muescas Laterales de Retención** | **$1.50\text{ mm}$ prof.** | $\pm 0.10\text{ mm}$ | Ubicadas a $Y = 20.50\text{ mm}$ para ganchos metálicos del zócalo. |
| **Chaflanes en Esquinas Superiores** | **$1.00 \times 1.00\text{ mm}$** | $45^\circ$ | Biseles de seguridad en los vértices del sustrato. |

---

## 3. ARQUITECTURA ELÉCTRICA Y DISTRIBUCIÓN DE COMPONENTES

* **Voltaje de Operación (VDD/VDDQ):** **1.20 V nominal** ($\pm 0.06\text{ V}$).
* **Regulación de Voltaje:** La conversión de potencia se realiza en el VRM de la placa base de la laptop. **DDR4 no posee PMIC onboard ni bobinas de potencia de ferrita**.
* **Chip EEPROM SPD:** Encapsulado TSSOP-8 / DFN-8 ($2.50 \times 2.00 \times 0.75\text{ mm}$) centrado en la cara superior. Almacena latencias JEDEC (CL22-22-22 a 3200 MT/s) comunicándose mediante **I2C / SMBus**.
* **Topología DRAM (16 GB):**
  * **8x circuitos integrados FBGA-96 de $16\text{ Gb}$ ($2\text{ GB}$ cada uno)**:
    * 4 chips en cara frontal ($Z > 0$).
    * 4 chips en cara posterior ($Z < 0$).
  * Configuración: **Dual-Rank 2Rx8** o **Single-Rank 1Rx8** (según generación de densidades).
  * Encapsulado: $9.80 \times 7.50 \times 0.80\text{ mm}$ con matriz BGA SAC305.
* **Canal de Datos:** Un canal de **64 bits** estándar.
* **Red Pasiva de Desacoplo:** Múltiples condensadores cerámicos multicapa SMD 0402 ubicados alrededor de cada circuito DRAM para filtrar fluctuaciones de tensión a 1.20V.

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\DDR4_16GB_SODIMM_LAPTOP`

* [DDR4_16GB_SODIMM_LAPTOP.obj](file:///C:/Users/mathi/Documents/3D/DDR4_16GB_SODIMM_LAPTOP/DDR4_16GB_SODIMM_LAPTOP.obj) &bull; Modelo CAD 3D paramétrico a escala real 1:1.
* [DDR4_16GB_SODIMM_LAPTOP.mtl](file:///C:/Users/mathi/Documents/3D/DDR4_16GB_SODIMM_LAPTOP/DDR4_16GB_SODIMM_LAPTOP.mtl) &bull; Definición de materiales PBR (Sustrato verde, contactos oro, silicio DRAM, soldadura SAC305).
* [Visor_3D_DDR4_16GB_Laptop.html](file:///C:/Users/mathi/Documents/3D/DDR4_16GB_SODIMM_LAPTOP/Visor_3D_DDR4_16GB_Laptop.html) &bull; Visor 3D interactivo offline con cotas 3D y ficha técnica.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/DDR4_16GB_SODIMM_LAPTOP/01_render_isometrico_3d.png) &bull; Render isométrico fotorrealista 2K QHD.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/DDR4_16GB_SODIMM_LAPTOP/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas de ingeniería sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/DDR4_16GB_SODIMM_LAPTOP/03_render_perfil_lateral_espesor.png) &bull; Perfil lateral (espesor de doble cara 3.80 mm).
* [04_render_macro_eeprom_spd.png](file:///C:/Users/mathi/Documents/3D/DDR4_16GB_SODIMM_LAPTOP/04_render_macro_eeprom_spd.png) &bull; Macro detalle del chip EEPROM SPD de 8 pines y red MLCC.
* [05_render_posterior_plano_masa.png](file:///C:/Users/mathi/Documents/3D/DDR4_16GB_SODIMM_LAPTOP/05_render_posterior_plano_masa.png) &bull; Vista posterior con los 4 chips DRAM traseros.
