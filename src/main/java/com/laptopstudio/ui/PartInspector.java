package com.laptopstudio.ui;

import com.laptopstudio.model.Laptop;
import com.laptopstudio.viewer.LaptopModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Ficha de un componente con A y B frente a frente. Solo muestra datos registrados del laptop y el estado real
 * del modelo 3D (si hay interior reconstruido, cuantas piezas y de que fuentes). No inventa especificaciones.
 */
public final class PartInspector {

    public record Row(String label, String a, String b) {
    }

    public record Result(String title, List<Row> rows) {
    }

    private PartInspector() {
    }

    public static Result inspect(String part, Laptop a, Laptop b) {
        List<Row> r = new ArrayList<>();
        String title;
        switch (part) {
            case "cpu" -> {
                title = "Procesador";
                t(r, "Modelo", a, b, Laptop::getCpu);
                n(r, "Núcleos", a, b, Laptop::getCpuCores, "");
                n(r, "Potencia base", a, b, Laptop::getCpuBaseWatts, "W");
                n(r, "Potencia máxima", a, b, Laptop::getCpuBoostWatts, "W");
            }
            case "gpu" -> {
                title = "Gráficos";
                t(r, "Modelo", a, b, Laptop::getGpu);
                n(r, "VRAM", a, b, Laptop::getGpuVramGb, "GB");
                n(r, "TGP", a, b, Laptop::getGpuTgpWatts, "W");
            }
            case "ram" -> {
                title = "Memoria RAM";
                n(r, "Capacidad", a, b, Laptop::getRamGb, "GB");
                t(r, "Tipo", a, b, Laptop::getRamType);
                t(r, "Ampliable", a, b, l -> Fmt.bool(l.getRamUpgradable(), "Sí, módulos SO-DIMM", "No, soldada"));
            }
            case "ssd" -> {
                title = "Almacenamiento";
                n(r, "Capacidad", a, b, Laptop::getStorageGb, "GB");
                n(r, "Ranuras M.2", a, b, Laptop::getStorageSlots, "");
            }
            case "battery" -> {
                title = "Batería";
                n(r, "Capacidad", a, b, Laptop::getBatteryWh, "Wh");
            }
            case "cooling" -> {
                title = "Refrigeración";
                t(r, "CPU + GPU (suma de potencias máximas registradas)", a, b, l -> {
                    int w = (l.getCpuBoostWatts() == null ? 0 : l.getCpuBoostWatts())
                            + (l.getGpuTgpWatts() == null ? 0 : l.getGpuTgpWatts());
                    return w == 0 ? Fmt.ND : w + " W";
                });
            }
            case "pcb" -> title = "Placa base";
            case "display", "lid", "hinge" -> {
                title = "Pantalla y tapa";
                n(r, "Tamaño", a, b, Laptop::getScreenInches, "in");
                t(r, "Resolución", a, b, Laptop::getResolution);
                n(r, "Frecuencia", a, b, Laptop::getRefreshRateHz, "Hz");
                t(r, "Panel", a, b, Laptop::getPanelType);
            }
            default -> {
                title = "Chasis";
                t(r, "Material", a, b, Laptop::getChassisMaterial);
                n(r, "Ancho", a, b, Laptop::getWidthMm, "mm");
                n(r, "Fondo", a, b, Laptop::getDepthMm, "mm");
                n(r, "Grosor", a, b, Laptop::getThicknessMm, "mm");
                n(r, "Peso", a, b, Laptop::getWeightKg, "kg");
            }
        }
        boolean inside = List.of("cpu", "gpu", "ram", "ssd", "battery", "cooling", "pcb").contains(part);
        if (inside) {
            r.add(new Row("Modelo 3D", state(part, a), state(part, b)));
            r.add(new Row("Fuente del modelo", source(part, a), source(part, b)));
        } else {
            r.add(new Row("Modelo 3D", a == null ? Fmt.ND : "Exterior modelado", b == null ? Fmt.ND : "Exterior modelado"));
        }
        return new Result(title, r);
    }

    private static String state(String part, Laptop l) {
        if (l == null) {
            return Fmt.ND;
        }
        LaptopModel.InteriorInfo info = LaptopModel.interiorInfo(l);
        if (!info.reconstructed()) {
            return "Interior aproximado, sin reconstruir";
        }
        Integer n = info.counts().get(part);
        return n == null ? "No modelado (sin datos de posición)" : "Modelado, " + n + (n == 1 ? " pieza" : " piezas");
    }

    private static String source(String part, Laptop l) {
        if (l == null) {
            return Fmt.ND;
        }
        var src = LaptopModel.interiorInfo(l).sources().get(part);
        return src == null || src.isEmpty() ? Fmt.ND : String.join("; ", src);
    }

    private static void t(List<Row> r, String label, Laptop a, Laptop b, Function<Laptop, String> f) {
        r.add(new Row(label, a == null ? Fmt.ND : Fmt.text(f.apply(a)), b == null ? Fmt.ND : Fmt.text(f.apply(b))));
    }

    private static void n(List<Row> r, String label, Laptop a, Laptop b, Function<Laptop, ? extends Number> f, String unit) {
        r.add(new Row(label, a == null ? Fmt.ND : Fmt.num(f.apply(a), unit), b == null ? Fmt.ND : Fmt.num(f.apply(b), unit)));
    }
}
