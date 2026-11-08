# Informe de investigación - reconstrucción interna por componentes (Fases A-D)

Estado: **solo investigación y auditoría. No se ha modificado ningún modelo 3D ni ningún archivo de las carpetas de laptops.**
Fecha: 2026-11-07. Versión instalada al redactarlo: 1.0.37.

## 1. Fuentes primarias que YA están en las carpetas (`Documents\mdelado 3d\<modelo>\`)

Son las mejores referencias del modelo exacto: incluyen vistas despiezadas, lista de FRU/CRU y procedimientos de desmontaje.

| Modelo | Documento en su carpeta | Pág. | Confianza |
|---|---|---|---|
| Lenovo Legion Pro 7i Gen 10 | `legion_pro_7_16_10_hmm.pdf` (Hardware Maintenance Manual) | 72 | Alta (modelo exacto) |
| ASUS ROG Strix SCAR 18 (G835) | `E27891_G815_G835L_G_SM_WEB_1.pdf` (Service Manual, jul 2026) + `ID27892 ... Upgrade_Guide` | 133 + 32 | Alta |
| ASUS TUF Gaming A18 (FA808U) | `040C_F27771_FA808U_SM_V2_A.pdf` + `0404_T27213_..._EM` | 116 + 66 | Alta |
| Acer Predator Helios 16 AI (PH16-73) | `4072be.pdf` (Lifecycle extension manual) | 96 | Alta |
| Acer Predator Helios 18 (PH18-71) | `Lifecycle Ext. Guide_Acer_1.0_A_A` | 48 | Alta |
| HyperX OMEN MAX 16 | `pdf_14526875_en-US-1.pdf` (Maintenance and Service Guide, modelo 16-ah1xxx) + User Guide | 104 + 83 | Alta, pero el modelo es 16-ah1xxx; el de la app puede ser 16-ah0000: confirmar |
| HP ZBook Fury 16 G11 | `pdf_10278697_en-US-1.pdf` (Maintenance and Service Guide) | 207 | Alta |
| HP Victus 16 (16-r0xxx) | `pdf_7660048_en-US-1.pdf` (Maintenance and Service Guide) | 91 | Alta |
| Lenovo LOQ 15AHP10 | `..._loq_15ahp10_15irx10_hmm_en_en.pdf` | 59 | Alta |
| Alienware 18 Area-51 (AA18250) | Owner's Manual rev. A12 | 150 | Media: ubica componentes, pero no es manual de servicio con despiece completo |
| Alienware m18 R2 | Owner's Manual (es) | 135 | Media |
| Dell G16 7630 | Owner's Manual rev. A02 | 99 | Media |
| ORIGIN PC EON18-X | `manual-service-schenker-key-18-pro...` (Clevo X580WNx) | 46 | **Por confirmar**: hay que verificar que el EON18-X use el chasis Clevo X580WN |
| MacBook Pro 16 M5 | imágenes (`125838_exploded_view.png` y otras), sin PDF | - | Media |
| ASUS ROG Zephyrus G16 (2025) | `GU603Z ... Service_guide` | 38 | **Baja / incorrecta**: GU603Z es la generación de 2022, no la de 2025 |
| AORUS 17X (2024) | solo imágenes y .txt | - | Sin manual: hay que investigar |
| Razer Blade 18 (2024) | solo imágenes y .txt | - | Sin manual: hay que investigar |
| MSI Raider 18 HX AI | solo un .txt (no hay modelo 3D en la app) | - | - |

Los ~14 modelos restantes de la app (XPS 16, Surface 7, Zenbook 14, X1 Carbon, Titan 18, Helios 16, m16 R2, gram 16, Framework 16, MacBook Air 15, MacBook Pro M3, Blade 16, OMEN Transcend 16, Legion Gen 9, Zephyrus 2024) **no tienen carpeta** en `mdelado 3d`. Para ellos solo hay lo recopilado de Internet.

## 2. Fuentes externas ya localizadas (de la sesión anterior)

- LaptopMedia, guías "How to open ...": Legion Pro 7i Gen 10 <https://laptopmedia.com/guides/how-to-open-lenovo-legion-pro-7i-gen-10-16%e2%80%b3-intel-disassembly-and-upgrade-options/>, SCAR 18 G835, OMEN MAX 16 (Intel 16-ah0000), ZBook Fury 16 G10, Helios 18, Helios 16 AI, Helios 16 PH16-72, Victus 16, Dell G16 7630, Alienware m18 R2 y m16 R2, Zephyrus G16 GU605, XPS 16 9640, Zenbook 14 UX3405, MSI Titan 18 HX A2XW, OMEN Transcend 16, ThinkPad X1 Carbon Gen 11.
- iFixit (guías de reparación): MacBook Pro 16" 2024 (M4), MacBook Air 15" 2024, Framework Laptop 16", ThinkPad X1 Carbon Gen 12.
- Microsoft Learn, guía de servicio del Surface Laptop 7.

Observaciones: estas fuentes dan **vistas cenitales completas** (fotografías) pero **no dimensiones ni números de parte** de cada pieza. Los números de parte y medidas hay que sacarlos de los manuales de servicio (sección 1) o de tiendas de repuestos (aún sin investigar).

## 3. Componentes por identificar en cada modelo (lo que pide la metodología)

Ventilador izquierdo/derecho (y ventilador central cuando exista), módulo(s) térmico(s) y heat pipes, placa base, CPU/GPU/VRAM, RAM (SO-DIMM o soldada), SSD M.2 (y su disipador), batería, tarjeta Wi-Fi, altavoces, flex y conectores, placa de E/S, bisagras.
Ejemplo ya verificado con el manual del Legion Pro 7i Gen 10 (Tabla 4, pág. 21): 28 categorías de FRU, entre ellas *Thermal module 1* y *Thermal module 2* (dos módulos térmicos), *System board*, *I/O board*, *PMIC board*, *Wi-Fi card*, *SSD + standoff*, *Speakers*, *Antenna*, *Battery pack*, *Battery sponge (modelos de 80 W)*, *Memory module*, *DC-in bracket*. El PDF trae una vista despiezada (pág. 20) y 168 imágenes.

**Números de parte y dimensiones**: NO CONFIRMADOS todavía en ningún modelo. El manual del Legion nombra las categorías de FRU, pero los códigos de parte hay que cruzarlos con las páginas de repuestos de cada fabricante; no los he extraído todavía.

## 4. Errores detectados en el modelo actual (auditoría honesta)

1. **Ventiladores desencajados en el despiece.** En los 20 modelos con foto de interior, el ventilador es un disco con la imagen de la foto, colocado en la capa 3, mientras la placa con su foto está en la capa 2. Al separar las capas, los ventiladores flotan por encima de la placa en lugar de seguir unidos a ella. Es un defecto de diseño de mi implementación, no un problema de las fotos.
2. **Interior "prestado" de otro equipo (versión 1.0.37).** Area-51, AORUS 17X, Blade 18, Blade 16, EON18-X, gram 16 y Framework 16 muestran el interior real de **otro** portátil. Esto **contradice** la regla de no usar componentes de otras laptops. Debe revertirse o marcarse como NO CONFIRMADO.
3. **La placa es una textura plana.** Las piezas (RAM, SSD, disipador, CPU) no tienen volumen propio; son solo zonas transparentes colocadas a ojo (posible error de varios milímetros). No hay puntos de fijación ni conectores modelados.
4. **Interiores procedurales antiguos** (LOQ, TUF, SCAR, Area-51, ZBook, Helios, Victus, G16, Blade 18, m18, EON18, MacBook Pro…): piezas con dimensiones estimadas, no verificadas contra manuales. No se ha medido nada de esto de forma automática.
5. **Sin verificación numérica del ensamblaje.** Todavía no existe una comprobación automática de intersecciones, piezas flotantes ni de ventiladores alineados con su conducto. Hasta ahora solo he verificado con capturas.
6. **Fotos de modelos hermanos** (Zephyrus 2025 con la foto de la 2024, X1 Carbon Gen 12 con la de la Gen 11, Titan 18 con la del A2XW): coincidencia de chasis probable pero no confirmada.

## 5. Componentes sin información suficiente (NO CONFIRMADO)

Todos los números de parte; las dimensiones de ventiladores, baterías, SSD y disipadores en los modelos sin manual de servicio (AORUS 17X, Razer Blade 18, EON18-X, gram 16, Framework 16, MacBook Pro/Air, Surface 7); el chasis exacto del EON18-X; la distribución real del Zephyrus G16 2025.

## 6. Plan de reconstrucción propuesto (sin ejecutar)

1. **Piloto: Lenovo Legion Pro 7i Gen 10.** Es el que mejor documentado está (manual HMM del modelo exacto + guía LaptopMedia del mismo modelo + dimensiones en la ficha). Resultado esperado: carpeta `Referencias/` con subcarpetas por componente, tabla de piezas (posición X/Y/Z, rotación, dimensiones, fuente, confianza) y modelo 3D por piezas.
2. **Herramienta de verificación** antes de modelar: sistema de coordenadas común, comprobación de cajas que se solapan, piezas sin apoyo, y ventiladores alineados con su conducto, en modo ensamblado y en despiece.
3. **Arreglar el despiece:** el ventilador, el módulo térmico y la placa deben moverse juntos como una sola capa.
4. **Segunda ola** (manual de servicio exacto disponible): SCAR 18, TUF A18, Helios 16 AI, Helios 18, OMEN MAX 16, ZBook Fury, Victus 16, LOQ.
5. **Tercera ola** (manual de propietario o solo fotos): Area-51, m18 R2, G16, MacBook Pro M5, EON18-X (tras confirmar el chasis Clevo).
6. **Modelos sin carpeta:** investigar en Internet con el mismo método y marcar lo no encontrado como NO CONFIRMADO.
7. **Sustituir los interiores prestados** (sección 4, punto 2) por NO CONFIRMADO o por una reconstrucción verificada.

Las referencias se organizarán dentro de la carpeta existente de cada laptop (`Referencias/Ventiladores`, `Refrigeracion`, `Motherboard`, `Bateria`, `RAM`, `SSD`, `Otros`), sin tocar ni mover los archivos originales.
