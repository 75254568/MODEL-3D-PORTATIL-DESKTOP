package com.laptopstudio.ui;

import java.util.Locale;

/** Formato de valores para tablas y tarjetas. */
public final class Fmt {

    public static final String ND = "Sin dato";

    private Fmt() {
    }

    public static String num(Number v, String unit) {
        if (v == null || v.doubleValue() == 0) {
            return ND;
        }
        double d = v.doubleValue();
        String s = d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.US, "%.2f", d).replaceAll("0+$", "");
        return unit == null || unit.isEmpty() ? s : s + " " + unit;
    }

    public static String text(String v) {
        return v == null || v.isBlank() ? ND : v;
    }

    public static String bool(Boolean v, String yes, String no) {
        return v == null ? ND : v ? yes : no;
    }

    public static String diff(Number a, Number b) {
        if (a == null || b == null || a.doubleValue() == 0 || b.doubleValue() == 0) {
            return "";
        }
        double p = (a.doubleValue() - b.doubleValue()) / b.doubleValue() * 100.0;
        if (Math.abs(p) < 0.05) {
            return "=";
        }
        return String.format(Locale.US, "%+.1f%%", p);
    }
}
