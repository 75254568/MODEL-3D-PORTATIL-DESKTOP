package com.laptopexplorer.service;

import java.util.List;
import java.util.Locale;

/** Resultado de comparar el portátil A contra el B. */
public record AMJComparison(List<AMJDelta> deltas) {

    /**
     * Diferencia de una magnitud. {@code pct} es el cambio de A respecto a B, en porcentaje.
     * Si alguno de los dos valores no esta publicado, {@code available} es falso.
     */
    public record AMJDelta(String label, double a, double b, String unit, boolean available, boolean lowerIsBetter) {

        public double amjPct() {
            return !available || b == 0 ? 0 : (a - b) / b * 100.0;
        }

        /** Texto corto tipo "A: -11% Espesor". */
        public String amjHeadline() {
            if (!available) {
                return label + ": sin dato";
            }
            return String.format(Locale.US, "A: %+.0f%% %s", amjPct(), label);
        }

        public String amjPctText() {
            return available ? String.format(Locale.US, "%+.1f%%", amjPct()) : "n/d";
        }
    }

    public AMJDelta amjGet(String label) {
        return deltas.stream().filter(d -> d.label().equals(label)).findFirst().orElse(null);
    }
}
