package com.laptopstudio.viewer;

import javafx.scene.paint.Color;

/** Gradiente termico sobrio: azul ahumado en frio, cobre y bronce en caliente. */
final class Heat {

    private static final double[] T = {30, 45, 58, 70, 82, 95};
    private static final Color[] C = {
            Color.rgb(43, 58, 85), Color.rgb(77, 95, 128), Color.rgb(125, 111, 138),
            Color.rgb(176, 122, 90), Color.rgb(207, 154, 93), Color.rgb(224, 192, 138)
    };

    private Heat() {
    }

    static Color at(double t) {
        if (t <= T[0]) {
            return C[0];
        }
        for (int i = 1; i < T.length; i++) {
            if (t <= T[i]) {
                return C[i - 1].interpolate(C[i], (t - T[i - 1]) / (T[i] - T[i - 1]));
            }
        }
        return C[C.length - 1];
    }
}
