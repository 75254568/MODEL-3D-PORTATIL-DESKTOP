package com.laptopexplorer.service;

import java.util.Map;

/**
 * Precios de referencia aproximados (USD, configuración típica de lanzamiento) de los modelos del catálogo
 * inicial. NO son precios verificados: se guardan marcados como estimados y se pueden corregir en el catálogo.
 * Clave: {@code marca|modelo} en minúsculas.
 */
final class AMJPrecioReferencia {

    private static final Map<String, Integer> PRECIOS = Map.ofEntries(
            Map.entry("lenovo|loq 15ahp10", 1100),
            Map.entry("asus|tuf gaming a18 (fa808u)", 1400),
            Map.entry("asus|rog strix scar 18 (2026) g835", 4300),
            Map.entry("alienware|18 area-51 (aa18250)", 3700),
            Map.entry("lenovo|legion pro 7i gen 10 (16)", 3300),
            Map.entry("hyperx|omen max 16", 2900),
            Map.entry("apple|macbook pro 16 (m5 max)", 4500),
            Map.entry("hp|zbook fury 16 g11", 4200),
            Map.entry("acer|predator helios 18 (ph18-71)", 2800),
            Map.entry("acer|predator helios 16 ai (ph16-73)", 3500),
            Map.entry("hp|victus 16 (16-r0xxx)", 1300),
            Map.entry("dell|g16 7630", 1700),
            Map.entry("gigabyte|aorus 17x (2024)", 3000),
            Map.entry("razer|blade 18 (2024)", 4300),
            Map.entry("alienware|m18 r2 (2024)", 3300),
            Map.entry("origin pc|eon18-x (2025)", 4800),
            Map.entry("asus|rog zephyrus g16 (gu605my)", 2700),
            Map.entry("apple|macbook pro 16 (m3 max)", 3700),
            Map.entry("lenovo|legion pro 7i gen 9", 3200),
            Map.entry("razer|blade 16 (2024)", 3200),
            Map.entry("dell|xps 16 (9640)", 2500),
            Map.entry("hp|omen transcend 16 (u1000)", 2000),
            Map.entry("apple|macbook air 15 (m3)", 1500),
            Map.entry("lenovo|thinkpad x1 carbon gen 12", 2000),
            Map.entry("microsoft|surface laptop 7 15", 1300),
            Map.entry("framework|laptop 16", 2100),
            Map.entry("asus|zenbook 14 oled (ux3405)", 1300),
            Map.entry("msi|titan 18 hx (a14v)", 5000),
            Map.entry("acer|predator helios 16 (ph16-72)", 2200),
            Map.entry("lg|gram 16 (16z90s)", 1700),
            Map.entry("alienware|m16 r2", 2300),
            Map.entry("asus|rog zephyrus g16 (2025) platinum white", 2500));

    private AMJPrecioReferencia() {
    }

    static Integer amjDe(String brand, String model) {
        return PRECIOS.get((brand + "|" + model).toLowerCase(java.util.Locale.ROOT));
    }
}
