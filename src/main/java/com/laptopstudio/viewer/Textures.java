package com.laptopstudio.viewer;

import com.laptopstudio.model.Laptop;
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
final class Textures {

    record Key(KeyCode code, double weight) {
    }

    private static Key k(KeyCode c) {
        return new Key(c, 1);
    }

    private static Key k(KeyCode c, double w) {
        return new Key(c, w);
    }

    static final List<List<Key>> ROWS = List.of(
            List.of(k(KeyCode.ESCAPE), k(KeyCode.F1), k(KeyCode.F2), k(KeyCode.F3), k(KeyCode.F4), k(KeyCode.F5),
                    k(KeyCode.F6), k(KeyCode.F7), k(KeyCode.F8), k(KeyCode.F9), k(KeyCode.F10), k(KeyCode.F11),
                    k(KeyCode.F12), k(KeyCode.DELETE)),
            List.of(k(KeyCode.BACK_QUOTE), k(KeyCode.DIGIT1), k(KeyCode.DIGIT2), k(KeyCode.DIGIT3), k(KeyCode.DIGIT4),
                    k(KeyCode.DIGIT5), k(KeyCode.DIGIT6), k(KeyCode.DIGIT7), k(KeyCode.DIGIT8), k(KeyCode.DIGIT9),
                    k(KeyCode.DIGIT0), k(KeyCode.MINUS), k(KeyCode.EQUALS), k(KeyCode.BACK_SPACE, 1.8)),
            List.of(k(KeyCode.TAB, 1.5), k(KeyCode.Q), k(KeyCode.W), k(KeyCode.E), k(KeyCode.R), k(KeyCode.T),
                    k(KeyCode.Y), k(KeyCode.U), k(KeyCode.I), k(KeyCode.O), k(KeyCode.P), k(KeyCode.OPEN_BRACKET),
                    k(KeyCode.CLOSE_BRACKET), k(KeyCode.BACK_SLASH, 1.3)),
            List.of(k(KeyCode.CAPS, 1.8), k(KeyCode.A), k(KeyCode.S), k(KeyCode.D), k(KeyCode.F), k(KeyCode.G),
                    k(KeyCode.H), k(KeyCode.J), k(KeyCode.K), k(KeyCode.L), k(KeyCode.SEMICOLON), k(KeyCode.QUOTE),
                    k(KeyCode.ENTER, 2.1)),
            List.of(k(KeyCode.SHIFT, 2.3), k(KeyCode.Z), k(KeyCode.X), k(KeyCode.C), k(KeyCode.V), k(KeyCode.B),
                    k(KeyCode.N), k(KeyCode.M), k(KeyCode.COMMA), k(KeyCode.PERIOD), k(KeyCode.SLASH), k(KeyCode.UP, 2.7)),
            List.of(k(KeyCode.CONTROL, 1.3), k(KeyCode.WINDOWS, 1.3), k(KeyCode.ALT, 1.3), k(KeyCode.SPACE, 6.2),
                    k(KeyCode.ALT_GRAPH, 1.3), k(KeyCode.LEFT), k(KeyCode.DOWN), k(KeyCode.RIGHT))
    );

    private Textures() {
    }

    /** Imagen empaquetada en /models/loq (fotografias rectificadas del equipo real). */
    static Image resource(String dir, String name) {
        return new Image(Textures.class.getResource("/models/" + dir + "/" + name).toExternalForm(), 0, 0, true, true, false);
    }

    static Image resource(String name) {
        return new Image(Textures.class.getResource("/models/loq/" + name).toExternalForm(), 0, 0, true, true, false);
    }

    private static Image snap(Canvas c) {
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return c.snapshot(sp, null);
    }

    /** Cubierta C: teclado con teclas que se iluminan, altavoces y trackpad. */
    static Image keyboard(double w, double d, Color deck, Color accent, Set<KeyCode> down) {
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
            List<Key> row = ROWS.get(r);
            double total = row.stream().mapToDouble(Key::weight).sum();
            double gap = kw * 0.009;
            double unit = (kw - gap * (row.size() - 1)) / total;
            double x = kx;
            double y = ky + r * (rowH + rowGap);
            for (Key key : row) {
                double kwid = unit * key.weight();
                boolean on = down != null && down.contains(key.code());
                g.setFill(on ? accent : Color.rgb(20, 23, 28));
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
        return snap(c);
    }

    /** Pantalla de la tapa. Con carga activa muestra una animacion de benchmark. */
    static Image screen(Laptop l, double w, double d, Color accent, boolean stress, double phase) {
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

        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(Color.rgb(220, 228, 238, 0.65));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, ph * 0.045));
        g.fillText(String.valueOf(l.getModel()).toUpperCase(), ix + iw * 0.05, iy + ih * 0.10);
        g.setFill(Color.rgb(174, 182, 194, 0.45));
        g.setFont(Font.font("Consolas", ph * 0.032));
        String meta = fmt(l.getScreenInches()) + " in  " + nz(l.getResolution()) + "  "
                + (l.getRefreshRateHz() == null ? "" : l.getRefreshRateHz() + " Hz  ") + nz(l.getPanelType());
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
        return snap(c);
    }

    /** Logotipo de texto en la tapa exterior (sin marcas registradas, solo el nombre del fabricante). */
    static Image logo(String brand, Color base) {
        int pw = 900, ph = 640;
        Canvas c = new Canvas(pw, ph);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(base);
        g.fillRect(0, 0, pw, ph);
        g.setFill(Color.rgb(255, 255, 255, 0.22));
        g.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 54));
        g.setTextAlign(TextAlignment.LEFT);
        String t = brand.toUpperCase();
        double spacing = 22;
        double total = 0;
        for (char ch : t.toCharArray()) {
            total += 38 + spacing;
        }
        double x = (pw - total) / 2;
        for (char ch : t.toCharArray()) {
            g.fillText(String.valueOf(ch), x, ph * 0.55);
            x += 38 + spacing;
        }
        return snap(c);
    }

    /** Fondo de la base con rejillas de ventilacion y patas de goma. */
    static Image baseBottom(Color base) {
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
        return snap(c);
    }

    /** Sombra de contacto suave. */
    static Image shadow() {
        int s = 512;
        Canvas c = new Canvas(s, s);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0.62)), new Stop(0.45, Color.rgb(0, 0, 0, 0.28)),
                new Stop(1, Color.rgb(0, 0, 0, 0))));
        g.fillRect(0, 0, s, s);
        return snap(c);
    }

    /** Aspas del ventilador sobre fondo transparente. */
    static Image blades() {
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
        return snap(c);
    }

    /** Etiqueta de texto centrada sobre un fondo de color (bateria, chips). */
    static Image label(String text, Color bg, Color fg, int w, int h, double fontSize) {
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(bg);
        g.fillRect(0, 0, w, h);
        g.setFill(fg);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, fontSize));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(text, w / 2.0, h / 2.0 + fontSize * 0.35);
        return snap(c);
    }

    /** Tecla con contorno retroiluminado y leyenda principal y secundaria. */
    static Image keycap(String main, String sub, Color cap, Color glow, int w, int h) {
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
            return snap(c);
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
        return snap(c);
    }

    /** Tecla TUF: cara oscura con contorno naranja retroiluminado; las teclas WASD van rellenas en rojo. */
    static Image keycapTuf(String main, boolean filled, int w, int h) {
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
        return snap(c);
    }

    /**
     * Mapa de normales (relieve) generado a partir de la luminosidad de una foto: lo oscuro (huecos entre teclas,
     * puertos, rejillas) queda hundido y lo claro en relieve.
     */
    static Image reliefFrom(Image src, double strength) {
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

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String fmt(Double v) {
        return v == null ? "" : (v == Math.rint(v) ? String.valueOf(v.intValue()) : String.valueOf(v));
    }
}
