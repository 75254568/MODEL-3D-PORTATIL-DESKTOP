# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR INTEL CORE ULTRA 7 258V (LUNAR LAKE MoP)
### Zócalo Mobile BGA MoP • 32GB Memoria LPDDR5X On-Package • Foveros 3D • Gráficos Arc 140V Xe2

---

## 1. DESCRIPCIÓN GENERAL Y LA REVOLUCIÓN DEL ENCAPSULADO MoP

El **Intel Core Ultra 7 258V** ("Lunar Lake") introduce el mayor salto de diseño y eficiencia en la historia de la computación móvil x86, implementando una arquitectura de **Memoria en el Encapsulado (MoP - Memory on Package)** con 32 GB de memoria LPDDR5X-8533 soldada directamente junto a los tiles de silicio de cómputo.

### Innovaciones Clave de Ingeniería:
1. **Memoria en el Encapsulado (MoP - Memory on Package):**
   - El 258V incorpora dos chips de memoria LPDDR5X-8533 de 16 GB cada uno (32 GB total) montados lado a lado sobre el mismo sustrato orgánico que el procesador.
   - **Beneficios revolucionarios:**
     - Elimina pistas de placa madre de hasta 100 mm, reduciéndolas a milímetros dentro del paquete, lo que disminuye el consumo energético del bus en un 40%.
     - Libera hasta 250 mm² de espacio físico en la placa madre de la notebook, permitiendo baterías significativamente más grandes y chasis de menos de 10 mm de grosor.
2. **Empaquetado 3D Foveros con Litografías TSMC:**
   - **Compute Tile (TSMC N3B 3nm):** Núcleos P Lion Cove y núcleos E Skymont de ultra-bajo consumo energético, acompañados de la GPU Intel Arc 140V de arquitectura Xe2 Battlemage.
   - **Platform Controller Tile (TSMC N6):** Gestión de seguridad, conectividad Wi-Fi 7, Thunderbolt 4 y PCIe 5.0.
   - **Base Tile Pasivo (Intel 22nm):** Matriz de interconexión 3D micro-bump para comunicación de bajísima latencia.
3. **El Encapsulado x86 Más Delgado del Mundo:** Con tan solo **1.15 mm de espesor total**, permite un acoplamiento térmico plano con disipadores ultra-slim sin añadir volumen al equipo.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (LUNAR LAKE MoP)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **27.50 mm** | $\pm 0.15\text{ mm}$ | Formato cuadrado ultracompacto con memoria integrada. |
| **Largo del Sustrato (Y)** | **27.00 mm** | $\pm 0.15\text{ mm}$ | Área reducida en un 60% respecto a combinaciones previas con RAM. |
| **Espesor del Sustrato PCB** | **0.60 mm** | $\pm 0.05\text{ mm}$ | Sustrato ultrafino azul profundo de micro-vías ciegas. |
| **Espesor Total Paquete (Z)** | **1.15 mm** | $\pm 0.06\text{ mm}$ | Perfil más delgado del mercado para laptops de 10 mm. |
| **Tile de Cómputo CPU/GPU** | **$15.00 \times 11.50\text{ mm}$** | $\pm 0.04\text{ mm}$ | Silicio Foveros TSMC 3nm con acabado reflectivo espejo. |
| **Chips de Memoria MoP (x2)**| **$6.20 \times 9.80\text{ mm}$** | $\pm 0.05\text{ mm}$ | 2x Encapsulados epoxi negros con 16 GB LPDDR5X-8533 cada uno. |
| **Indicador Pin 1** | **$1.70 \times 1.70\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Matriz de Soldadura BGA** | **Micro-BGA SAC305** | Exacto | Alta densidad de esferas de soldadura en cara posterior. |

---

## 3. FICHA TÉCNICA Y RENDIMIENTO

* **Núcleos e Hilos:** 8 Núcleos (4 Lion Cove P-Cores + 4 Skymont LP-E Cores) • 8 Hilos (Sin Hyper-Threading para máxima eficiencia energética por watt).
* **Frecuencias:** P-Core Boost hasta 4.80 GHz | LP-E Core Boost hasta 3.70 GHz.
* **Memoria Integrada:** **32 GB LPDDR5X-8533 Dual Channel (MoP)** con ancho de banda de 136 GB/s.
* **Potencia Térmica (TDP):** 17W Base (Configurable entre 8W en modo ahorro y 37W en turbo máximo PL2).
* **NPU de IA Integrada:** Intel NPU 4 con **47 TOPS** dedicados para Copilot+.
* **Gráficos Integrados:** Intel Arc 140V (8 núcleos Xe2 Battlemage a 1.95 GHz, 64 motores XMX).