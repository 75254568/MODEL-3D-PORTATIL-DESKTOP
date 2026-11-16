# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## UNIDAD DE ESTADO SÓLIDO (SSD) M.2 2280 NVMe PCIe 4.0 – 2 TB (GAMING LAPTOP)
### Estándar Oficial PCI-SIG M.2 Tipo 2280-S3-M &bull; Protocolo NVMe 2.0 &bull; Perfil Single-Sided Slim

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA

Esta unidad de 2 Terabytes representa el punto dulce (*sweet spot*) de actualización para laptops gaming actuales, duplicando el espacio de almacenamiento para bibliotecas masivas de juegos AAA y suites de creación de contenido en 4K/8K sin comprometer el factor térmico de la máquina.

Manteniendo el estricto formato **Single-Sided (M.2 2280-S3-M)**, la unidad integra dos encapsulados de memoria 3D TLC NAND Flash de alta densidad apilada ($1\text{ TB}$ por chip BGA) junto a **2 GB de caché DRAM LPDDR4** dedicada. La disipación térmica se logra a través de la lámina híbrida de **grafeno con sustrato de cobre de $0.15\text{ mm}$**, manteniendo el espesor total en solo **2.15 mm**, permitiendo un contacto óptimo con los disipadores térmicos integrados en el chasis de la laptop.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (PCI-SIG M.2 2280)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Longitud Total del PCB (Eje Y)** | **80.00 mm** | $\pm 0.15\text{ mm}$ | Formato estándar de longitud tipo 2280 para laptops. |
| **Ancho Total del PCB (Eje X)** | **22.00 mm** | $\pm 0.15\text{ mm}$ | Ancho estándar de ranuras M.2 en motherboards de laptop. |
| **Espesor del Sustrato PCB** | **0.80 mm** | $\pm 0.08\text{ mm}$ | Circuito impreso multicapa de 8 capas FR-4 High-Tg negro mate. |
| **Espesor Total con Componentes** | **2.15 mm** | Máx. $2.38\text{ mm}$ | Cumple estrictamente la categoría S3 (Single-Sided) para portátiles. |
| **Altura Inferior (Cara Trasera)** | **0.00 mm** | Máx. $0.00\text{ mm}$ | Cara trasera 100% plana; compatible con zócalos de bajo perfil. |
| **Ancho Lengüeta de Conexión** | **19.85 mm** | $\pm 0.10\text{ mm}$ | Rebaje lateral simétrico de $1.075\text{ mm}$ en cada hombro. |
| **Tipo de Clave (Keying)** | **M-Key** | PCI-SIG | Ranura de polarización mecánica exclusiva para buses PCIe x4. |
| **Posición Muesca M-Key** | **Pines 59 a 66** | Exacto | Ancho $1.20\text{ mm} \times 4.00\text{ mm}$ de profundidad ($X = 3.65\text{ mm}$). |
| **Cantidad Total de Contactos** | **75 Pines** | 67 Activos | Contactos dorados de paso $0.50\text{ mm}$ en cara superior e inferior. |
| **Espesor Baño de Oro** | **$30\,\mu\text{in}$** | Mín. $30\,\mu\text{in}$ | Oro duro electrolítico sobre barrera de níquel ($100\,\mu\text{in}$). |
| **Bisel de Entrada (Chamfer)** | **$20^\circ$** | $\pm 5^\circ$ | Rampa de inserción a $20^\circ$ para terminales de presión del conector. |
| **Muesca de Retención de Tornillo** | **Radio 1.75 mm** | $\pm 0.08\text{ mm}$ | Corte semicircular de $3.50\text{ mm}$ de diámetro para tornillo M2. |
| **Anillo de Puesta a Tierra (GND)**| **5.50 mm OD** | $\pm 0.10\text{ mm}$ | Corona circular metalizada dorada que asegura masa con el chasis. |

---

## 3. ARQUITECTURA DE SILICIO Y RENDIMIENTO

* **Controlador NVMe:**
  * Procesador multinúcleo ARM Cortex-R8 de 8 canales de memoria NAND Flash.
  * Encapsulado: FCCSP ($12.00 \times 12.00 \times 0.85\text{ mm}$).
  * **Difusor Térmico de Níquel:** Lámina superficial niquelada para rápida homogenización del calor de los núcleos.
* **Memoria Caché DRAM:**
  * **2 GB LPDDR4-4266** dedicada ($8.00 \times 6.50 \times 0.70\text{ mm}$).
  * Ratio óptimo de memoria caché ($1\text{ MB}$ DRAM por cada $1\text{ GB}$ de almacenamiento Flash).
* **Memoria No Volátil (Capacidad 2 TB):**
  * **2x encapsulados BGA-132 de $1024\text{ GB}$ ($1\text{ TB}$) cada uno** con matrices de 16 matrices de silicio apiladas (die-stacking) de 3D TLC NAND Flash de 232 capas.
* **Métricas de Rendimiento Gaming:**
  * Velocidad de Lectura Secuencial: **Hasta 7,450 MB/s**.
  * Velocidad de Escritura Secuencial: **Hasta 6,900 MB/s**.
  * Lectura Aleatoria (4KB QD32): **1,400,000 IOPS**.
  * Escritura Aleatoria (4KB QD32): **1,550,000 IOPS**.
  * Soporte de **Microsoft DirectStorage API** y comandos NVMe 2.0 ZNS.
* **Eficiencia Térmica y Consumo:**
  * Consumo Máximo Activo: $5.6\text{ W}$.
  * Consumo en Reposo (PS4 L1.2): $0.040\text{ W}$ ($40\text{ mW}$).
  * Resistencia y Vida Útil: **1,200 TBW** (Terabytes Escritos).

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\SSD_M2_2TB_NVME_PCIE4_LAPTOP`

* [SSD_M2_2TB_NVME_PCIE4_LAPTOP.obj](file:///C:/Users/mathi/Documents/3D/SSD_M2_2TB_NVME_PCIE4_LAPTOP/SSD_M2_2TB_NVME_PCIE4_LAPTOP.obj) &bull; Modelo CAD 3D paramétrico a escala real 1:1.
* [SSD_M2_2TB_NVME_PCIE4_LAPTOP.mtl](file:///C:/Users/mathi/Documents/3D/SSD_M2_2TB_NVME_PCIE4_LAPTOP/SSD_M2_2TB_NVME_PCIE4_LAPTOP.mtl) &bull; Definición de materiales PBR (Grafeno, níquel, oro M-Key, silicio).
* [Visor_3D_SSD_M2_2TB_Laptop.html](file:///C:/Users/mathi/Documents/3D/SSD_M2_2TB_NVME_PCIE4_LAPTOP/Visor_3D_SSD_M2_2TB_Laptop.html) &bull; Visor 3D interactivo offline con cotas 3D y ficha técnica.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_2TB_NVME_PCIE4_LAPTOP/01_render_isometrico_3d.png) &bull; Render isométrico 2K QHD con disipador de grafeno.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_2TB_NVME_PCIE4_LAPTOP/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas de ingeniería sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_2TB_NVME_PCIE4_LAPTOP/03_render_perfil_lateral_espesor.png) &bull; Perfil ortogonal lateral (espesor slim 2.15 mm).
* [04_render_macro_controlador_nvme.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_2TB_NVME_PCIE4_LAPTOP/04_render_macro_controlador_nvme.png) &bull; Macro detalle del controlador niquelado y memoria DRAM.
* [05_render_posterior_plano_cobre.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_2TB_NVME_PCIE4_LAPTOP/05_render_posterior_plano_cobre.png) &bull; Vista posterior con plano de retorno de cobre y vías térmicas.
