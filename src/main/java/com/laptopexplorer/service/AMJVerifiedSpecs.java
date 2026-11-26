package com.laptopexplorer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopexplorer.model.AMJLaptop;
import com.laptopexplorer.repository.AMJLaptopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fichas verificadas de los laptops ({@code specs/verified/*.json}): especificaciones comprobadas en fuentes del
 * fabricante y análisis, con lo que no se pudo confirmar y las discrepancias entre fuentes.
 * Se aplican a la base una sola vez por laptop, y solo a los campos vacíos o que siguen con el valor de la semilla:
 * lo que el usuario haya cambiado a mano no se toca.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AMJVerifiedSpecs {

    /** Marca en {@code seed_aplicado}; cambiar la versión vuelve a aplicar las fichas a los campos no editados. */
    private static final String MARCA = "#ficha-v1|";
    private static final int MAX_NOTAS = 400, MAX_FUENTE = 300;

    /** Ficha de un laptop tal como se muestra en la aplicación. */
    public record AMJFicha(String config, List<String> sources, List<String> notVerified, List<String> conflicts, JsonNode specs) {
    }

    private record AMJCampo<T>(String key, String label, Function<AMJLaptop, T> get, BiConsumer<AMJLaptop, T> set, Function<JsonNode, T> read) {
        /** Copia el valor verificado si el campo está vacío o conserva el de la semilla. Devuelve si lo cambió. */
        boolean amjAplicar(JsonNode specs, AMJLaptop l, AMJLaptop semilla) {
            JsonNode n = specs.get(key);
            if (n == null || n.isNull()) {
                return false;
            }
            T actual = get.apply(l), nuevo = read.apply(n);
            // un número en 0 cuenta como dato vacío: medidas, peso y batería en 0 no son válidos
            boolean libre = actual == null || actual instanceof String s && s.isBlank()
                    || actual instanceof Number num && num.doubleValue() == 0
                    || semilla != null && Objects.equals(actual, get.apply(semilla));
            if (!libre || Objects.equals(actual, nuevo)) {
                return false;
            }
            set.accept(l, nuevo);
            return true;
        }
    }

    private static final Function<JsonNode, String> TEXTO = JsonNode::asText;
    private static final Function<JsonNode, Integer> ENTERO = JsonNode::asInt;
    private static final Function<JsonNode, Double> DECIMAL = JsonNode::asDouble;

    private static final List<AMJCampo<?>> CAMPOS = List.of(
            new AMJCampo<>("cpu", "procesador", AMJLaptop::getCpu, AMJLaptop::setCpu, TEXTO),
            new AMJCampo<>("cpuCores", "núcleos", AMJLaptop::getCpuCores, AMJLaptop::setCpuCores, ENTERO),
            new AMJCampo<>("cpuBaseWatts", "potencia base del procesador", AMJLaptop::getCpuBaseWatts, AMJLaptop::setCpuBaseWatts, ENTERO),
            new AMJCampo<>("cpuBoostWatts", "potencia máxima del procesador", AMJLaptop::getCpuBoostWatts, AMJLaptop::setCpuBoostWatts, ENTERO),
            new AMJCampo<>("gpu", "gráficos", AMJLaptop::getGpu, AMJLaptop::setGpu, TEXTO),
            new AMJCampo<>("gpuVramGb", "VRAM", AMJLaptop::getGpuVramGb, AMJLaptop::setGpuVramGb, ENTERO),
            new AMJCampo<>("gpuTgpWatts", "TGP", AMJLaptop::getGpuTgpWatts, AMJLaptop::setGpuTgpWatts, ENTERO),
            new AMJCampo<>("ramGb", "cantidad de RAM", AMJLaptop::getRamGb, AMJLaptop::setRamGb, ENTERO),
            new AMJCampo<>("ramType", "tipo de RAM", AMJLaptop::getRamType, AMJLaptop::setRamType, TEXTO),
            new AMJCampo<>("ramUpgradable", "RAM ampliable", AMJLaptop::getRamUpgradable, AMJLaptop::setRamUpgradable, JsonNode::asBoolean),
            new AMJCampo<>("storageGb", "almacenamiento", AMJLaptop::getStorageGb, AMJLaptop::setStorageGb, ENTERO),
            new AMJCampo<>("storageSlots", "ranuras M.2", AMJLaptop::getStorageSlots, AMJLaptop::setStorageSlots, ENTERO),
            new AMJCampo<>("screenInches", "tamaño de pantalla", AMJLaptop::getScreenInches, AMJLaptop::setScreenInches, DECIMAL),
            new AMJCampo<>("resolution", "resolución", AMJLaptop::getResolution, AMJLaptop::setResolution, TEXTO),
            new AMJCampo<>("refreshRateHz", "frecuencia de pantalla", AMJLaptop::getRefreshRateHz, AMJLaptop::setRefreshRateHz, ENTERO),
            new AMJCampo<>("panelType", "tipo de panel", AMJLaptop::getPanelType, AMJLaptop::setPanelType, TEXTO),
            new AMJCampo<>("widthMm", "ancho", AMJLaptop::getWidthMm, AMJLaptop::setWidthMm, DECIMAL),
            new AMJCampo<>("depthMm", "fondo", AMJLaptop::getDepthMm, AMJLaptop::setDepthMm, DECIMAL),
            new AMJCampo<>("thicknessMm", "grosor", AMJLaptop::getThicknessMm, AMJLaptop::setThicknessMm, DECIMAL),
            new AMJCampo<>("weightKg", "peso", AMJLaptop::getWeightKg, AMJLaptop::setWeightKg, DECIMAL),
            new AMJCampo<>("batteryWh", "batería", AMJLaptop::getBatteryWh, AMJLaptop::setBatteryWh, DECIMAL),
            new AMJCampo<>("releaseYear", "año", AMJLaptop::getReleaseYear, AMJLaptop::setReleaseYear, ENTERO),
            new AMJCampo<>("chassisMaterial", "material del chasis", AMJLaptop::getChassisMaterial, AMJLaptop::setChassisMaterial, TEXTO));

    private static final Map<String, String> OTRAS_ETIQUETAS = Map.of("priceUsd", "precio", "colorHex", "color");
    private static volatile Map<String, AMJFicha> fichas;

    private final AMJLaptopRepository repository;
    private final ObjectMapper mapper;

    private static String amjKey(String brand, String model) {
        return (brand + "|" + model).toLowerCase(Locale.ROOT);
    }

    private static String amjEtiqueta(String campo) {
        for (AMJCampo<?> c : CAMPOS) {
            if (c.key().equals(campo)) {
                return c.label();
            }
        }
        return OTRAS_ETIQUETAS.getOrDefault(campo, campo);
    }

    private static List<String> amjTextos(JsonNode array, Function<String, String> f) {
        List<String> out = new ArrayList<>();
        if (array != null) {
            array.forEach(n -> out.add(f.apply(n.asText())));
        }
        return out;
    }

    private static Map<String, AMJFicha> amjFichas() {
        Map<String, AMJFicha> map = fichas;
        if (map == null) {
            map = new LinkedHashMap<>();
            try {
                ObjectMapper json = new ObjectMapper();
                for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath*:specs/verified/*.json")) {
                    try (InputStream in = r.getInputStream()) {
                        for (JsonNode e : json.readTree(in)) {
                            map.put(amjKey(e.path("brand").asText(), e.path("model").asText()), new AMJFicha(e.path("config").asText(""),
                                    amjTextos(e.get("sources"), Function.identity()), amjTextos(e.get("notVerified"), AMJVerifiedSpecs::amjEtiqueta),
                                    amjTextos(e.get("conflicts"), Function.identity()), e.path("specs")));
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                log.warn("No se pudieron leer las fichas verificadas: {}", e.toString());
            }
            fichas = map;
        }
        return map;
    }

    /** Ficha verificada del laptop, o null si no tiene (por ejemplo, un equipo añadido por el usuario). */
    public static AMJFicha amjOf(AMJLaptop l) {
        return l == null ? null : amjFichas().get(amjKey(l.getBrand(), l.getModel()));
    }

    /** Número de fichas disponibles. */
    public static int amjCount() {
        return amjFichas().size();
    }

    /**
     * Aplica las fichas aun no aplicadas. {@code aplicados} son las marcas ya guardadas y {@code marcar} guarda una nueva.
     */
    void amjAplicar(java.util.Set<String> aplicados, Consumer<String> marcar) throws IOException {
        Map<String, AMJLaptop> semilla = new HashMap<>();
        for (String archivo : List.of("seed/laptops.json", "seed/laptops-2026.json")) {
            try (InputStream in = new ClassPathResource(archivo).getInputStream()) {
                for (AMJLaptop l : mapper.readValue(in, AMJLaptop[].class)) {
                    semilla.put(amjKey(l.getBrand(), l.getModel()), l);
                }
            }
        }
        int laptops = 0, campos = 0;
        for (Map.Entry<String, AMJFicha> e : amjFichas().entrySet()) {
            String marca = MARCA + e.getKey();
            if (aplicados.contains(marca)) {
                continue;
            }
            AMJFicha f = e.getValue();
            AMJLaptop base = semilla.get(e.getKey());
            String[] partes = e.getKey().split("\\|", 2);
            AMJLaptop l = repository.findByBrandIgnoreCaseAndModelIgnoreCase(partes[0], partes[1]).orElse(null);
            if (l == null) {
                marcar.accept(marca);       // el usuario lo elimino: no se vuelve a crear
                continue;
            }
            try {
                int cambios = 0;
                for (AMJCampo<?> c : CAMPOS) {
                    if (c.amjAplicar(f.specs(), l, base)) {
                        cambios++;
                    }
                }
                if (amjOrigenLibre(l, base)) {
                    f.sources().stream().filter(s -> s.length() <= MAX_FUENTE).findFirst().ifPresent(l::setSourceUrl);
                    l.setNotes(amjNota(f));
                    cambios++;
                }
                if (cambios > 0) {
                    repository.save(l);
                    laptops++;
                    campos += cambios;
                }
                marcar.accept(marca);
            } catch (RuntimeException ex) {
                log.warn("No se pudo aplicar la ficha verificada de {}: {}", l.amjGetDisplayName(),
                        org.springframework.core.NestedExceptionUtils.getMostSpecificCause(ex).toString());
            }
        }
        if (laptops > 0) {
            log.info("Fichas verificadas aplicadas a {} laptops ({} campos)", laptops, campos);
        }
    }

    private static boolean amjOrigenLibre(AMJLaptop l, AMJLaptop base) {
        boolean sinTocar = base != null && Objects.equals(l.getNotes(), base.getNotes()) && Objects.equals(l.getSourceUrl(), base.getSourceUrl());
        return sinTocar || (l.getNotes() == null || l.getNotes().isBlank()) && (l.getSourceUrl() == null || l.getSourceUrl().isBlank());
    }

    private static String amjNota(AMJFicha f) {
        StringBuilder sb = new StringBuilder("Ficha verificada con ").append(f.sources().size())
                .append(f.sources().size() == 1 ? " fuente." : " fuentes.");
        if (!f.notVerified().isEmpty()) {
            sb.append(" Sin confirmar: ").append(String.join(", ", f.notVerified())).append('.');
        }
        if (!f.conflicts().isEmpty()) {
            sb.append(" Discrepancias: ").append(String.join(" ", f.conflicts()));
        }
        return sb.length() <= MAX_NOTAS ? sb.toString() : sb.substring(0, MAX_NOTAS - 1) + "…";
    }
}
