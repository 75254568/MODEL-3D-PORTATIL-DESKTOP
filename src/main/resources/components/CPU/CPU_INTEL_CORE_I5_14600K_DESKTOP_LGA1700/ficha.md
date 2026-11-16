# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR INTEL CORE i5-14600K (DESKTOP LGA1700)
### Zócalo Oficial Intel LGA1700 • 14 Núcleos (6P + 8E) / 20 Hilos • Frecuencia Turbo hasta 5.30 GHz • 24 MB L3 Cache

---

## 1. DESCRIPCIÓN GENERAL Y EL ESTÁNDAR PARA GAMING COMPETITIVO

El **Intel Core i5-14600K** es el referente de la gama media-alta de la familia **Raptor Lake-S Refresh**, reconocido ampliamente en la comunidad entusiasta por ofrecer una relación rendimiento/coste insuperable en videojuegos y creación de contenido ligero.

### Innovaciones Clave de Ingeniería:
1. **Topología Híbrida 6P + 8E:**
   - 6 núcleos de rendimiento (*Raptor Cove*) optimizados para entregar frecuencias pico de hasta **5.30 GHz**, asegurando la máxima tasa de cuadros por segundo en títulos eSports y triple A.
   - 8 núcleos eficientes (*Gracemont*) que liberan a los núcleos principales de procesos secundarios del sistema operativo, transmisión por streaming y tareas en segundo plano.
2. **Encapsulado LGA1700 Rectangular:**
   - Dimensiones de sustrato de $37.50 \times 45.00\text{ mm}$ con 1,700 contactos planos dorados (*lands*).
   - Difusor térmico integrado (IHS) de cobre puro electrolítico con recubrimiento de níquel anticorrosivo y pestañas mecánicas laterales para el mecanismo de bloqueo del zócalo.
3. **Plataforma Versátil y Soporte de Memoria:**
   - Soporte nativo para módulos de memoria **DDR5-5600** y retrocompatibilidad completa con placas madre económicas **DDR4-3200**.
   - 16 líneas PCIe 5.0 para la GPU y 4 líneas PCIe 4.0 directas a la CPU para unidades NVMe.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (SOCKET LGA1700)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **37.50 mm** | $\pm 0.10\text{ mm}$ | Ancho normalizado zócalo LGA1700. |
| **Largo del Sustrato (Y)** | **45.00 mm** | $\pm 0.10\text{ mm}$ | Perfil rectangular alargado Intel 7. |
| **Espesor del Sustrato PCB** | **1.15 mm** | $\pm 0.05\text{ mm}$ | PCB multicapa de 10 capas de rigidez reforzada. |
| **Espesor Total con IHS (Z)** | **4.40 mm** | $\pm 0.08\text{ mm}$ | Altura estandarizada con soldadura térmica STIM de Indio. |
| **Cuerpo Central del IHS** | **$28.20 \times 38.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Superficie plana rectificada con tolerancia $\le 15\,\mu\text{m}$. |
| **Paso de Contactos LGA** | **0.80 mm** | $\pm 0.02\text{ mm}$ | Cuadrícula de 1,700 contactos con baño de oro de $30\,\mu\text{in}$. |
| **Alas de Sujeción Lateral** | **$4.65 \times 16.00\text{ mm}$** | $\pm 0.05\text{ mm}$ | Pestañas de sujeción para el mecanismo ILM. |

---

## 3. FICHA TÉCNICA Y ESPECIFICACIONES ELÉCTRICAS

* **Litografía:** Intel 7 (proceso FinFET de 10nm perfeccionado).
* **Núcleos e Hilos:** 14 Núcleos (6 Performance + 8 Efficient) • 20 Hilos de ejecución.
* **Frecuencias de Reloj:**
  * P-Core: 3.50 GHz Base / 5.30 GHz Turbo Boost.
  * E-Core: 2.60 GHz Base / 4.00 GHz Turbo.
* **Memoria Caché:**
  * **Caché L3 (Intel Smart Cache):** **24 MB**.
  * **Caché L2 Total:** **20 MB** (2 MB por P-Core + 4 MB por clúster de E-Cores).
* **Potencia Térmica:** 125W Potencia Base (PBP) • 181W Potencia Turbo Máxima (MTP).
* **Gráficos Integrados:** Intel UHD Graphics 770 (32 EUs a 1.55 GHz) con doble codificador QuickSync.