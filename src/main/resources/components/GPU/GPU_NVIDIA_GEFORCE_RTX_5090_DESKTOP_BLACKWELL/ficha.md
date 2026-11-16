# ESPECIFICACIONES TÉCNICAS Y COTAS DE INGENIERÍA CAD
## TARJETA GRÁFICA NVIDIA GEFORCE RTX 5090 DESKTOP
### Arquitectura NVIDIA Blackwell (GB202-300) • 32 GB GDDR7 de 512 bits • Chasis Unibody de Titanio Fundido • 1.79 TB/s de Ancho de Banda

---

## 1. DESCRIPCIÓN GENERAL Y CUMBRE DE LA COMPUTACIÓN GRÁFICA

La **NVIDIA GeForce RTX 5090** representa el mayor salto generacional en procesamiento gráfico masivo e inteligencia artificial generativa local, impulsada por la arquitectura **Blackwell GB202**. Diseñada para entusiastas extremos, investigadores de IA y creadores audiovisuales a resoluciones nativas 8K, introduce por primera vez en la historia de las GPUs comerciales la memoria ultra-veloz **GDDR7** con una interfaz monumental de **512 bits**.

### Innovaciones Clave de Ingeniería:
1. **Memoria GDDR7 de 512 Bits y 1,792 GB/s de Ancho de Banda:**
   - Monta 16 chips de memoria GDDR7 perimétricos funcionando a una velocidad sin precedentes de **28 Gbps**, pulverizando cualquier cuello de botella en modelos de lenguaje masivos (LLMs) y trazado de rayos completo (*Full Path Tracing*).
2. **Estructura Monolítica de Titanio y Chasis Flow-Through:**
   - Marco perimetral unibody fundido a presión en aleación de titanio y magnesio con acabado gris espacial (*Gunmetal Titanium*), integrando aletas radiadoras con cámaras de vapor soldadas por difusión térmica.
   - Sistema de flujo de aire contrarrotativo independiente: el ventilador frontal expulsa aire caliente por los brackets I/O, mientras el ventilador posterior pasante succiona aire a través del chasis directamente al túnel de escape superior del gabinete.
3. **Alimentación y Conectividad de Última Generación:**
   - Interfaz nativa **PCI Express 5.0 x16** (hasta 64 GB/s bidireccional).
   - Conector de energía reforzado **12V-2x6 (PCIe Gen 5)** con pines de detección de inserción mejorados para suministrar con total seguridad hasta **600W TGP**.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS CAD (ATX / PCIe 5.0)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Longitud Total (X)** | **304.00 mm** | $\pm 0.50\text{ mm}$ | Largo total del conjunto chasis y placa I/O. |
| **Altura / Profundidad (Y)** | **137.00 mm** | $\pm 0.30\text{ mm}$ | Altura desde el contacto PCIe hasta el lomo exterior. |
| **Grosor / Espesor de Ranura (Z)** | **61.00 mm** | $\pm 0.25\text{ mm}$ | Factor de forma de **3.0 Ranuras (Triple Slot)**. |
| **Pines del Zócalo PCIe** | **164 Contactos** | Estándar CEM | Baño electrolítico de oro duro ($30\,\mu\text{in}$) PCIe 5.0. |
| **Diámetro de Ventiladores** | **$2\times 120.00\text{ mm}$** | $\pm 0.20\text{ mm}$ | Aspas aerodinámicas de polímero reforzado con anillo exterior. |
| **Conector de Alimentación** | **12V-2x6 (16-Pin)** | Estándar ATX 3.1 | Capacidad nominal de corriente continua hasta 600 Watts. |
| **Aletas del Disipador** | **0.35 mm** (x70) | $\pm 0.05\text{ mm}$ | Micro-aletas de aluminio estañado unidas por soldadura al vacío. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES DE COMPUTACIÓN

* **Silicio:** NVIDIA Blackwell GB202-300-A1 fabricado en el nodo avanzado TSMC 4NP.
* **Transistores:** Más de **96,000 Millones** de compuertas lógicas integradas.
* **Núcleos de Cómputo:**
  * **21,760 Núcleos CUDA** a una frecuencia Boost de 2,550 MHz.
  * **680 Núcleos Tensor de 5.ª Generación** con soporte Transformer Engine FP4 / FP8.
  * **170 Núcleos RT de 4.ª Generación** con micromallas de opacidad y desplazamiento acelerado.
* **Subsistema de Memoria:**
  * **Capacidad:** 32 GB GDDR7 de ultra-baja latencia.
  * **Bus de Memoria:** 512-bit (16 canales independientes de 32-bit).
  * **Velocidad de Reloj:** 28 Gbps efectivos.
  * **Ancho de Banda Bruto:** **1,792 GB/s (1.79 TB/s)**.
* **Consumo Energético:** 600W TGP (Total Graphics Power) con fuente recomendada $\ge 1000\text{W}$.
* **Salidas de Video:** 3x DisplayPort 2.1a (UHBR20 nativo) + 1x HDMI 2.1b (FRL 48 Gbps).

---

## 4. ARCHIVOS DEL COMPONENTE EN EL DIRECTORIO LOCAL

1. [GPU_NVIDIA_GEFORCE_RTX_5090_DESKTOP_BLACKWELL.obj](file:///C:/Users/mathi/Documents/3D/GPU/GPU_NVIDIA_GEFORCE_RTX_5090_DESKTOP_BLACKWELL/GPU_NVIDIA_GEFORCE_RTX_5090_DESKTOP_BLACKWELL.obj) - Malla poligonal CAD 1:1 con ventiladores de flujo pasante contrarotativos.
2. [GPU_NVIDIA_GEFORCE_RTX_5090_DESKTOP_BLACKWELL.mtl](file:///C:/Users/mathi/Documents/3D/GPU/GPU_NVIDIA_GEFORCE_RTX_5090_DESKTOP_BLACKWELL/GPU_NVIDIA_GEFORCE_RTX_5090_DESKTOP_BLACKWELL.mtl) - Materiales fotométricos y acabados de titanio mecanizado CNC.
3. [Visor_3D_NVIDIA_GeForce_RTX_5090_Desktop_Blackwell.html](file:///C:/Users/mathi/Documents/3D/GPU/GPU_NVIDIA_GEFORCE_RTX_5090_DESKTOP_BLACKWELL/Visor_3D_NVIDIA_GeForce_RTX_5090_Desktop_Blackwell.html) - Visor interactivo Three.js con ventiladores contrarotativos animados.