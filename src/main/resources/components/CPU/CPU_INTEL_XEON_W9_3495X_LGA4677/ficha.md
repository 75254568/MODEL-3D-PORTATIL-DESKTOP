# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR INTEL XEON w9-3495X (WORKSTATION SOCKET LGA4677)
### Zócalo Oficial Intel Socket LGA4677 • 56 Núcleos / 112 Hilos • Arquitectura Sapphire Rapids EMIB

---

## 1. DESCRIPCIÓN GENERAL Y TOPOLOGÍA MULTI-TILE EMIB

El **Intel Xeon w9-3495X** es el procesador insignia de la plataforma para estaciones de trabajo profesionales de Intel ("Sapphire Rapids-WS"), compatible con placas madre con zócalo **Socket LGA4677** (Socket E, chipset Intel W790).

### Innovaciones Clave de Ingeniería:
1. **Dimensiones Industriales Gigantes ($56.50 \times 77.50\text{ mm}$):**
   - Diseñado para cargas ininterrumpidas de renderizado 3D, simulación multifísica e inteligencia artificial.
   - Cuenta con **4,677 contactos planos dorados** con 4 cavidades centrales que albergan los condensadores MLCC para las interconexiones internas EMIB.
2. **Arquitectura Multi-Tile con Puentes EMIB (Embedded Multi-die Interconnect Bridge):**
   - Cuatro matrices de silicio fabricadas en litografía Intel 7 se interconectan mediante puentes de silicio embebidos en el sustrato orgánico a distancias micrométricas, comportándose virtualmente como un único die monolítico gigante.
3. **Mecanismo de Inserción Tool-less con Marco Carrier:**
   - Incorpora clips plásticos de transporte en los laterales del sustrato que se acoplan directamente a la bandeja portadora del zócalo LGA4677, permitiendo al operador instalar el procesador sin tocar los contactos dorados ni el silicio.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (LGA4677)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **56.50 mm** | $\pm 0.15\text{ mm}$ | Formato rectangular masivo Socket E. |
| **Largo del Sustrato (Y)** | **77.50 mm** | $\pm 0.15\text{ mm}$ | Longitud para alojar 4 tiles EMIB y 4,677 contactos. |
| **Espesor del Sustrato PCB** | **1.55 mm** | $\pm 0.08\text{ mm}$ | Sustrato multicapa Intel Teal de grado industrial. |
| **Espesor Total con IHS (Z)** | **4.55 mm** | $\pm 0.10\text{ mm}$ | Altura Z estandarizada LGA4677. |
| **Domo Central del IHS** | **$43.00 \times 62.00\text{ mm}$** | $\pm 0.10\text{ mm}$ | Superficie plana niquelada con grabado oficial Xeon w9. |
| **Cavidades Centrales (x4)** | **4 Cavidades MLCC** | Exacto | Espacios para desacoplo de voltajes transitorios de los tiles. |
| **Indicador Pin 1** | **$2.20 \times 2.20\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Cantidad de Contactos LGA** | **4,677 Lands** | Exacto | Baño de oro duro de $30\,\mu\text{in}$ sobre níquel químico. |

---

## 3. FICHA TÉCNICA Y RENDIMIENTO

* **Núcleos e Hilos:** 56 Núcleos Golden Cove • 112 Hilos.
* **Frecuencias de Reloj:** Base 1.90 GHz | Turbo Boost Max 3.0 hasta 4.80 GHz (Desbloqueado para Overclocking).
* **Memoria Caché Intel:** 105 MB Intel Smart Cache (L3) + 112 MB L2 (Total: 217 MB).
* **Consumo Energético:** 350W Processor Base Power (PBP) | 420W Maximum Turbo Power (MTP).
* **Memoria:** 8 Canales DDR5-4800 ECC Registered con ancho de banda superior a 307 GB/s.
* **Conectividad:** 112 Líneas PCI Express 5.0 nativas.