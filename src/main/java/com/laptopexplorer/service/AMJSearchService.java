package com.laptopexplorer.service;

import com.laptopexplorer.model.AMJLaptop;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Búsqueda por necesidades: filtra el catálogo con los criterios del usuario y, además, propone alternativas que
 * casi los cumplen y laptops parecidos a uno dado. Trabaja en memoria sobre el catálogo (decenas de registros).
 * Un criterio sin valor (null o vacío) no filtra. Un laptop sin dato en un campo filtrado no cumple ese criterio.
 */
@Service
@RequiredArgsConstructor
public class AMJSearchService {

    private final AMJLaptopService laptops;

    /** Criterios de búsqueda. Todos opcionales. */
    public record AMJCriteria(String text, Integer minPrice, Integer maxPrice, String brand, String cpu, Integer minRamGb,
                           Integer minStorageGb, String gpu, Double minScreen, Double maxScreen, Double maxWeightKg,
                           String category) {

        public static AMJCriteria amjEmpty() {
            return new AMJCriteria(null, null, null, null, null, null, null, null, null, null, null, null);
        }
    }

    /**
     * Motivo por el que un laptop no cumple un criterio, o rasgo en el que se parece a otro. Si {@code usd} no es
     * nulo, la nota es un importe (siempre en USD, como se guardan los precios) seguido de {@code text}: lo escribe
     * la interfaz en la moneda que el usuario tenga elegida.
     */
    public record AMJNote(String text, Integer usd) {

        static AMJNote amjOf(String text) {
            return new AMJNote(text, null);
        }
    }

    /** Laptop con su puntaje y la lista de criterios que no cumple o por cuanto se queda corto. */
    public record AMJHit(AMJLaptop laptop, double score, List<AMJNote> notes) {
    }

    public record AMJResult(List<AMJHit> matches, List<AMJHit> alternatives) {
    }

    public List<String> amjMarcas() {
        TreeSet<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        laptops.amjTodos().forEach(l -> s.add(l.getBrand()));
        return new ArrayList<>(s);
    }

    public List<String> amjCategorias() {
        TreeSet<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        laptops.amjTodos().forEach(l -> s.add(l.getCategory()));
        return new ArrayList<>(s);
    }

    /** Campos del buscador que ofrecen sugerencias. */
    public enum AMJCampo { TEXTO, CPU, GPU, RAM, ALMACENAMIENTO, PANTALLA_MIN, PANTALLA_MAX }

    private static final java.util.regex.Pattern CPU_FAMILIA = java.util.regex.Pattern.compile(
            "(Intel Core Ultra \\d|Intel Core i\\d|AMD Ryzen \\d|Apple M\\d|Snapdragon X)", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern GPU_FAMILIA = java.util.regex.Pattern.compile(
            "(RTX \\d{4}(?: Ti)?|RTX \\d{4} Ada|Radeon RX \\d{4}\\w*|Arc|Adreno)", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Sugerencias para un campo, calculadas sobre los laptops que cumplen el RESTO de los criterios: así las
     * opciones se van acotando según lo que el usuario ya eligio. Para procesador y gráficos incluye primero
     * las familias ("Intel Core Ultra 9", "RTX 5080") y después los nombres completos.
     */
    public List<String> amjSugerencias(AMJCampo campo, AMJCriteria c) {
        AMJCriteria resto = switch (campo) {
            case TEXTO -> new AMJCriteria(null, c.minPrice(), c.maxPrice(), c.brand(), c.cpu(), c.minRamGb(), c.minStorageGb(), c.gpu(), c.minScreen(), c.maxScreen(), c.maxWeightKg(), c.category());
            case CPU -> new AMJCriteria(c.text(), c.minPrice(), c.maxPrice(), c.brand(), null, c.minRamGb(), c.minStorageGb(), c.gpu(), c.minScreen(), c.maxScreen(), c.maxWeightKg(), c.category());
            case GPU -> new AMJCriteria(c.text(), c.minPrice(), c.maxPrice(), c.brand(), c.cpu(), c.minRamGb(), c.minStorageGb(), null, c.minScreen(), c.maxScreen(), c.maxWeightKg(), c.category());
            case RAM -> new AMJCriteria(c.text(), c.minPrice(), c.maxPrice(), c.brand(), c.cpu(), null, c.minStorageGb(), c.gpu(), c.minScreen(), c.maxScreen(), c.maxWeightKg(), c.category());
            case ALMACENAMIENTO -> new AMJCriteria(c.text(), c.minPrice(), c.maxPrice(), c.brand(), c.cpu(), c.minRamGb(), null, c.gpu(), c.minScreen(), c.maxScreen(), c.maxWeightKg(), c.category());
            case PANTALLA_MIN, PANTALLA_MAX -> new AMJCriteria(c.text(), c.minPrice(), c.maxPrice(), c.brand(), c.cpu(), c.minRamGb(), c.minStorageGb(), c.gpu(), null, null, c.maxWeightKg(), c.category());
        };
        java.util.LinkedHashSet<String> familias = new java.util.LinkedHashSet<>();
        TreeSet<String> textos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<Double> numeros = new TreeSet<>();
        for (AMJLaptop l : laptops.amjTodos()) {
            if (amjEvaluate(l, resto, new ArrayList<>()) > 0) {
                continue;
            }
            switch (campo) {
                case TEXTO -> {
                    textos.add(l.getModel());
                    familias.add(l.getBrand());
                }
                case CPU -> {
                    amjFamilia(CPU_FAMILIA, l.getCpu(), familias);
                    textos.add(l.getCpu());
                }
                case GPU -> {
                    amjFamilia(GPU_FAMILIA, l.getGpu(), familias);
                    textos.add(l.getGpu());
                }
                case RAM -> amjAdd(numeros, l.getRamGb());
                case ALMACENAMIENTO -> amjAdd(numeros, l.getStorageGb());
                case PANTALLA_MIN, PANTALLA_MAX -> amjAdd(numeros, l.getScreenInches());
            }
        }
        List<String> out = new ArrayList<>(new TreeSet<>(familias));
        out.addAll(textos);
        for (Double n : numeros) {
            out.add(n == Math.rint(n) ? String.valueOf(n.longValue()) : String.valueOf(n));
        }
        return out;
    }

    private static void amjFamilia(java.util.regex.Pattern p, String valor, java.util.Set<String> into) {
        java.util.regex.Matcher m = p.matcher(valor == null ? "" : valor);
        while (m.find()) {
            into.add(m.group(1));
        }
    }

    private static void amjAdd(TreeSet<Double> into, Number n) {
        if (n != null && n.doubleValue() > 0) {
            into.add(n.doubleValue());
        }
    }

    /** Coincidencias exactas (de menor a mayor precio) y, si hay pocas, hasta 6 alternativas cercanas. */
    public AMJResult amjBuscar(AMJCriteria c) {
        List<AMJHit> matches = new ArrayList<>();
        List<AMJHit> near = new ArrayList<>();
        for (AMJLaptop l : laptops.amjTodos()) {
            List<AMJNote> fails = new ArrayList<>();
            double gap = amjEvaluate(l, c, fails);
            if (fails.isEmpty()) {
                matches.add(new AMJHit(l, 0, List.of()));
            } else {
                near.add(new AMJHit(l, gap, fails));
            }
        }
        matches.sort(Comparator.comparing((AMJHit h) -> h.laptop().getPriceUsd() == null)
                .thenComparing(h -> h.laptop().getPriceUsd() == null ? 0 : h.laptop().getPriceUsd()));
        near.sort(Comparator.comparingDouble(AMJHit::score));
        // Una alternativa solo tiene sentido si falla pocos criterios y por poco
        List<AMJHit> alt = near.stream().filter(h -> h.notes().size() <= 2 && h.score() <= 0.35).limit(6).toList();
        return new AMJResult(matches, alt);
    }

    /**
     * Mide cuanto se aleja el laptop de los criterios. Devuelve una distancia normalizada (0 = cumple todo) y
     * anota en {@code fails} cada criterio incumplido, con texto para el usuario.
     */
    private double amjEvaluate(AMJLaptop l, AMJCriteria c, List<AMJNote> fails) {
        double gap = 0;
        if (amjHas(c.brand()) && !amjNorm(l.getBrand()).equals(amjNorm(c.brand()))) {
            fails.add(AMJNote.amjOf("marca distinta (" + l.getBrand() + ")"));
            gap += 1;
        }
        if (amjHas(c.category()) && !amjNorm(l.getCategory()).equals(amjNorm(c.category()))) {
            fails.add(AMJNote.amjOf("categoría " + l.getCategory()));
            gap += 0.3;
        }
        if (amjHas(c.text()) && !amjNorm(l.amjGetDisplayName() + " " + l.getCpu() + " " + l.getGpu() + " " + l.getCategory() + " "
                + (l.getReleaseYear() == null ? "" : l.getReleaseYear()) + " "
                + (l.getAvailability() == null ? "" : l.getAvailability())).contains(amjNorm(c.text()))) {
            fails.add(AMJNote.amjOf("no coincide con \"" + c.text().trim() + "\""));
            gap += 1;
        }
        if (amjHas(c.cpu()) && !amjNorm(l.getCpu()).contains(amjNorm(c.cpu()))) {
            fails.add(AMJNote.amjOf("procesador " + l.getCpu()));
            gap += 0.5;
        }
        if (amjHas(c.gpu()) && !amjNorm(l.getGpu()).contains(amjNorm(c.gpu()))) {
            fails.add(AMJNote.amjOf("gráficos " + l.getGpu()));
            gap += 0.5;
        }
        Integer p = l.getPriceUsd();
        if (c.minPrice() != null || c.maxPrice() != null) {
            if (p == null) {
                fails.add(AMJNote.amjOf("sin precio registrado"));
                gap += 1;
            } else {
                if (c.maxPrice() != null && p > c.maxPrice()) {
                    fails.add(new AMJNote("sobre el presupuesto", p - c.maxPrice()));
                    gap += (double) (p - c.maxPrice()) / Math.max(c.maxPrice(), 1);
                }
                if (c.minPrice() != null && p < c.minPrice()) {
                    fails.add(new AMJNote("bajo el mínimo", c.minPrice() - p));
                    gap += (double) (c.minPrice() - p) / Math.max(c.minPrice(), 1);
                }
            }
        }
        gap += amjAtLeast(fails, "RAM", l.getRamGb(), c.minRamGb(), "GB");
        gap += amjAtLeast(fails, "almacenamiento", l.getStorageGb(), c.minStorageGb(), "GB");
        if (c.minScreen() != null || c.maxScreen() != null) {
            Double s = l.getScreenInches();
            if (s == null) {
                fails.add(AMJNote.amjOf("sin tamaño de pantalla"));
                gap += 1;
            } else {
                if (c.minScreen() != null && s < c.minScreen() - 0.05) {
                    fails.add(AMJNote.amjOf("pantalla de " + s + " in (menor)"));
                    gap += (c.minScreen() - s) / c.minScreen();
                }
                if (c.maxScreen() != null && s > c.maxScreen() + 0.05) {
                    fails.add(AMJNote.amjOf("pantalla de " + s + " in (mayor)"));
                    gap += (s - c.maxScreen()) / c.maxScreen();
                }
            }
        }
        if (c.maxWeightKg() != null) {
            Double w = l.getWeightKg();
            if (w == null) {
                fails.add(AMJNote.amjOf("sin peso registrado"));
                gap += 1;
            } else if (w > c.maxWeightKg()) {
                fails.add(AMJNote.amjOf(String.format(Locale.US, "%.2f kg sobre el límite", w - c.maxWeightKg())));
                gap += (w - c.maxWeightKg()) / c.maxWeightKg();
            }
        }
        return gap;
    }

    private static double amjAtLeast(List<AMJNote> fails, String label, Integer have, Integer need, String unit) {
        if (need == null) {
            return 0;
        }
        if (have == null) {
            fails.add(AMJNote.amjOf("sin dato de " + label));
            return 1;
        }
        if (have < need) {
            fails.add(AMJNote.amjOf(label + " " + have + " " + unit + " (pides " + need + ")"));
            return (double) (need - have) / need;
        }
        return 0;
    }

    /** Hasta {@code max} laptops parecidos a {@code ref}: misma categoría, precio, potencia gráfica, RAM y pantalla. */
    public List<AMJHit> amjSimilares(AMJLaptop ref, int max) {
        List<AMJHit> out = new ArrayList<>();
        for (AMJLaptop l : laptops.amjTodos()) {
            if (l.equals(ref)) {
                continue;
            }
            double d = 0;
            List<AMJNote> why = new ArrayList<>();
            if (!amjNorm(l.getCategory()).equals(amjNorm(ref.getCategory()))) {
                d += 1.0;
            } else {
                why.add(AMJNote.amjOf("misma categoría"));
            }
            d += amjRel(l.getPriceUsd(), ref.getPriceUsd(), 2.0);
            if (l.getPriceUsd() != null && ref.getPriceUsd() != null
                    && Math.abs(l.getPriceUsd() - ref.getPriceUsd()) <= ref.getPriceUsd() * 0.2) {
                why.add(AMJNote.amjOf("precio parecido"));
            }
            d += amjRel(amjGpuTier(l), amjGpuTier(ref), 1.5);
            d += amjRel(l.getRamGb(), ref.getRamGb(), 0.5);
            d += amjRel(l.getScreenInches(), ref.getScreenInches(), 1.0);
            if (l.getScreenInches() != null && ref.getScreenInches() != null
                    && Math.abs(l.getScreenInches() - ref.getScreenInches()) <= 0.5) {
                why.add(AMJNote.amjOf("pantalla de tamaño similar"));
            }
            if (amjNorm(l.getBrand()).equals(amjNorm(ref.getBrand()))) {
                d -= 0.2;
                why.add(AMJNote.amjOf("misma marca"));
            }
            if (amjGpuTier(l) != null && amjGpuTier(l).equals(amjGpuTier(ref))) {
                why.add(AMJNote.amjOf("misma gama de gráficos"));
            }
            out.add(new AMJHit(l, d, why));
        }
        out.sort(Comparator.comparingDouble(AMJHit::score));
        return out.stream().limit(max).toList();
    }

    /** Número de serie de la GPU (4090, 5070...) como gama aproximada; null si es integrada o no se reconoce. */
    static Integer amjGpuTier(AMJLaptop l) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b([3-5]0[5-9]0)\\b").matcher(l.getGpu());
        if (m.find()) {
            return Integer.valueOf(m.group(1));
        }
        String g = amjNorm(l.getGpu());
        if (g.contains("integrada") || g.contains("arc") || g.contains("adreno") || g.contains("intel graphics")) {
            return 0;
        }
        return null;
    }

    private static double amjRel(Number a, Number b, double weight) {
        if (a == null || b == null || b.doubleValue() == 0) {
            return weight * 0.5;
        }
        return Math.min(1.0, Math.abs(a.doubleValue() - b.doubleValue()) / Math.max(Math.abs(b.doubleValue()), 1)) * weight;
    }

    private static boolean amjHas(String s) {
        return s != null && !s.isBlank();
    }

    private static String amjNorm(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
