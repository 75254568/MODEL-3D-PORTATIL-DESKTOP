package com.laptopexplorer.viewer;

import com.laptopexplorer.model.AMJLaptop;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.Set;

/** Texturas dibujadas con Canvas: teclado, pantalla, logotipo, rejillas y sombra. */
final class AMJTextures {

    record AMJKey(KeyCode code, double weight) {
    }

    private static AMJKey amjK(KeyCode c) {
        return new AMJKey(c, 1);
    }

    private static AMJKey amjK(KeyCode c, double w) {
        return new AMJKey(c, w);
    }

    static final List<List<AMJKey>> ROWS = List.of(
            List.of(amjK(KeyCode.ESCAPE), amjK(KeyCode.F1), amjK(KeyCode.F2), amjK(KeyCode.F3), amjK(KeyCode.F4), amjK(KeyCode.F5),
                    amjK(KeyCode.F6), amjK(KeyCode.F7), amjK(KeyCode.F8), amjK(KeyCode.F9), amjK(KeyCode.F10), amjK(KeyCode.F11),
                    amjK(KeyCode.F12), amjK(KeyCode.DELETE)),
            List.of(amjK(KeyCode.BACK_QUOTE), amjK(KeyCode.DIGIT1), amjK(KeyCode.DIGIT2), amjK(KeyCode.DIGIT3), amjK(KeyCode.DIGIT4),
                    amjK(KeyCode.DIGIT5), amjK(KeyCode.DIGIT6), amjK(KeyCode.DIGIT7), amjK(KeyCode.DIGIT8), amjK(KeyCode.DIGIT9),
                    amjK(KeyCode.DIGIT0), amjK(KeyCode.MINUS), amjK(KeyCode.EQUALS), amjK(KeyCode.BACK_SPACE, 1.8)),
            List.of(amjK(KeyCode.TAB, 1.5), amjK(KeyCode.Q), amjK(KeyCode.W), amjK(KeyCode.E), amjK(KeyCode.R), amjK(KeyCode.T),
                    amjK(KeyCode.Y), amjK(KeyCode.U), amjK(KeyCode.I), amjK(KeyCode.O), amjK(KeyCode.P), amjK(KeyCode.OPEN_BRACKET),
                    amjK(KeyCode.CLOSE_BRACKET), amjK(KeyCode.BACK_SLASH, 1.3)),
            List.of(amjK(KeyCode.CAPS, 1.8), amjK(KeyCode.A), amjK(KeyCode.S), amjK(KeyCode.D), amjK(KeyCode.F), amjK(KeyCode.G),
                    amjK(KeyCode.H), amjK(KeyCode.J), amjK(KeyCode.K), amjK(KeyCode.L), amjK(KeyCode.SEMICOLON), amjK(KeyCode.QUOTE),
                    amjK(KeyCode.ENTER, 2.1)),
            List.of(amjK(KeyCode.SHIFT, 2.3), amjK(KeyCode.Z), amjK(KeyCode.X), amjK(KeyCode.C), amjK(KeyCode.V), amjK(KeyCode.B),
                    amjK(KeyCode.N), amjK(KeyCode.M), amjK(KeyCode.COMMA), amjK(KeyCode.PERIOD), amjK(KeyCode.SLASH), amjK(KeyCode.UP, 2.7)),
            List.of(amjK(KeyCode.CONTROL, 1.3), amjK(KeyCode.WINDOWS, 1.3), amjK(KeyCode.ALT, 1.3), amjK(KeyCode.SPACE, 6.2),
                    amjK(KeyCode.ALT_GRAPH, 1.3), amjK(KeyCode.LEFT), amjK(KeyCode.DOWN), amjK(KeyCode.RIGHT))
    );

    private AMJTextures() {
    }

    /** Imagen empaquetada en /models/loq (fotografías rectificadas del equipo real). */
    static Image amjResource(String dir, String name) {
        return amjCached("/models/" + dir + "/" + name);
    }

    /**
     * Imágenes del classpath ya decodificadas. Cada modelo usa varias fotos grandes; sin esta cache se volvian a
     * leer y decodificar cada vez que se reconstruia un laptop. SoftReference: la JVM las suelta si falta memoria.
     */
    private static final java.util.Map<String, java.lang.ref.SoftReference<Image>> CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static Image amjCached(String path) {
        java.lang.ref.SoftReference<Image> ref = CACHE.get(path);
        Image img = ref == null ? null : ref.get();
        if (img == null) {
            img = new Image(AMJTextures.class.getResource(path).toExternalForm(), 0, 0, true, true, false);
            CACHE.put(path, new java.lang.ref.SoftReference<>(img));
        }
        return img;
    }

    static Image amjResource(String name) {
        return amjCached("/models/loq/" + name);
    }

    private static Image amjSnap(Canvas c) {
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return c.snapshot(sp, null);
    }

    /**
     * Cubierta C: teclado con teclas que se iluminan, altavoces y trackpad.
     *
     * @param key  color de las teclas
     * @param glow color de la retroiluminación, que asoma entre las teclas; null = sin retroiluminación visible
     */
    static Image amjKeyboard(double w, double d, Color deck, Color accent, Set<KeyCode> down, Color key, Color glow) {
        int pw = 1100;
        int ph = (int) Math.round(pw * d / w);
        Canvas c = new Canvas(pw, ph);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(deck);
        g.fillRect(0, 0, pw, ph);

        // altavoces
        g.setStroke(Color.rgb(0, 0, 0, 0.45));
        g.setLineWidth(1.2);
        for (int side = 0; side < 2; side++) {
            double x0 = side == 0 ? pw * 0.025 : pw * 0.91;
            for (double x = x0; x < x0 + pw * 0.065; x += 5) {
                g.strokeLine(x, ph * 0.08, x, ph * 0.50);
            }
        }

        // teclas
        double kx = pw * 0.11, ky = ph * 0.07, kw = pw * 0.78, kh = ph * 0.47;
        double rowGap = kh * 0.018;
        double rowH = (kh - rowGap * (ROWS.size() - 1)) / ROWS.size();
        for (int r = 0; r < ROWS.size(); r++) {
            List<AMJKey> row = ROWS.get(r);
            double total = row.stream().mapToDouble(AMJKey::weight).sum();
            double gap = kw * 0.009;
            double unit = (kw - gap * (row.size() - 1)) / total;
            double x = kx;
            double y = ky + r * (rowH + rowGap);
            for (AMJKey k : row) {
                double kwid = unit * k.weight();
                boolean on = down != null && down.contains(k.code());
                if (glow != null && !on) {
                    // halo de la retroiluminación: un marco de color algo mayor que la tecla
                    g.setFill(Color.color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0.55));
                    g.fillRoundRect(x - 1.5, y - 1.5, kwid + 3, rowH + 3, 6, 6);
                }
                g.setFill(on ? accent : key);
                g.fillRoundRect(x, y, kwid, rowH, 4, 4);
                g.setStroke(on ? accent.brighter() : Color.rgb(255, 255, 255, 0.07));
                g.setLineWidth(1);
                g.strokeRoundRect(x + 0.5, y + 0.5, kwid - 1, rowH - 1, 4, 4);
                x += kwid + gap;
            }
        }

        // trackpad haptico
        g.setFill(Color.rgb(255, 255, 255, 0.045));
        g.fillRoundRect(pw * 0.35, ph * 0.62, pw * 0.30, ph * 0.26, 10, 10);
        g.setStroke(Color.rgb(255, 255, 255, 0.14));
        g.setLineWidth(1.2);
        g.strokeRoundRect(pw * 0.35, ph * 0.62, pw * 0.30, ph * 0.26, 10, 10);
        return amjSnap(c);
    }

    /**
     * Pantalla de la tapa. Con carga activa muestra una animación de benchmark.
     *
     * @param wallpaper color del fondo de pantalla (el de la serie); null = fondo neutro
     * @param notch     muesca de la cámara en el borde superior
     */
    static Image amjScreen(AMJLaptop l, double w, double d, Color accent, boolean stress, double phase, Color wallpaper,
                        boolean notch) {
        int pw = 900;
        int ph = (int) Math.round(pw * d / w);
        Canvas c = new Canvas(pw, ph);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(Color.rgb(5, 6, 10));
        g.fillRect(0, 0, pw, ph);
        double ix = pw * 0.028, iy = ph * 0.034, iw = pw - 2 * ix, ih = ph - 2 * iy;
        g.setFill(new javafx.scene.paint.LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(24, 34, 46)), new Stop(0.55, Color.rgb(16, 21, 28)),
                new Stop(1, Color.rgb(27, 27, 40))));
        g.fillRect(ix, iy, iw, ih);
        if (wallpaper != null) {
            // fondo de pantalla: dos manchas de luz del color de la serie sobre el fondo oscuro
            Color strong = Color.color(wallpaper.getRed(), wallpaper.getGreen(), wallpaper.getBlue(), 0.80);
            Color soft = Color.color(wallpaper.getRed(), wallpaper.getGreen(), wallpaper.getBlue(), 0.0);
            g.setFill(new RadialGradient(0, 0, ix + iw * 0.72, iy + ih * 0.78, iw * 0.62, false, CycleMethod.NO_CYCLE,
                    new Stop(0, strong), new Stop(1, soft)));
            g.fillRect(ix, iy, iw, ih);
            Color second = wallpaper.deriveColor(48, 1, 1, 0.55);
            g.setFill(new RadialGradient(0, 0, ix + iw * 0.22, iy + ih * 0.95, iw * 0.45, false, CycleMethod.NO_CYCLE,
                    new Stop(0, second), new Stop(1, soft)));
            g.fillRect(ix, iy, iw, ih);
        }
        if (notch) {
            double nw = iw * 0.13, nh = ih * 0.045;
            g.setFill(Color.rgb(5, 6, 10));
            g.fillRoundRect(ix + iw / 2 - nw / 2, iy - nh, nw, nh * 2, nh * 0.9, nh * 0.9);
        }

        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(Color.rgb(220, 228, 238, 0.65));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, ph * 0.045));
        g.fillText(String.valueOf(l.getModel()).toUpperCase(), ix + iw * 0.05, iy + ih * 0.10);
        g.setFill(Color.rgb(174, 182, 194, 0.45));
        g.setFont(Font.font("Consolas", ph * 0.032));
        String meta = amjFmt(l.getScreenInches()) + " in  " + amjNz(l.getResolution()) + "  "
                + (l.getRefreshRateHz() == null ? "" : l.getRefreshRateHz() + " Hz  ") + amjNz(l.getPanelType());
        g.fillText(meta, ix + iw * 0.05, iy + ih * 0.165);

        if (stress) {
            int bars = 14;
            double bw = iw * 0.84 / bars;
            double base = iy + ih * 0.92;
            for (int i = 0; i < bars; i++) {
                double v = 0.5 + 0.5 * Math.sin(phase * 3.1 + i * 0.9) * Math.cos(phase * 1.7 + i * 0.37);
                double bh = ih * 0.40 * (0.15 + 0.85 * Math.abs(v));
                g.setFill(Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.72));
                g.fillRect(ix + iw * 0.08 + i * bw, base - bh, bw * 0.78, bh);
            }
            g.setFill(Color.rgb(207, 161, 93, 0.9));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, ph * 0.04));
            g.fillText("BENCHMARK 100%", ix + iw * 0.05, iy + ih * 0.50);
        }
        return amjSnap(c);
    }

    /**
     * Tapa exterior: color liso, rótulo de texto (sin logotipos registrados, solo el nombre de la marca o la
     * serie) y, si la serie lo tiene, un trazo decorativo.
     *
     * @param text   rótulo; vacío = tapa sin rótulo
     * @param ink    color del rótulo; null = blanco tenue
     * @param corner verdadero para ponerlo pequeño en una esquina; falso, centrado
     * @param accent "slash" (línea diagonal luminosa), "lines" (trazos finos grabados) o null
     * @param glow   color del trazo "slash"
     */
    static Image amjLid(Color base, String text, Color ink, boolean corner, String accent, Color glow) {
        int pw = 900, ph = 640;
        Canvas c = new Canvas(pw, ph);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(base);
        g.fillRect(0, 0, pw, ph);
        boolean light = base.getBrightness() > 0.6;
        Color subtle = light ? Color.rgb(0, 0, 0, 0.16) : Color.rgb(255, 255, 255, 0.14);
        if ("lines".equals(accent)) {
            g.setStroke(subtle);
            g.setLineWidth(1.6);
            g.strokeLine(pw * 0.04, ph * 0.86, pw * 0.70, ph * 0.10);
            g.strokeLine(pw * 0.30, ph * 0.96, pw * 0.96, ph * 0.22);
            g.strokeLine(pw * 0.04, ph * 0.52, pw * 0.96, ph * 0.74);
            g.strokeLine(pw * 0.52, ph * 0.04, pw * 0.80, ph * 0.96);
        } else if ("slash".equals(accent)) {
            Color line = glow == null ? Color.rgb(255, 255, 255, 0.85) : Color.color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0.25);
            g.setStroke(line);
            g.setLineWidth(16);
            g.strokeLine(pw * 0.14, ph * 0.95, pw * 0.86, ph * 0.05);
            g.setStroke(Color.rgb(255, 255, 255, 0.82));
            g.setLineWidth(4);
            g.strokeLine(pw * 0.14, ph * 0.95, pw * 0.86, ph * 0.05);
        }
        if (text != null && !text.isBlank()) {
            g.setFill(ink != null ? ink : light ? Color.rgb(0, 0, 0, 0.30) : Color.rgb(255, 255, 255, 0.24));
            double size = corner ? 30 : 54, advance = corner ? 26 : 60;
            g.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, size));
            g.setTextAlign(TextAlignment.LEFT);
            double total = text.length() * advance;
            double x = corner ? pw * 0.07 : (pw - total) / 2;
            double y = corner ? ph * 0.14 : ph * 0.55;
            for (char ch : text.toCharArray()) {
                g.fillText(String.valueOf(ch), x, y);
                x += advance;
            }
        }
        return amjSnap(c);
    }

    /** Fondo de la base con rejillas de ventilación y patas de goma. */
    static Image amjBaseBottom(Color base) {
        int pw = 900, ph = 640;
        Canvas c = new Canvas(pw, ph);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(base.darker());
        g.fillRect(0, 0, pw, ph);
        g.setStroke(Color.rgb(0, 0, 0, 0.55));
        g.setLineWidth(1.4);
        for (double y = ph * 0.12; y < ph * 0.46; y += 7) {
            g.strokeLine(pw * 0.14, y, pw * 0.86, y);
        }
        g.setFill(Color.rgb(11, 12, 14));
        double[][] feet = {{0.07, 0.06}, {0.84, 0.06}, {0.07, 0.90}, {0.84, 0.90}};
        for (double[] f : feet) {
            g.fillRoundRect(pw * f[0], ph * f[1], pw * 0.09, ph * 0.035, 8, 8);
        }
        return amjSnap(c);
    }

    /** Sombra de contacto suave. */
    static Image amjShadow() {
        int s = 512;
        Canvas c = new Canvas(s, s);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0.62)), new Stop(0.45, Color.rgb(0, 0, 0, 0.28)),
                new Stop(1, Color.rgb(0, 0, 0, 0))));
        g.fillRect(0, 0, s, s);
        return amjSnap(c);
    }

    /** Aspas del ventilador sobre fondo transparente. */
    static Image amjBlades() {
        int s = 400;
        Canvas c = new Canvas(s, s);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(Color.rgb(21, 24, 28));
        g.fillOval(0, 0, s, s);
        g.setFill(Color.rgb(70, 80, 92));
        for (int i = 0; i < 9; i++) {
            g.fillArc(10, 10, s - 20, s - 20, i * 40, 22, ArcType.ROUND);
        }
        g.setFill(Color.rgb(21, 24, 28));
        g.fillOval(s * 0.30, s * 0.30, s * 0.40, s * 0.40);
        g.setFill(Color.rgb(36, 40, 46));
        g.fillOval(s * 0.40, s * 0.40, s * 0.20, s * 0.20);
        return amjSnap(c);
    }

    /** Etiqueta de texto centrada sobre un fondo de color (batería, chips). */
    static Image amjLabel(String text, Color bg, Color fg, int w, int h, double fontSize) {
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(bg);
        g.fillRect(0, 0, w, h);
        g.setFill(fg);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, fontSize));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(text, w / 2.0, h / 2.0 + fontSize * 0.35);
        return amjSnap(c);
    }

    /** Tecla con contorno retroiluminado y leyenda principal y secundaria. */
    static Image amjKeycap(String main, String sub, Color cap, Color glow, int w, int h) {
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(cap);
        g.fillRect(0, 0, w, h);
        double m = Math.min(w, h);
        double inset = m * 0.07;
        g.setFill(cap.deriveColor(0, 1, 0.78, 1));
        g.fillRoundRect(inset, inset, w - 2 * inset, h - 2 * inset, m * 0.22, m * 0.22);
        g.setStroke(glow);
        g.setLineWidth(Math.max(2, m * 0.045));
        g.strokeRoundRect(inset, inset, w - 2 * inset, h - 2 * inset, m * 0.22, m * 0.22);
        g.setFill(glow.deriveColor(0, 1, 1, 0.95));
        if (main.equals("PWR")) {
            double r = m * 0.24;
            g.setStroke(glow);
            g.setLineWidth(m * 0.07);
            g.strokeArc(w / 2.0 - r, h / 2.0 - r, 2 * r, 2 * r, 120, 300, ArcType.OPEN);
            g.strokeLine(w / 2.0, h / 2.0 - r * 1.15, w / 2.0, h / 2.0 - r * 0.1);
            return amjSnap(c);
        }
        g.setTextAlign(TextAlignment.CENTER);
        boolean word = main.length() > 2;
        double fs = word ? m * (main.length() > 4 ? 0.2 : 0.26) : m * 0.46;
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, fs));
        double cy = sub.isEmpty() ? h / 2.0 + fs * 0.35 : h * 0.66 + fs * 0.2;
        g.fillText(main, w / 2.0, cy);
        if (!sub.isEmpty()) {
            g.setFont(Font.font("Segoe UI", FontWeight.NORMAL, m * 0.24));
            g.fillText(sub, w / 2.0, h * 0.30 + m * 0.08);
        }
        return amjSnap(c);
    }

    /** Tecla TUF: cara oscura con contorno naranja retroiluminado; las teclas WASD van rellenas en rojo. */
    static Image amjKeycapTuf(String main, boolean filled, int w, int h) {
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        Color glow = Color.rgb(255, 112, 78);
        g.setFill(filled ? Color.rgb(170, 22, 8) : Color.rgb(20, 20, 22));
        g.fillRect(0, 0, w, h);
        double m = Math.min(w, h);
        double inset = m * 0.06;
        g.setFill(filled ? Color.rgb(205, 34, 14) : Color.rgb(26, 26, 28));
        g.fillRoundRect(inset, inset, w - 2 * inset, h - 2 * inset, m * 0.26, m * 0.26);
        g.setStroke(filled ? Color.rgb(255, 150, 120) : glow);
        g.setLineWidth(Math.max(2, m * 0.05));
        g.strokeRoundRect(inset, inset, w - 2 * inset, h - 2 * inset, m * 0.26, m * 0.26);
        g.setFill(filled ? Color.rgb(255, 230, 220) : glow.deriveColor(0, 1, 1, 0.95));
        g.setTextAlign(TextAlignment.CENTER);
        double fs = main.length() > 3 ? m * 0.22 : main.length() > 1 ? m * 0.30 : m * 0.44;
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, fs));
        g.fillText(main, w / 2.0, h / 2.0 + fs * 0.35);
        return amjSnap(c);
    }

    /**
     * Mapa de normales (relieve) generado a partir de la luminosidad de una foto: lo oscuro (huecos entre teclas,
     * puertos, rejillas) queda hundido y lo claro en relieve.
     */
    static Image amjReliefFrom(Image src, double strength) {
        int w = (int) src.getWidth(), h = (int) src.getHeight();
        if (w < 8 || h < 8) {
            return null;
        }
        int step = Math.max(1, Math.max(w, h) / 1400);
        int ow = w / step, oh = h / step;
        int[] px = new int[w * h];
        src.getPixelReader().getPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, w);
        float[] lum = new float[ow * oh];
        for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
                int c = px[(y * step) * w + (x * step)];
                int a = c >>> 24;
                float l = (0.299f * ((c >> 16) & 255) + 0.587f * ((c >> 8) & 255) + 0.114f * (c & 255)) / 255f;
                lum[y * ow + x] = a < 40 ? 0f : l;
            }
        }
        float[] b = new float[ow * oh];
        for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
                float sum = 0;
                int n = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int xx = Math.min(ow - 1, Math.max(0, x + dx)), yy = Math.min(oh - 1, Math.max(0, y + dy));
                        sum += lum[yy * ow + xx];
                        n++;
                    }
                }
                b[y * ow + x] = sum / n;
            }
        }
        int[] out = new int[ow * oh];
        for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
                float hl = b[y * ow + Math.max(0, x - 1)], hr = b[y * ow + Math.min(ow - 1, x + 1)];
                float hu = b[Math.max(0, y - 1) * ow + x], hd = b[Math.min(oh - 1, y + 1) * ow + x];
                double nx = -(hr - hl) * strength, ny = (hd - hu) * strength, nz = 1.0;
                double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                int r = (int) Math.round((nx / len * 0.5 + 0.5) * 255);
                int g = (int) Math.round((ny / len * 0.5 + 0.5) * 255);
                int bl = (int) Math.round((nz / len * 0.5 + 0.5) * 255);
                out[y * ow + x] = 0xFF000000 | (r << 16) | (g << 8) | bl;
            }
        }
        javafx.scene.image.WritableImage img = new javafx.scene.image.WritableImage(ow, oh);
        img.getPixelWriter().setPixels(0, 0, ow, oh, javafx.scene.image.PixelFormat.getIntArgbInstance(), out, 0, ow);
        return img;
    }

    private static String amjNz(String s) {
        return s == null ? "" : s;
    }

    private static String amjFmt(Double v) {
        return v == null ? "" : (v == Math.rint(v) ? String.valueOf(v.intValue()) : String.valueOf(v));
    }
}
