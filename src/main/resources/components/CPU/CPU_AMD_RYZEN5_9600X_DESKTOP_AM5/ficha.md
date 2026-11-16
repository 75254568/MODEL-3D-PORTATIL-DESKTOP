# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR AMD RYZEN 5 9600X (DESKTOP SOCKET AM5)
### Zócalo Oficial AMD Socket AM5 (LGA1718) • 6 Núcleos / 12 Hilos Zen 5 • Arquitectura "Granite Ridge" 4nm • 65W TDP Base

---

## 1. DESCRIPCIÓN GENERAL Y EFICIENCIA EN ARQUITECTURA ZEN 5

El **AMD Ryzen 5 9600X** es el procesador de 6 núcleos de última generación de AMD impulsado por la microarquitectura **Zen 5**. Fabricado en un proceso litográfico avanzado de **4nm TSMC**, ofrece un incremento superior al **16% en IPC** respecto a su predecesor, combinando velocidades de hasta 5.40 GHz con un consumo extraordinariamente contenido de solo **65W**.

### Innovaciones Clave de Ingeniería:
1. **Núcleos Zen 5 con Soporte AVX-512 Completo:**
   - 6 núcleos y 12 hilos de procesamiento simultáneo con una tubería de ejecución ensanchada a 6 vías y vectorización matemática AVX-512 nativa a paso completo.
2. **Difusor Térmico Cut-Out "Octopus" AM5:**
   - El disipador niquelado cuenta con el diseño característico de **8 patas perimétricas**, dejando accesibles los condensadores cerámicos multicapa (MLCC) expuestos en la cara superior del sustrato.
   - Soldadura de indio líquido entre el Core Complex Die (CCD) de cómputo y el IHS exterior para una disipación instantánea.
3. **Plataforma AM5 Preparada para el Futuro:**
   - Zócalo **LGA1718** con 1,718 contactos elásticos dorados divididos en dos matrices simétricas.
   - Interfaz nativa **PCIe 5.0** (28 líneas) y soporte de memoria **DDR5-5600** con perfiles de sobreaceleración AMD EXPO.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (SOCKET AM5)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Formato cuadrado normalizado Socket AM5. |
| **Largo del Sustrato (Y)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Sustrato de resina BT de 16 capas de ultra-alta rigidez. |
| **Espesor del Sustrato PCB** | **1.50 mm** | $\pm 0.06\text{ mm}$ | Sustrato reforzado contra deformación por coolers pesados. |
| **Espesor Total con IHS (Z)** | **4.85 mm** | $\pm 0.08\text{ mm}$ | Altura Z compatible con coolers AM4 y AM5. |
| **Cuerpo Central del IHS** | **$28.00 \times 28.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Superficie plana niquelada rectificada por CNC. |
| **Indicador Pin 1** | **$2.20 \times 2.20\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Cantidad de Contactos LGA** | **1,718 Pads** | Exacto | Matriz dual simétrica con baño de oro de $30\,\mu\text{in}$. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES DE COMPUTACIÓN

* **Silicio:** 1x CCD Zen 5 (TSMC 4nm, $70.6\text{ mm}^2$) + 1x cIOD (TSMC 6nm, $117.8\text{ mm}^2$).
* **Transistores:** Más de **8,315 Millones** de compuertas lógicas integradas.
* **Núcleos e Hilos:** 6 Núcleos Zen 5 • 12 Hilos de procesamiento simultáneo (SMT).
* **Frecuencias de Reloj:** Frecuencia Base de 3.90 GHz / Frecuencia Boost Máxima de **5.40 GHz**.
* **Memoria Caché:**
  * **Caché L3:** 32 MB compartidos en un único complejo unificado.
  * **Caché L2:** 6 MB (1 MB dedicado por núcleo Zen 5).
  * **Caché Total:** **38 MB**.
* **Potencia y Eficiencia:** 65W TDP Base (configurable mediante BIOS a modo 105W cTDP).
* **Gráficos Integrados:** AMD RDNA 2 (2 Compute Units a 2,200 MHz).