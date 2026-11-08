package com.laptopstudio.ui;

import com.laptopstudio.model.Laptop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Tabla de comparacion de 2 a 4 laptops, por secciones. El "mejor valor" solo se marca en magnitudes donde
 * mayor o menor es objetivamente mejor y los valores difieren; no se calculan puntuaciones.
 */
public final class CompareMatrix {

    /** Fila de la tabla: encabezado de seccion o dato. {@code best} contiene las columnas con el mejor valor. */
    public record Row(boolean group, String label, List<String> values, Set<Integer> best, boolean same) {
    }

    private CompareMatrix() {
    }

    private record Spec(String label, Function<Laptop, ?> get, String unit, int better) {
    }

    private static Spec s(String label, Function<Laptop, ?> get, String unit, int better) {
        return new Spec(label, get, unit, better);
    }

    private static final List<Object[]> GROUPS = List.of(
            new Object[]{"Chasis", List.of(
                    s("Ancho", Laptop::getWidthMm, "mm", 0), s("Fondo", Laptop::getDepthMm, "mm", 0),
                    s("Grosor", Laptop::getThicknessMm, "mm", -1), s("Peso", Laptop::getWeightKg, "kg", -1),
                    s("Material", Laptop::getChassisMaterial, "", 0))},
            new Object[]{"Procesador", List.of(
                    s("Modelo", Laptop::getCpu, "", 0), s("Núcleos", Laptop::getCpuCores, "", 1),
                    s("Potencia base", Laptop::getCpuBaseWatts, "W", 0), s("Potencia máxima", Laptop::getCpuBoostWatts, "W", 0))},
            new Object[]{"Gráficos", List.of(
                    s("Modelo", Laptop::getGpu, "", 0), s("VRAM", Laptop::getGpuVramGb, "GB", 1),
                    s("TGP", Laptop::getGpuTgpWatts, "W", 1))},
            new Object[]{"Memoria y almacenamiento", List.of(
                    s("RAM", Laptop::getRamGb, "GB", 1), s("Tipo de RAM", Laptop::getRamType, "", 0),
                    s("RAM ampliable", (Laptop l) -> l.getRamUpgradable() == null ? null : l.getRamUpgradable() ? "Sí" : "No", "", 0),
                    s("Almacenamiento", Laptop::getStorageGb, "GB", 1), s("Ranuras M.2", Laptop::getStorageSlots, "", 1))},
            new Object[]{"Pantalla", List.of(
                    s("Tamaño", Laptop::getScreenInches, "in", 0), s("Resolución", Laptop::getResolution, "", 0),
                    s("Frecuencia", Laptop::getRefreshRateHz, "Hz", 1), s("Panel", Laptop::getPanelType, "", 0))},
            new Object[]{"Batería", List.of(s("Capacidad", Laptop::getBatteryWh, "Wh", 1))});

    @SuppressWarnings("unchecked")
    public static List<Row> build(List<Laptop> laptops, boolean onlyDifferences) {
        List<Row> out = new ArrayList<>();
        for (Object[] g : GROUPS) {
            List<Row> rows = new ArrayList<>();
            for (Spec sp : (List<Spec>) g[1]) {
                List<Object> raw = new ArrayList<>();
                for (Laptop l : laptops) {
                    Object v = sp.get().apply(l);
                    raw.add(v instanceof Number n && n.doubleValue() == 0 ? null : v instanceof String str && str.isBlank() ? null : v);
                }
                Set<String> distinct = new HashSet<>();
                raw.forEach(v -> distinct.add(String.valueOf(v)));
                boolean same = distinct.size() == 1;
                if (onlyDifferences && same) {
                    continue;
                }
                Set<Integer> best = new HashSet<>();
                if (sp.better() != 0 && !same) {
                    double target = sp.better() > 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                    for (Object v : raw) {
                        if (v instanceof Number n) {
                            target = sp.better() > 0 ? Math.max(target, n.doubleValue()) : Math.min(target, n.doubleValue());
                        }
                    }
                    for (int i = 0; i < raw.size(); i++) {
                        if (raw.get(i) instanceof Number n && n.doubleValue() == target) {
                            best.add(i);
                        }
                    }
                }
                List<String> texts = new ArrayList<>();
                for (Object v : raw) {
                    texts.add(v == null ? Fmt.ND : v instanceof Number n ? Fmt.num(n, sp.unit()) : String.valueOf(v));
                }
                rows.add(new Row(false, sp.label(), texts, best, same));
            }
            if (!rows.isEmpty()) {
                out.add(new Row(true, (String) g[0], List.of(), Set.of(), true));
                out.addAll(rows);
            }
        }
        return out;
    }
}
