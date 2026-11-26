package com.laptopexplorer.ui;

import com.laptopexplorer.model.AMJLaptop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Tabla de comparación de 2 a 4 laptops, por secciones. El "mejor valor" solo se marca en magnitudes donde
 * mayor o menor es objetivamente mejor y los valores difieren; no se calculan puntuaciones.
 */
public final class AMJCompareMatrix {

    /** Fila de la tabla: encabezado de sección o dato. {@code best} contiene las columnas con el mejor valor. */
    public record AMJRow(boolean group, String label, List<String> values, Set<Integer> best, boolean same) {
    }

    private AMJCompareMatrix() {
    }

    private record AMJSpec(String label, Function<AMJLaptop, ?> get, String unit, int better) {
    }

    private static AMJSpec amjS(String label, Function<AMJLaptop, ?> get, String unit, int better) {
        return new AMJSpec(label, get, unit, better);
    }

    private static final List<Object[]> GROUPS = List.of(
            new Object[]{"Precio", List.of(amjS("Precio", AMJLaptop::getPriceUsd, "$", -1))},
            new Object[]{"Chasis", List.of(
                    amjS("Ancho", AMJLaptop::getWidthMm, "mm", 0), amjS("Fondo", AMJLaptop::getDepthMm, "mm", 0),
                    amjS("Grosor", AMJLaptop::getThicknessMm, "mm", -1), amjS("Peso", AMJLaptop::getWeightKg, "kg", -1),
                    amjS("Material", AMJLaptop::getChassisMaterial, "", 0))},
            new Object[]{"Procesador", List.of(
                    amjS("Modelo", AMJLaptop::getCpu, "", 0), amjS("Núcleos", AMJLaptop::getCpuCores, "", 1),
                    amjS("Potencia base", AMJLaptop::getCpuBaseWatts, "W", 0), amjS("Potencia máxima", AMJLaptop::getCpuBoostWatts, "W", 0))},
            new Object[]{"Gráficos", List.of(
                    amjS("Modelo", AMJLaptop::getGpu, "", 0), amjS("VRAM", AMJLaptop::getGpuVramGb, "GB", 1),
                    amjS("TGP", AMJLaptop::getGpuTgpWatts, "W", 1))},
            new Object[]{"Memoria y almacenamiento", List.of(
                    amjS("RAM", AMJLaptop::getRamGb, "GB", 1), amjS("Tipo de RAM", AMJLaptop::getRamType, "", 0),
                    amjS("RAM ampliable", (AMJLaptop l) -> l.getRamUpgradable() == null ? null : l.getRamUpgradable() ? "Sí" : "No", "", 0),
                    amjS("Almacenamiento", AMJLaptop::getStorageGb, "GB", 1), amjS("Ranuras M.2", AMJLaptop::getStorageSlots, "", 1))},
            new Object[]{"Pantalla", List.of(
                    amjS("Tamaño", AMJLaptop::getScreenInches, "in", 0), amjS("Resolución", AMJLaptop::getResolution, "", 0),
                    amjS("Frecuencia", AMJLaptop::getRefreshRateHz, "Hz", 1), amjS("Panel", AMJLaptop::getPanelType, "", 0))},
            new Object[]{"Batería", List.of(amjS("Capacidad", AMJLaptop::getBatteryWh, "Wh", 1))});

    @SuppressWarnings("unchecked")
    public static List<AMJRow> amjBuild(List<AMJLaptop> laptops, boolean onlyDifferences) {
        List<AMJRow> out = new ArrayList<>();
        for (Object[] g : GROUPS) {
            List<AMJRow> rows = new ArrayList<>();
            for (AMJSpec sp : (List<AMJSpec>) g[1]) {
                List<Object> raw = new ArrayList<>();
                for (AMJLaptop l : laptops) {
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
                    AMJLaptop l = laptops.get(texts.size());
                    texts.add(v == null ? AMJFmt.ND : "$".equals(sp.unit()) ? AMJMoney.amjFormat(l.getPriceUsd(), l.getPriceEstimated())
                            : v instanceof Number n ? AMJFmt.amjNum(n, sp.unit()) : String.valueOf(v));
                }
                rows.add(new AMJRow(false, sp.label(), texts, best, same));
            }
            if (!rows.isEmpty()) {
                out.add(new AMJRow(true, (String) g[0], List.of(), Set.of(), true));
                out.addAll(rows);
            }
        }
        return out;
    }
}
