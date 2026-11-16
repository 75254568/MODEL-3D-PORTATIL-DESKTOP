package com.laptopstudio.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;

/**
 * Moneda en la que se muestran los precios. Los precios se GUARDAN siempre en USD; aqui solo se decide como
 * mostrarlos. El tipo de cambio lo fija el usuario en Ajustes (con su fecha y origen), no se inventa.
 * Estado unico de interfaz: cualquier pantalla puede escuchar {@link #currencyProperty()} y {@link #rateProperty()}.
 */
public final class Money {

    public enum Currency { USD, PEN }

    /** Tipo de cambio venta publicado por SUNAT con informacion de la SBS para el 9 de noviembre de 2026. */
    public static final double DEFAULT_PEN_RATE = 3.433;
    public static final String DEFAULT_PEN_NOTE = "SUNAT/SBS, venta, 9 nov 2026";

    private static final ObjectProperty<Currency> CURRENCY = new SimpleObjectProperty<>(Currency.USD);
    private static final DoubleProperty PEN_RATE = new SimpleDoubleProperty(DEFAULT_PEN_RATE);

    private Money() {
    }

    public static ObjectProperty<Currency> currencyProperty() {
        return CURRENCY;
    }

    public static DoubleProperty rateProperty() {
        return PEN_RATE;
    }

    public static Currency currency() {
        return CURRENCY.get();
    }

    public static String symbol() {
        return currency() == Currency.PEN ? "S/" : "$";
    }

    public static String code() {
        return currency().name();
    }

    /** Convierte un importe en USD a la moneda elegida. */
    public static double fromUsd(double usd) {
        return currency() == Currency.PEN ? usd * PEN_RATE.get() : usd;
    }

    /** Convierte a USD un importe escrito por el usuario en la moneda elegida (filtros de presupuesto). */
    public static int toUsd(double amount) {
        return (int) Math.round(currency() == Currency.PEN ? amount / PEN_RATE.get() : amount);
    }

    /** Precio listo para mostrar; "aprox." delante si el precio guardado es una referencia no verificada. */
    public static String format(Integer usd, Boolean estimated) {
        if (usd == null || usd <= 0) {
            return Fmt.ND;
        }
        long v = Math.round(fromUsd(usd));
        String s = (currency() == Currency.PEN ? "S/ " : "$") + String.format(Locale.US, "%,d", v);
        return Boolean.TRUE.equals(estimated) ? "aprox. " + s : s;
    }
}
