package com.laptopexplorer.ui;

import com.laptopexplorer.model.AMJLaptop;
import com.laptopexplorer.viewer.AMJLaptopModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Ficha de un componente con A y B frente a frente. Solo muestra datos registrados del laptop y el estado real
 * del modelo 3D (si hay interior reconstruido, cuantas piezas y de que fuentes). No inventa especificaciones.
 */
public final class AMJPartInspector {

    public record AMJRow(String label, String a, String b) {
    }

    public record AMJResult(String title, List<AMJRow> rows) {
    }

    private AMJPartInspector() {
    }

    public static AMJResult amjInspect(String part, AMJLaptop a, AMJLaptop b) {
        List<AMJRow> r = new ArrayList<>();
        String title;
        switch (part) {
            case "cpu" -> {
                title = "Procesador";
                amjT(r, "Modelo", a, b, AMJLaptop::getCpu);
                amjN(r, "Núcleos", a, b, AMJLaptop::getCpuCores, "");
                amjN(r, "Potencia base", a, b, AMJLaptop::getCpuBaseWatts, "W");
                amjN(r, "Potencia máxima", a, b, AMJLaptop::getCpuBoostWatts, "W");
            }
            case "gpu" -> {
                title = "Gráficos";
                amjT(r, "Modelo", a, b, AMJLaptop::getGpu);
                amjN(r, "VRAM", a, b, AMJLaptop::getGpuVramGb, "GB");
                amjN(r, "TGP", a, b, AMJLaptop::getGpuTgpWatts, "W");
            }
            case "ram" -> {
                title = "Memoria RAM";
                amjN(r, "Capacidad", a, b, AMJLaptop::getRamGb, "GB");
                amjT(r, "Tipo", a, b, AMJLaptop::getRamType);
                amjT(r, "Ampliable", a, b, l -> AMJFmt.amjBool(l.getRamUpgradable(), "Sí, módulos SO-DIMM", "No, soldada"));
            }
            case "ssd" -> {
                title = "Almacenamiento";
                amjN(r, "Capacidad", a, b, AMJLaptop::getStorageGb, "GB");
                amjN(r, "Ranuras M.2", a, b, AMJLaptop::getStorageSlots, "");
            }
            case "battery" -> {
                title = "Batería";
                amjN(r, "Capacidad", a, b, AMJLaptop::getBatteryWh, "Wh");
            }
            case "cooling" -> {
                title = "Refrigeración";
                amjT(r, "CPU + GPU (suma de potencias máximas registradas)", a, b, l -> {
                    int w = (l.getCpuBoostWatts() == null ? 0 : l.getCpuBoostWatts())
                            + (l.getGpuTgpWatts() == null ? 0 : l.getGpuTgpWatts());
                    return w == 0 ? AMJFmt.ND : w + " W";
                });
            }
            case "pcb" -> title = "Placa base";
            case "display", "lid", "hinge" -> {
                title = "Pantalla y tapa";
                amjN(r, "Tamaño", a, b, AMJLaptop::getScreenInches, "in");
                amjT(r, "Resolución", a, b, AMJLaptop::getResolution);
                amjN(r, "Frecuencia", a, b, AMJLaptop::getRefreshRateHz, "Hz");
                amjT(r, "Panel", a, b, AMJLaptop::getPanelType);
            }
            default -> {
                title = "Chasis";
                amjT(r, "Material", a, b, AMJLaptop::getChassisMaterial);
                amjN(r, "Ancho", a, b, AMJLaptop::getWidthMm, "mm");
                amjN(r, "Fondo", a, b, AMJLaptop::getDepthMm, "mm");
                amjN(r, "Grosor", a, b, AMJLaptop::getThicknessMm, "mm");
                amjN(r, "Peso", a, b, AMJLaptop::getWeightKg, "kg");
            }
        }
        boolean inside = List.of("cpu", "gpu", "ram", "ssd", "battery", "cooling", "pcb").contains(part);
        if (inside) {
            r.add(new AMJRow("Modelo 3D", amjState(part, a), amjState(part, b)));
            r.add(new AMJRow("Fuente del modelo", amjSource(part, a), amjSource(part, b)));
        } else {
            r.add(new AMJRow("Modelo 3D", a == null ? AMJFmt.ND : "Exterior modelado", b == null ? AMJFmt.ND : "Exterior modelado"));
        }
        return new AMJResult(title, r);
    }

    private static String amjState(String part, AMJLaptop l) {
        if (l == null) {
            return AMJFmt.ND;
        }
        AMJLaptopModel.AMJInteriorInfo info = AMJLaptopModel.amjInteriorInfo(l);
        if (!info.reconstructed()) {
            return "Interior aproximado, sin reconstruir";
        }
        Integer n = info.counts().get(part);
        return n == null ? "No modelado (sin datos de posición)" : "Modelado, " + n + (n == 1 ? " pieza" : " piezas");
    }

    private static String amjSource(String part, AMJLaptop l) {
        if (l == null) {
            return AMJFmt.ND;
        }
        var src = AMJLaptopModel.amjInteriorInfo(l).sources().get(part);
        return src == null || src.isEmpty() ? AMJFmt.ND : String.join("; ", src);
    }

    private static void amjT(List<AMJRow> r, String label, AMJLaptop a, AMJLaptop b, Function<AMJLaptop, String> f) {
        r.add(new AMJRow(label, a == null ? AMJFmt.ND : AMJFmt.amjText(f.apply(a)), b == null ? AMJFmt.ND : AMJFmt.amjText(f.apply(b))));
    }

    private static void amjN(List<AMJRow> r, String label, AMJLaptop a, AMJLaptop b, Function<AMJLaptop, ? extends Number> f, String unit) {
        r.add(new AMJRow(label, a == null ? AMJFmt.ND : AMJFmt.amjNum(f.apply(a), unit), b == null ? AMJFmt.ND : AMJFmt.amjNum(f.apply(b), unit)));
    }
}
