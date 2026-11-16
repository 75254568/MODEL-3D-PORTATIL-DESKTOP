# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## MEMORIA RAM DDR4 32 GB SO-DIMM (3200 MHz) – EXCLUSIVO PARA LAPTOPS
### Estándar Oficial JEDEC MO-310 &bull; Factor de Forma Portátil (260 Pines) &bull; High-Density Double-Sided

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA

Este módulo representa la **máxima capacidad estándar por ranura de la generación DDR4 para portátiles**: **32 Gigabytes (32,768 MB)** de memoria **DDR4 SO-DIMM** a **3200 MT/s** (PC4-25600), destinada a estaciones de trabajo portátiles (workstations móviles), laptops para virtualización, renderizado 3D pesado y entornos de desarrollo intensivo.

Para alcanzar los 32 GB en un único módulo SO-DIMM sin sobrepasar las dimensiones JEDEC MO-310 ($69.60 \times 30.00\text{ mm}$), el módulo implementa una arquitectura **High-Density Double-Sided con 16 circuitos integrados DRAM en total (8 chips en cara frontal y 8 chips en cara trasera)** distribuidos en dos hileras horizontales. El espesor máximo resultante es de **3.90 mm**, respetando los límites de inserción mecánica de zócalos de laptop estándar.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (JEDEC MO-310)

| Parámetro Geométrico | Cota Nominal | Tolerancia JEDEC | Descripción Técnica |
| :--- | :--- | :--- | :--- |
| **Longitud Total del PCB (Eje X)** | **69.60 mm** | $\pm 0.15\text{ mm}$ | Ancho estándar JEDEC MO-310 para zócalos de notebook. |
| **Altura Total del PCB (Eje Y)** | **30.00 mm** | $\pm 0.15\text{ mm}$ | Altura estandarizada con distribución en doble fila de chips. |
| **Espesor del Sustrato PCB** | **1.20 mm** | $\pm 0.10\text{ mm}$ | Circuito impreso multicapa de 8 capas FR-4 High-Tg. |
| **Espesor Total Máximo (Eje Z)** | **3.90 mm** | Máx. $4.00\text{ mm}$ | Perfil de alta densidad con 16 encapsulados BGA. |
| **Cantidad de Pines de Borde** | **260 Pines** | Exacto | 130 contactos dorados frontales + 130 posteriores. |
| **Paso de Pin (Pitch)** | **0.50 mm** | $\pm 0.03\text{ mm}$ | Distancia entre centros de contactos contiguos. |
| **Ancho de Cada Pin Dorado** | **0.28 mm** | $\pm 0.02\text{ mm}$ | Baño electrolítico de oro duro de $30\,\mu\text{in}$ sobre níquel. |
| **Altura del Contacto Dorado** | **3.40 mm** | $\pm 0.10\text{ mm}$ | Longitud de inserción para terminales de resorte. |
| **Bisel de Entrada (Chamfer)** | **$20^\circ$** | $\pm 5^\circ$ | Rampa de inserción frontal y trasera para evitar atascos. |
| **Posición Muesca Clave (Key Notch)** | **30.85 mm** | $\pm 0.10\text{ mm}$ | Medida desde el borde izquierdo (Pin 1) hasta el centro de la muesca. |
| **Distribución de Pines por Muesca** | **58 / 72** | Exacto | 58 pines en el bloque izquierdo y 72 pines en el bloque derecho. |
| **Dimensiones de la Muesca Clave** | **$1.50 \times 4.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Ranura de polarización mecánica (impide conexión errónea). |
| **Muescas Laterales de Retención** | **$1.50\text{ mm}$ prof.** | $\pm 0.10\text{ mm}$ | Ubicadas a $Y = 20.50\text{ mm}$ para pestillos metálicos del zócalo. |
| **Chaflanes en Esquinas Superiores** | **$1.00 \times 1.00\text{ mm}$** | $45^\circ$ | Biseles de seguridad en los vértices superiores del sustrato. |

---

## 3. ARQUITECTURA ELÉCTRICA Y ALTA DENSIDAD

* **Voltaje de Operación (VDD/VDDQ):** **1.20 V nominal** ($\pm 0.06\text{ V}$).
* **Regulación de Voltaje:** La conversión de potencia se realiza íntegramente en el VRM de la placa base del equipo portátil. **DDR4 no posee PMIC onboard ni bobinas de potencia de ferrita**.
* **Chip EEPROM SPD:** Encapsulado TSSOP-8 / DFN-8 ($2.50 \times 2.00 \times 0.75\text{ mm}$) centrado entre las dos hileras de chips. Almacena las tablas de temporización SPD JEDEC a 3200 MT/s comunicándose mediante **I2C / SMBus**.
* **Topología DRAM de Alta Densidad (32 GB):**
  * **16x circuitos integrados FBGA-96 de $16\text{ Gb}$ ($2\text{ GB}$ cada uno)**:
    * 8 chips en cara frontal ($Z > 0$) organizados en hilera inferior ($Y = 6.00\text{ mm}$) e hilera superior ($Y = 16.50\text{ mm}$).
    * 8 chips en cara posterior ($Z < 0$) organizados simétricamente.
  * Configuración: **Dual-Rank 2Rx8** (2 rangos físicos de 64 bits con alternancia de bancos).
  * Encapsulado: $9.80 \times 7.20 \times 0.80\text{ mm}$ con matriz de microesferas de soldadura BGA SAC305.
* **Canal de Datos:** Un canal de **64 bits** estándar.
* **Térmica y Disipación:** Al concentrar 16 chips en una superficie compacta de $69.60 \times 30.00\text{ mm}$, la disipación se reparte de forma simétrica entre ambas caras, maximizando el contacto con las almohadillas térmicas (thermal pads) del chasis.

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\DDR4_32GB_SODIMM_LAPTOP`

* [DDR4_32GB_SODIMM_LAPTOP.obj](file:///C:/Users/mathi/Documents/3D/DDR4_32GB_SODIMM_LAPTOP/DDR4_32GB_SODIMM_LAPTOP.obj) &bull; Modelo CAD 3D paramétrico a escala real 1:1.
* [DDR4_32GB_SODIMM_LAPTOP.mtl](file:///C:/Users/mathi/Documents/3D/DDR4_32GB_SODIMM_LAPTOP/DDR4_32GB_SODIMM_LAPTOP.mtl) &bull; Definición de materiales PBR (Sustrato verde, contactos oro, silicio DRAM, soldadura SAC305).
* [Visor_3D_DDR4_32GB_Laptop.html](file:///C:/Users/mathi/Documents/3D/DDR4_32GB_SODIMM_LAPTOP/Visor_3D_DDR4_32GB_Laptop.html) &bull; Visor 3D interactivo offline con cotas 3D y ficha técnica.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/DDR4_32GB_SODIMM_LAPTOP/01_render_isometrico_3d.png) &bull; Render isométrico fotorrealista 2K QHD.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/DDR4_32GB_SODIMM_LAPTOP/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas de ingeniería sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/DDR4_32GB_SODIMM_LAPTOP/03_render_perfil_lateral_espesor.png) &bull; Perfil lateral (espesor de alta densidad 3.90 mm).
* [04_render_macro_eeprom_spd.png](file:///C:/Users/mathi/Documents/3D/DDR4_32GB_SODIMM_LAPTOP/04_render_macro_eeprom_spd.png) &bull; Macro detalle del chip EEPROM SPD de 8 pines y red MLCC.
* [05_render_posterior_plano_masa.png](file:///C:/Users/mathi/Documents/3D/DDR4_32GB_SODIMM_LAPTOP/05_render_posterior_plano_masa.png) &bull; Vista posterior con los 8 chips DRAM traseros.
