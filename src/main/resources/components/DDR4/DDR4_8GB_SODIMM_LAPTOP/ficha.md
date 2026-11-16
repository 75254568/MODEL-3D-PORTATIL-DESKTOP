# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## MEMORIA RAM DDR4 8 GB SO-DIMM (3200 MHz) – EXCLUSIVO PARA LAPTOPS
### Estándar Oficial JEDEC MO-310 &bull; Factor de Forma Portátil (260 Pines) &bull; Single-Sided Slim Profile

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA

Este módulo corresponde a la especificación estándar de **8 Gigabytes (8,192 MB)** de memoria **DDR4 SO-DIMM** a **3200 MT/s** (PC4-25600), diseñada específicamente para computadoras portátiles (laptops), mini-PCs e infraestructura compacta.

A diferencia de los módulos de mayor densidad de doble cara, el módulo de **8 GB** presenta un diseño **Single-Sided (todos los circuitos integrados DRAM montados en la cara frontal)**. Esto proporciona un espesor ultra-delgado de solo **2.65 mm máximo**, permitiendo una óptima disipación térmica y compatibilidad con las bahías más estrechas de notebooks gamers y de oficina.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (JEDEC MO-310)

| Parámetro Geométrico | Cota Nominal | Tolerancia JEDEC | Descripción Técnica |
| :--- | :--- | :--- | :--- |
| **Longitud Total del PCB (Eje X)** | **69.60 mm** | $\pm 0.15\text{ mm}$ | Ancho estándar JEDEC MO-310 para zócalos SO-DIMM de laptop. |
| **Altura Total del PCB (Eje Y)** | **30.00 mm** | $\pm 0.15\text{ mm}$ | Altura estandarizada compatible con blindajes térmicos de laptop. |
| **Espesor del Sustrato PCB** | **1.20 mm** | $\pm 0.10\text{ mm}$ | Circuito impreso multicapa de 8 capas FR-4 High-Tg verde. |
| **Espesor Total Máximo (Eje Z)** | **2.65 mm** | Máx. $2.80\text{ mm}$ | Perfil ultra-delgado por montaje en cara frontal (Single-Sided). |
| **Cantidad de Pines de Borde** | **260 Pines** | Exacto | 130 contactos dorados frontales + 130 posteriores. |
| **Paso de Pin (Pitch)** | **0.50 mm** | $\pm 0.03\text{ mm}$ | Distancia entre centros de contactos contiguos. |
| **Ancho de Cada Pin Dorado** | **0.28 mm** | $\pm 0.02\text{ mm}$ | Baño electrolítico de oro de $30\,\mu\text{in}$ sobre barrera de níquel. |
| **Altura del Contacto Dorado** | **3.40 mm** | $\pm 0.10\text{ mm}$ | Longitud de inserción para terminales de resorte del conector. |
| **Bisel de Entrada (Chamfer)** | **$20^\circ$** | $\pm 5^\circ$ | Rampa de inserción frontal y trasera para evitar atascos. |
| **Posición Muesca Clave (Key Notch)** | **30.85 mm** | $\pm 0.10\text{ mm}$ | Medida desde el borde izquierdo (Pin 1) hasta el centro de la muesca. |
| **Distribución de Pines por Muesca** | **58 / 72** | Exacto | 58 pines en el bloque izquierdo y 72 pines en el bloque derecho. |
| **Dimensiones de la Muesca Clave** | **$1.50 \times 4.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Ranura de polarización mecánica (impide conexión en zócalos DDR5). |
| **Muescas Laterales de Retención** | **$1.50\text{ mm}$ prof.** | $\pm 0.10\text{ mm}$ | Ubicadas a $Y = 20.50\text{ mm}$ para pestañas de retención del zócalo. |
| **Chaflanes en Esquinas Superiores** | **$1.00 \times 1.00\text{ mm}$** | $45^\circ$ | Biseles para manipulación segura sin rebabas en el sustrato. |

---

## 3. ARQUITECTURA ELÉCTRICA Y DIFERENCIAS CLAVE CON DDR5

* **Voltaje de Operación (VDD/VDDQ):** **1.20 V nominal** ($\pm 0.06\text{ V}$).
* **Regulación de Voltaje:** La regulación la realiza directamente la placa madre de la laptop mediante su propio VRM. **DDR4 NO incluye chip PMIC onboard ni bobinas de potencia de ferrita**.
* **Chip EEPROM SPD:** Encapsulado TSSOP-8 / DFN-8 ($2.50 \times 2.00 \times 0.75\text{ mm}$) en el centro superior. Contiene la tabla de temporizaciones estándar JEDEC y perfiles XMP 2.0, comunicándose a través de un bus serie **I2C / SMBus a 400 kHz**.
* **Topología DRAM (8 GB):**
  * **4x circuitos integrados FBGA-96 de $16\text{ Gb}$ ($2\text{ GB}$ cada uno)**.
  * Configuración: **Single-Rank 1Rx8 / 1Rx16**.
  * Encapsulado: $9.80 \times 7.50 \times 0.80\text{ mm}$ con matriz de esferas BGA sin plomo SAC305.
* **Canal de Datos:** Un único canal de **64 bits** de ancho (a diferencia del doble subcanal independiente de 32 bits de DDR5).
* **Cara Posterior:** Completamente despejada de chips de memoria, con plano de masa de apantallamiento EMI y condensadores cerámicos de desacoplo SMD 0402.

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\DDR4_8GB_SODIMM_LAPTOP`

* [DDR4_8GB_SODIMM_LAPTOP.obj](file:///C:/Users/mathi/Documents/3D/DDR4_8GB_SODIMM_LAPTOP/DDR4_8GB_SODIMM_LAPTOP.obj) &bull; Modelo CAD 3D paramétrico a escala real 1:1.
* [DDR4_8GB_SODIMM_LAPTOP.mtl](file:///C:/Users/mathi/Documents/3D/DDR4_8GB_SODIMM_LAPTOP/DDR4_8GB_SODIMM_LAPTOP.mtl) &bull; Definición de materiales PBR (Sustrato verde, contactos oro, silicio DRAM, estaño).
* [Visor_3D_DDR4_8GB_Laptop.html](file:///C:/Users/mathi/Documents/3D/DDR4_8GB_SODIMM_LAPTOP/Visor_3D_DDR4_8GB_Laptop.html) &bull; Visor 3D interactivo offline con cotas 3D y ficha técnica.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/DDR4_8GB_SODIMM_LAPTOP/01_render_isometrico_3d.png) &bull; Render isométrico fotorrealista 2K QHD.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/DDR4_8GB_SODIMM_LAPTOP/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas de ingeniería sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/DDR4_8GB_SODIMM_LAPTOP/03_render_perfil_lateral_espesor.png) &bull; Perfil ortogonal lateral (espesor slim 2.65 mm).
* [04_render_macro_eeprom_spd.png](file:///C:/Users/mathi/Documents/3D/DDR4_8GB_SODIMM_LAPTOP/04_render_macro_eeprom_spd.png) &bull; Macro detalle del chip EEPROM SPD de 8 pines y red MLCC.
* [05_render_posterior_plano_masa.png](file:///C:/Users/mathi/Documents/3D/DDR4_8GB_SODIMM_LAPTOP/05_render_posterior_plano_masa.png) &bull; Vista posterior del plano de retorno de masa.
