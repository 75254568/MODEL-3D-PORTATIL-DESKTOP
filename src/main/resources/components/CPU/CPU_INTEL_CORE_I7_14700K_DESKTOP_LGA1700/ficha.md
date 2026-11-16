# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR INTEL CORE i7-14700K (DESKTOP LGA1700)
### Zócalo Oficial Intel LGA1700 • 20 Núcleos (8P + 12E) / 28 Hilos • Frecuencia Turbo hasta 5.60 GHz • 33 MB L3 Cache

---

## 1. DESCRIPCIÓN GENERAL Y EQUILIBRIO EN RENDIMIENTO

El **Intel Core i7-14700K** es el procesador más popular y versátil de la generación **Raptor Lake-S Refresh**, destacando como el único modelo de la serie 14000 en recibir un aumento directo en el recuento físico de núcleos (+4 Gracemont E-Cores), sumando un total de 20 núcleos y 28 hilos de ejecución paralela.

### Innovaciones Clave de Ingeniería:
1. **Topología Híbrida 8P + 12E:**
   - 8 núcleos de rendimiento (*Raptor Cove*) con hiper-subprocesamiento que alcanzan hasta **5.60 GHz** mediante Intel Turbo Boost Max Technology 3.0.
   - 12 núcleos eficientes (*Gracemont*) agrupados en 3 clústeres para gestionar cargas masivas en segundo plano y renderizado 3D multi-hilo.
2. **Encapsulado LGA1700 Rectangular:**
   - Dimensiones de sustrato de $37.50 \times 45.00\text{ mm}$ con 1,700 contactos planos dorados (*lands*).
   - Difusor térmico integrado (IHS) de cobre puro electrolítico con recubrimiento de níquel anticorrosivo y alas laterales de anclaje para el marco de carga independiente (ILM).
3. **Memoria y Conectividad Híbrida:**
   - Compatibilidad nativa con memorias **DDR5-5600** y retrocompatibilidad con placas base **DDR4-3200**.
   - 16 líneas PCIe 5.0 dedicadas a la tarjeta gráfica y 4 líneas PCIe 4.0 para almacenamiento SSD M.2 de alta velocidad.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (SOCKET LGA1700)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **37.50 mm** | $\pm 0.10\text{ mm}$ | Ancho normalizado zócalo LGA1700. |
| **Largo del Sustrato (Y)** | **45.00 mm** | $\pm 0.10\text{ mm}$ | Perfil alargado rectangular característico de Intel 7. |
| **Espesor del Sustrato PCB** | **1.15 mm** | $\pm 0.05\text{ mm}$ | PCB de 10 capas de alta rigidez dieléctrica. |
| **Espesor Total con IHS (Z)** | **4.40 mm** | $\pm 0.08\text{ mm}$ | Altura estandarizada con soldadura STIM de Indio. |
| **Cuerpo Central del IHS** | **$28.20 \times 38.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Superficie plana rectificada con tolerancia $\le 15\,\mu\text{m}$. |
| **Paso de Contactos LGA** | **0.80 mm** | $\pm 0.02\text{ mm}$ | Cuadrícula de 1,700 almohadillas doradas de $30\,\mu\text{in}$. |
| **Alas de Sujeción Lateral** | **$4.65 \times 16.00\text{ mm}$** | $\pm 0.05\text{ mm}$ | Pestañas de presión mecánica para el socket ILM. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES ELÉCTRICAS

* **Litografía:** Intel 7 (proceso FinFET de 10nm perfeccionado).
* **Núcleos e Hilos:** 20 Núcleos (8 Performance + 12 Efficient) • 28 Hilos de ejecución.
* **Frecuencias de Reloj:**
  * P-Core: 3.40 GHz Base / 5.50 GHz Turbo / 5.60 GHz Turbo Boost Max 3.0.
  * E-Core: 2.50 GHz Base / 4.30 GHz Turbo.
* **Memoria Caché:**
  * **Caché L3 (Intel Smart Cache):** **33 MB**.
  * **Caché L2 Total:** **28 MB** (2 MB por P-Core + 4 MB por clúster de E-Cores).
* **Potencia Térmica:** 125W Potencia Base (PBP) • 253W Potencia Turbo Máxima (MTP).
* **Gráficos Integrados:** Intel UHD Graphics 770 (32 EUs a 1.60 GHz) con motores duales QuickSync.