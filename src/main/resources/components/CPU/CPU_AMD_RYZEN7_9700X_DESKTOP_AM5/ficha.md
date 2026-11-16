# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR AMD RYZEN 7 9700X (DESKTOP SOCKET AM5)
### Zócalo Oficial AMD Socket AM5 (LGA1718) • 8 Núcleos / 16 Hilos Zen 5 • Arquitectura "Granite Ridge" 4nm • 65W TDP Base

---

## 1. DESCRIPCIÓN GENERAL Y EL SALTO ARQUITECTÓNICO ZEN 5

El **AMD Ryzen 7 9700X** es el procesador de 8 núcleos de última generación de AMD basado en la microarquitectura **Zen 5** ("Granite Ridge"). Con un incremento promedio del **16% en instrucciones por ciclo de reloj (IPC)** y un salto masivo en rendimiento por vatio, redefine los estándares de eficiencia energética al operar con una envolvente térmica base de solo **65W TDP**.

### Innovaciones Clave de Ingeniería:
1. **Microarquitectura Zen 5 de Doble Tubería:**
   - Fabricado en el nodo de **4nm FinFET de TSMC** para el Core Complex Die (CCD) de cómputo, complementado por un Client I/O Die (cIOD) en 6nm.
   - Decodificación dual de instrucciones, unidad de coma flotante de 512 bits completa (soporte nativo AVX-512) y predicción de saltos cuántico-neuronal de ultra-baja latencia.
2. **Difusor Térmico Cut-Out "Octopus" AM5:**
   - El disipador niquelado cuenta con el diseño característico de **8 patas perimétricas con huecos recortados**, donde se alojan condensadores cerámicos multicapa (MLCC) expuestos en la cara superior del sustrato.
   - Soldadura de indio metálico líquido de alta conductividad térmica entre el CCD y el disipador exterior.
3. **Plataforma AM5 de Larga Duración:**
   - Zócalo **LGA1718** con 1,718 contactos elásticos dorados divididos en dos matrices simétricas.
   - Interfaz nativa **PCIe 5.0** (28 líneas) y memoria **DDR5-5600** con perfiles de sobreaceleración AMD EXPO.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (SOCKET AM5)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Formato cuadrado normalizado Socket AM5. |
| **Largo del Sustrato (Y)** | **40.00 mm** | $\pm 0.10\text{ mm}$ | Sustrato de resina BT de 16 capas de ultra-alta rigidez. |
| **Espesor del Sustrato PCB** | **1.50 mm** | $\pm 0.06\text{ mm}$ | Sustrato reforzado contra curvatura por presión del cooler. |
| **Espesor Total con IHS (Z)** | **4.85 mm** | $\pm 0.08\text{ mm}$ | Altura Z compatible con coolers AM4 y AM5. |
| **Cuerpo Central del IHS** | **$28.00 \times 28.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Placa superior rectificada para contacto con pasta térmica. |
| **Indicador Pin 1** | **$2.20 \times 2.20\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Cantidad de Contactos LGA** | **1,718 Pads** | Exacto | Matriz dual simétrica con baño de oro electrolítico de $30\,\mu\text{in}$. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES DE COMPUTACIÓN

* **Silicio:** 1x CCD Zen 5 (TSMC 4nm, $70.6\text{ mm}^2$) + 1x cIOD (TSMC 6nm, $117.8\text{ mm}^2$).
* **Transistores:** Más de **8,315 Millones** de compuertas lógicas integradas.
* **Núcleos e Hilos:** 8 Núcleos Zen 5 • 16 Hilos de procesamiento simultáneo (SMT).
* **Frecuencias de Reloj:** Frecuencia Base de 3.80 GHz / Frecuencia Boost Máxima de **5.50 GHz**.
* **Memoria Caché:**
  * **Caché L3:** 32 MB compartidos en un único complejo unificado.
  * **Caché L2:** 8 MB (1 MB dedicado por núcleo Zen 5).
  * **Caché Total:** **40 MB**.
* **Potencia y Eficiencia:** 65W TDP Base (configurable mediante BIOS a modo 105W cTDP para cargas extremas).
* **Gráficos Integrados:** AMD RDNA 2 (2 Compute Units a 2,200 MHz) para diagnóstico y salida de pantalla.