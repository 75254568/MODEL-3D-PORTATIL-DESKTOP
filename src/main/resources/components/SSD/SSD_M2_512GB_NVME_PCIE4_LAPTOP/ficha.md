# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## UNIDAD DE ESTADO SÓLIDO (SSD) M.2 2280 NVMe PCIe 4.0 – 512 GB (GAMING LAPTOP)
### Estándar Oficial PCI-SIG M.2 Tipo 2280-S3-M &bull; Protocolo NVMe 2.0 &bull; Perfil Single-Sided Slim

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA

Esta unidad de 512 Gigabytes representa la configuración de entrada óptima para almacenamiento de sistema operativo y arranque ultrarrápido en portátiles para videojuegos (como Lenovo LOQ, IdeaPad Gaming, ASUS TUF, Acer Nitro y HP Victus), con una capacidad nominal formateada de **512 GB (500,107 MB)** sobre interfaz **PCIe Gen 4.0 x4**.

Cumpliendo con la estricta especificación **Single-Sided M.2 2280-S3-M**, la unidad ubica todos sus circuitos integrados en la cara frontal y mantiene la cara trasera completamente plana ($0.00\text{ mm}$ de altura inferior), garantizando una compatibilidad perfecta con los compartimentos M.2 ultra-delgados de notebooks sin forzar el conector ni arquear el circuito impreso. Integra un chip **3D TLC NAND Flash de 512 GB** con interfaz de alta velocidad, acompañado por **512 MB de memoria caché DRAM LPDDR4** dedicada y una **etiqueta térmica híbrida de grafeno y lámina de cobre ($0.15\text{ mm}$)** para disipación directa hacia el chasis.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (PCI-SIG M.2 2280)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Longitud Total del PCB (Eje Y)** | **80.00 mm** | $\pm 0.15\text{ mm}$ | Formato estándar de longitud tipo 2280 para laptops. |
| **Ancho Total del PCB (Eje X)** | **22.00 mm** | $\pm 0.15\text{ mm}$ | Ancho estándar de zócalos M.2 en motherboards de laptop. |
| **Espesor del Sustrato PCB** | **0.80 mm** | $\pm 0.08\text{ mm}$ | Circuito impreso multicapa de 8 capas FR-4 High-Tg negro mate. |
| **Espesor Total con Componentes** | **2.15 mm** | Máx. $2.38\text{ mm}$ | Cumple estrictamente la categoría S3 (Single-Sided) para portátiles. |
| **Altura Inferior (Cara Trasera)** | **0.00 mm** | Máx. $0.00\text{ mm}$ | Sin componentes en la cara inferior; no tropieza con la motherboard. |
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
  * Procesador multinúcleo ARM Cortex-R8 de 8 canales NAND con interfaz PCIe 4.0 x4.
  * Encapsulado: FCCSP ($12.00 \times 12.00 \times 0.85\text{ mm}$).
  * **Difusor Térmico de Níquel:** Tapa de níquel para homogenización de calor bajo cargas sostenidas de escritura.
* **Memoria Caché DRAM:**
  * **512 MB LPDDR4-4266** dedicada ($6.00 \times 5.50 \times 0.60\text{ mm}$).
  * Acelera la tabla de traducción de direcciones FTL para tiempos de respuesta ultra-bajos.
* **Memoria No Volátil (Capacidad 512 GB):**
  * **1x encapsulado BGA-132 de $512\text{ GB}$** con tecnología 3D TLC NAND Flash de 176 capas.
  * Posición secundaria de pistas con pads de soldadura expuestos sin poblar (arquitectura común en SSDs de 512GB).
* **Métricas de Rendimiento Gaming:**
  * Velocidad de Lectura Secuencial: **Hasta 7,000 MB/s**.
  * Velocidad de Escritura Secuencial: **Hasta 5,000 MB/s**.
  * Lectura Aleatoria (4KB QD32): **850,000 IOPS**.
  * Escritura Aleatoria (4KB QD32): **1,000,000 IOPS**.
  * Soporte de **Microsoft DirectStorage API** para arranque instantáneo y carga rápida de juegos.
* **Eficiencia Térmica y Consumo:**
  * Consumo Máximo Activo: $5.0\text{ W}$.
  * Consumo en Reposo (PS4 L1.2): $0.030\text{ W}$ ($30\text{ mW}$).
  * Resistencia y Vida Útil: **300 TBW** (Terabytes Escritos).

---

## 4. INVENTARIO DE ARCHIVOS EN LA CARPETA

📁 `C:\Users\mathi\Documents\3D\SSD_M2_512GB_NVME_PCIE4_LAPTOP`

* [SSD_M2_512GB_NVME_PCIE4_LAPTOP.obj](file:///C:/Users/mathi/Documents/3D/SSD_M2_512GB_NVME_PCIE4_LAPTOP/SSD_M2_512GB_NVME_PCIE4_LAPTOP.obj) &bull; Modelo CAD 3D paramétrico a escala real 1:1.
* [SSD_M2_512GB_NVME_PCIE4_LAPTOP.mtl](file:///C:/Users/mathi/Documents/3D/SSD_M2_512GB_NVME_PCIE4_LAPTOP/SSD_M2_512GB_NVME_PCIE4_LAPTOP.mtl) &bull; Definición de materiales PBR (Grafeno, níquel, oro M-Key, silicio).
* [Visor_3D_SSD_M2_512GB_Laptop.html](file:///C:/Users/mathi/Documents/3D/SSD_M2_512GB_NVME_PCIE4_LAPTOP/Visor_3D_SSD_M2_512GB_Laptop.html) &bull; Visor 3D interactivo offline con cotas 3D y ficha técnica.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_512GB_NVME_PCIE4_LAPTOP/01_render_isometrico_3d.png) &bull; Render isométrico 2K QHD con disipador de grafeno.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_512GB_NVME_PCIE4_LAPTOP/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas de ingeniería sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_512GB_NVME_PCIE4_LAPTOP/03_render_perfil_lateral_espesor.png) &bull; Perfil ortogonal lateral (espesor slim 2.15 mm).
* [04_render_macro_controlador_nvme.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_512GB_NVME_PCIE4_LAPTOP/04_render_macro_controlador_nvme.png) &bull; Macro detalle del controlador niquelado y memoria DRAM.
* [05_render_posterior_plano_cobre.png](file:///C:/Users/mathi/Documents/3D/SSD_M2_512GB_NVME_PCIE4_LAPTOP/05_render_posterior_plano_cobre.png) &bull; Vista posterior con plano de retorno de cobre y vías térmicas.
