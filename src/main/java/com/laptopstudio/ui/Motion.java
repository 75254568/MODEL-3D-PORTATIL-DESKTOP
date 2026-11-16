package com.laptopstudio.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;
import java.util.function.IntFunction;

/**
 * Animaciones de la interfaz, equivalentes a las de la referencia (framer-motion): entrada con fundido y
 * desplazamiento, entrada escalonada de tarjetas, elevacion al pasar el raton y conteo de cifras.
 * Todas son cortas y usan solo opacidad y traslacion, que JavaFX compone en la GPU.
 */
public final class Motion {

    /** Curva de salida suave (rapida al inicio, se frena al final), como el "easeOut" de la referencia. */
    public static final Interpolator EASE_OUT = Interpolator.SPLINE(0.16, 1, 0.3, 1);

    private static final String LIFT_KEY = "motion.lift";

    private Motion() {
    }

    /** Entrada de una pantalla o panel: aparece y sube unos pixeles. */
    public static void enter(Node node) {
        enter(node, 0, 16, 340);
    }

    public static void enter(Node node, double delayMs, double fromY, double millis) {
        node.setOpacity(0);
        node.setTranslateY(fromY);
        FadeTransition fade = new FadeTransition(Duration.millis(millis), node);
        fade.setToValue(1);
        fade.setInterpolator(EASE_OUT);
        TranslateTransition move = new TranslateTransition(Duration.millis(millis), node);
        move.setToY(0);
        move.setInterpolator(EASE_OUT);
        ParallelTransition pt = new ParallelTransition(fade, move);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    /** Entrada escalonada: cada elemento empieza un poco despues que el anterior. */
    public static void stagger(List<? extends Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            // a partir del duodecimo ya estan fuera de la vista: sin retraso extra para no alargar la espera
            enter(nodes.get(i), Math.min(i, 12) * 45.0, 14, 320);
        }
    }

    /** Elevacion al pasar el raton: la tarjeta sube 4 px y gana sombra. Seguro de llamar varias veces. */
    public static void hoverLift(Node node) {
        if (node.getProperties().containsKey(LIFT_KEY)) {
            return;
        }
        node.getProperties().put(LIFT_KEY, Boolean.TRUE);
        DropShadow shadow = new DropShadow(10, 0, 3, Color.rgb(6, 40, 62, 0.10));
        node.setEffect(shadow);
        DoubleProperty t = new SimpleDoubleProperty(0);
        t.addListener((o, a, v) -> {
            double k = v.doubleValue();
            node.setTranslateY(-4 * k);
            shadow.setRadius(10 + 14 * k);
            shadow.setOffsetY(3 + 7 * k);
            shadow.setColor(Color.rgb(6, 40, 62, 0.10 + 0.12 * k));
        });
        Timeline[] running = new Timeline[1];
        Runnable stop = () -> {
            if (running[0] != null) {
                running[0].stop();
            }
        };
        node.setOnMouseEntered(e -> {
            stop.run();
            running[0] = new Timeline(new KeyFrame(Duration.millis(180), new KeyValue(t, 1, EASE_OUT)));
            running[0].play();
        });
        node.setOnMouseExited(e -> {
            stop.run();
            running[0] = new Timeline(new KeyFrame(Duration.millis(220), new KeyValue(t, 0, EASE_OUT)));
            running[0].play();
        });
    }

    /** Cuenta desde 0 hasta {@code target} y escribe cada paso con {@code format}. */
    public static void countUp(Label label, int target, IntFunction<String> format) {
        DoubleProperty v = new SimpleDoubleProperty(0);
        v.addListener((o, a, b) -> label.setText(format.apply((int) Math.round(b.doubleValue()))));
        label.setText(format.apply(0));
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(600), new KeyValue(v, target, EASE_OUT)));
        tl.setOnFinished(e -> label.setText(format.apply(target)));
        tl.play();
    }
}
