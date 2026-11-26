package com.laptopexplorer.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;

/**
 * Moneda en la que se muestran los precios. Los precios se GUARDAN siempre en USD; aquí solo se decide como
 * mostrarlos. El tipo de cambio lo fija el usuario en Ajustes (con su fecha y origen), no se inventa.
 * Estado único de interfaz: cualquier pantalla puede escuchar {@link #amjCurrencyProperty()} y {@link #amjRateProperty()}.
 */
public final class AMJMoney {

    public enum AMJCurrency { USD, PEN }

    /** Tipo de cambio venta publicado por SUNAT con información de la SBS para el 9 de noviembre de 2026. */
    public static final double DEFAULT_PEN_RATE = 3.433;
    public static final String DEFAULT_PEN_NOTE = "SUNAT/SBS, venta, 9 nov 2026";

    private static final ObjectProperty<AMJCurrency> CURRENCY = new SimpleObjectProperty<>(AMJCurrency.USD);
    private static final DoubleProperty PEN_RATE = new SimpleDoubleProperty(DEFAULT_PEN_RATE);

    private AMJMoney() {
    }

    public static ObjectProperty<AMJCurrency> amjCurrencyProperty() {
        return CURRENCY;
    }

    public static DoubleProperty amjRateProperty() {
        return PEN_RATE;
    }

    public static AMJCurrency amjCurrency() {
        return CURRENCY.get();
    }

    public static String amjSymbol() {
        return amjCurrency() == AMJCurrency.PEN ? "S/" : "$";
    }

    public static String amjCode() {
        return amjCurrency().name();
    }

    /** Convierte un importe en USD a la moneda elegida. */
    public static double amjFromUsd(double usd) {
        return amjCurrency() == AMJCurrency.PEN ? usd * PEN_RATE.get() : usd;
    }

    /** Convierte a USD un importe escrito por el usuario en la moneda elegida (filtros de presupuesto). */
    public static int amjToUsd(double amount) {
        return (int) Math.round(amjCurrency() == AMJCurrency.PEN ? amount / PEN_RATE.get() : amount);
    }

    /** Precio listo para mostrar; "aprox." delante si el precio guardado es una referencia no verificada. */
    public static String amjFormat(Integer usd, Boolean estimated) {
        if (usd == null || usd <= 0) {
            return AMJFmt.ND;
        }
        long v = Math.round(amjFromUsd(usd));
        String s = (amjCurrency() == AMJCurrency.PEN ? "S/ " : "$") + String.format(Locale.US, "%,d", v);
        return Boolean.TRUE.equals(estimated) ? "aprox. " + s : s;
    }
}
