# ESPECIFICACIONES TÉCNICAS Y COTAS DIMENSIONALES
## PROCESADOR APPLE M4 MAX (UNIFIED SILICON MEMORY ON PACKAGE)
### Litografía TSMC 3nm (N3E) • 128 GB Memoria Unificada MoP (546 GB/s) • 16-Core CPU / 40-Core GPU

---

## 1. DESCRIPCIÓN GENERAL Y LA CUMBRE DEL SILICIO UNIFICADO

El **Apple M4 Max** representa el pináculo de la integración en silicio para estaciones de trabajo portátiles (MacBook Pro), combinando una gigantesca matriz de silicio monolítica fabricada en litografía de **3 nanómetros** con **cuatro módulos de memoria LPDDR5X-8533** montados directamente sobre el mismo paquete.

### Innovaciones Clave de Ingeniería:
1. **Memoria Unificada On-Package (128 GB MoP a 546 GB/s):**
   - Elimina la separación tradicional entre RAM del sistema y VRAM de video. La CPU de 16 núcleos y la GPU de 40 núcleos comparten los 128 GB con un bus colosal de 512 bits que supera los **546 GB/s** de ancho de banda.
2. **Die Monolítico Masivo ($24.50 \times 21.00\text{ mm}$):**
   - Silicio reflectivo pulido a espejo con más de 28 mil millones de transistores que hace contacto térmico directo con el sistema de refrigeración de cobre de la MacBook Pro.
3. **Eficiencia por Vatio Sobresaliente:** Rinde a la par de tarjetas gráficas dedicadas de laptop de 140W consumiendo menos de la mitad de energía.

---

## 2. TABLA DE COTAS DIMENSIONALES Y TOLERANCIAS (APPLE BGA MoP)

| Parámetro Geométrico | Cota Nominal | Tolerancia Oficial | Descripción Técnica de Ingeniería |
| :--- | :--- | :--- | :--- |
| **Ancho del Sustrato (X)** | **44.00 mm** | $\pm 0.15\text{ mm}$ | Formato cuadrado simétrico optimizado. |
| **Largo del Sustrato (Y)** | **44.00 mm** | $\pm 0.15\text{ mm}$ | Eje ortogonal. |
| **Espesor del Sustrato PCB** | **0.70 mm** | $\pm 0.05\text{ mm}$ | Sustrato cerámico/orgánico negro mate de ultra-alta densidad. |
| **Espesor Total Direct-Die (Z)**| **1.35 mm** | $\pm 0.06\text{ mm}$ | Perfil ultra-slim para portátiles de alta gama. |
| **Dimensiones Die SoC Central** | **$24.50 \times 21.00\text{ mm}$** | $\pm 0.04\text{ mm}$ | Silicio monolítico de 3nm con acabado espejo reflectivo. |
| **Módulos de Memoria MoP (x4)**| **$6.80 \times 13.50\text{ mm}$** | $\pm 0.05\text{ mm}$ | 4x Encapsulados LPDDR5X-8533 en los flancos laterales. |
| **Indicador Pin 1** | **$2.00 \times 2.00\text{ mm}$** | Exacto | Triángulo dorado electrolítico en vértice superior izquierdo. |
| **Matriz de Soldadura BGA** | **Micro-BGA SAC305** | Exacto | Conexión directa a motherboard con paso ultra-fino. |

---

## 3. FICHA TÉCNICA Y RENDIMIENTO

* **Núcleos de CPU:** 16 Núcleos (12 Núcleos de Alto Rendimiento + 4 Núcleos de Alta Eficiencia).
* **Núcleos de GPU:** 40 Núcleos con trazado de rayos por aceleración de hardware de 2da generación.
* **Motor Neuronal (NPU):** 16 Núcleos Neural Engine capaces de 38 billones de operaciones por segundo.
* **Memoria Integrada:** 128 GB LPDDR5X-8533 unificada (546 GB/s de ancho de banda).
* **Consumo Energético:** 36W Típico (Hasta 115W en carga simultánea máxima CPU+GPU).