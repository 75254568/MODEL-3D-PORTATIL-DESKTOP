# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## UNIDAD DE ESTADO SÓLIDO (SSD) M.2 2280 NVMe PCIe 4.0 – 4 TB (GAMING LAPTOP)
### Estándar Oficial PCI-SIG M.2 Tipo 2280-S3-M &bull; Protocolo NVMe 2.0 &bull; Perfil Single-Sided Ultra-High-Density

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA

Esta unidad de 4 Terabytes representa la **máxima capacidad posible en formato Single-Sided para laptops de alto rendimiento**. Diseñada para entusiastas, desarrolladores de videojuegos, creadores de contenido audiovisual masivo y gamers con bibliotecas enteras instaladas en almacenamiento ultrarrápido sin recurrir a ranuras secundarias.

El gran desafío de ingeniería en portátiles es que la mayoría de los SSDs de 4 TB del mercado son de doble cara (*Double-Sided D5*), lo que provoca que los chips traseros choquen contra los componentes de la placa madre de la laptop o fuercen el tornillo M2 hacia arriba, provocando arqueamiento (*bending*) y fallos de contacto. Este modelo está diseñado bajo la especificación **Single-Sided M.2 2280-S3-M**, agrupando **4 encapsulados compactos de memoria 3D TLC NAND Flash de $1\text{ TB}$ cada uno en la cara frontal** en un arreglo matricial simétrico con **4 GB de memoria DRAM LPDDR4** dedicada. La disipación térmica cuenta con una **etiqueta térmica extendida de grafeno de alta conductividad con lámina de cobre de $0.15\text{ mm}$**, manteniendo el espesor máximo de la unidad en **2.15 mm**.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (PCI-SIG M.2 2280)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Longitud Total del PCB (Eje Y)** | **80.00 mm** | $\pm 0.15\text{ mm}$ | Formato estándar de longitud tipo 2280 para laptops. |
| **Ancho Total del PCB (Eje X)** | **22.00 mm** | $\pm 0.15\text{ mm}$ | Ancho estándar de zócalos M.2 en motherboards de laptop. |
| **Espesor del Sustrato PCB** | **0.80 mm** | $\pm 0.08\text{ mm}$ | Circuito impreso multicapa de 8 capas FR-4 High-Tg negro mate. |
| **Espesor Total con Componentes** | **2.15 mm** | Máx. $2.38\text{ mm}$ | Cumple estrictamente la categoría S3 (Single-Sided) para portátiles. |
| **Altura Inferior (Cara Trasera)** | **0.00 mm** | Máx. $0.00\text{ mm}$ | Cara trasera 100% plana; cero riesgo de colisión con la motherboard. |
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
  * Procesador multinúcleo ARM Cortex-R8 de 8 canales de memoria NAND Flash con soporte de 32 *chip-enables* (CE).
  * Encapsulado: FCCSP ($12.00 \times 12.00 \times 0.85\text{ mm}$).
  * **Difusor Térmico de Níquel:** Tapa de níquel para homogenización de calor bajo cargas sostenidas de escritura.
* **Memoria Caché DRAM:**
  * **4 GB LPDDR4-4266** dedicada ($8.50 \times 7.00 \times 0.72\text{ mm}$).
  * Capacidad masiva para mantener en memoria volátil de acceso inmediato la tabla de bloques FTL completa de los 4 TB.
* **Memoria No Volátil (Capacidad 4 TB):**
  * **4x encapsulados BGA-132 compactos de $1024\text{ GB}$ ($1\text{ TB}$) cada uno** organizados en matriz cuádruple 2x2 en la cara frontal ($Z > 0$).
  * Tecnología 3D TLC NAND Flash de 232 capas con interfaz de transferencia de datos de $2400\text{ MT/s}$.
* **Métricas de Rendimiento Gaming:**
  * Velocidad de Lectura Secuencial: **Hasta 7,450 MB/s**.
  * Velocidad de Escritura Secuencial: **Hasta 6,900 MB/s**.
  * Lectura Aleatoria (4KB QD32): **1,450,000 IOPS**.
  * Escritura Aleatoria (4KB QD32): **1,550,000 IOPS**.
  * Optimizado para **Microsoft DirectStorage API** en títulos de nueva generación.
* **Eficiencia Térmica y Consumo:**
  * Consumo Máximo Activo: $5.9\text{ W}$.
  * Consumo en Reposo (PS4 L1.2): $0.045\text{ W}$ ($45\text{ mW}$).
  * Resistencia y Vida Útil: **2,400 TBW** (Terabytes Escritos).

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\SSD_M2_4TB_NVME_PCIE4_LAPTOP`

* [SSD_M2_4TB_NVME_PCIE4_LAPTOP.obj](file:///C:/Users/mathi/Documents/3D/SSD_M2_4TB_NVME_PCIE4_LAPTOP/SSD_M2_4TB_NVME_PCIE4_LAPTOP.obj) &bull; Modelo CAD 3D paramétrico a escala real 1:1.
* [SSD_M2_4TB_NVME_PCIE4_LAPTOP.mtl](file:///C:/Users/mathi/Documents/3D/SSD_M2_4TB_NVME_PCIE4_LAPTOP/SSD_M2_4TB_NVME_PCIE4_LAPTOP.mtl) &bull; Definición de materiales PBR (Grafeno, níquel, oro M-Key, silicio).
* [Visor_3D_SSD_M2_4TB_Laptop.html](file:///C:/Users/mathi/Documents/3D/SSD_M2_4TB_NVME_PCIE4_LAPTOP/Visor_3D_SSD_M2_4TB_Laptop.html) &bull; Visor 3D interactivo offline con cotas 3D y ficha técnica.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_4TB_NVME_PCIE4_LAPTOP/01_render_isometrico_3d.png) &bull; Render isométrico 2K QHD con disipador de grafeno.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_4TB_NVME_PCIE4_LAPTOP/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas de ingeniería sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_4TB_NVME_PCIE4_LAPTOP/03_render_perfil_lateral_espesor.png) &bull; Perfil ortogonal lateral (espesor slim 2.15 mm).
* [04_render_macro_controlador_nvme.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_4TB_NVME_PCIE4_LAPTOP/04_render_macro_controlador_nvme.png) &bull; Macro detalle del controlador niquelado y memoria DRAM.
* [05_render_posterior_plano_cobre.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_4TB_NVME_PCIE4_LAPTOP/05_render_posterior_plano_cobre.png) &bull; Vista posterior con plano de retorno de cobre y vías térmicas.
