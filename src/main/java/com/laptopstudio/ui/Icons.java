package com.laptopstudio.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

/** Familia unica de iconos de linea (rejilla de 24 px, trazo de 1,6 px). Se colorean desde el CSS con la clase "icon". */
public final class Icons {

    private Icons() {
    }

    public static final String LIST = "M4 6h16M4 12h16M4 18h10";
    public static final String COMPARE = "M9 4v16M15 4v16M4 9h5M15 15h5";
    public static final String CUBE = "M12 3l8 4.5v9L12 21l-8-4.5v-9zM12 12l8-4.5M12 12v9M12 12L4 7.5";
    public static final String SETTINGS = "M12 9a3 3 0 1 0 0.01 0M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M18.4 5.6l-2.1 2.1M7.7 16.3l-2.1 2.1";
    public static final String SEARCH = "M11 5a6 6 0 1 0 0.01 0M20 20l-4-4";
    public static final String EYE = "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zM12 9a3 3 0 1 0 0.01 0";
    public static final String EYE_OFF = "M3 3l18 18M10.6 6.1A9.6 9.6 0 0 1 12 5c6 0 10 7 10 7a17 17 0 0 1-3.2 3.9M6.3 7.6C3.6 9.4 2 12 2 12s4 7 10 7a9.7 9.7 0 0 0 4.2-1";
    public static final String CLOSE = "M6 6l12 12M18 6L6 18";

    /** Devuelve un icono de {@code size} px a partir de un trazado definido en la rejilla de 24 px. */
    public static Node of(String path, double size) {
        SVGPath p = new SVGPath();
        p.setContent(path);
        p.getStyleClass().add("icon");
        double k = size / 24.0;
        p.setScaleX(k);
        p.setScaleY(k);
        p.setTranslateX(-(24 - size) / 2);
        p.setTranslateY(-(24 - size) / 2);
        Group g = new Group(p);
        g.setMouseTransparent(true);
        return g;
    }
}
