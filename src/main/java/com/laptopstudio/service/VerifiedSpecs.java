package com.laptopstudio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopstudio.model.Laptop;
import com.laptopstudio.repository.LaptopRepository;
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
 * fabricante y analisis, con lo que no se pudo confirmar y las discrepancias entre fuentes.
 * Se aplican a la base una sola vez por laptop, y solo a los campos vacios o que siguen con el valor de la semilla:
 * lo que el usuario haya cambiado a mano no se toca.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerifiedSpecs {

    /** Marca en {@code seed_aplicado}; cambiar la version vuelve a aplicar las fichas a los campos no editados. */
    private static final String MARCA = "#ficha-v1|";
    private static final int MAX_NOTAS = 400, MAX_FUENTE = 300;

    /** Ficha de un laptop tal como se muestra en la aplicacion. */
    public record Ficha(String config, List<String> sources, List<String> notVerified, List<String> conflicts, JsonNode specs) {
    }

    private record Campo<T>(String key, String label, Function<Laptop, T> get, BiConsumer<Laptop, T> set, Function<JsonNode, T> read) {
        /** Copia el valor verificado si el campo esta vacio o conserva el de la semilla. Devuelve si lo cambio. */
        boolean aplicar(JsonNode specs, Laptop l, Laptop semilla) {
            JsonNode n = specs.get(key);
            if (n == null || n.isNull()) {
                return false;
            }
            T actual = get.apply(l), nuevo = read.apply(n);
            boolean libre = actual == null || actual instanceof String s && s.isBlank()
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

    private static final List<Campo<?>> CAMPOS = List.of(
            new Campo<>("cpu", "procesador", Laptop::getCpu, Laptop::setCpu, TEXTO),
            new Campo<>("cpuCores", "núcleos", Laptop::getCpuCores, Laptop::setCpuCores, ENTERO),
            new Campo<>("cpuBaseWatts", "potencia base del procesador", Laptop::getCpuBaseWatts, Laptop::setCpuBaseWatts, ENTERO),
            new Campo<>("cpuBoostWatts", "potencia máxima del procesador", Laptop::getCpuBoostWatts, Laptop::setCpuBoostWatts, ENTERO),
            new Campo<>("gpu", "gráficos", Laptop::getGpu, Laptop::setGpu, TEXTO),
            new Campo<>("gpuVramGb", "VRAM", Laptop::getGpuVramGb, Laptop::setGpuVramGb, ENTERO),
            new Campo<>("gpuTgpWatts", "TGP", Laptop::getGpuTgpWatts, Laptop::setGpuTgpWatts, ENTERO),
            new Campo<>("ramGb", "cantidad de RAM", Laptop::getRamGb, Laptop::setRamGb, ENTERO),
            new Campo<>("ramType", "tipo de RAM", Laptop::getRamType, Laptop::setRamType, TEXTO),
            new Campo<>("ramUpgradable", "RAM ampliable", Laptop::getRamUpgradable, Laptop::setRamUpgradable, JsonNode::asBoolean),
            new Campo<>("storageGb", "almacenamiento", Laptop::getStorageGb, Laptop::setStorageGb, ENTERO),
            new Campo<>("storageSlots", "ranuras M.2", Laptop::getStorageSlots, Laptop::setStorageSlots, ENTERO),
            new Campo<>("screenInches", "tamaño de pantalla", Laptop::getScreenInches, Laptop::setScreenInches, DECIMAL),
            new Campo<>("resolution", "resolución", Laptop::getResolution, Laptop::setResolution, TEXTO),
            new Campo<>("refreshRateHz", "frecuencia de pantalla", Laptop::getRefreshRateHz, Laptop::setRefreshRateHz, ENTERO),
            new Campo<>("panelType", "tipo de panel", Laptop::getPanelType, Laptop::setPanelType, TEXTO),
            new Campo<>("widthMm", "ancho", Laptop::getWidthMm, Laptop::setWidthMm, DECIMAL),
            new Campo<>("depthMm", "fondo", Laptop::getDepthMm, Laptop::setDepthMm, DECIMAL),
            new Campo<>("thicknessMm", "grosor", Laptop::getThicknessMm, Laptop::setThicknessMm, DECIMAL),
            new Campo<>("weightKg", "peso", Laptop::getWeightKg, Laptop::setWeightKg, DECIMAL),
            new Campo<>("batteryWh", "batería", Laptop::getBatteryWh, Laptop::setBatteryWh, DECIMAL),
            new Campo<>("releaseYear", "año", Laptop::getReleaseYear, Laptop::setReleaseYear, ENTERO),
            new Campo<>("chassisMaterial", "material del chasis", Laptop::getChassisMaterial, Laptop::setChassisMaterial, TEXTO));

    private static final Map<String, String> OTRAS_ETIQUETAS = Map.of("priceUsd", "precio", "colorHex", "color");
    private static volatile Map<String, Ficha> fichas;

    private final LaptopRepository repository;
    private final ObjectMapper mapper;

    private static String key(String brand, String model) {
        return (brand + "|" + model).toLowerCase(Locale.ROOT);
    }

    private static String etiqueta(String campo) {
        for (Campo<?> c : CAMPOS) {
            if (c.key().equals(campo)) {
                return c.label();
            }
        }
        return OTRAS_ETIQUETAS.getOrDefault(campo, campo);
    }

    private static List<String> textos(JsonNode array, Function<String, String> f) {
        List<String> out = new ArrayList<>();
        if (array != null) {
            array.forEach(n -> out.add(f.apply(n.asText())));
        }
        return out;
    }

    private static Map<String, Ficha> fichas() {
        Map<String, Ficha> map = fichas;
        if (map == null) {
            map = new LinkedHashMap<>();
            try {
                ObjectMapper json = new ObjectMapper();
                for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath*:specs/verified/*.json")) {
                    try (InputStream in = r.getInputStream()) {
                        for (JsonNode e : json.readTree(in)) {
                            map.put(key(e.path("brand").asText(), e.path("model").asText()), new Ficha(e.path("config").asText(""),
                                    textos(e.get("sources"), Function.identity()), textos(e.get("notVerified"), VerifiedSpecs::etiqueta),
                                    textos(e.get("conflicts"), Function.identity()), e.path("specs")));
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
    public static Ficha of(Laptop l) {
        return l == null ? null : fichas().get(key(l.getBrand(), l.getModel()));
    }

    /** Numero de fichas disponibles. */
    public static int count() {
        return fichas().size();
    }

    /**
     * Aplica las fichas aun no aplicadas. {@code aplicados} son las marcas ya guardadas y {@code marcar} guarda una nueva.
     */
    void aplicar(java.util.Set<String> aplicados, Consumer<String> marcar) throws IOException {
        Map<String, Laptop> semilla = new HashMap<>();
        for (String archivo : List.of("seed/laptops.json", "seed/laptops-2026.json")) {
            try (InputStream in = new ClassPathResource(archivo).getInputStream()) {
                for (Laptop l : mapper.readValue(in, Laptop[].class)) {
                    semilla.put(key(l.getBrand(), l.getModel()), l);
                }
            }
        }
        int laptops = 0, campos = 0;
        for (Map.Entry<String, Ficha> e : fichas().entrySet()) {
            String marca = MARCA + e.getKey();
            if (aplicados.contains(marca)) {
                continue;
            }
            Ficha f = e.getValue();
            Laptop base = semilla.get(e.getKey());
            String[] partes = e.getKey().split("\\|", 2);
            Laptop l = repository.findByBrandIgnoreCaseAndModelIgnoreCase(partes[0], partes[1]).orElse(null);
            if (l == null) {
                marcar.accept(marca);       // el usuario lo elimino: no se vuelve a crear
                continue;
            }
            try {
                int cambios = 0;
                for (Campo<?> c : CAMPOS) {
                    if (c.aplicar(f.specs(), l, base)) {
                        cambios++;
                    }
                }
                if (origenLibre(l, base)) {
                    f.sources().stream().filter(s -> s.length() <= MAX_FUENTE).findFirst().ifPresent(l::setSourceUrl);
                    l.setNotes(nota(f));
                    cambios++;
                }
                if (cambios > 0) {
                    repository.save(l);
                    laptops++;
                    campos += cambios;
                }
                marcar.accept(marca);
            } catch (RuntimeException ex) {
                log.warn("No se pudo aplicar la ficha verificada de {}: {}", l.getDisplayName(), ex.getMessage());
            }
        }
        if (laptops > 0) {
            log.info("Fichas verificadas aplicadas a {} laptops ({} campos)", laptops, campos);
        }
    }

    private static boolean origenLibre(Laptop l, Laptop base) {
        boolean sinTocar = base != null && Objects.equals(l.getNotes(), base.getNotes()) && Objects.equals(l.getSourceUrl(), base.getSourceUrl());
        return sinTocar || (l.getNotes() == null || l.getNotes().isBlank()) && (l.getSourceUrl() == null || l.getSourceUrl().isBlank());
    }

    private static String nota(Ficha f) {
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
