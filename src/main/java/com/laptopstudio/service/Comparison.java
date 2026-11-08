package com.laptopstudio.service;

import java.util.List;
import java.util.Locale;

/** Resultado de comparar el portatil A contra el B. */
public record Comparison(List<Delta> deltas) {

    /**
     * Diferencia de una magnitud. {@code pct} es el cambio de A respecto a B, en porcentaje.
     * Si alguno de los dos valores no esta publicado, {@code available} es falso.
     */
    public record Delta(String label, double a, double b, String unit, boolean available, boolean lowerIsBetter) {

        public double pct() {
            return !available || b == 0 ? 0 : (a - b) / b * 100.0;
        }

        /** Texto corto tipo "A: -11% Espesor". */
        public String headline() {
            if (!available) {
                return label + ": sin dato";
            }
            return String.format(Locale.US, "A: %+.0f%% %s", pct(), label);
        }

        public String pctText() {
            return available ? String.format(Locale.US, "%+.1f%%", pct()) : "n/d";
        }
    }

    public Delta get(String label) {
        return deltas.stream().filter(d -> d.label().equals(label)).findFirst().orElse(null);
    }
}
