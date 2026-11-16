# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## MEMORIA RAM DDR5 16 GB SO-DIMM (5600 MHz) – EXCLUSIVO PARA LAPTOPS
### Estándar Oficial JEDEC MO-329 &bull; Factor de Forma Portátil (262 Pines)

---

## 1. DESCRIPCIÓN GENERAL Y FACTOR DE FORMA

Este componente corresponde a un **módulo de memoria RAM DDR5 SO-DIMM (Small Outline Dual In-line Memory Module)** de **16 Gigabytes** de capacidad y una velocidad de transferencia de **5600 MT/s**, diseñado y estandarizado específicamente para **ordenadores portátiles (laptops)**, mini-PCs compactos y placas madre con zócalos horizontales de inserción angular a 30°.

> [!IMPORTANT]
> **Diferencia Fundamental: Laptop (SO-DIMM) vs. PC de Escritorio (U-DIMM):**
> * **Laptop (SO-DIMM):** Longitud de **69.60 mm**, altura de **30.00 mm**, **262 pines** con paso de $0.50\text{ mm}$ y muesca a los **41.25 mm**.
> * **PC de Escritorio (U-DIMM):** Longitud de **133.35 mm**, altura de **31.25 mm**, **288 pines** con paso de $0.85\text{ mm}$ y muesca central.
> * **Este modelo 3D y documentación corresponden estrictamente a la versión de LAPTOP.**

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (ESCALA 1:1)

Todas las dimensiones están normalizadas bajo la especificación **JEDEC Design Outline MO-329**:

| Parámetro Geométrico | Cota Nominal | Tolerancia JEDEC | Descripción Técnica |
| :--- | :--- | :--- | :--- |
| **Longitud Total del PCB (Eje X)** | **69.60 mm** | $\pm 0.15\text{ mm}$ | Ancho total del sustrato de izquierda a derecha. |
| **Altura Total del PCB (Eje Y)** | **30.00 mm** | $\pm 0.15\text{ mm}$ | Altura desde el borde inferior de inserción hasta el borde superior. |
| **Espesor del Sustrato PCB** | **1.20 mm** | $\pm 0.10\text{ mm}$ | Circuito impreso multicapa de 8 a 10 capas FR-4 High-Tg ($>170^\circ\text{C}$). |
| **Espesor Total con Componentes (Eje Z)** | **3.80 mm** | Máx. $4.00\text{ mm}$ | Espesor medido sobre los componentes más altos (bobinas de potencia). |
| **Cantidad Total de Pines** | **262 Pines** | Exacto | 131 pines en cara frontal A + 131 pines en cara posterior B. |
| **Paso de Pin (Pitch)** | **0.50 mm** | $\pm 0.03\text{ mm}$ | Separación de centro a centro entre contactos dorados contiguos. |
| **Ancho de Cada Pin Dorado** | **0.28 mm** | $\pm 0.02\text{ mm}$ | Pistas de cobre con baño electrolítico de oro duro sobre níquel. |
| **Altura de Contacto Dorado** | **3.40 mm** | $\pm 0.10\text{ mm}$ | Superficie de frotamiento efectiva para los contactos del zócalo. |
| **Bisel de Entrada (Insertion Bevel)** | **$20^\circ$** | $\pm 5^\circ$ | Chaflán inferior en el borde de inserción para evitar dañar los pines. |
| **Posición de Muesca Clave (Key Notch)** | **41.25 mm** | $\pm 0.10\text{ mm}$ | Medida desde el borde izquierdo del PCB hasta el centro de la muesca. |
| **Ancho de la Muesca Clave** | **1.50 mm** | $\pm 0.10\text{ mm}$ | Anchura de corte que encaja en el poste de polarización del zócalo. |
| **Profundidad / Altura de la Muesca** | **4.00 mm** | $\pm 0.15\text{ mm}$ | Profundidad del corte vertical desde el borde inferior de pines. |
| **Muescas Laterales de Retención** | **$Y = 20.50 \text{ a } 23.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Ranuras en los laterales de $1.50\text{ mm}$ de profundidad para los clips de laptop. |
| **Chaflanes en Esquinas Superiores** | **$1.00 \times 1.00\text{ mm}$** | $45^\circ$ | Esquinas redondeadas/biseladas para fácil manipulación durante el ensamble. |

---

## 3. ARQUITECTURA ELECTRÓNICA Y COMPONENTES INTEGRADOS

La memoria DDR5 introduce una revolución arquitectónica respecto a generaciones anteriores (DDR4), trasladando componentes críticos directamente a la placa del módulo SO-DIMM:

```
┌────────────────────────────────────────────────────────────────────────┐
│ [▲ 45°]                        69.60 mm                        [▲ 45°] │
│ ┌─────────┬──────────────────────────────────────────────────────────┐ │
│ │ SPD HUB │   [ INDUCTOR L1 ]  [  PMIC QFN-32  ]  [ INDUCTOR L2 ]    │ │
│ │ (I3C)   │   (2.5 x 2.0 mm)   (4.0 x 4.0 mm)     (2.5 x 2.0 mm)     │ │
│ └─────────┴──────────────────────┬───────────────────────────────────┘ │
│ ┌──────────────┐  ┌──────────────┤ [ INDUCTOR L3 ] ┌─────────────────┐ │
│ │ DRAM CHIP U1 │  │ DRAM CHIP U2 │                 │  OEM SPEC LABEL │ │
│ │ (32Gb FBGA)  │  │ (32Gb FBGA)  │  ┌───────────┐  │  (LENOVO 16GB)  │ │
│ │ 11.0x7.8 mm  │  │ 11.0x7.8 mm  │  │ DRAM CHIP │  │  BARCODE / QR   │ │
│ └──────────────┘  └──────────────┘  │ (32Gb) U3 │  └─────────────────┘ │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ └───────────┘ ░░░░░░░░░░░░░░░░░░░  │
│ [ 82 PINES DORADOS FRONTALES ]    [NOTCH]    [ 49 PINES DORADOS ]      │
│ └─────────────────────────────────┘ 41.25mm └─────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### A. Sub-sistema de Gestión de Potencia (PMIC Onboard)
* **Encapsulado:** QFN-32 de perfil ultra-delgado ($4.00 \times 4.00 \times 0.85\text{ mm}$).
* **Función:** En DDR4 la placa madre regulaba el voltaje; en DDR5 para laptops, la placa suministra $5.0\text{V}$ constantes y el **PMIC local** se encarga de reducir y estabilizar el voltaje:
  * **$\text{VDD} = 1.10\text{V}$:** Alimentación del núcleo lógico de las celdas DRAM.
  * **$\text{VDDQ} = 1.10\text{V}$:** Tensión para los búferes de entrada/salida (I/O) de alta velocidad.
  * **$\text{VPP} = 1.80\text{V}$:** Voltaje de activación de filas (*wordline boost voltage*).
* **Eficiencia Térmica:** Cuenta con un disipador metálico central expuesto (*exposed pad* de $2.60 \times 2.60\text{ mm}$) soldado directamente a las capas internas de cobre para disipación de calor por convección.

### B. Inductores de Potencia Blindados (Power Chokes)
* **Cantidad:** 3 bobinas de ferrita moldeada ($2.50 \times 2.00 \times 1.10\text{ mm}$).
* **Ubicación:** Dispuestas en configuración trifásica alrededor del PMIC (L1 izquierda, L2 derecha, L3 inferior).
* **Propósito:** Filtrado de ruido de alta frecuencia en los convertidores reductores síncronos (*buck converters*).

### C. Microcontrolador SPD Hub + Sensor Térmico
* **Encapsulado:** DFN-8 ($2.50 \times 2.00 \times 0.75\text{ mm}$).
* **Interfaz de Comunicación:** Bus **I3C Basic** de alta velocidad (compatible retroactivamente con I2C/SMBus a 1 MHz y 12.5 MHz).
* **Capacidades:** Almacena la tabla JEDEC y perfiles de memoria; incorpora un **sensor de temperatura digital calibrado ($\pm 0.5^\circ\text{C}$)** que envía lecturas en tiempo real a la BIOS para prevenir sobrecalentamiento (*thermal throttling*).

### D. Chips de Memoria DRAM (Capacidad: 16 GB)
* **Configuración:** 4 matrices BGA de $32\text{ Gigabits}$ ($4\text{ GB}$) cada una, conformando los $16\text{ GB}$ totales en topología 1Rx8 o 1Rx16.
* **Encapsulado:** FBGA-102 (Fine-pitch Ball Grid Array) de $11.00 \times 7.80 \times 0.85\text{ mm}$.
* **Separación de Montaje (Standoff):** $0.15\text{ mm}$ mediante matriz de micro-esferas de aleación SAC305 (Sn-Ag-Cu libre de plomo).
* **On-Die ECC:** Mecanismo integrado de corrección de errores dentro de cada chip DRAM que corrige fallos de un solo bit en tiempo real sin sobrecargar el bus del procesador.

### E. Matriz de Desacoplo Pasivo MLCC
* **Componentes:** 48 condensadores cerámicos multicapa (MLCC) tamaño SMD 0402 ($1.00 \times 0.50 \times 0.45\text{ mm}$) y 0603.
* **Distribución:** Matriz de baja inductancia situada inmediatamente debajo y detrás de cada chip DRAM para amortiguar picos de corriente $dI/dt$ generados a $5600\text{ MT/s}$.

---

## 4. ESTRUCTURA DE SUBCANALES DUALES (VENTAJA EN LAPTOPS)

A diferencia de las memorias DDR4 (que utilizaban un único canal de 64 bits por módulo), cada módulo **DDR5 SO-DIMM para laptop** se divide internamente en **dos subcanales independientes de 32 bits** (más 8 bits de ECC por subcanal):

1. **Subcanal A (32 bits + 8 bits ECC = 40 bits):** Gestionado por los pines 1 al 130 del conector JEDEC.
2. **Subcanal B (32 bits + 8 bits ECC = 40 bits):** Gestionado por los pines 131 al 262 del conector JEDEC.

* **Impacto en Rendimiento:** Un solo módulo de $16\text{ GB}$ DDR5 en una laptop opera en modo dual-channel interno, reduciendo la latencia de acceso a memoria y duplicando la eficiencia en multitarea y cargas de gaming pesado frente a DDR4.

---

## 5. GUÍA DE INSTALACIÓN Y MONTAJE EN LAPTOP

1. **Desconexión de Seguridad:** Apagar el portátil, retirar la tapa inferior de servicio y desconectar el conector principal de la batería de la placa madre.
2. **Orientación de la Muesca:** Alinear la muesca polarizadora de $1.50\text{ mm}$ del módulo con el resalte del zócalo SO-DIMM (ubicado a los $41.25\text{ mm}$).
3. **Ángulo de Inserción:** Insertar el módulo de memoria en la ranura en un **ángulo de $30^\circ$** con respecto a la placa madre, asegurando que los 262 pines dorados penetren completamente.
4. **Fijación:** Presionar suavemente el borde superior del módulo hacia abajo (hacia el chasis) hasta que los clips metálicos laterales encajen con un chasquido auditivo en las **muescas de retención laterales** de $1.50\text{ mm}$.
5. **Blindaje Térmico:** Volver a colocar el protector metálico estampado de aluminio / mylar negro antiestático del portátil.

---

## 6. INVENTARIO DE ARCHIVOS EN LA CARPETA

Todos los recursos se encuentran centralizados en:
📁 `C:\Users\mathi\Documents\3D\DDR5_16GB_SODIMM_LAPTOP`

* [DDR5_16GB_SODIMM_LAPTOP.obj](file:///C:/Users/mathi/Documents/3D/DDR5_16GB_SODIMM_LAPTOP/DDR5_16GB_SODIMM_LAPTOP.obj) &bull; Malla CAD paramétrica 3D a escala real 1:1.
* [DDR5_16GB_SODIMM_LAPTOP.mtl](file:///C:/Users/mathi/Documents/3D/DDR5_16GB_SODIMM_LAPTOP/DDR5_16GB_SODIMM_LAPTOP.mtl) &bull; Biblioteca de materiales físicos PBR.
* [Visor_3D_DDR5_16GB_Laptop.html](file:///C:/Users/mathi/Documents/3D/DDR5_16GB_SODIMM_LAPTOP/Visor_3D_DDR5_16GB_Laptop.html) &bull; Visor 3D WebGL con Three.js, medidor de cotas y modos de inspección.
* [01_render_isometrico_3d.png](file:///C:/Users/mathi/Documents/3D/DDR5_16GB_SODIMM_LAPTOP/01_render_isometrico_3d.png) &bull; Render 2K QHD Isométrico.
* [02_render_frontal_cotas_ingenieria.png](file:///C:/Users/mathi/Documents/3D/DDR5_16GB_SODIMM_LAPTOP/02_render_frontal_cotas_ingenieria.png) &bull; Plano frontal con cotas dimensionales sobreimpresas.
* [03_render_perfil_lateral_espesor.png](file:///C:/Users/mathi/Documents/3D/DDR5_16GB_SODIMM_LAPTOP/03_render_perfil_lateral_espesor.png) &bull; Perfil ortogonal lateral (espesor de PCB y componentes).
* [04_render_macro_pmic_circuiteria.png](file:///C:/Users/mathi/Documents/3D/DDR5_16GB_SODIMM_LAPTOP/04_render_macro_pmic_circuiteria.png) &bull; Macro fotografía del sub-sistema PMIC de potencia.
* [05_render_posterior_plano_masa.png](file:///C:/Users/mathi/Documents/3D/DDR5_16GB_SODIMM_LAPTOP/05_render_posterior_plano_masa.png) &bull; Vista posterior de desacoplo y apantallamiento.
* [three.min.js](file:///C:/Users/mathi/Documents/3D/DDR5_16GB_SODIMM_LAPTOP/three.min.js) &bull; Motor 3D WebGL local.
* [OrbitControls.js](file:///C:/Users/mathi/Documents/3D/DDR5_16GB_SODIMM_LAPTOP/OrbitControls.js) &bull; Control orbital de cámara 3D local.
