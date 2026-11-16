# FICHA TÉCNICA DE INGENIERÍA: MÓDULO DE REFRIGERACIÓN DEDICADO PARA GPU DE LAPTOP
## Cámara de Vapor Direct-Die y Turbina Centrífuga de 84 Micro-Aspas LCP (175W-200W TGP)

---

## 1. ESPECIFICACIONES GENERALES DEL COMPONENTE

| Parámetro | Especificación Oficial de Ingeniería |
| :--- | :--- |
| **Identificador del Sistema** | Módulo Térmico Dedicado de Alto Rendimiento para GPU Móvil (Vapor Chamber + Centrifugal Blower) |
| **Compatibilidad de GPUs Móviles** | NVIDIA GeForce RTX 4090/4080/4070 Mobile (BGA) y AMD Radeon RX 7900M Mobile |
| **Capacidad de Disipación Térmica** | Diseñado para cargas continuas de 150W a 175W + 25W Dynamic Boost (hasta 200W pico) |
| **Tipo de Ventilador** | Turbina centrífuga radial de expulsión lateral/trasera a 90 grados |
| **Material de los Álaves** | Polímero de Cristal Líquido (LCP - Liquid Crystal Polymer) de rigidez aeroespacial |
| **Cantidad de Micro-Aspas** | 84 micro-álaves curvados hacia adelante de espesor variable |
| **Espesor de las Aspas** | 0.10 mm en la punta exterior, ensanchándose a 0.35 mm en la raíz |
| **Motor de la Turbina** | Motor eléctrico sin escobillas (Brushless DC) trifásico de 12V con 6 polos |
| **Rango de Velocidad PWM** | 0 RPM (Modo silencioso por debajo de 45°C) hasta 6,500 RPM a plena carga |
| **Bloque de Intercambio (Coldplate)** | Cámara de vapor de cobre sinterizado sellada al vacío (.8\text{ mm}$ espesor) |
| **Radiador de Disipación** | Doble bloque de aletas de cobre ultrafinas de 0.1 mm con paso de 0.8 mm |
| **Dimensiones Generales (L x An x Gr)** | .0 \times 115.0 \times 12.5\text{ mm}$ (Perfil ultra-delgado para portátiles gamer) |
| **Peso Total del Módulo** | \text{ g}$ |

---

## 2. POR QUÉ LAS LAPTOPS UTILIZAN TURBINAS CENTRÍFUGAS EN LUGAR DE VENTILADORES AXIALES

En las tarjetas gráficas dedicadas de laptops, el espacio vertical está restringido a menos de 15 mm. Un ventilador axial convencional requeriría al menos 30-40 mm para mover aire perpendicularmente. La turbina centrífuga resuelve este reto físico:

1. **Admisión Axial y Expulsión Radial a 90°:** El aire frío se aspira por la parte superior e inferior de la turbina (a través del teclado y la base perforada) y es acelerado centrífugamente para ser expulsado a través de los radiadores de cobre laterales y posteriores hacia afuera del chasis.
2. **Micro-Aspas LCP de 0.1 mm:** Al utilizar Polímero de Cristal Líquido, las 84 aspas pueden ser sumamente delgadas y rígidas. Esto reduce la fricción aerodinámica en un 16% y maximiza la presión estática requerida para forzar el aire a través de la densa matriz de aletas de cobre.
3. **Cámara de Vapor con Contacto Direct-Die:** El silicio de la GPU dedicada no tiene difusor metálico (IHS). La cámara de vapor de cobre pulida a espejo hace contacto térmico directo con el núcleo mediante metal líquido (conductividad térmica $\approx 73\text{ W/m}\cdot\text{K}$) o láminas PCM PTM7950 ($\approx 8.5\text{ W/m}\cdot\text{K}$).

---

## 3. FLUJO TERMODINÁMICO DE REFRIGERACIÓN MÓVIL

`mermaid
flowchart LR
    GPU["Silicio GPU BGA (Direct-Die)"] -->|Metal Líquido| VC["Cámara de Vapor Sinterizada"]
    VRAM["8x Chips GDDR6"] -->|Thermal Pads 14 W/mK| VC
    VC -->|Heatpipes de Cobre Planos| RAD["Radiador de Micro-Aletas de Cobre"]
    
    INTAKE["Entrada de Aire (Arriba/Abajo)"] --> TURB["Turbina LCP 84 Micro-Aspas (6500 RPM)"]
    TURB -->|Flujo de Alta Presión 90°| RAD
    RAD -->|Aire Caliente| EXHAUST["Escape Exterior Lateral/Trasero"]
`

---

## 4. ARCHIVOS DEL COMPONENTE EN EL DIRECTORIO LOCAL

1. [GPU_LAPTOP_DEDICATED_COOLING_VAPOR_CHAMBER_TURBINE.obj](file:///C:/Users/mathi/Documents/3D/GPU/GPU_LAPTOP_DEDICATED_COOLING_VAPOR_CHAMBER_TURBINE/GPU_LAPTOP_DEDICATED_COOLING_VAPOR_CHAMBER_TURBINE.obj) - Malla poligonal CAD 1:1.
2. [GPU_LAPTOP_DEDICATED_COOLING_VAPOR_CHAMBER_TURBINE.mtl](file:///C:/Users/mathi/Documents/3D/GPU/GPU_LAPTOP_DEDICATED_COOLING_VAPOR_CHAMBER_TURBINE/GPU_LAPTOP_DEDICATED_COOLING_VAPOR_CHAMBER_TURBINE.mtl) - Materiales fotométricos y acabados PBR.
3. [Visor_3D_GPU_Laptop_Dedicated_Cooling_Vapor_Chamber_Turbine.html](file:///C:/Users/mathi/Documents/3D/GPU/GPU_LAPTOP_DEDICATED_COOLING_VAPOR_CHAMBER_TURBINE/Visor_3D_GPU_Laptop_Dedicated_Cooling_Vapor_Chamber_Turbine.html) - Visor interactivo Three.js.