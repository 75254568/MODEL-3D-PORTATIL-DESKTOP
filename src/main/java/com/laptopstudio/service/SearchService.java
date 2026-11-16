package com.laptopstudio.service;

import com.laptopstudio.model.Laptop;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Busqueda por necesidades: filtra el catalogo con los criterios del usuario y, ademas, propone alternativas que
 * casi los cumplen y laptops parecidos a uno dado. Trabaja en memoria sobre el catalogo (decenas de registros).
 * Un criterio sin valor (null o vacio) no filtra. Un laptop sin dato en un campo filtrado no cumple ese criterio.
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private final LaptopService laptops;

    /** Criterios de busqueda. Todos opcionales. */
    public record Criteria(String text, Integer minPrice, Integer maxPrice, String brand, String cpu, Integer minRamGb,
                           Integer minStorageGb, String gpu, Double minScreen, Double maxScreen, Double maxWeightKg,
                           String category) {

        public static Criteria empty() {
            return new Criteria(null, null, null, null, null, null, null, null, null, null, null, null);
        }
    }

    /**
     * Motivo por el que un laptop no cumple un criterio, o rasgo en el que se parece a otro. Si {@code usd} no es
     * nulo, la nota es un importe (siempre en USD, como se guardan los precios) seguido de {@code text}: lo escribe
     * la interfaz en la moneda que el usuario tenga elegida.
     */
    public record Note(String text, Integer usd) {

        static Note of(String text) {
            return new Note(text, null);
        }
    }

    /** Laptop con su puntaje y la lista de criterios que no cumple o por cuanto se queda corto. */
    public record Hit(Laptop laptop, double score, List<Note> notes) {
    }

    public record Result(List<Hit> matches, List<Hit> alternatives) {
    }

    public List<String> marcas() {
        TreeSet<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        laptops.todos().forEach(l -> s.add(l.getBrand()));
        return new ArrayList<>(s);
    }

    public List<String> categorias() {
        TreeSet<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        laptops.todos().forEach(l -> s.add(l.getCategory()));
        return new ArrayList<>(s);
    }

    /** Campos del buscador que ofrecen sugerencias. */
    public enum Campo { TEXTO, CPU, GPU, RAM, ALMACENAMIENTO, PANTALLA_MIN, PANTALLA_MAX }

    private static final java.util.regex.Pattern CPU_FAMILIA = java.util.regex.Pattern.compile(
            "(Intel Core Ultra \\d|Intel Core i\\d|AMD Ryzen \\d|Apple M\\d|Snapdragon X)", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern GPU_FAMILIA = java.util.regex.Pattern.compile(
            "(RTX \\d{4}(?: Ti)?|RTX \\d{4} Ada|Radeon RX \\d{4}\\w*|Arc|Adreno)", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Sugerencias para un campo, calculadas sobre los laptops que cumplen el RESTO de los criterios: asi las
     * opciones se van acotando segun lo que el usuario ya eligio. Para procesador y graficos incluye primero
     * las familias ("Intel Core Ultra 9", "RTX 5080") y despues los nombres completos.
     */
    public List<String> sugerencias(Campo campo, Criteria c) {
        Criteria resto = switch (campo) {
            case TEXTO -> new Criteria(null, c.minPrice(), c.maxPrice(), c.brand(), c.cpu(), c.minRamGb(), c.minStorageGb(), c.gpu(), c.minScreen(), c.maxScreen(), c.maxWeightKg(), c.category());
            case CPU -> new Criteria(c.text(), c.minPrice(), c.maxPrice(), c.brand(), null, c.minRamGb(), c.minStorageGb(), c.gpu(), c.minScreen(), c.maxScreen(), c.maxWeightKg(), c.category());
            case GPU -> new Criteria(c.text(), c.minPrice(), c.maxPrice(), c.brand(), c.cpu(), c.minRamGb(), c.minStorageGb(), null, c.minScreen(), c.maxScreen(), c.maxWeightKg(), c.category());
            case RAM -> new Criteria(c.text(), c.minPrice(), c.maxPrice(), c.brand(), c.cpu(), null, c.minStorageGb(), c.gpu(), c.minScreen(), c.maxScreen(), c.maxWeightKg(), c.category());
            case ALMACENAMIENTO -> new Criteria(c.text(), c.minPrice(), c.maxPrice(), c.brand(), c.cpu(), c.minRamGb(), null, c.gpu(), c.minScreen(), c.maxScreen(), c.maxWeightKg(), c.category());
            case PANTALLA_MIN, PANTALLA_MAX -> new Criteria(c.text(), c.minPrice(), c.maxPrice(), c.brand(), c.cpu(), c.minRamGb(), c.minStorageGb(), c.gpu(), null, null, c.maxWeightKg(), c.category());
        };
        java.util.LinkedHashSet<String> familias = new java.util.LinkedHashSet<>();
        TreeSet<String> textos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<Double> numeros = new TreeSet<>();
        for (Laptop l : laptops.todos()) {
            if (evaluate(l, resto, new ArrayList<>()) > 0) {
                continue;
            }
            switch (campo) {
                case TEXTO -> {
                    textos.add(l.getModel());
                    familias.add(l.getBrand());
                }
                case CPU -> {
                    familia(CPU_FAMILIA, l.getCpu(), familias);
                    textos.add(l.getCpu());
                }
                case GPU -> {
                    familia(GPU_FAMILIA, l.getGpu(), familias);
                    textos.add(l.getGpu());
                }
                case RAM -> add(numeros, l.getRamGb());
                case ALMACENAMIENTO -> add(numeros, l.getStorageGb());
                case PANTALLA_MIN, PANTALLA_MAX -> add(numeros, l.getScreenInches());
            }
        }
        List<String> out = new ArrayList<>(new TreeSet<>(familias));
        out.addAll(textos);
        for (Double n : numeros) {
            out.add(n == Math.rint(n) ? String.valueOf(n.longValue()) : String.valueOf(n));
        }
        return out;
    }

    private static void familia(java.util.regex.Pattern p, String valor, java.util.Set<String> into) {
        java.util.regex.Matcher m = p.matcher(valor == null ? "" : valor);
        while (m.find()) {
            into.add(m.group(1));
        }
    }

    private static void add(TreeSet<Double> into, Number n) {
        if (n != null && n.doubleValue() > 0) {
            into.add(n.doubleValue());
        }
    }

    /** Coincidencias exactas (de menor a mayor precio) y, si hay pocas, hasta 6 alternativas cercanas. */
    public Result buscar(Criteria c) {
        List<Hit> matches = new ArrayList<>();
        List<Hit> near = new ArrayList<>();
        for (Laptop l : laptops.todos()) {
            List<Note> fails = new ArrayList<>();
            double gap = evaluate(l, c, fails);
            if (fails.isEmpty()) {
                matches.add(new Hit(l, 0, List.of()));
            } else {
                near.add(new Hit(l, gap, fails));
            }
        }
        matches.sort(Comparator.comparing((Hit h) -> h.laptop().getPriceUsd() == null)
                .thenComparing(h -> h.laptop().getPriceUsd() == null ? 0 : h.laptop().getPriceUsd()));
        near.sort(Comparator.comparingDouble(Hit::score));
        // Una alternativa solo tiene sentido si falla pocos criterios y por poco
        List<Hit> alt = near.stream().filter(h -> h.notes().size() <= 2 && h.score() <= 0.35).limit(6).toList();
        return new Result(matches, alt);
    }

    /**
     * Mide cuanto se aleja el laptop de los criterios. Devuelve una distancia normalizada (0 = cumple todo) y
     * anota en {@code fails} cada criterio incumplido, con texto para el usuario.
     */
    private double evaluate(Laptop l, Criteria c, List<Note> fails) {
        double gap = 0;
        if (has(c.brand()) && !norm(l.getBrand()).equals(norm(c.brand()))) {
            fails.add(Note.of("marca distinta (" + l.getBrand() + ")"));
            gap += 1;
        }
        if (has(c.category()) && !norm(l.getCategory()).equals(norm(c.category()))) {
            fails.add(Note.of("categoría " + l.getCategory()));
            gap += 0.3;
        }
        if (has(c.text()) && !norm(l.getDisplayName() + " " + l.getCpu() + " " + l.getGpu() + " " + l.getCategory() + " "
                + (l.getReleaseYear() == null ? "" : l.getReleaseYear()) + " "
                + (l.getAvailability() == null ? "" : l.getAvailability())).contains(norm(c.text()))) {
            fails.add(Note.of("no coincide con \"" + c.text().trim() + "\""));
            gap += 1;
        }
        if (has(c.cpu()) && !norm(l.getCpu()).contains(norm(c.cpu()))) {
            fails.add(Note.of("procesador " + l.getCpu()));
            gap += 0.5;
        }
        if (has(c.gpu()) && !norm(l.getGpu()).contains(norm(c.gpu()))) {
            fails.add(Note.of("gráficos " + l.getGpu()));
            gap += 0.5;
        }
        Integer p = l.getPriceUsd();
        if (c.minPrice() != null || c.maxPrice() != null) {
            if (p == null) {
                fails.add(Note.of("sin precio registrado"));
                gap += 1;
            } else {
                if (c.maxPrice() != null && p > c.maxPrice()) {
                    fails.add(new Note("sobre el presupuesto", p - c.maxPrice()));
                    gap += (double) (p - c.maxPrice()) / Math.max(c.maxPrice(), 1);
                }
                if (c.minPrice() != null && p < c.minPrice()) {
                    fails.add(new Note("bajo el mínimo", c.minPrice() - p));
                    gap += (double) (c.minPrice() - p) / Math.max(c.minPrice(), 1);
                }
            }
        }
        gap += atLeast(fails, "RAM", l.getRamGb(), c.minRamGb(), "GB");
        gap += atLeast(fails, "almacenamiento", l.getStorageGb(), c.minStorageGb(), "GB");
        if (c.minScreen() != null || c.maxScreen() != null) {
            Double s = l.getScreenInches();
            if (s == null) {
                fails.add(Note.of("sin tamaño de pantalla"));
                gap += 1;
            } else {
                if (c.minScreen() != null && s < c.minScreen() - 0.05) {
                    fails.add(Note.of("pantalla de " + s + " in (menor)"));
                    gap += (c.minScreen() - s) / c.minScreen();
                }
                if (c.maxScreen() != null && s > c.maxScreen() + 0.05) {
                    fails.add(Note.of("pantalla de " + s + " in (mayor)"));
                    gap += (s - c.maxScreen()) / c.maxScreen();
                }
            }
        }
        if (c.maxWeightKg() != null) {
            Double w = l.getWeightKg();
            if (w == null) {
                fails.add(Note.of("sin peso registrado"));
                gap += 1;
            } else if (w > c.maxWeightKg()) {
                fails.add(Note.of(String.format(Locale.US, "%.2f kg sobre el límite", w - c.maxWeightKg())));
                gap += (w - c.maxWeightKg()) / c.maxWeightKg();
            }
        }
        return gap;
    }

    private static double atLeast(List<Note> fails, String label, Integer have, Integer need, String unit) {
        if (need == null) {
            return 0;
        }
        if (have == null) {
            fails.add(Note.of("sin dato de " + label));
            return 1;
        }
        if (have < need) {
            fails.add(Note.of(label + " " + have + " " + unit + " (pides " + need + ")"));
            return (double) (need - have) / need;
        }
        return 0;
    }

    /** Hasta {@code max} laptops parecidos a {@code ref}: misma categoria, precio, potencia grafica, RAM y pantalla. */
    public List<Hit> similares(Laptop ref, int max) {
        List<Hit> out = new ArrayList<>();
        for (Laptop l : laptops.todos()) {
            if (l.equals(ref)) {
                continue;
            }
            double d = 0;
            List<Note> why = new ArrayList<>();
            if (!norm(l.getCategory()).equals(norm(ref.getCategory()))) {
                d += 1.0;
            } else {
                why.add(Note.of("misma categoría"));
            }
            d += rel(l.getPriceUsd(), ref.getPriceUsd(), 2.0);
            if (l.getPriceUsd() != null && ref.getPriceUsd() != null
                    && Math.abs(l.getPriceUsd() - ref.getPriceUsd()) <= ref.getPriceUsd() * 0.2) {
                why.add(Note.of("precio parecido"));
            }
            d += rel(gpuTier(l), gpuTier(ref), 1.5);
            d += rel(l.getRamGb(), ref.getRamGb(), 0.5);
            d += rel(l.getScreenInches(), ref.getScreenInches(), 1.0);
            if (l.getScreenInches() != null && ref.getScreenInches() != null
                    && Math.abs(l.getScreenInches() - ref.getScreenInches()) <= 0.5) {
                why.add(Note.of("pantalla de tamaño similar"));
            }
            if (norm(l.getBrand()).equals(norm(ref.getBrand()))) {
                d -= 0.2;
                why.add(Note.of("misma marca"));
            }
            if (gpuTier(l) != null && gpuTier(l).equals(gpuTier(ref))) {
                why.add(Note.of("misma gama de gráficos"));
            }
            out.add(new Hit(l, d, why));
        }
        out.sort(Comparator.comparingDouble(Hit::score));
        return out.stream().limit(max).toList();
    }

    /** Numero de serie de la GPU (4090, 5070...) como gama aproximada; null si es integrada o no se reconoce. */
    static Integer gpuTier(Laptop l) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b([3-5]0[5-9]0)\\b").matcher(l.getGpu());
        if (m.find()) {
            return Integer.valueOf(m.group(1));
        }
        String g = norm(l.getGpu());
        if (g.contains("integrada") || g.contains("arc") || g.contains("adreno") || g.contains("intel graphics")) {
            return 0;
        }
        return null;
    }

    private static double rel(Number a, Number b, double weight) {
        if (a == null || b == null || b.doubleValue() == 0) {
            return weight * 0.5;
        }
        return Math.min(1.0, Math.abs(a.doubleValue() - b.doubleValue()) / Math.max(Math.abs(b.doubleValue()), 1)) * weight;
    }

    private static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private static String norm(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
