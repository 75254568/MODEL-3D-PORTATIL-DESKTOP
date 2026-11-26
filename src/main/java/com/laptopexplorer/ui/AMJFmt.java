package com.laptopexplorer.ui;

import java.util.Locale;

/** Formato de valores para tablas y tarjetas. */
public final class AMJFmt {

    public static final String ND = "Sin dato";

    private AMJFmt() {
    }

    public static String amjNum(Number v, String unit) {
        if (v == null || v.doubleValue() == 0) {
            return ND;
        }
        double d = v.doubleValue();
        String s = d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.US, "%.2f", d).replaceAll("0+$", "");
        return unit == null || unit.isEmpty() ? s : s + " " + unit;
    }

    /** Precio en la moneda elegida (ver {@link AMJMoney}); si es una referencia aproximada se antepone "aprox.". */
    public static String amjPrice(Integer usd, Boolean estimated) {
        if (usd == null || usd <= 0) {
            return ND;
        }
        return AMJMoney.amjFormat(usd, estimated);
    }

    public static String amjText(String v) {
        return v == null || v.isBlank() ? ND : v;
    }

    public static String amjBool(Boolean v, String yes, String no) {
        return v == null ? ND : v ? yes : no;
    }

    public static String amjDiff(Number a, Number b) {
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
