package com.laptopstudio.viewer;

import com.laptopstudio.model.Laptop;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Modelo 3D procedural de un portatil, construido con primitivas de JavaFX 3D a partir de las
 * medidas reales de la base de datos. Convenciones: 1 unidad = 1 mm, el eje Y apunta hacia abajo
 * (la parte superior del equipo es Y negativo), el borde delantero esta en Z negativo y la bisagra en Z positivo.
 */
public final class LaptopModel {

    public enum Mode { STUDIO, XRAY, THERMAL }

    public static final double GAP = 46;
    public static final double ZSTEP = 22;

    private static final class Item {
        final Node node;
        final String name;
        final double tier;          // 0 = fijo, 1..5 = capa que se separa al despiezar, -1 = dentro de la tapa
        final double baseY;
        final double baseZ;
        final boolean shell;
        final boolean decal;
        Color base;
        final PhongMaterial mat;
        final Color spec;
        final boolean wire;
        Image map;
        String id;               // identificador de pieza (control de calidad del montaje)
        java.util.List<PhongMaterial> mats;   // piezas con varios materiales (aspas, buje...)
        double xray = 1.0;       // opacidad en rayos X (1 = la de siempre)
        boolean photo;           // foto del interior: se ve entera tambien en rayos X
        boolean zone;            // zona invisible de una pieza (se tine en termico o al seleccionarla)

        Item(Node node, String name, double tier, double baseY, boolean shell, boolean decal,
             Color base, PhongMaterial mat, Color spec) {
            this(node, name, tier, baseY, shell, decal, base, mat, spec, false);
        }

        Item(Node node, String name, double tier, double baseY, boolean shell, boolean decal,
             Color base, PhongMaterial mat, Color spec, boolean wire) {
            this.node = node;
            this.name = name;
            this.tier = tier;
            this.baseY = baseY;
            this.baseZ = node.getTranslateZ();
            this.shell = shell;
            this.decal = decal;
            this.base = base;
            this.mat = mat;
            this.spec = spec;
            this.wire = wire;
        }
    }

    private final Laptop d;
    private final Color tint;          // no nulo solo en la superposicion Ghost
    private final Color accent;
    private final boolean first;

    private final Group root = new Group();
    private final Group innerRoot = new Group();
    private final Group shellRoot = new Group();
    private final Group lidPivot = new Group();
    private final Rotate lidRot = new Rotate(0, Rotate.X_AXIS);
    private final List<Item> items = new ArrayList<>();
    private final List<Rotate> fanRotates = new ArrayList<>();
    private MeshView shadow;

    private final double W, D, T, lt, bodyH, yHinge;
    private double hingeZ;
    private double photoLum = 1.0;
    private boolean below;
    private double lidLen;
    private final Color deckColor;
    private final Set<KeyCode> down = new HashSet<>();
    private final PhongMaterial wireMat = new PhongMaterial(Color.WHITE);
    private PhongMaterial keyboardMat;
    private final java.util.Map<KeyCode, Item> keyItems = new java.util.HashMap<>();
    private PhongMaterial screenMat;

    private Mode mode = Mode.STUDIO;
    private boolean stress;
    private double explode;
    private double lidDeg = 105;
    private String selected;
    private double fanAngle;
    private double screenClock;
    private double screenPhase;

    private double tCpu = 44, tGpu = 41, tRam = 38, tSsd = 37, tBat = 33;

    public LaptopModel(Laptop laptop, Color accent, Color tint, boolean first) {
        this.d = laptop;
        this.accent = accent;
        this.tint = tint;
        this.first = first;
        this.W = nz(laptop.getWidthMm(), 350);
        this.D = nz(laptop.getDepthMm(), 240);
        this.T = nz(laptop.getThicknessMm(), 16);
        this.lt = isZephyr(laptop) ? 5.0 : isObjModel(laptop) ? T - 14.5 : isScar(laptop) ? T - 17.0 : isAlienware(laptop) ? T - 21.0 : isLegion(laptop) ? T - 17.0 : isOmen(laptop) ? T - 18.0 : isMbp(laptop) ? T - 11.8 : isZbook(laptop) ? T - 22.0 : isHelios(laptop) ? T - 22.0 : isHelios16(laptop) ? T - 17.0 : isLoq() ? T - 15.7 : isVictus(laptop) ? T - 18.4 : isDellG16(laptop) ? T - 18.9 : isAorus(laptop) ? T - 16.3 : isBlade18(laptop) ? T - 17.0 : isM18(laptop) ? T - 18.6 : isEon18(laptop) ? T - 22.0 : genLidT(laptop);
        this.bodyH = T - lt;
        this.yHinge = T / 2 - bodyH;
        Color rawDeck = parse(genColor(laptop));
        this.deckColor = rawDeck.getBrightness() > 0.74 ? rawDeck.deriveColor(0, 1, 0.74 / rawDeck.getBrightness(), 1) : rawDeck;
        this.hingeZ = D / 2;
        this.lidLen = D;
        this.photoModel = !(!isObjModel(laptop) && !isZephyr(laptop) && !isEon18(laptop) && !isM18(laptop) && !isBlade18(laptop) && !isAorus(laptop)
                && !isDellG16(laptop) && !isVictus(laptop) && !isHelios16(laptop) && !isHelios(laptop) && !isZbook(laptop) && !isMbp(laptop)
                && !isOmen(laptop) && !isLegion(laptop) && !isAlienware(laptop) && !isScar(laptop) && !isLoq());
        if (isObjModel(laptop)) {
            buildObj();
        } else if (isZephyr(laptop)) {
            buildZephyr();
        } else if (isEon18(laptop)) {
            buildEon18();
        } else if (isM18(laptop)) {
            buildM18();
        } else if (isBlade18(laptop)) {
            buildBlade18();
        } else if (isAorus(laptop)) {
            buildAorus();
        } else if (isDellG16(laptop)) {
            buildDellG16();
        } else if (isVictus(laptop)) {
            buildVictus();
        } else if (isHelios16(laptop)) {
            buildHelios16();
        } else if (isHelios(laptop)) {
            buildHelios();
        } else if (isZbook(laptop)) {
            buildZbook();
        } else if (isMbp(laptop)) {
            buildMbp();
        } else if (isOmen(laptop)) {
            buildOmen();
        } else if (isLegion(laptop)) {
            buildLegion();
        } else if (isAlienware(laptop)) {
            buildAlienware();
        } else if (isScar(laptop)) {
            buildScar();
        } else if (isLoq()) {
            buildLoq();
        } else {
            build();
        }
        applyInsidePhoto();
        root.getChildren().addAll(innerRoot, shellRoot);
        if (shadow != null) {
            root.getChildren().add(0, shadow);
        }
        setLid(lidDeg);
        repaint();
    }

    /* ------------------------------------------------------------------ construccion */

    private static double nz(Double v, double dflt) {
        return v == null || v <= 0 ? dflt : v;
    }

    private static Color parse(String hex) {
        try {
            return Color.web(hex == null ? "#444444" : hex);
        } catch (IllegalArgumentException e) {
            return Color.web("#444444");
        }
    }

    private double y(double z0, double h) {
        return T / 2 - (z0 + h / 2);
    }

    private Item box(Group parent, String name, double tier, boolean shell, double w, double h, double dd,
                     double x, double yy, double z, Color base) {
        Box b = new Box(w, h, dd);
        PhongMaterial m = new PhongMaterial(base);
        Color spec = shell ? Color.gray(0.40) : Color.gray(0.16);
        m.setSpecularColor(spec);
        m.setSpecularPower(shell ? 30 : 12);
        b.setMaterial(m);
        b.setTranslateX(x);
        b.setTranslateY(yy);
        b.setTranslateZ(z);
        b.setUserData(name);
        parent.getChildren().add(b);
        Item it = new Item(b, name, tier, yy, shell, false, base, m, spec);
        items.add(it);
        return it;
    }

    private Item cylinderY(Group parent, String name, double tier, double r, double len,
                           double x, double yy, double z, Color base) {
        Cylinder c = new Cylinder(r, len, 20);
        PhongMaterial m = new PhongMaterial(base);
        Color spec = Color.gray(0.5);
        m.setSpecularColor(spec);
        m.setSpecularPower(60);
        c.setMaterial(m);
        c.setTranslateX(x);
        c.setTranslateY(yy);
        c.setTranslateZ(z);
        c.setUserData(name);
        parent.getChildren().add(c);
        Item it = new Item(c, name, tier, yy, false, false, base, m, spec);
        items.add(it);
        return it;
    }

    private Item cylinderX(Group parent, String name, double tier, boolean shell, double r, double len,
                           double x, double yy, double z, Color base) {
        Cylinder c = new Cylinder(r, len);
        c.setRotationAxis(Rotate.Z_AXIS);
        c.setRotate(90);
        PhongMaterial m = new PhongMaterial(base);
        Color spec = Color.gray(0.45);
        m.setSpecularColor(spec);
        m.setSpecularPower(40);
        c.setMaterial(m);
        c.setTranslateX(x);
        c.setTranslateY(yy);
        c.setTranslateZ(z);
        c.setUserData(name);
        parent.getChildren().add(c);
        Item it = new Item(c, name, tier, yy, shell, false, base, m, spec);
        items.add(it);
        return it;
    }

    /** Cuadrilatero texturizado. p = 4 esquinas (x,y,z) en el orden: arriba-izq, arriba-der, abajo-der, abajo-izq de la imagen. */
    private static MeshView quad(Image img, float[] p) {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(p);
        mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
        MeshView mv = new MeshView(mesh);
        PhongMaterial m = new PhongMaterial(Color.WHITE);
        m.setDiffuseMap(img);
        m.setSpecularColor(Color.gray(0.12));
        mv.setMaterial(m);
        mv.setCullFace(CullFace.NONE);
        return mv;
    }

    private static float[] flat(double x0, double z0, double x1, double z1, double x2, double z2,
                                double x3, double z3, double yy) {
        return new float[]{(float) x0, (float) yy, (float) z0, (float) x1, (float) yy, (float) z1,
                (float) x2, (float) yy, (float) z2, (float) x3, (float) yy, (float) z3};
    }


    /** Radios de las esquinas (mm): delante, detras, tapa arriba, tapa junto a la bisagra, y bisel. */
    private static final double RF = 12, RB = 7, RLT = 12, RLB = 3, BEV = 1.0;

    private void slabR(Group parent, String name, double tier, double w, double dd, double h, double rF, double rB,
                       double x, double yy, double z, Color col) {
        slab(parent, name, tier, w, dd, h, rF, rB, Math.min(BEV, h * 0.4), x, yy, z, col, null, 0, null, 0);
    }

    private void wallR(Group parent, String name, double tier, double w, double dd, double h, double rF, double rB,
                       double t, double x, double yy, double z, Color col) {
        wall(parent, name, tier, w, dd, h, rF, rB, t, x, yy, z, col);
    }

    /** Dentro del contorno redondeado (centrado en el origen), con un margen interior. */
    private static boolean inside(double x, double z, double w, double dd, double rF, double rB, double m) {
        double hw = w / 2 - m, hd = dd / 2 - m;
        double rf = Math.max(0.1, Math.min(rF, Math.min(w, dd) / 2 - 0.1) - m);
        double rb = Math.max(0.1, Math.min(rB, Math.min(w, dd) / 2 - 0.1) - m);
        if (Math.abs(x) > hw || Math.abs(z) > hd) {
            return false;
        }
        if (z < -hd + rf) {
            double cx = hw - rf;
            if (Math.abs(x) > cx) {
                double dx = Math.abs(x) - cx, dz = (-hd + rf) - z;
                return dx * dx + dz * dz <= rf * rf;
            }
        } else if (z > hd - rb) {
            double cx = hw - rb;
            if (Math.abs(x) > cx) {
                double dx = Math.abs(x) - cx, dz = z - (hd - rb);
                return dx * dx + dz * dz <= rb * rb;
            }
        }
        return true;
    }

    /** Recorta las esquinas de las fotos planas para que sigan el contorno redondeado de la carcasa. */
    private void roundDecal(Group parent, MeshView mv, double zOff) {
        if (!(mv.getMesh() instanceof TriangleMesh mesh) || mesh.getPoints().size() != 12
                || !(mv.getMaterial() instanceof PhongMaterial pm) || pm.getDiffuseMap() == null || pm == keyboardMat) {
            return;
        }
        float[] p = new float[12];
        mesh.getPoints().toArray(p);
        Image src = pm.getDiffuseMap();
        int w = (int) src.getWidth(), h = (int) src.getHeight();
        if (w < 2 || h < 2) {
            return;
        }
        boolean flatY = Math.abs(p[1] - p[4]) < 1e-3 && Math.abs(p[1] - p[7]) < 1e-3 && Math.abs(p[1] - p[10]) < 1e-3;
        boolean sideX = Math.abs(p[0] - p[3]) < 1e-3 && Math.abs(p[0] - p[6]) < 1e-3 && Math.abs(Math.abs(p[0]) - W / 2) < 1;
        boolean rear = Math.abs(p[2] - p[5]) < 1e-3 && Math.abs(p[2] - p[8]) < 1e-3 && Math.abs(p[2] - D / 2) < 1;
        double minX = Math.min(Math.min(p[0], p[3]), Math.min(p[6], p[9])), maxX = Math.max(Math.max(p[0], p[3]), Math.max(p[6], p[9]));
        double minZ = Math.min(Math.min(p[2], p[5]), Math.min(p[8], p[11])), maxZ = Math.max(Math.max(p[2], p[5]), Math.max(p[8], p[11]));
        boolean lid = parent == lidPivot;
        double dl = -minZ;
        if (flatY) {
            if (maxX - minX < W - 4 || (lid && maxZ < -1)) {
                return;
            }
        } else if (!(parent == shellRoot && (sideX || rear))) {
            return;
        }
        int[] px = new int[w * h];
        src.getPixelReader().getPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, w);
        for (int j = 0; j < h; j++) {
            double v = (j + 0.5) / h;
            for (int i = 0; i < w; i++) {
                double u = (i + 0.5) / w;
                double x = p[0] + u * (p[3] - p[0]) + v * (p[9] - p[0]);
                double z = p[2] + u * (p[5] - p[2]) + v * (p[11] - p[2]);
                boolean ok;
                if (flatY) {
                    ok = lid ? inside(x, z + dl / 2, W, dl, RLT, RLB, BEV) : inside(x, z + zOff, W, D, RF, RB, BEV);
                } else if (sideX) {
                    double zz = z + zOff;
                    ok = zz > -D / 2 + RF && zz < D / 2 - RB;
                } else {
                    ok = Math.abs(x) < W / 2 - RB;
                }
                if (!ok) {
                    px[j * w + i] = 0;
                }
            }
        }
        javafx.scene.image.WritableImage out = new javafx.scene.image.WritableImage(w, h);
        out.getPixelWriter().setPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, w);
        pm.setDiffuseMap(out);
    }

    private boolean photoModel;
    private boolean insidePhoto;     // el interior sale de una fotografia real (applyInsidePhoto)
    static double reliefStrength = Double.parseDouble(System.getProperty("studio.relief", "6"));

    private Item decal(Group parent, String name, double tier, MeshView mv, double yy, double z) {
        roundDecal(parent, mv, z);
        if (photoModel && ("deck".equals(name) || "base".equals(name)) && reliefStrength > 0
                && mv.getMaterial() instanceof PhongMaterial rm && rm.getDiffuseMap() != null) {
            Image rel = Textures.reliefFrom(rm.getDiffuseMap(), reliefStrength);
            if (rel != null) {
                rm.setBumpMap(rel);
            }
        }
        mv.setUserData(name);
        mv.setTranslateY(yy);
        mv.setTranslateZ(z);
        parent.getChildren().add(mv);
        Item it = new Item(mv, name, tier, yy, false, true, null, (PhongMaterial) mv.getMaterial(), null);
        items.add(it);
        return it;
    }

    private Group wireBox(double w, double h, double dd) {
        Group g = new Group();
        double r = 0.35;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                Cylinder c = new Cylinder(r, dd);
                c.setRotationAxis(Rotate.X_AXIS);
                c.setRotate(90);
                c.setTranslateX(sx * w / 2);
                c.setTranslateY(sy * h / 2);
                c.setMaterial(wireMat);
                g.getChildren().add(c);
            }
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                Cylinder c = new Cylinder(r, h);
                c.setTranslateX(sx * w / 2);
                c.setTranslateZ(sz * dd / 2);
                c.setMaterial(wireMat);
                g.getChildren().add(c);
            }
        }
        for (int sy = -1; sy <= 1; sy += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                Cylinder c = new Cylinder(r, w);
                c.setRotationAxis(Rotate.Z_AXIS);
                c.setRotate(90);
                c.setTranslateY(sy * h / 2);
                c.setTranslateZ(sz * dd / 2);
                c.setMaterial(wireMat);
                g.getChildren().add(c);
            }
        }
        g.setMouseTransparent(true);
        return g;
    }

    private void addWire(Group parent, double tier, double w, double h, double dd, double x, double yy, double z) {
        Group g = wireBox(w, h, dd);
        g.setTranslateX(x);
        g.setTranslateY(yy);
        g.setTranslateZ(z);
        g.setVisible(false);
        parent.getChildren().add(g);
        items.add(new Item(g, "wire", tier, yy, false, true, null, wireMat, null, true));
    }

    /**
     * Modelos genericos con fotografias oficiales del fabricante.
     * {fragmento del modelo, carpeta, luminosidad de las fotos, grosor de la tapa en mm (0 = automatico),
     * [x0, y0, x1, y1 del teclado como fraccion de la cubierta; si no hay keys.csv]}.
     * Si la carpeta tiene keys.csv, las teclas 3D salen de la deteccion sobre la foto real.
     */
    private static final String[][] GEN_PHOTO = {
            {"XPS 16", "gen/xps16", "0.92", "0", "0.101", "0.127", "0.887", "0.552"},
            {"THINKPAD X1 CARBON", "gen/x1c", "0.84", "5.8"},
            {"ZENBOOK 14", "gen/zen14", "0.74", "6.0"},
            {"TITAN 18", "gen/titan18", "0.90", "8.0"},
            {"GRAM 16", "gen/gram16", "0.86", "5.0"},
            {"M16 R2", "gen/m16r2", "0.86", "7.0"},
            {"PH16-72", "gen/ph16_72", "0.86", "8.0"},
            {"ZEPHYRUS G16 (GU605MY)", "gen/zep24", "0.80", "5.0"},
            {"SURFACE LAPTOP 7", "gen/surf7", "0.68", "0"},
            {"OMEN TRANSCEND 16", "gen/omen16", "0.86", "6.0"},
            {"LAPTOP 16", "gen/fw16", "0.68", "0", "0.110", "0.111", "0.890", "0.535"},
            {"MACBOOK AIR 15", "gen/mba15", "0.80", "0"}
    };

    /** Color real del chasis segun las fotos oficiales (la ficha puede traer otro acabado). */
    private static final java.util.Map<String, String> GEN_COLOR = java.util.Map.of(
            "gen/x1c", "#202125",
            "gen/zen14", "#c4c7cd",
            "gen/titan18", "#1d1e22",
            "gen/gram16", "#4b4d52",
            "gen/m16r2", "#3a3b3f",
            "gen/ph16_72", "#1c1e2b",
            "gen/zep24", "#54555a",
            "gen/surf7", "#a9acb2",
            "gen/omen16", "#1f2123",
            "gen/fw16", "#a2a5aa");

    private static String genColor(Laptop l) {
        String m = l.getModel() == null ? "" : l.getModel().toUpperCase(Locale.ROOT);
        for (String[] g : GEN_PHOTO) {
            String c = GEN_COLOR.get(g[1]);
            if (c != null && m.contains(g[0].toUpperCase(Locale.ROOT)) && LaptopModel.class.getResource("/models/" + g[1] + "/deck.jpg") != null) {
                return c;
            }
        }
        return l.getColorHex();
    }

    private static double genLidT(Laptop l) {
        String m = l.getModel() == null ? "" : l.getModel().toUpperCase(Locale.ROOT);
        for (String[] g : GEN_PHOTO) {
            if (m.contains(g[0].toUpperCase(Locale.ROOT)) && Double.parseDouble(g[3]) > 0) {
                return Double.parseDouble(g[3]);
            }
        }
        double t = nz(l.getThicknessMm(), 16);
        return Math.max(2.6, Math.min(6.5, t * 0.26));
    }

    private String[] genPhoto() {
        String m = d.getModel() == null ? "" : d.getModel().toUpperCase(Locale.ROOT);
        for (String[] g : GEN_PHOTO) {
            if (m.contains(g[0].toUpperCase(Locale.ROOT)) && (hasRes(g[1], "deck.jpg") || hasRes(g[1], "screen.jpg")
                    || hasRes(g[1], "lid.jpg") || hasRes(g[1], "left.png") || hasRes(g[1], "right.png"))) {
                return g;
            }
        }
        return null;
    }

    private static boolean hasRes(String dir, String file) {
        return LaptopModel.class.getResource("/models/" + dir + "/" + file) != null;
    }

    private void build() {
        double s = W / 354.0;
        double hB = bodyH * 0.30, hD = bodyH * 0.24, hZ = bodyH - hB - hD;
        boolean ghost = tint != null;
        final String[] gp = genPhoto();
        final boolean gDeck = gp != null && hasRes(gp[1], "deck.jpg");
        final boolean gScreen = gp != null && hasRes(gp[1], "screen.jpg");
        if (gp != null) {
            photoModel = gDeck;
            photoLum = Double.parseDouble(gp[2]);
        }

        // sombra de contacto
        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // tier 0: chasis inferior D
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, deckColor);
        double wt = 2.2;
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, deckColor);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        boolean flanks = gp != null && (hasRes(gp[1], "left.png") || hasRes(gp[1], "right.png"));
        if (!ghost) {
            double yTopB = T / 2 - bodyH, yBotB = T / 2;
            if (gp != null && hasRes(gp[1], "bottom.jpg")) {
                decal(shellRoot, "base", 0, quad(Textures.resource(gp[1], "bottom.jpg"),
                        flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), T / 2 + 0.08, 0);
            } else {
                Image bottom = Textures.baseBottom(deckColor);
                decal(shellRoot, "base", 0, quad(bottom, flat(-W / 2, D / 2, W / 2, D / 2, W / 2, -D / 2, -W / 2, -D / 2, 0)),
                        T / 2 + 0.08, 0);
            }
            if (gp != null && hasRes(gp[1], "rear.png")) {
                decal(shellRoot, "base", 0, quad(Textures.resource(gp[1], "rear.png"),
                        pts(W / 2, yTopB, D / 2 + 0.1, -W / 2, yTopB, D / 2 + 0.1, -W / 2, yBotB, D / 2 + 0.1, W / 2, yBotB, D / 2 + 0.1)), 0, 0);
            }
            if (gp != null && hasRes(gp[1], "right.png")) {
                decal(shellRoot, "base", 0, quad(Textures.resource(gp[1], "right.png"),
                        pts(W / 2 + 0.1, yTopB, -D / 2, W / 2 + 0.1, yTopB, D / 2, W / 2 + 0.1, yBotB, D / 2, W / 2 + 0.1, yBotB, -D / 2)), 0, 0);
            }
            if (gp != null && hasRes(gp[1], "left.png")) {
                decal(shellRoot, "base", 0, quad(Textures.resource(gp[1], "left.png"),
                        pts(-W / 2 - 0.1, yTopB, D / 2, -W / 2 - 0.1, yTopB, -D / 2, -W / 2 - 0.1, yBotB, -D / 2, -W / 2 - 0.1, yBotB, D / 2)), 0, 0);
            }
        }

        // tier 1: bateria
        double batW = W * 0.66, batD = D * 0.30, batH = hZ * 0.80;
        double batY = y(hB + hZ * 0.05, batH);
        double batZ = -(D / 2 - batD / 2 - D * 0.07);
        box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
        if (!ghost) {
            Image lab = Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 600, 220, 54);
            double yy = batY - batH / 2 - 0.08;
            decal(innerRoot, "battery", 1, quad(lab, flat(-batW / 2, batD / 2, batW / 2, batD / 2, batW / 2, -batD / 2, -batW / 2, -batD / 2, 0)),
                    yy, batZ);
        }

        // tier 2: placa base y silicio
        double pcbY0 = hB + hZ * 0.08, pcbH = 1.4;
        double pcbZ = D * 0.20;
        box(innerRoot, "pcb", 2, false, W * 0.90, pcbH, D * 0.50, 0, y(pcbY0, pcbH), pcbZ, Color.rgb(24, 52, 42));
        double chipTop = pcbY0 + pcbH + 0.05;
        double chipH = Math.max(1.2, Math.min(hZ * 0.26, 3));
        double cx = -0.13 * W, gx = 0.13 * W, cz = 0.28 * D;
        chip("cpu", 28 * s, 28 * s, cx, cz, Color.rgb(94, 122, 150), chipTop, chipH);
        boolean dedicated = nzI(d.getGpuVramGb()) > 0 || nzI(d.getGpuTgpWatts()) > 0;
        if (dedicated) {
            chip("gpu", 34 * s, 34 * s, gx, cz, Color.rgb(64, 74, 92), chipTop, chipH);
            double[][] vr = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] p : vr) {
                chip("gpu", 12 * s, 14 * s, gx + p[0] * 27 * s, cz + p[1] * 18 * s, Color.rgb(43, 47, 56), chipTop, chipH * 0.8);
            }
        } else {
            chip("gpu", 16 * s, 16 * s, cx + 24 * s, cz, Color.rgb(74, 88, 108), chipTop, chipH);
        }
        if (Boolean.TRUE.equals(d.getRamUpgradable())) {
            chip("ram", 68 * s, 30 * s, -0.30 * W, 0.08 * D, Color.rgb(59, 63, 74), chipTop, chipH);
            chip("ram", 68 * s, 30 * s, -0.30 * W, 0.08 * D + 34 * s, Color.rgb(59, 63, 74), chipTop, chipH);
        } else {
            double[][] rp = {{-0.32, 0.14}, {-0.32, 0.30}, {-0.25, 0.14}, {-0.25, 0.30}};
            for (double[] p : rp) {
                chip("ram", 14 * s, 14 * s, p[0] * W, p[1] * D, Color.rgb(59, 63, 74), chipTop, chipH);
            }
        }
        int slots = Math.max(1, Math.min(3, d.getStorageSlots() == null ? 1 : d.getStorageSlots()));
        for (int i = 0; i < slots; i++) {
            chip("ssd", 80 * s, 22 * s, 0.30 * W, 0.04 * D + i * 26 * s, Color.rgb(47, 58, 72), chipTop, chipH);
        }

        // tier 3: refrigeracion (cobre, aluminio y dos ventiladores)
        double cy0 = hB + hZ * 0.55, cH = Math.max(1.4, hZ * 0.22);
        Color copper = Color.rgb(168, 113, 74), alu = Color.rgb(138, 143, 150);
        coolBox(42 * s, 42 * s, cx, cz, alu, cy0, cH);
        coolBox(52 * s, 52 * s, gx, cz, alu, cy0, cH);
        double pr = Math.max(1.0, 2.4 * s);
        double pipeY = y(cy0, cH);
        cylinderX(innerRoot, "cooling", 3, false, pr, W * 0.21, -0.235 * W, pipeY, cz + 3 * s, copper);
        cylinderX(innerRoot, "cooling", 3, false, pr, W * 0.21, 0.235 * W, pipeY, cz + 3 * s, copper);
        cylinderX(innerRoot, "cooling", 3, false, pr, W * 0.26, 0, pipeY, cz - 10 * s, copper);
        cylinderX(innerRoot, "cooling", 3, false, pr, W * 0.26, 0, pipeY, cz - 20 * s, copper);
        double fanD = D * 0.26;
        for (double fx : new double[]{-0.34 * W, 0.34 * W}) {
            coolBox(fanD, fanD, fx, cz, Color.rgb(30, 33, 38), cy0 - 0.2, cH);
            if (!ghost) {
                double top = y(cy0 - 0.2, cH) - cH / 2 - 0.1;
                Group g = new Group();
                MeshView blades = quad(Textures.blades(), flat(-fanD * 0.46, -fanD * 0.46, fanD * 0.46, -fanD * 0.46,
                        fanD * 0.46, fanD * 0.46, -fanD * 0.46, fanD * 0.46, 0));
                Rotate spin = new Rotate(0, Rotate.Y_AXIS);
                g.getTransforms().add(spin);
                g.getChildren().add(blades);
                fanRotates.add(spin);
                Group holder = new Group(g);
                holder.setTranslateX(fx);
                holder.setTranslateZ(cz);
                holder.setTranslateY(top);
                holder.setUserData("cooling");
                g.setUserData("cooling");
                blades.setUserData("cooling");
                innerRoot.getChildren().add(holder);
                items.add(new Item(holder, "cooling", 3, top, false, true, null, (PhongMaterial) blades.getMaterial(), null));
            }
        }

        // tier 4: cubierta C con teclado
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, D, hD, RF, RB, 0, deckY, 0, deckColor);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            double yy = T / 2 - bodyH - 0.08;
            Image kb = Textures.keyboard(W, D, deckColor, accent, down);
            MeshView mv = quad(kb, flat(-W / 2, D / 2, W / 2, D / 2, W / 2, -D / 2, -W / 2, -D / 2, 0));
            if (gDeck) {
                Image deckImg = Textures.resource(gp[1], "deck.jpg");
                decal(shellRoot, "deck", 4, quad(deckImg, flat(-W / 2, D / 2, W / 2, D / 2, W / 2, -D / 2, -W / 2, -D / 2, 0)), yy, 0);
                if (hasRes(gp[1], "keys.csv")) {
                    addKeyboardAtlas(gp[1], D, T / 2 - bodyH, Math.max(1.3, Math.min(2.2, bodyH * 0.17)));
                } else if (gp.length >= 8) {
                    addKeysGenericPhoto(deckImg, Double.parseDouble(gp[4]), Double.parseDouble(gp[5]), Double.parseDouble(gp[6]),
                            Double.parseDouble(gp[7]), T / 2 - bodyH);
                }
            } else {
                keyboardMat = (PhongMaterial) mv.getMaterial();
                decal(shellRoot, "deck", 4, mv, yy, 0);
                addKeysGeneric(T / 2 - bodyH);
            }
            if (!flanks) {
                addPortsGeneric(T / 2 - bodyH, bodyH);
            }
        }

        // tier 5: tapa con pantalla, pivotando sobre la bisagra trasera
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(D / 2);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, D, lt, RLT, RLB, 0, -lt / 2, -D / 2, deckColor);
        addWireOutline(lidPivot, -1, W, D, RLT, RLB, lt, 0, -lt / 2, -D / 2);
        cylinderX(lidPivot, "hinge", -1, false, lt * 0.5, W * 0.72, 0, -lt * 0.5, 0, Color.rgb(40, 42, 46));
        if (!ghost) {
            double sw = W * 0.97, sd = D * 0.97;
            Image scr = Textures.screen(d, sw, sd, accent, false, 0);
            MeshView mv = quad(scr, flat(-sw / 2, -D * 0.985, sw / 2, -D * 0.985, sw / 2, -D * 0.015, -sw / 2, -D * 0.015, 0));
            screenMat = (PhongMaterial) mv.getMaterial();
            if (gScreen) {
                mv = quad(Textures.resource(gp[1], "screen.jpg"), flat(-W / 2, -D, W / 2, -D, W / 2, 0, -W / 2, 0, 0));
                screenMat = null;
                decal(lidPivot, "display", -1, mv, 0.08, 0);
            } else {
                decal(lidPivot, "display", -1, mv, 0.08, 0);
            }
            cylinderY(lidPivot, "lid", -1, 1.6, 0.3, 0, 0.12, -D * 0.99 + 1.2, Color.rgb(30, 30, 34));
            cylinderY(lidPivot, "lid", -1, 0.85, 0.4, 0, 0.18, -D * 0.99 + 1.2, Color.rgb(6, 8, 16));
            if (gp != null && hasRes(gp[1], "lid.jpg")) {
                decal(lidPivot, "lid", -1, quad(Textures.resource(gp[1], "lid.jpg"),
                        flat(W / 2, -D, -W / 2, -D, -W / 2, 0, W / 2, 0, 0)), -lt - 0.08, 0);
            } else {
                Image logo = Textures.logo(String.valueOf(d.getBrand()), deckColor);
                decal(lidPivot, "lid", -1, quad(logo, flat(W / 2, -D, -W / 2, -D, -W / 2, 0, W / 2, 0, 0)), -lt - 0.08, 0);
            }
        }
    }

    /** Teclas 3D del modelo generico: misma distribucion que la textura, con paredes inclinadas y color propio. */
    private void addKeysGeneric(double topY) {
        double kx = W * 0.11, kwTotal = W * 0.78, ky = D * 0.07, kh = D * 0.47;
        double rowGap = kh * 0.018;
        int nRows = Textures.ROWS.size();
        double rowH = (kh - rowGap * (nRows - 1)) / nRows;
        double keyH = Math.max(1.4, Math.min(2.2, bodyH * 0.16));
        for (int r = 0; r < nRows; r++) {
            java.util.List<Textures.Key> row = Textures.ROWS.get(r);
            double total = row.stream().mapToDouble(Textures.Key::weight).sum();
            double gap = kwTotal * 0.009;
            double unit = (kwTotal - gap * (row.size() - 1)) / total;
            double x = kx;
            double z = D / 2 - (ky + r * (rowH + rowGap) + rowH / 2);
            for (Textures.Key key : row) {
                double w = unit * key.weight();
                double cx = -W / 2 + x + w / 2;
                double rad = Math.min(2.2, Math.min(w, rowH) * 0.22);
                partItem(shellRoot, Shapes.keycapBody(w, rowH, keyH, 0.7, rad), "deck", 4, false, Color.rgb(14, 16, 20), null, cx, topY - keyH / 2, z);
                partItem(shellRoot, Shapes.keycapTop(w, rowH, keyH, 0.7, rad), "deck", 4, false, Color.rgb(30, 33, 40), null, cx, topY - keyH / 2, z);
                keyItems.put(key.code(), items.get(items.size() - 1));
                x += w + gap;
            }
        }
    }

    /** Teclas 3D sobre la foto oficial: distribucion generica ajustada al recuadro del teclado, cada tecla con su recorte. */
    private void addKeysGenericPhoto(Image deckImg, double fx0, double fy0, double fx1, double fy1, double topY) {
        int iw = (int) deckImg.getWidth(), ih = (int) deckImg.getHeight();
        double kx = fx0 * W, kwTotal = (fx1 - fx0) * W, ky = fy0 * D, kh = (fy1 - fy0) * D;
        double rowGap = kh * 0.022;
        int nRows = Textures.ROWS.size();
        double rowH = (kh - rowGap * (nRows - 1)) / nRows;
        double keyH = Math.max(1.4, Math.min(2.0, bodyH * 0.14));
        for (int r = 0; r < nRows; r++) {
            java.util.List<Textures.Key> row = Textures.ROWS.get(r);
            double total = row.stream().mapToDouble(Textures.Key::weight).sum();
            double gap = kwTotal * 0.010;
            double unit = (kwTotal - gap * (row.size() - 1)) / total;
            double x = kx;
            double zc = D / 2 - (ky + r * (rowH + rowGap) + rowH / 2);
            for (Textures.Key key : row) {
                double w = unit * key.weight();
                double cx = -W / 2 + x + w / 2;
                int px = (int) Math.max(0, Math.round((cx + W / 2 - w / 2) / W * iw)), py = (int) Math.max(0, Math.round((D / 2 - zc - rowH / 2) / D * ih));
                int pw = (int) Math.max(4, Math.min(iw - px, Math.round(w / W * iw))), ph = (int) Math.max(4, Math.min(ih - py, Math.round(rowH / D * ih)));
                Image tile = new javafx.scene.image.WritableImage(deckImg.getPixelReader(), px, py, pw, ph);
                double rad = Math.min(2.0, Math.min(w, rowH) * 0.2);
                partItem(shellRoot, Shapes.keycapBody(w, rowH, keyH, 0.6, rad), "deck", 4, false, Color.rgb(16, 17, 20), null, cx, topY - keyH / 2, zc);
                partItem(shellRoot, Shapes.keycapTop(w, rowH, keyH, 0.6, rad), "deck", 4, false, Color.WHITE, tile, cx, topY - keyH / 2, zc);
                keyItems.put(key.code(), items.get(items.size() - 1));
                x += w + gap;
            }
        }
    }

    /** Puertos estimados segun la marca y el tipo de equipo (sin fotografias del modelo). */
    private void addPortsGeneric(double deckTop, double bodyFlat) {
        double yc = deckTop + bodyFlat * 0.45;
        String brand = String.valueOf(d.getBrand()).toLowerCase(Locale.ROOT);
        boolean gaming = "Gaming".equalsIgnoreCase(d.getCategory()) || bodyFlat > 17;
        if (brand.contains("framework")) {
            // 6 ranuras de modulos de expansion (3 por lado), con sus tarjetas USB-C al ras
            for (int s = -1; s <= 1; s += 2) {
                for (int i = 0; i < 3; i++) {
                    tufRect(s, D / 2 - (0.30 + 0.17 * i) * D, yc, 17.0, 5.6, Color.rgb(176, 178, 182));
                }
            }
            return;
        }
        if (brand.contains("apple")) {
            tufRect(-1, D / 2 - 0.30 * D, yc, 7.5, 2.6, null);
            tufStadium(-1, D / 2 - 0.42 * D, yc, 8.6, 3.2);
            tufStadium(-1, D / 2 - 0.54 * D, yc, 8.6, 3.2);
            cylinderX(shellRoot, "base", 0, false, 2.5, 0.5, W / 2 + 0.25, yc, D / 2 - 0.40 * D, Color.rgb(3, 3, 5));
            return;
        }
        if (gaming) {
            tufRect(-1, D / 2 - 0.12 * D, yc, 8.0, 4.2, null);
            tufRect(-1, D / 2 - 0.22 * D, yc, 15.0, 5.4, null);
            tufStadium(-1, D / 2 - 0.34 * D, yc, 9.0, 3.6);
            tufRect(-1, D / 2 - 0.46 * D, yc, 13.2, 5.2, Color.rgb(60, 62, 70));
            tufRect(1, D / 2 - 0.16 * D, yc, 13.2, 5.2, Color.rgb(60, 62, 70));
            tufRect(1, D / 2 - 0.28 * D, yc, 13.2, 5.2, Color.rgb(60, 62, 70));
            cylinderX(shellRoot, "base", 0, false, 2.6, 0.5, W / 2 + 0.25, yc, D / 2 - 0.42 * D, Color.rgb(3, 3, 5));
            return;
        }
        tufStadium(-1, D / 2 - 0.20 * D, yc, 8.8, 3.3);
        tufStadium(-1, D / 2 - 0.32 * D, yc, 8.8, 3.3);
        tufRect(1, D / 2 - 0.22 * D, yc, 13.0, 5.0, Color.rgb(60, 62, 70));
        cylinderX(shellRoot, "base", 0, false, 2.5, 0.5, W / 2 + 0.25, yc, D / 2 - 0.36 * D, Color.rgb(3, 3, 5));
    }

    /* ------------------------------------------------------------------ Lenovo LOQ 15AHP10 (con fotografias reales) */

    private boolean isLoq() {
        return d.getModel() != null && d.getModel().toUpperCase(Locale.ROOT).contains("LOQ");
    }

    private static float[] pts(double x0, double y0, double z0, double x1, double y1, double z1,
                               double x2, double y2, double z2, double x3, double y3, double z3) {
        return new float[]{(float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1,
                (float) x2, (float) y2, (float) z2, (float) x3, (float) y3, (float) z3};
    }

    /** Pieza interna: z0 es la altura sobre la cara inferior del equipo. */
    private void comp(String name, double tier, double w, double h, double dd, double x, double z0, double z, Color c) {
        box(innerRoot, name, tier, false, w, h, dd, x, y(z0, h), z, c);
    }

    private void buildLoq() {
        boolean ghost = tint != null;
        double wt = 2.2;
        double hB = 3.6, hD = 2.8, hZ = bodyH - hB - hD;
        double shelf = 22.5;                       // repisa trasera detras de las bisagras
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;                     // la tapa mide lo mismo que la cubierta C
        lidLen = Dl;
        photoLum = 0.80;
        final double lt = 6.2;                     // grosor real de la tapa; el cuerpo mide 15,7 mm
        Color shellCol = deckColor.deriveColor(0, 1, 0.86, 1);
        Color baseCol = deckColor.deriveColor(0, 1, 0.70, 1);

        // cuna de la ficha: 21,9 mm delante y 23,9 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 2.0 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior D con paredes y fotos de la base, la trasera y el flanco derecho
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shellCol);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - (hB + hZ + hD);
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("bottom.png"),
                    flat(-W / 2, D / 2, W / 2, D / 2, W / 2, -D / 2, -W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("rear.jpg"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot - 3.0, D / 2 + 0.1, W / 2, yBot - 3.0, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("flank.png"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("flank_left.png"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
            for (double fx : new double[]{-W / 2 + 30, W / 2 - 30}) {
                for (double fz : new double[]{-D / 2 + 26, D / 2 - 26}) {
                    slabR(shellRoot, "base", 0, 22, 14, 2.4, 5, 5, fx, T / 2 + 1.2, fz, Color.rgb(34, 34, 37));
                }
            }
        }

        if (!ghost) {
            addPortsLoq(T / 2 - bodyH);
        }

        // El interior sigue la foto real del equipo abierto (bateria delante, ventiladores y tubos detras).
        // Conversion: la caja interior mide ~1195 x 780 px en la foto => 3,31 px/mm en X y 3,01 px/mm en Z.
        if (!ghost && !loadParts("in/loq15").isEmpty()) {
            // interior por piezas (parts.csv): foto cenital del equipo exacto + manual HMM + lista oficial de repuestos
            partsDir = "in/loq15";
            partsFloor = hB;
            partsCeil = hB + hZ;
            partsWall = wt;
            legion10Interior();
        } else {
        // ---- tier 1: bateria L24B4PK4 de 60 Wh (318 x 78 mm segun la foto) y los dos altavoces laterales
        double batW = 318, batD = 78, batH = Math.min(6.8, hZ - 1.0);
        double batZ = -63.7;
        double batY = y(hB + 0.4, batH);
        box(innerRoot, "battery", 1, false, batW, batH, batD, 4, batY, batZ, Color.rgb(24, 26, 30));
        if (!ghost) {
            double lw = batW * 0.94, ld = lw / 4.98;
            decal(innerRoot, "battery", 1, quad(Textures.resource("battery.jpg"),
                    flat(-lw / 2, ld / 2, lw / 2, ld / 2, lw / 2, -ld / 2, -lw / 2, -ld / 2, 0)), batY - batH / 2 - 0.08, batZ).node.setTranslateX(4);
        }
        comp("pcb", 1, 10, 6, 28, -166, hB + 0.4, batZ + 2, Color.rgb(15, 16, 18));
        comp("pcb", 1, 10, 6, 28, 167, hB + 0.4, batZ + 2, Color.rgb(15, 16, 18));

        // ---- tier 2: placa base azul, soporte metalico derecho, escudo de RAM, SSD y tarjeta Wi-Fi
        double pcbZ0 = hB + 0.4, pcbH = 1.4;
        comp("pcb", 2, 250, pcbH, 148, -22, pcbZ0, 51, Color.rgb(24, 62, 98));
        comp("pcb", 2, 58, 0.8, 140, 140, pcbZ0, 51, Color.rgb(120, 124, 130));
        double top = pcbZ0 + pcbH + 0.05;
        double cH = 2.2;
        comp("cpu", 2, 24, cH, 24, -22, top, 92, Color.rgb(170, 175, 180));
        comp("gpu", 2, 28, cH, 28, 40, top, 95, Color.rgb(38, 41, 47));
        double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
        for (double[] v : vram) {
            comp("gpu", 2, 11, cH * 0.8, 12, 40 + v[0] * 24, top, 95 + v[1] * 14, Color.rgb(26, 28, 32));
        }
        comp("ram", 2, 78, 4.6, 72, 30, top, 11, Color.rgb(140, 136, 124));
        comp("ssd", 2, 38, cH, 21, 97, top, 1, Color.rgb(47, 58, 72));
        comp("pcb", 2, 33, 1.6, 22, -108, top, -14, Color.rgb(184, 186, 190));

        // ---- tier 3: modulo termico real (foto del disipador) con las dos turbinas girando
        double fanY = pcbZ0 + 0.1;
        comp("cooling", 3, 58, 8.4, 58, -105, fanY, 66, Color.rgb(24, 26, 30));
        comp("cooling", 3, 58, 8.4, 58, 114, fanY, 66, Color.rgb(24, 26, 30));
        comp("cooling", 3, 70, 4.0, 62, 6, fanY, 98, Color.rgb(16, 16, 18));
        comp("cooling", 3, 64, 9.0, 10, -128, fanY, 119, Color.rgb(190, 122, 72));
        comp("cooling", 3, 64, 9.0, 10, 128, fanY, 119, Color.rgb(190, 122, 72));
        double pipeY = y(fanY + 4.0, 6.0);
        cylinderX(innerRoot, "cooling", 3, false, 3.0, 210, 5, pipeY, 108, Color.rgb(14, 14, 15));
        cylinderX(innerRoot, "cooling", 3, false, 3.0, 150, 5, pipeY, 100, Color.rgb(14, 14, 15));
        cylinderX(innerRoot, "cooling", 3, false, 3.0, 90, 5, pipeY, 92, Color.rgb(14, 14, 15));
        if (!ghost) {
            double coolTop = y(fanY + 8.4, 0.0) - 0.06;
            double mw = 307, mh = 84;
            decal(innerRoot, "cooling", 3, quad(Textures.resource("cooling.png"),
                    flat(-mw / 2, mh / 2, mw / 2, mh / 2, mw / 2, -mh / 2, -mw / 2, -mh / 2, 0)), coolTop, 77.5).node.setTranslateX(1.4);
            for (double fx : new double[]{-105, 114}) {
                double topY = y(fanY, 8.4) - 4.3;
                double r = 26;
                Group g = new Group();
                MeshView blades = quad(Textures.blades(), flat(-r, -r, r, -r, r, r, -r, r, 0));
                Rotate spin = new Rotate(0, Rotate.Y_AXIS);
                g.getTransforms().add(spin);
                g.getChildren().add(blades);
                fanRotates.add(spin);
                Group holder = new Group(g);
                holder.setTranslateX(fx);
                holder.setTranslateZ(66);
                holder.setTranslateY(topY);
                holder.setUserData("cooling");
                g.setUserData("cooling");
                blades.setUserData("cooling");
                innerRoot.getChildren().add(holder);
                items.add(new Item(holder, "cooling", 3, topY, false, true, null, (PhongMaterial) blades.getMaterial(), null));
            }
        }
        }

        // ---- tier 4: cubierta C (foto real rectificada) con teclado 3D, boton de encendido y repisa trasera
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shellCol);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shellCol);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyH - 0.08, deckCz);
            addKeyboardLoq(T / 2 - bodyH);
            addPowerRingLoq(T / 2 - bodyH, -D / 2 + 214.9, shellCol);
            addTrackpadLoq(T / 2 - bodyH, shellCol);
        }

        // ---- tier 5: tapa con la pantalla real, modulo de camara y bisagras expuestas
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lt, RLT, RLB, 0, -lt / 2, -Dl / 2, shellCol);
        partItem(lidPivot, Shapes.prism(new double[][]{{-52, 0.2}, {52, 0.2}, {44, -4.7}, {-44, -4.7}}, lt), "lid", -1, true,
                Color.rgb(56, 56, 60), null, 0, -lt / 2, -Dl);
        if (!ghost) {
            cylinderY(lidPivot, "lid", -1, 3.0, 0.3, 0, 0.12, -Dl - 2.4, Color.rgb(54, 54, 58));
            cylinderY(lidPivot, "lid", -1, 1.6, 0.4, 0, 0.18, -Dl - 2.4, Color.rgb(6, 8, 16));
        }
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lt, 0, -lt / 2, -Dl / 2);
        for (double hx : new double[]{-136.4, 136.4}) {
            slabR(lidPivot, "hinge", -1, 29, 11, lt + 1.0, 2.0, 2.0, hx, -(lt + 1.0) / 2 + 0.6, -5.5, Color.rgb(138, 141, 146));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("screen.png"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "display", -1, quad(Textures.resource("bump.png"),
                    flat(-50, -(Dl + 4.7 - 0.2), 50, -(Dl + 4.7 - 0.2), 50, -Dl, -50, -Dl, 0)), 0.1, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lt - 0.08, 0);
        }
    }

    /** Boton de encendido de la LOQ: aro blanco en relieve sobre un disco gris con junta oscura. */
    private void addPowerRingLoq(double topY, double z, Color shellCol) {
        partItem(shellRoot, Shapes.torus(9.0, 0.45, 40, 8), "deck", 4, false, Color.rgb(40, 41, 44), null, 0, topY - 0.15, z);
        cylinderY(shellRoot, "deck", 4, 8.6, 0.5, 0, topY - 0.1, z, Color.rgb(46, 47, 51));
        cylinderY(shellRoot, "deck", 4, 7.5, 0.9, 0, topY - 0.3, z, shellCol.deriveColor(0, 1, 1.12, 1));
        partItem(shellRoot, Shapes.torus(6.7, 0.85, 40, 10), "deck", 4, false, Color.rgb(246, 247, 250), null, 0, topY - 0.85, z);
        cylinderY(shellRoot, "deck", 4, 5.3, 0.7, 0, topY - 0.7, z, shellCol.deriveColor(0, 1, 1.02, 1));
        box(shellRoot, "deck", 4, false, 1.6, 0.9, 1.4, 4.8, topY - 0.85, z + 4.8, shellCol.deriveColor(0, 1, 1.02, 1));
    }

    /** Borde en relieve del touchpad (75 x 120 mm, esquinas redondeadas). */
    private void addTrackpadLoq(double topY, Color shellCol) {
        double tz = -D / 2 + 42.6;
        partItem(shellRoot, Shapes.ring(120.0, 75.5, 0.5, 4.0, 4.0, 0.7), "deck", 4, false, shellCol.deriveColor(0, 1, 0.72, 1), null,
                -42.6, topY - 0.25, tz);
    }

    private void portBox(boolean rear, double u, double yc, double off, double depth, double w, double h, Color c) {
        double face = rear ? D / 2 : W / 2;
        if (rear) {
            box(shellRoot, "base", 0, false, w, h, depth, u, yc, face + off, c);
        } else {
            box(shellRoot, "base", 0, false, depth, h, w, face + off, yc, u, c);
        }
    }

    /** Puerto rectangular con marco metalico, hueco oscuro y lengueta. */
    private void rectPort(boolean rear, double u, double yc, double w, double h, Color tongue) {
        portBox(rear, u, yc, 0.25, 0.5, w + 1.2, h + 1.2, Color.rgb(112, 114, 120));
        portBox(rear, u, yc, 0.42, 0.25, w, h, Color.rgb(4, 4, 6));
        if (tongue != null) {
            portBox(rear, u, yc - h * 0.12, 0.5, 0.2, w * 0.78, h * 0.34, tongue);
        }
    }

    /** USB-C: hueco con forma de pastilla en el flanco derecho. */
    private void stadiumPortSide(double z, double yc, double w, double h) {
        double face = W / 2;
        double len = w - h;
        for (int k = 0; k < 2; k++) {
            double rr = k == 0 ? (h + 1.2) / 2 : h / 2;
            double dd = k == 0 ? 0.5 : 0.25;
            double off = k == 0 ? 0.25 : 0.42;
            Color c = k == 0 ? Color.rgb(112, 114, 120) : Color.rgb(4, 4, 6);
            box(shellRoot, "base", 0, false, dd, rr * 2, len, face + off, yc, z, c);
            cylinderX(shellRoot, "base", 0, false, rr, dd, face + off, yc, z - len / 2, c);
            cylinderX(shellRoot, "base", 0, false, rr, dd, face + off, yc, z + len / 2, c);
        }
        box(shellRoot, "base", 0, false, 0.2, 0.5, len * 0.9, face + 0.5, yc, z, Color.rgb(44, 44, 50));
    }

    /** Puertos, interruptor de camara y rejillas 3D de la LOQ segun las fotos reales. */
    private void addPortsLoq(double deckTop) {
        double yc = deckTop + 5.3;
        double face = W / 2;
        // flanco derecho (de delante hacia atras): USB-C, interruptor de camara, jack, USB-A
        stadiumPortSide(-D / 2 + 0.489 * D, yc, 9.2, 3.3);
        double sz = -D / 2 + 0.615 * D;
        box(shellRoot, "base", 0, false, 0.4, 3.8, 9.4, face + 0.2, yc, sz, Color.rgb(36, 36, 40));
        box(shellRoot, "base", 0, false, 0.3, 2.6, 4.2, face + 0.4, yc, sz + 2.2, Color.rgb(212, 38, 34));
        double jz = -D / 2 + 0.735 * D;
        cylinderX(shellRoot, "base", 0, false, 3.7, 0.5, face + 0.25, yc, jz, Color.rgb(112, 114, 120));
        cylinderX(shellRoot, "base", 0, false, 2.7, 0.25, face + 0.42, yc, jz, Color.rgb(4, 4, 6));
        cylinderX(shellRoot, "base", 0, false, 1.1, 0.2, face + 0.5, yc, jz, Color.rgb(30, 30, 34));
        rectPort(false, -D / 2 + 0.842 * D, yc, 13.4, 5.6, Color.rgb(36, 120, 230));

        // trasera: USB-A, HDMI, RJ-45, USB-A y conector de corriente
        double ry = deckTop + 5.1;
        double[] us = {0.382, 0.445, 0.513, 0.586, 0.658};
        double[] xs = new double[5];
        for (int i = 0; i < 5; i++) {
            xs[i] = (0.5 - us[i]) * W;
        }
        rectPort(true, xs[0], ry, 13.6, 5.6, Color.rgb(36, 120, 230));
        rectPort(true, xs[1], ry, 14.8, 5.4, Color.rgb(176, 142, 60));
        rectPort(true, xs[2], ry, 14.0, 9.6, Color.rgb(176, 142, 60));
        rectPort(true, xs[3], ry, 13.6, 5.6, Color.rgb(36, 120, 230));
        rectPort(true, xs[4], ry, 10.4, 4.4, Color.rgb(150, 150, 156));
        // rejillas de salida de aire con lamas
        for (double[] v : new double[][]{{70.5, 165.6}, {-168.5, -71.3}}) {
            double cx = (v[0] + v[1]) / 2, vw = v[1] - v[0];
            box(shellRoot, "base", 0, false, vw, 9.6, 0.6, cx, ry, D / 2 + 0.3, Color.rgb(6, 6, 8));
            int slats = 13;
            for (int i = 0; i < slats; i++) {
                double sx = v[0] + (i + 0.5) * vw / slats;
                box(shellRoot, "base", 0, false, vw / slats * 0.42, 8.2, 0.45, sx, ry, D / 2 + 0.55, Color.rgb(58, 58, 63));
            }
        }
    }

    /** Teclado 3D a partir de la foto de la cubierta: cada tecla es un recorte de la foto (keys.png) colocado segun keys.csv. */
    private void addKeyboardAtlas(String dir, double deckLen, double topY, double keyH) {
        Image atlas = Textures.resource(dir, "keys.png");
        try (java.io.InputStream in = LaptopModel.class.getResourceAsStream("/models/" + dir + "/keys.csv")) {
            if (in == null) {
                return;
            }
            String[] lines = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\r?\n");
            for (String ln : lines) {
                if (ln.isBlank()) {
                    continue;
                }
                String[] f = ln.trim().split(",");
                double cx = Double.parseDouble(f[0]) * W, zf = Double.parseDouble(f[1]) * deckLen;
                double kw = Double.parseDouble(f[2]) * W, kh = Double.parseDouble(f[3]) * deckLen;
                int ax = Integer.parseInt(f[4]), ay = Integer.parseInt(f[5]), aw = Integer.parseInt(f[6]), ah = Integer.parseInt(f[7]);
                Image tile = new javafx.scene.image.WritableImage(atlas.getPixelReader(), ax, ay, aw, ah);
                double z = -D / 2 + zf;
                partItem(shellRoot, Shapes.keycapBody(kw, kh, keyH, 0.8, Math.min(2.8, Math.min(kw, kh) * 0.22)), "deck", 4, false,
                        Color.rgb(14, 14, 16), null, cx, topY - keyH / 2, z);
                partItem(shellRoot, Shapes.keycapTop(kw, kh, keyH, 0.8, Math.min(2.8, Math.min(kw, kh) * 0.22)), "deck", 4, false,
                        Color.WHITE, tile, cx, topY - keyH / 2, z);
            }
        } catch (java.io.IOException | RuntimeException e) {
            // sin teclas 3D: queda la foto plana
        }
    }

    /* ------------------------------------------------------------------ Legion Pro 7i Gen 10: interior por piezas (parts.csv) */

    private boolean isLegionGen10() {
        String m = d.getModel() == null ? "" : d.getModel().toUpperCase(Locale.ROOT);
        return m.contains("LEGION PRO 7I GEN 10");
    }

    /** Fila de parts.csv: una pieza con posicion (fracciones de la foto cenital desde abajo), medidas en mm, soporte y fuente. */
    static final class PartSpec {
        String id, type, support, conf, src;
        double tier, u, v, w, dd, h, z0, rot;
        final java.util.Map<String, Double> ex = new java.util.HashMap<>();

        double ex(String k, double dflt) {
            return ex.getOrDefault(k, dflt);
        }
    }

    static java.util.List<PartSpec> loadParts(String dir) {
        java.util.List<PartSpec> out = new ArrayList<>();
        try (java.io.InputStream in = LaptopModel.class.getResourceAsStream("/models/" + dir + "/parts.csv")) {
            if (in == null) {
                return out;
            }
            for (String ln : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\r?\n")) {
                String t = ln.trim();
                if (t.isEmpty() || t.startsWith("#") || t.startsWith("id,")) {
                    continue;
                }
                String[] f = t.split(",", -1);
                PartSpec p = new PartSpec();
                p.id = f[0].trim();
                p.type = f[1].trim();
                p.tier = Double.parseDouble(f[2]);
                p.u = Double.parseDouble(f[3]);
                p.v = Double.parseDouble(f[4]);
                p.w = Double.parseDouble(f[5]);
                p.dd = Double.parseDouble(f[6]);
                p.h = Double.parseDouble(f[7]);
                p.z0 = Double.parseDouble(f[8]);
                p.rot = Double.parseDouble(f[9]);
                p.support = f[10].trim();
                p.conf = f[11].trim();
                p.src = f[12].trim();
                if (f.length > 13 && !f[13].isBlank()) {
                    for (String kv : f[13].split("\\|")) {
                        String[] a = kv.split("=");
                        if (a.length == 2) {
                            p.ex.put(a[0].trim(), Double.parseDouble(a[1]));
                        }
                    }
                }
                out.add(p);
            }
        } catch (java.io.IOException | RuntimeException e) {
            // sin tabla: no se construye nada
        }
        return out;
    }

    private void tagFrom(int from, String id) {
        for (int i = from; i < items.size(); i++) {
            if (items.get(i).id == null) {
                items.get(i).id = id;
            }
        }
    }

    private Item l10Box(PartSpec p, String name, double x, double z, double w, double dd, Color c) {
        Item it = box(innerRoot, name, p.tier, false, w, p.h, dd, x, y(p.z0, p.h), z, c);
        if (p.rot != 0) {
            it.node.setRotationAxis(Rotate.Y_AXIS);
            it.node.setRotate(p.rot);
        }
        it.id = p.id;
        return it;
    }

    /** Interior del Legion Pro 7i Gen 10 construido pieza por pieza segun parts.csv (fotos + manual HMM). */
    /** Carpeta de recursos con parts.csv del modelo que usa el interior por piezas, y alturas del suelo/techo del interior (mm sobre la cara inferior). */
    private String partsDir = "in/legion10";
    private double partsFloor = 3.0, partsCeil = 14.0, partsWall = 2.4;

    private void legion10Interior() {
        java.util.List<PartSpec> parts = loadParts(partsDir);
        insidePhoto = !parts.isEmpty();   // en rayos X se retiran cubierta y teclas para ver las piezas
        Image full = hasRes(partsDir, "full.jpg") ? Textures.resource(partsDir, "full.jpg") : null;
        Image board = hasRes(partsDir, "board.jpg") ? Textures.resource(partsDir, "board.jpg") : null;
        for (PartSpec p : parts) {
            // la foto es una vista desde ABAJO con la bisagra arriba: izquierda y derecha quedan invertidas respecto a la vista superior
            double x = (0.5 - p.u) * W, z = (0.5 - p.v) * D;
            int from = items.size();
            switch (p.type) {
                case "pcb" -> {
                    Item it = l10Box(p, "pcb", x, z, p.w, p.dd, Color.rgb(22, 28, 36));
                    it.xray = 0.16;
                }
                case "battery" -> {
                    Item it = l10Box(p, "battery", x, z, p.w, p.dd, Color.rgb(24, 26, 30));
                    if (full != null) {
                        double u0 = p.u - p.w / W / 2, u1 = p.u + p.w / W / 2, v0 = p.v - p.dd / D / 2, v1 = p.v + p.dd / D / 2;
                        Item li = decal(innerRoot, "battery", p.tier, quad(cropFrac(full, new double[]{u0, v0, u1, v1}),
                                flat(p.w / 2, p.dd / 2, -p.w / 2, p.dd / 2, -p.w / 2, -p.dd / 2, p.w / 2, -p.dd / 2, 0)),
                                T / 2 - p.z0 + 0.06, z);
                        li.node.setTranslateX(x);
                        li.photo = true;
                        li.id = p.id;
                    }
                }
                case "speaker" -> l10Box(p, "pcb", x, z, p.w, p.dd, Color.rgb(20, 21, 24));
                case "plate" -> l10Box(p, "cooling", x, z, p.w, p.dd, Color.rgb(30, 31, 35));
                case "pipe" -> l10Box(p, "cooling", x, z, p.w, p.dd, p.ex("dark", 0) > 0 ? Color.rgb(26, 27, 30) : Color.rgb(184, 115, 70));
                case "shield" -> l10Box(p, "pcb", x, z, p.w, p.dd, Color.rgb(176, 180, 186));
                case "foam" -> l10Box(p, "pcb", x, z, p.w, p.dd, Color.rgb(20, 20, 22));
                case "fins" -> l10Fins(p, x, z);
                case "fan" -> l10Fan(p, x, z);
                case "ram" -> {
                    l10Box(p, "ram", x, z, p.w, p.dd, Color.rgb(36, 104, 66));
                    for (int i = 0; i < (p.ex("nochips", 0) > 0 ? 0 : 4); i++) {
                        double cx = x - p.w / 2 + p.w * (0.20 + 0.20 * i);
                        Item chip = box(innerRoot, "ram", p.tier, false, 11, 0.9, 8.5, cx, y(p.z0 - 0.9, 0.9), z, Color.rgb(18, 18, 20));
                        chip.id = p.id;
                    }
                }
                case "ssd" -> {
                    l10Box(p, "ssd", x, z, p.w, p.dd, Color.rgb(34, 66, 52));
                    Item st = box(innerRoot, "ssd", p.tier, false, p.w * 0.55, 0.15, p.dd * 0.8, x + (isTilted(p) ? 0 : p.w * 0.08), y(p.z0 - 0.15, 0.15), z, Color.rgb(176, 180, 186));
                    if (p.rot != 0) {
                        st.node.setRotationAxis(Rotate.Y_AXIS);
                        st.node.setRotate(p.rot);
                    }
                    st.id = p.id;
                }
                case "wifi" -> l10Box(p, "pcb", x, z, p.w, p.dd, Color.rgb(34, 70, 56));
                case "cpu" -> l10Box(p, "cpu", x, z, p.w, p.dd, Color.rgb(170, 174, 180));
                case "gpu" -> l10Box(p, "gpu", x, z, p.w, p.dd, Color.rgb(46, 49, 56));
                case "screw" -> {
                    Item it = cylinderY(innerRoot, "pcb", p.tier, p.w / 2, p.h, x, y(p.z0, p.h), z, Color.rgb(150, 152, 158));
                    it.id = p.id;
                }
                default -> { }
            }
            tagFrom(from, p.id);
            // cara que mira a la tapa inferior: recorte de la foto real del mismo lugar (la pieza ya tiene su geometria)
            Image src = "pcb".equals(p.type) && board != null ? board : full != null ? full : board;
            if (src != null && !isTilted(p) && !"fan".equals(p.type) && !"battery".equals(p.type) && !"screw".equals(p.type) && !"pipe".equals(p.type)
                    && !"cpu".equals(p.type) && !"gpu".equals(p.type)) {
                double u0 = p.u - p.w / W / 2, u1 = p.u + p.w / W / 2, v0 = p.v - p.dd / D / 2, v1 = p.v + p.dd / D / 2;
                Item li = decal(innerRoot, "ssd".equals(p.type) || "ram".equals(p.type) ? p.type : "pcb".equals(p.type) || "speaker".equals(p.type) || "wifi".equals(p.type) ? "pcb" : "cooling",
                        p.tier, quad(cropFrac(src, new double[]{u0, v0, u1, v1}),
                                flat(p.w / 2, p.dd / 2, -p.w / 2, p.dd / 2, -p.w / 2, -p.dd / 2, p.w / 2, -p.dd / 2, 0)),
                        T / 2 - p.z0 + 0.05, z);
                li.node.setTranslateX(x);
                li.photo = true;
                li.id = p.id;
            }
        }
    }

    private void l10Fins(PartSpec p, double x, double z) {
        int n = (int) Math.floor(p.w / 1.6);
        double pitch = p.w / n;
        for (int i = 0; i < n; i++) {
            double fx = x - p.w / 2 + pitch * (i + 0.5);
            Item it = box(innerRoot, "cooling", p.tier, false, 0.35, p.h, p.dd, fx, y(p.z0, p.h), z, Color.rgb(44, 46, 50));
            it.id = p.id;
        }
    }

    /** Ventilador: carcasa hueca (tapa cerrada arriba, marco con la boca de entrada abajo) y rodete con aspas que gira. */
    private void l10Fan(PartSpec p, double x, double z) {
        double w = p.w, dd = p.dd, h = p.h, top = 0.8, wall = 1.0, base = 0.8;
        Color hs = Color.rgb(34, 36, 40);
        double R = p.ex("R", 24), icx = (0.5 - p.ex("iu", p.u)) * W, icz = (0.5 - p.ex("iv", p.v)) * D;
        double zTop = p.z0 + h;
        // tapa superior cerrada
        partItem(innerRoot, Shapes.slabBody(w, dd, top, 9, 5, 0.3), "cooling", p.tier, false, hs, null, x, y(zTop - top, top), z);
        partItem(innerRoot, Shapes.slabCap(w, dd, top, 9, 5, 0.3, true, 0), "cooling", p.tier, false, hs, null, x, y(zTop - top, top), z);
        partItem(innerRoot, Shapes.slabCap(w, dd, top, 9, 5, 0.3, false, 0), "cooling", p.tier, false, hs, null, x, y(zTop - top, top), z);
        // paredes
        partItem(innerRoot, Shapes.ring(w, dd, h - top, 9, 5, wall), "cooling", p.tier, false, hs, null, x, y(p.z0, h - top), z);
        // marco inferior con la boca de entrada (cuadrada, algo mayor que el rodete)
        double hole = R * 1.08, dx = icx - x, dz = icz - z;
        double[][] strips = {
                {-w / 2, dx - hole, -dd / 2, dd / 2},
                {dx + hole, w / 2, -dd / 2, dd / 2},
                {dx - hole, dx + hole, -dd / 2, dz - hole},
                {dx - hole, dx + hole, dz + hole, dd / 2}};
        for (double[] s : strips) {
            double sw = s[1] - s[0], sd = s[3] - s[2];
            if (sw > 0.5 && sd > 0.5) {
                box(innerRoot, "cooling", p.tier, false, sw, base, sd, x + (s[0] + s[1]) / 2, y(p.z0, base), z + (s[2] + s[3]) / 2, hs);
            }
        }
        // rodete: buje + 17 aspas
        double bh = (h - top) * 0.85;
        PhongMaterial bm = new PhongMaterial(Color.rgb(26, 28, 32));
        PhongMaterial hm = new PhongMaterial(Color.rgb(50, 52, 58));
        bm.setSpecularColor(Color.gray(0.16));
        hm.setSpecularColor(Color.gray(0.3));
        Group g = new Group();
        int nb = 17;
        for (int i = 0; i < nb; i++) {
            double th = 2 * Math.PI * i / nb, rc = R * 0.64;
            Box b = new Box(R * 0.66, bh, 0.55);
            b.setMaterial(bm);
            b.setTranslateX(rc * Math.cos(th));
            b.setTranslateZ(rc * Math.sin(th));
            b.setRotationAxis(Rotate.Y_AXIS);
            b.setRotate(-Math.toDegrees(th) + 24);
            g.getChildren().add(b);
        }
        Cylinder hub = new Cylinder(R * 0.30, bh, 24);
        hub.setMaterial(hm);
        g.getChildren().add(hub);
        Rotate spin = new Rotate(0, Rotate.Y_AXIS);
        g.getTransforms().add(spin);
        fanRotates.add(spin);
        double yMid = y(p.z0 + base / 2, bh);
        Group holder = new Group(g);
        holder.setTranslateX(icx);
        holder.setTranslateY(yMid);
        holder.setTranslateZ(icz);
        holder.setUserData("cooling");
        innerRoot.getChildren().add(holder);
        Item it = new Item(holder, "cooling", p.tier, yMid, false, false, Color.rgb(26, 28, 32), bm, Color.gray(0.16));
        it.mats = new ArrayList<>(List.of(hm));
        it.id = p.id + "_rodete";
        items.add(it);
    }

    /** Datos para el control de calidad del montaje: caja de cada pieza en el sistema del interior. */
    java.util.Map<String, double[]> qcBounds() {
        return qcBounds(false);
    }

    /** lid = true: solo las piezas de la tapa (sistema de la tapa); false: solo las del interior. */
    java.util.Map<String, double[]> qcBounds(boolean lid) {
        java.util.Map<String, double[]> out = new java.util.LinkedHashMap<>();
        for (Item it : items) {
            if (it.id == null || it.node == null || (it.node.getParent() == lidPivot) != lid || it.decal && !it.photo || it.decal && it.photo && !(it.node instanceof Group)) {
                continue;
            }
            javafx.geometry.Bounds b = it.node.getBoundsInParent();
            double[] cur = out.get(it.id);
            double[] nb = {b.getMinX(), b.getMaxX(), b.getMinY(), b.getMaxY(), b.getMinZ(), b.getMaxZ()};
            if (cur == null) {
                out.put(it.id, nb);
            } else {
                cur[0] = Math.min(cur[0], nb[0]);
                cur[1] = Math.max(cur[1], nb[1]);
                cur[2] = Math.min(cur[2], nb[2]);
                cur[3] = Math.max(cur[3], nb[3]);
                cur[4] = Math.min(cur[4], nb[4]);
                cur[5] = Math.max(cur[5], nb[5]);
            }
        }
        return out;
    }

    private static boolean isTilted(PartSpec p) {
        double r = Math.abs(p.rot) % 90;
        return r > 1e-6 && Math.abs(r - 90) > 1e-6;
    }

    /** Huella en planta (4 esquinas {x, z}) de una pieza: las giradas, de su caja real transformada por el nodo; el resto, de su caja envolvente. */
    private double[][] rectOf(PartSpec p, double[] bb) {
        if (isTilted(p)) {
            for (Item it : items) {
                if (p.id.equals(it.id) && it.node instanceof javafx.scene.shape.Box bx && !it.decal) {
                    double hw = bx.getWidth() / 2, hd = bx.getDepth() / 2;
                    double[][] c = new double[4][];
                    double[][] loc = {{-hw, -hd}, {hw, -hd}, {hw, hd}, {-hw, hd}};
                    for (int i = 0; i < 4; i++) {
                        javafx.geometry.Point3D q = bx.localToParent(loc[i][0], 0, loc[i][1]);
                        c[i] = new double[]{q.getX(), q.getZ()};
                    }
                    return c;
                }
            }
        }
        return new double[][]{{bb[0], bb[4]}, {bb[1], bb[4]}, {bb[1], bb[5]}, {bb[0], bb[5]}};
    }

    /** Penetracion minima (mm) entre dos poligonos convexos (SAT): <= 0 si no se tocan. */
    private static double satDepth(double[][] a, double[][] b) {
        double min = Double.MAX_VALUE;
        double[][][] polys = {a, b};
        for (double[][] poly : polys) {
            for (int i = 0; i < poly.length; i++) {
                double[] p1 = poly[i], p2 = poly[(i + 1) % poly.length];
                double ax = -(p2[1] - p1[1]), az = p2[0] - p1[0];
                double len = Math.hypot(ax, az);
                if (len < 1e-9) {
                    continue;
                }
                ax /= len;
                az /= len;
                double minA = Double.MAX_VALUE, maxA = -Double.MAX_VALUE, minB = Double.MAX_VALUE, maxB = -Double.MAX_VALUE;
                for (double[] q : a) {
                    double t = q[0] * ax + q[1] * az;
                    minA = Math.min(minA, t);
                    maxA = Math.max(maxA, t);
                }
                for (double[] q : b) {
                    double t = q[0] * ax + q[1] * az;
                    minB = Math.min(minB, t);
                    maxB = Math.max(maxB, t);
                }
                min = Math.min(min, Math.min(maxA, maxB) - Math.max(minA, minB));
            }
        }
        return min;
    }

    /** Control de calidad del montaje del Legion Gen 10: contacto con el soporte, interferencias, limites del chasis y despiece reversible. */
    public String qcLegion10() {
        StringBuilder out = new StringBuilder();
        java.util.List<PartSpec> parts = loadParts(partsDir);
        java.util.Map<String, PartSpec> byId = new java.util.HashMap<>();
        for (PartSpec p : parts) {
            byId.put(p.id, p);
        }
        double savedExplode = explode;
        setExplode(0);
        java.util.Map<String, double[]> bb = qcBounds();
        final double TOL = 0.30;
        int floating = 0, inter = 0, outside = 0, checked = 0;
        double floorY = T / 2 - partsFloor, ceilY = T / 2 - partsCeil;
        // 1) contacto con el soporte
        for (PartSpec p : parts) {
            double[] a = bb.get(p.id);
            if (a == null) {
                out.append("SIN GEOMETRIA ").append(p.id).append('\n');
                continue;
            }
            checked++;
            double gap;
            String sup = p.support;
            if ("base".equals(sup)) {
                gap = Math.abs(a[3] - floorY);
            } else if ("deck".equals(sup)) {
                gap = Math.abs(a[2] - ceilY);
            } else {
                double[] b = bb.get(sup);
                if (b == null) {
                    out.append("SOPORTE INEXISTENTE ").append(p.id).append(" -> ").append(sup).append('\n');
                    floating++;
                    continue;
                }
                double gx = Math.max(0, Math.max(a[0] - b[1], b[0] - a[1]));
                double gy = Math.max(0, Math.max(a[2] - b[3], b[2] - a[3]));
                double gz = Math.max(0, Math.max(a[4] - b[5], b[4] - a[5]));
                gap = Math.sqrt(gx * gx + gy * gy + gz * gz);
            }
            if (gap > TOL) {
                floating++;
                out.append(String.format(Locale.US, "FLOTANTE %-10s separado %.2f mm de %s%n", p.id, gap, sup));
            }
        }
        // 2) interferencias entre piezas sin relacion de soporte
        java.util.List<String> ids = new ArrayList<>(bb.keySet());
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                String ia = ids.get(i), ib = ids.get(j);
                PartSpec pa = byId.get(ia.replace("_rodete", "")), pb = byId.get(ib.replace("_rodete", ""));
                if (pa == null || pb == null || pa == pb) {
                    continue;
                }
                if (pa.support.equals(pb.id) || pb.support.equals(pa.id)) {
                    continue;
                }
                // los heatpipes van embebidos en las placas y las aletas del mismo modulo termico
                if (("pipe".equals(pa.type) && ("plate".equals(pb.type) || "fins".equals(pb.type)))
                        || ("pipe".equals(pb.type) && ("plate".equals(pa.type) || "fins".equals(pa.type)))) {
                    continue;
                }
                double[] a = bb.get(ia), b = bb.get(ib);
                double ox = Math.min(a[1], b[1]) - Math.max(a[0], b[0]);
                double oy = Math.min(a[3], b[3]) - Math.max(a[2], b[2]);
                double oz = Math.min(a[5], b[5]) - Math.max(a[4], b[4]);
                // los heatpipes de un mismo modulo van en contacto entre si
                if ("pipe".equals(pa.type) && "pipe".equals(pb.type)) {
                    continue;
                }
                // piezas giradas: la caja envolvente sobrestima, asi que se compara la huella real (SAT sobre los rectangulos)
                boolean hit;
                if (isTilted(pa) || isTilted(pb)) {
                    double[][] ra = rectOf(pa, a), rb = rectOf(pb, b);
                    hit = oy > TOL && satDepth(ra, rb) > TOL;
                    if (hit) {
                        out.append("  huella ").append(ia).append(": ").append(java.util.Arrays.deepToString(ra)).append('\n');
                        out.append("  huella ").append(ib).append(": ").append(java.util.Arrays.deepToString(rb)).append('\n');
                    }
                } else {
                    hit = ox > TOL && oy > TOL && oz > TOL;
                }
                if (hit) {
                    inter++;
                    out.append(String.format(Locale.US, "INTERFERENCIA %s x %s : %.1f x %.1f x %.1f mm%n", ia, ib, ox, oy, oz));
                }
            }
        }
        // 3) dentro del chasis
        double hx = W / 2 - partsWall, hz = D / 2 - partsWall;
        for (java.util.Map.Entry<String, double[]> e : bb.entrySet()) {
            double[] a = e.getValue();
            if (a[0] < -hx - TOL || a[1] > hx + TOL || a[4] < -hz - TOL || a[5] > hz + TOL || a[3] > floorY + TOL || a[2] < ceilY - TOL) {
                outside++;
                out.append(String.format(Locale.US, "FUERA DEL CHASIS %s x[%.1f,%.1f] y[%.1f,%.1f] z[%.1f,%.1f]%n", e.getKey(), a[0], a[1], a[2], a[3], a[4], a[5]));
            }
        }
        // 4) despiece reversible y piezas de una capa que se mueven juntas
        setExplode(1.0);
        java.util.Map<String, double[]> ex = qcBounds();
        setExplode(0);
        java.util.Map<String, double[]> back = qcBounds();
        int drift = 0, split = 0;
        for (String id : bb.keySet()) {
            double[] a = bb.get(id), b = back.get(id);
            for (int k = 0; k < 6; k++) {
                if (Math.abs(a[k] - b[k]) > 0.01) {
                    drift++;
                    out.append("NO VUELVE A SU SITIO ").append(id).append('\n');
                    break;
                }
            }
        }
        for (PartSpec p : parts) {
            PartSpec sp = byId.get(p.support);
            if (sp == null || Math.abs(sp.tier - p.tier) > 1e-9) {
                continue;
            }
            double[] a0 = bb.get(p.id), b0 = bb.get(sp.id), a1 = ex.get(p.id), b1 = ex.get(sp.id);
            if (a0 == null || b0 == null || a1 == null || b1 == null) {
                continue;
            }
            for (int k = 0; k < 6; k++) {
                if (Math.abs((a1[k] - b1[k]) - (a0[k] - b0[k])) > 0.01) {
                    split++;
                    out.append("SE SEPARA DE SU SOPORTE EN EL DESPIECE ").append(p.id).append(" / ").append(sp.id).append('\n');
                    break;
                }
            }
        }
        setExplode(savedExplode);
        out.append(String.format(Locale.US, "RESUMEN piezas=%d flotantes=%d interferencias=%d fuera_chasis=%d no_vuelven=%d separadas_en_despiece=%d%n",
                checked, floating, inter, outside, drift, split));
        return out.toString();
    }


    /* ------------------------------------------------------------------ interior con fotografia real */

    /**
     * Interior con fotografia real (vista cenital con la tapa inferior retirada): {fragmento del modelo, carpeta}.
     * La carpeta lleva board.jpg (placa, sin bateria) y/o full.jpg (con bateria) recortadas al contorno del chasis
     * e inside.csv con: battery,u0,v0,u1,v1 / fan,u,v,r / zone,nombre,u0,v0,u1,v1 (fracciones de la foto, v desde arriba).
     */
    private static final String[][] INSIDE = {
            {"LEGION PRO 7I GEN 9", "in/legion9"},
            {"PH16-72", "in/ph16_72"},
            {"G16 7630", "in/g16"},
            {"ZEPHYRUS G16 (GU605MY)", "in/zep24"},
            {"ZEPHYRUS G16 (2025)", "in/zep24"},
            {"THINKPAD X1 CARBON", "in/x1c"},
            {"MACBOOK AIR 15", "in/mba15"},
            {"SURFACE LAPTOP 7", "in/surf7"},
            // sin foto propia: interior real de un equipo similar (misma marca/tamano y numero de ventiladores)
            {"AREA-51", "in/m18r2"},
            {"AORUS 17X", "in/helios18", "similar"},
            {"BLADE 18", "in/titan18", "similar"},
            {"EON18", "in/titan18", "similar"},
            {"BLADE 16", "in/zep24", "similar"},
            {"GRAM 16", "in/zen14", "similar"},
            {"LAPTOP 16", "in/m16r2", "similar"},
            {"M18 R2", "in/m18r2"},
            {"M16 R2", "in/m16r2"},
            {"XPS 16", "in/xps16"},
            {"ZENBOOK 14", "in/zen14"},
            {"TITAN 18", "in/titan18"},
            {"OMEN TRANSCEND 16", "in/omen16"}
    };

    private void applyInsidePhoto() {
        if (tint != null) {
            return;
        }
        String mdl = d.getModel() == null ? "" : d.getModel().toUpperCase(Locale.ROOT);
        String dir = null;
        boolean similar = false;
        for (String[] e : INSIDE) {
            if (mdl.contains(e[0]) && hasRes(e[1], "inside.csv") && (hasRes(e[1], "board.jpg") || hasRes(e[1], "full.jpg"))) {
                dir = e[1];
                similar = e.length > 2;
                break;
            }
        }
        if (dir == null) {
            return;
        }
        double[] bat = null;
        List<double[]> fans = new ArrayList<>();
        List<String[]> zones = new ArrayList<>();
        try (java.io.InputStream in = LaptopModel.class.getResourceAsStream("/models/" + dir + "/inside.csv")) {
            if (in == null) {
                return;
            }
            for (String ln : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\r?\n")) {
                String t = ln.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                String[] f = t.split(",");
                switch (f[0].trim()) {
                    case "battery" -> bat = new double[]{Double.parseDouble(f[1]), Double.parseDouble(f[2]),
                            Double.parseDouble(f[3]), Double.parseDouble(f[4])};
                    case "fan" -> fans.add(new double[]{Double.parseDouble(f[1]), Double.parseDouble(f[2]), Double.parseDouble(f[3])});
                    case "zone" -> zones.add(f);
                    default -> { }
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            return;
        }

        // suelo del equipo: la cara inferior de las piezas internas que se sustituyen
        double floorY = Double.NEGATIVE_INFINITY;
        for (Node n : innerRoot.getChildren()) {
            floorY = Math.max(floorY, n.getBoundsInParent().getMaxY());
        }
        if (Double.isInfinite(floorY)) {
            floorY = T / 2 - bodyH * 0.70;
        }
        double deckLowY = T / 2 - bodyH + bodyH * 0.24;
        double hInt = Math.max(4.0, floorY - deckLowY);
        final Set<Node> old = new HashSet<>(innerRoot.getChildren());
        items.removeIf(it -> old.contains(it.node));
        innerRoot.getChildren().clear();
        fanRotates.clear();

        Image full = hasRes(dir, "full.jpg") ? Textures.resource(dir, "full.jpg") : null;
        Image board = hasRes(dir, "board.jpg") ? Textures.resource(dir, "board.jpg") : null;
        if (board == null) {
            board = bat != null ? fillRect(full, bat) : full;
        }
        double yTop = floorY - 0.3;

        // tier 1: bateria (caja 3D con la etiqueta real recortada de la foto)
        if (bat != null) {
            double bw = (bat[2] - bat[0]) * W, bd = (bat[3] - bat[1]) * D;
            double bx = ((bat[0] + bat[2]) / 2 - 0.5) * W, bz = (0.5 - (bat[1] + bat[3]) / 2) * D;
            double batH = Math.min(7.5, hInt * 0.78);
            box(innerRoot, "battery", 1, false, bw, batH, bd, bx, yTop - batH / 2, bz, Color.rgb(24, 26, 30));
            Image battImg = similar
                    ? Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)), Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 600, 220, 54)
                    : hasRes(dir, "batt.jpg") ? Textures.resource(dir, "batt.jpg") : (full != null ? cropFrac(full, bat) : null);
            if (battImg != null) {
                Item li = decal(innerRoot, "battery", 1, quad(battImg,
                        flat(-bw / 2, bd / 2, bw / 2, bd / 2, bw / 2, -bd / 2, -bw / 2, -bd / 2, 0)), yTop - batH - 0.08, bz);
                li.node.setTranslateX(bx);
                li.photo = true;
            }
        }

        // tier 2: placa con todos sus componentes (la foto real sobre el contorno del chasis)
        partItem(innerRoot, Shapes.slabCap(W, D, 0.3, RF, RB, 0, true, Shapes.UV_DECK), "pcb", 2, false, Color.WHITE, board,
                0, yTop + 0.15, 0);
        items.get(items.size() - 1).photo = true;
        insidePhoto = true;

        // tier 3: ventiladores con las aspas de la foto girando
        double fanH = Math.min(6.0, hInt * 0.70);
        for (double[] f : fans) {
            double R = f[2] * W, fx = (f[0] - 0.5) * W, fz = (0.5 - f[1]) * D;
            cylinderY(innerRoot, "cooling", 3, R * 0.97, fanH, fx, yTop - fanH / 2, fz, Color.rgb(22, 24, 28));
            Image disc = circleCrop(board, f[0], f[1], f[2]);
            if (disc == null) {
                continue;
            }
            double topY = yTop - fanH - 0.06;
            Group g = new Group();
            MeshView blades = quad(disc, flat(-R, R, R, R, R, -R, -R, -R, 0));
            Rotate spin = new Rotate(0, Rotate.Y_AXIS);
            g.getTransforms().add(spin);
            g.getChildren().add(blades);
            fanRotates.add(spin);
            Group holder = new Group(g);
            holder.setTranslateX(fx);
            holder.setTranslateZ(fz);
            holder.setTranslateY(topY);
            holder.setUserData("cooling");
            g.setUserData("cooling");
            blades.setUserData("cooling");
            innerRoot.getChildren().add(holder);
            Item fi = new Item(holder, "cooling", 3, topY, false, true, null, (PhongMaterial) blades.getMaterial(), null);
            fi.photo = true;
            items.add(fi);
        }

        // zonas de las piezas: transparentes, para seleccionarlas y tenirlas en la vista termica
        for (String[] z : zones) {
            String name = z[1].trim();
            double u0 = Double.parseDouble(z[2]), v0 = Double.parseDouble(z[3]), u1 = Double.parseDouble(z[4]), v1 = Double.parseDouble(z[5]);
            double zw = (u1 - u0) * W, zd = (v1 - v0) * D;
            double zx = ((u0 + u1) / 2 - 0.5) * W, zz = (0.5 - (v0 + v1) / 2) * D;
            double zh = 1.0;
            double tier = "cooling".equals(name) ? 3 : 2;
            double zy = "cooling".equals(name) ? yTop - 0.5 : yTop - zh / 2 - 1.2;
            box(innerRoot, name, tier, false, zw, zh, zd, zx, zy, zz, Color.rgb(40, 44, 50)).zone = true;
        }
    }

    /** Recorta un rectangulo dado como fracciones (u0, v0, u1, v1) de la imagen. */
    private static Image cropFrac(Image src, double[] r) {
        int w = (int) src.getWidth(), h = (int) src.getHeight();
        int x0 = (int) Math.max(0, Math.round(r[0] * w)), y0 = (int) Math.max(0, Math.round(r[1] * h));
        int x1 = (int) Math.min(w, Math.round(r[2] * w)), y1 = (int) Math.min(h, Math.round(r[3] * h));
        return new javafx.scene.image.WritableImage(src.getPixelReader(), x0, y0, Math.max(2, x1 - x0), Math.max(2, y1 - y0));
    }

    /** Copia de la foto con el hueco de la bateria rellenado con el color del chasis vacio (para la capa de la placa). */
    private static Image fillRect(Image src, double[] r) {
        int w = (int) src.getWidth(), h = (int) src.getHeight();
        int[] px = new int[w * h];
        src.getPixelReader().getPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, w);
        int x0 = (int) Math.max(0, Math.round(r[0] * w)), x1 = (int) Math.min(w, Math.round(r[2] * w));
        int y0 = (int) Math.max(0, Math.round(r[1] * h)), y1 = (int) Math.min(h, Math.round(r[3] * h));
        long sr = 0, sg = 0, sb = 0, n = 0;
        int sy1 = Math.max(1, y0 - 2), sy0 = Math.max(0, y0 - (int) (0.035 * h));
        for (int y = sy0; y < sy1; y++) {
            for (int x = x0; x < x1; x += 3) {
                int p = px[y * w + x];
                sr += (p >> 16) & 255;
                sg += (p >> 8) & 255;
                sb += p & 255;
                n++;
            }
        }
        int br = n == 0 ? 26 : (int) (sr / n * 0.85), bg = n == 0 ? 28 : (int) (sg / n * 0.85), bb = n == 0 ? 32 : (int) (sb / n * 0.85);
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int v = ((x * 31 + y * 17) % 7) - 3;
                px[y * w + x] = 0xFF000000 | ((int) clamp(br + v, 0, 255) << 16) | ((int) clamp(bg + v, 0, 255) << 8) | (int) clamp(bb + v, 0, 255);
            }
        }
        javafx.scene.image.WritableImage out = new javafx.scene.image.WritableImage(w, h);
        out.getPixelWriter().setPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, w);
        return out;
    }

    /** Disco con el contenido circular de la foto (centro y radio como fracciones del ancho), transparente fuera. */
    private static Image circleCrop(Image src, double cu, double cv, double ru) {
        int iw = (int) src.getWidth(), ih = (int) src.getHeight();
        int r = (int) Math.round(ru * iw);
        if (r < 8) {
            return null;
        }
        int cx = (int) Math.round(cu * iw), cy = (int) Math.round(cv * ih);
        int s = 2 * r;
        int[] px = new int[s * s];
        javafx.scene.image.PixelReader pr = src.getPixelReader();
        for (int j = 0; j < s; j++) {
            for (int i = 0; i < s; i++) {
                int sx = cx - r + i, sy = cy - r + j;
                double dx = i + 0.5 - r, dy = j + 0.5 - r;
                if (dx * dx + dy * dy <= r * r && sx >= 0 && sy >= 0 && sx < iw && sy < ih) {
                    px[j * s + i] = pr.getArgb(sx, sy);
                }
            }
        }
        javafx.scene.image.WritableImage out = new javafx.scene.image.WritableImage(s, s);
        out.getPixelWriter().setPixels(0, 0, s, s, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, s);
        return out;
    }

    /** Teclado 3D de la LOQ: cada tecla recortada de la foto real (atlas keys.png + posiciones en keys.csv). */
    private void addKeyboardLoq(double topY) {
        Image atlas = Textures.resource("keys.png");
        double keyH = 2.4;
        try (java.io.InputStream in = LaptopModel.class.getResourceAsStream("/models/loq/keys.csv")) {
            if (in == null) {
                return;
            }
            String[] lines = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\r?\n");
            for (String ln : lines) {
                if (ln.isBlank()) {
                    continue;
                }
                String[] f = ln.trim().split(",");
                double cx = Double.parseDouble(f[0]), zf = Double.parseDouble(f[1]);
                double kw = Double.parseDouble(f[2]), kh = Double.parseDouble(f[3]);
                int ax = Integer.parseInt(f[4]), ay = Integer.parseInt(f[5]), aw = Integer.parseInt(f[6]), ah = Integer.parseInt(f[7]);
                Image tile = new javafx.scene.image.WritableImage(atlas.getPixelReader(), ax, ay, aw, ah);
                double z = -D / 2 + zf;
                partItem(shellRoot, Shapes.keycapBody(kw, kh, keyH, 0.9, 2.6), "deck", 4, false, Color.rgb(16, 16, 18), null,
                        cx, topY - keyH / 2, z);
                partItem(shellRoot, Shapes.keycapTop(kw, kh, keyH, 0.9, 2.6), "deck", 4, false, Color.WHITE, tile,
                        cx, topY - keyH / 2, z);
            }
        } catch (java.io.IOException | RuntimeException e) {
            // sin teclado 3D: se queda la foto plana de la cubierta
        }
    }

    /* ------------------------------------------------------------------ ASUS TUF Gaming A18 (carcasa con fotos + piezas internas OBJ) */

    private static boolean isObjModel(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("TUF GAMING A18");
    }

    private ObjImporter.Result lastBounds;

    /** Pieza interna importada de un OBJ. raise sube la pieza (mm) respecto a su posicion original. */
    private Item objPart(Group parent, String file, String name, double tier, Color base, double raise, boolean keepMap) {
        ObjImporter.Result r = ObjImporter.load("/models/tuf", file, T / 2 - raise, false);
        MeshView mv = r.mesh();
        mv.setUserData(name);
        parent.getChildren().add(mv);
        Item it = new Item(mv, name, tier, 0, false, false, base, r.material(), Color.gray(0.22));
        if (keepMap) {
            it.map = r.map();
        } else {
            r.material().setDiffuseMap(null);
        }
        items.add(it);
        r.material().setDiffuseColor(base);
        lastBounds = r;
        return it;
    }

    private void buildObj() {
        boolean ghost = tint != null;
        photoLum = 0.92;
        double wt = 2.4;
        double hB = 3.0, hD = 2.8, bodyFlat = 14.5, hZ = bodyFlat - hB - hD;
        double lidT = 7.0;
        double shelf = 18.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#2b2c2f");
        Color baseCol = Color.web("#232427");

        // cuna de la ficha: 18,9 mm delante y 26,7 mm detras (aqui 21,5 delante para que quepa el interior)
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 5.2 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        // las piezas internas se midieron con 294 mm de fondo; la ficha real da 283 mm
        boolean tufParts = !loadParts("in/tuf18").isEmpty();   // el interior por piezas ya esta a escala real (283 mm)
        if (!tufParts) {
            innerRoot.getTransforms().add(new javafx.scene.transform.Scale(1, 1, D / 294.0));
        }
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta D con paredes y las fotos de la base, los flancos y la trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("tuf/photo", "bottom.png"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("tuf/photo", "rear2.png"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("tuf/photo", "right2.png"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("tuf/photo", "left2.png"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
            addPortsTuf(T / 2 - bodyFlat);
            for (double fx : new double[]{-W / 2 + 34, W / 2 - 34}) {
                slabR(shellRoot, "base", 0, 28, 12, 2.4, 4, 4, fx, T / 2 + 1.2, -D / 2 + 22, Color.rgb(22, 22, 24));
                slabR(shellRoot, "base", 0, 44, 14, 7.6, 4, 4, fx, T / 2 + 3.8, D / 2 - 22, Color.rgb(22, 22, 24));
            }
        }

        if (!ghost && tufParts) {
            // interior por piezas (parts.csv) medido sobre el dibujo oficial del Service Manual ASUS
            partsDir = "in/tuf18";
            partsFloor = hB;
            partsCeil = hB + hZ;
            partsWall = wt;
            legion10Interior();
        } else if (!ghost) {
            // ---- tier 1: bateria de 90 Wh y altavoces
            objPart(innerRoot, "battery", "battery", 1, Color.rgb(22, 24, 28), 1.0, false);
            ObjImporter.Result bat = lastBounds;
            double lw = (bat.maxX() - bat.minX()) * 0.5, ld = lw / 3.2;
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.0f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-lw / 2, ld / 2, lw / 2, ld / 2, lw / 2, -ld / 2, -lw / 2, -ld / 2, 0)),
                    T / 2 - 1.0 - bat.maxY() - 0.08, bat.cz());
            objPart(innerRoot, "speakers", "pcb", 1, Color.rgb(24, 26, 30), 1.0, false);

            // ---- tier 2: placa base y componentes
            objPart(innerRoot, "pcb", "pcb", 2, Color.rgb(22, 62, 52), 1.0, false);
            objPart(innerRoot, "cpu", "cpu", 2, Color.rgb(176, 180, 186), 1.0, false);
            objPart(innerRoot, "gpu", "gpu", 2, Color.rgb(46, 49, 56), 1.0, false);
            objPart(innerRoot, "ram_1", "ram", 2, Color.gray(0.95), 1.0, true);
            objPart(innerRoot, "ram_2", "ram", 2, Color.gray(0.95), 1.0, true);
            objPart(innerRoot, "ssd_1", "ssd", 2, Color.gray(0.95), 1.0, true);
            objPart(innerRoot, "ssd_2", "ssd", 2, Color.gray(0.95), 1.0, true);
            objPart(innerRoot, "wifi", "pcb", 2, Color.gray(0.95), 1.0, true);

            // ---- tier 3: refrigeracion
            objPart(innerRoot, "cooling_heatpipes", "cooling", 3, Color.gray(0.95), 1.0, true);
            objPart(innerRoot, "cooling_radiator_left", "cooling", 3, Color.gray(0.95), 1.0, true);
            objPart(innerRoot, "cooling_radiator_right", "cooling", 3, Color.gray(0.95), 1.0, true);
            for (String side : new String[]{"left", "right"}) {
                objPart(innerRoot, "fan_" + side + "_housing", "cooling", 3, Color.gray(0.95), 1.0, true);
                ObjImporter.Result hb = lastBounds;
                ObjImporter.Result bl = ObjImporter.load("/models/tuf", "fan_" + side + "_blades", 0, true);
                MeshView bm = bl.mesh();
                bm.setUserData("cooling");
                Rotate spin = new Rotate(0, Rotate.Y_AXIS);
                Group g = new Group(bm);
                g.getTransforms().add(spin);
                fanRotates.add(spin);
                Group holder = new Group(g);
                double hy = T / 2 - 1.0 - (hb.minY() + hb.maxY()) / 2;
                holder.setTranslateX(hb.cx());
                holder.setTranslateY(hy);
                holder.setTranslateZ(hb.cz());
                holder.setUserData("cooling");
                g.setUserData("cooling");
                innerRoot.getChildren().add(holder);
                bl.material().setDiffuseMap(null);
                Color bc = Color.rgb(34, 37, 42);
                bl.material().setDiffuseColor(bc);
                items.add(new Item(holder, "cooling", 3, hy, false, false, bc, bl.material(), Color.gray(0.3)));
            }
        }

        // ---- tier 4: cubierta C (foto real rectificada) con teclado 3D, boton de encendido y touchpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("tuf/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardTuf(T / 2 - bodyFlat);
        }

        // ---- tier 5: tapa de aluminio con la pantalla real y bisagras en los extremos
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-164, 164}) {
            slabR(lidPivot, "hinge", -1, 38, 13, lidT + 3.0, 2.5, 2.5, hx, -(lidT + 3.0) / 2 + 1.5, -6.5, Color.rgb(34, 35, 38));
        }
        if (!ghost) {
            cylinderY(lidPivot, "lid", -1, 2.6, 0.3, 0, 0.12, -Dl + 4.2, Color.rgb(40, 40, 44));
            cylinderY(lidPivot, "lid", -1, 1.4, 0.4, 0, 0.18, -Dl + 4.2, Color.rgb(6, 8, 16));
            decal(lidPivot, "display", -1, quad(Textures.resource("tuf/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("tuf/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    // posicion de cada tecla en la foto cenital: etiqueta, centro X, centro Y, ancho, alto (en px de la vista ampliada 2,2x)
    private static final Object[][] TUF_KEYS = buildTufKeys();

    private static Object[][] buildTufKeys() {
        java.util.List<Object[]> k = new java.util.ArrayList<>();
        String[] hot = {"vol-", "vol+", "mic", "arm"};
        double[] hx = {184, 242, 300, 358};
        for (int i = 0; i < 4; i++) {
            k.add(new Object[]{hot[i], hx[i], 51.0, 46.0, 24.0});
        }
        k.add(new Object[]{"esc", 83.0, 102.0, 50.0, 28.0});
        double[] fx = {184, 242, 300, 358, 436, 494, 552, 610, 688, 746, 804, 862};
        for (int i = 0; i < 12; i++) {
            k.add(new Object[]{"F" + (i + 1), fx[i], 102.0, 46.0, 28.0});
        }
        String[] np0 = {"ins", "del", "pgup", "pgdn"};
        double[] nx = {927, 974, 1021, 1067};
        for (int i = 0; i < 4; i++) {
            k.add(new Object[]{np0[i], nx[i], 102.0, 38.0, 28.0});
        }
        k.add(new Object[]{"`", 76.0, 152.0, 36.0, 44.0});
        String[] r1 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "="};
        for (int i = 0; i < 12; i++) {
            k.add(new Object[]{r1[i], 127.0 + 57.6 * i, 152.0, 48.0, 44.0});
        }
        k.add(new Object[]{"back", 841.0, 152.0, 94.0, 44.0});
        String[] n1 = {"num", "/", "*", "-"};
        for (int i = 0; i < 4; i++) {
            k.add(new Object[]{n1[i], nx[i], 152.0, 38.0, 44.0});
        }
        k.add(new Object[]{"tab", 87.0, 208.0, 58.0, 44.0});
        String[] r2 = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]"};
        for (int i = 0; i < 12; i++) {
            k.add(new Object[]{r2[i], 154.0 + 57.6 * i, 208.0, 48.0, 44.0});
        }
        k.add(new Object[]{"\\", 858.0, 208.0, 62.0, 44.0});
        String[] n2 = {"7", "8", "9"};
        for (int i = 0; i < 3; i++) {
            k.add(new Object[]{n2[i], nx[i], 208.0, 38.0, 44.0});
        }
        k.add(new Object[]{"+", 1067.0, 237.0, 38.0, 105.0});
        k.add(new Object[]{"caps", 96.0, 266.0, 76.0, 44.0});
        String[] r3 = {"A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'"};
        for (int i = 0; i < 11; i++) {
            k.add(new Object[]{r3[i], 168.0 + 57.6 * i, 266.0, 48.0, 44.0});
        }
        k.add(new Object[]{"enter", 834.0, 266.0, 108.0, 44.0});
        String[] n3 = {"4", "5", "6"};
        for (int i = 0; i < 3; i++) {
            k.add(new Object[]{n3[i], nx[i], 266.0, 38.0, 44.0});
        }
        k.add(new Object[]{"shift", 111.0, 323.0, 106.0, 44.0});
        String[] r4 = {"Z", "X", "C", "V", "B", "N", "M", ",", ".", "/"};
        for (int i = 0; i < 10; i++) {
            k.add(new Object[]{r4[i], 197.0 + 57.6 * i, 323.0, 48.0, 44.0});
        }
        k.add(new Object[]{"shift", 790.0, 323.0, 80.0, 44.0});
        k.add(new Object[]{"\u25B2", 864.0, 323.0, 48.0, 44.0});
        String[] n4 = {"1", "2", "3"};
        for (int i = 0; i < 3; i++) {
            k.add(new Object[]{n4[i], nx[i], 323.0, 38.0, 44.0});
        }
        k.add(new Object[]{"ent", 1067.0, 355.0, 38.0, 100.0});
        k.add(new Object[]{"ctrl", 82.0, 380.0, 48.0, 44.0});
        k.add(new Object[]{"fn", 140.0, 380.0, 48.0, 44.0});
        k.add(new Object[]{"\u2756", 200.0, 380.0, 48.0, 44.0});
        k.add(new Object[]{"alt", 256.0, 380.0, 48.0, 44.0});
        k.add(new Object[]{"", 429.0, 380.0, 278.0, 44.0});
        k.add(new Object[]{"alt", 604.0, 380.0, 48.0, 44.0});
        k.add(new Object[]{"ctrl", 660.0, 380.0, 48.0, 44.0});
        k.add(new Object[]{"\u25C6", 718.0, 380.0, 48.0, 44.0});
        k.add(new Object[]{"\u25C0", 806.0, 380.0, 48.0, 44.0});
        k.add(new Object[]{"\u25BC", 864.0, 380.0, 48.0, 44.0});
        k.add(new Object[]{"\u25B6", 922.0, 380.0, 48.0, 44.0});
        k.add(new Object[]{"0", 950.0, 380.0, 85.0, 44.0});
        k.add(new Object[]{".", 1021.0, 380.0, 38.0, 44.0});
        return k.toArray(new Object[0][]);
    }

    /** Teclado 3D de la TUF con la distribucion de la foto: contorno naranja y teclas WASD rojas. */
    private void addKeyboardTuf(double topY) {
        double keyH = 2.2;
        double px = 1.4158 * 2.2;
        java.util.Map<String, Image> cache = new java.util.HashMap<>();
        for (Object[] kd : TUF_KEYS) {
            String label = (String) kd[0];
            double kx = (140 + (Double) kd[1] / 2.2 - 400.5) / 1.4158;
            double zf = (680 - (330 + (Double) kd[2] / 2.2)) / 1.4158;
            double kw = (Double) kd[3] / px, kh = (Double) kd[4] / px;
            boolean wasd = label.equals("W") || label.equals("A") || label.equals("S") || label.equals("D");
            int wpx = Math.max(16, (int) Math.round(kw * 9)), hpx = Math.max(16, (int) Math.round(kh * 9));
            String ck = label + "|" + wasd + "|" + wpx + "x" + hpx;
            Image tile = cache.computeIfAbsent(ck, t -> Textures.keycapTuf(label, wasd, wpx, hpx));
            double z = -D / 2 + zf;
            partItem(shellRoot, Shapes.keycapBody(kw, kh, keyH, 0.8, 2.2), "deck", 4, false, Color.rgb(14, 14, 16), null, kx, topY - keyH / 2, z);
            partItem(shellRoot, Shapes.keycapTop(kw, kh, keyH, 0.8, 2.2), "deck", 4, false, Color.WHITE, tile, kx, topY - keyH / 2, z);
        }
        // boton de encendido (arriba a la derecha) y borde del touchpad
        double bx = (140 + 1066.0 / 2.2 - 400.5) / 1.4158, bz = -D / 2 + (680 - (330 + 55.0 / 2.2)) / 1.4158;
        partItem(shellRoot, Shapes.slabBody(16, 16, 1.6, 4.5, 4.5, 0.5), "deck", 4, false, Color.rgb(24, 24, 27), null, bx, topY - 0.8, bz);
        Image pw = Textures.keycap("PWR", "", Color.rgb(26, 26, 29), Color.rgb(236, 238, 242), 128, 128);
        partItem(shellRoot, Shapes.slabCap(16, 16, 1.6, 4.5, 4.5, 0.5, true, Shapes.UV_SCREEN), "deck", 4, false, Color.WHITE, pw, bx, topY - 0.8, bz);
        partItem(shellRoot, Shapes.ring(150.0, 99.0, 0.5, 4.5, 4.5, 0.7), "deck", 4, false, Color.rgb(56, 57, 61), null,
                -37.4, topY - 0.25, -D / 2 + 55.0);
    }

    private void tufBox(int face, double u, double yc, double off, double depth, double w, double h, Color c) {
        if (face == 0) {
            box(shellRoot, "base", 0, false, w, h, depth, u, yc, D / 2 + off, c);
        } else {
            box(shellRoot, "base", 0, false, depth, h, w, face * (W / 2 + off), yc, u, c);
        }
    }

    private void tufRect(int face, double u, double yc, double w, double h, Color tongue) {
        tufBox(face, u, yc, 0.25, 0.5, w + 1.2, h + 1.2, Color.rgb(78, 80, 86));
        tufBox(face, u, yc, 0.42, 0.25, w, h, Color.rgb(3, 3, 5));
        if (tongue != null) {
            tufBox(face, u, yc - h * 0.12, 0.5, 0.2, w * 0.78, h * 0.34, tongue);
        }
    }

    private void tufStadium(int face, double z, double yc, double w, double h) {
        double len = w - h;
        for (int k = 0; k < 2; k++) {
            double rr = k == 0 ? (h + 1.2) / 2 : h / 2;
            double dd = k == 0 ? 0.5 : 0.25;
            double off = k == 0 ? 0.25 : 0.42;
            Color c = k == 0 ? Color.rgb(78, 80, 86) : Color.rgb(3, 3, 5);
            double x = face * (W / 2 + off);
            box(shellRoot, "base", 0, false, dd, rr * 2, len, x, yc, z, c);
            cylinderX(shellRoot, "base", 0, false, rr, dd, x, yc, z - len / 2, c);
            cylinderX(shellRoot, "base", 0, false, rr, dd, x, yc, z + len / 2, c);
        }
    }

    /** Puertos 3D de la TUF segun las fotos laterales (la parte trasera queda con la foto de las rejillas). */
    private void addPortsTuf(double deckTop) {
        double yc = deckTop + 5.2;
        // lado izquierdo, de atras hacia delante: corriente, RJ-45, HDMI, 2 USB-C, USB-A y audio
        double[] uL = {0.110, 0.177, 0.255, 0.345, 0.400, 0.482, 0.540};
        tufRect(-1, D / 2 - uL[0] * D, yc, 7.0, 4.0, null);
        tufRect(-1, D / 2 - uL[1] * D, yc - 0.3, 13.5, 11.0, Color.rgb(84, 72, 42));
        tufRect(-1, D / 2 - uL[2] * D, yc, 15.7, 5.8, null);
        tufStadium(-1, D / 2 - uL[3] * D, yc, 9.4, 3.9);
        tufStadium(-1, D / 2 - uL[4] * D, yc, 9.4, 3.9);
        tufRect(-1, D / 2 - uL[5] * D, yc, 13.0, 5.6, Color.rgb(60, 62, 70));
        double jz = D / 2 - uL[6] * D;
        cylinderX(shellRoot, "base", 0, false, 3.0, 0.5, -(W / 2 + 0.25), yc, jz, Color.rgb(78, 80, 86));
        cylinderX(shellRoot, "base", 0, false, 2.3, 0.25, -(W / 2 + 0.42), yc, jz, Color.rgb(3, 3, 5));
        // lado derecho: dos USB-A cerca de la parte trasera
        tufRect(1, D / 2 - 0.258 * D, yc, 13.0, 5.6, Color.rgb(60, 62, 70));
        tufRect(1, D / 2 - 0.171 * D, yc, 13.0, 5.6, Color.rgb(60, 62, 70));
    }


    /* ------------------------------------------------------------------ ASUS ROG Zephyrus G16 (2025) Platinum White */

    private static boolean isZephyr(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("ZEPHYRUS G16 (2025)");
    }

    private void buildZephyr() {
        boolean ghost = tint != null;
        photoLum = 0.56;
        double wt = 2.2;
        double bodyFlat = 12.2, hB = 2.6, hD = 2.4, hZ = bodyFlat - hB - hD;
        double lidT = 5.0, shelf = 14.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#a6a8af");
        Color baseCol = Color.web("#34353a");
        Color wallCol = Color.web("#3c3d42");

        // cuna de la ficha: 14,9 mm delante y 17,2 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 2.0 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior oscura con la foto de la base y los flancos reales
        slabR(shellRoot, "base", 0, W, D, hB, 10, 7, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, 10, 7, wt, 0, y(hB, hZ), 0, wallCol);
        addWireOutline(shellRoot, 0, W, D, 10, 7, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("zep", "bottom.png"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("zep", "rear.jpg"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            double zr = D / 2 - 7, zf = -D / 2 + 10;
            decal(shellRoot, "base", 0, quad(Textures.resource("zep", "right.png"),
                    pts(W / 2 + 0.1, yTop, zf, W / 2 + 0.1, yTop, zr, W / 2 + 0.1, yBot, zr, W / 2 + 0.1, yBot, zf)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("zep", "left.png"),
                    pts(-W / 2 - 0.1, yTop, zr, -W / 2 - 0.1, yTop, zf, -W / 2 - 0.1, yBot, zf, -W / 2 - 0.1, yBot, zr)), 0, 0);
            addPortsZephyr(T / 2 - bodyFlat, bodyFlat);
            for (double fx : new double[]{-W / 2 + 36, W / 2 - 36}) {
                for (double fz : new double[]{-D / 2 + 26, D / 2 - 24}) {
                    slabR(shellRoot, "base", 0, 26, 12, 2.0, 4, 4, fx, T / 2 + 1.0, fz, Color.rgb(24, 24, 26));
                }
            }

            // ---- tier 1: bateria de 90 Wh y altavoces
            double batW = 262, batD = 110, batH = Math.min(6.0, hZ - 0.8);
            double batZ = -D / 2 + wt + 10 + batD / 2;
            double batY = y(hB + 0.3, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(24, 26, 30));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.0f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(24, 26, 30), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-60, 19, 60, 19, 60, -19, -60, -19, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 34, 6, 70, -150, hB + 0.3, batZ - 4, Color.rgb(15, 16, 18));
            comp("pcb", 1, 34, 6, 70, 150, hB + 0.3, batZ - 4, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base con memoria soldada, 2 SSD, CPU y GPU
            double pcbZ0 = hB + 0.3, pcbH = 1.2;
            comp("pcb", 2, 300, pcbH, 108, 0, pcbZ0, 62, Color.rgb(28, 52, 74));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 1.4;
            comp("cpu", 2, 26, cH, 26, -22, top, 70, Color.rgb(176, 180, 186));
            comp("gpu", 2, 34, cH, 34, 30, top, 70, Color.rgb(46, 49, 56));
            for (double[] v : new double[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
                comp("gpu", 2, 11, cH * 0.8, 12, 30 + v[0] * 26, top, 70 + v[1] * 18, Color.rgb(26, 28, 32));
            }
            for (double rx : new double[]{-88, -62, 62, 88}) {
                comp("ram", 2, 14, cH * 0.8, 16, rx, top, 40, Color.rgb(30, 32, 36));
            }
            comp("ssd", 2, 80, cH, 22, 100, top, 36, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -100, top, 36, Color.rgb(47, 58, 72));

            // ---- tier 3: camara de vapor, heatpipes y 2 ventiladores
            double cool0 = top + cH + 0.1;
            comp("cooling", 3, 120, 1.0, 70, 4, cool0, 70, Color.rgb(112, 118, 126));
            double pipeY = y(cool0 + 1.0, 2.6);
            cylinderX(innerRoot, "cooling", 3, false, 1.3, 200, 4, pipeY, 98, Color.rgb(176, 112, 66));
            cylinderX(innerRoot, "cooling", 3, false, 1.3, 200, 4, pipeY, 106, Color.rgb(176, 112, 66));
            comp("cooling", 3, 70, 6, 10, 108, hB + 0.3, 116, Color.rgb(190, 122, 72));
            comp("cooling", 3, 70, 6, 10, -108, hB + 0.3, 116, Color.rgb(190, 122, 72));
            fanAt("fan_right", 112, 82, 74, hB, ghost);
            fanAt("fan_left", -112, 82, 74, hB, ghost);
        }

        // ---- tier 4: cubierta C blanca (foto real) con teclado 3D, boton de encendido y touchpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, 10, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, 7, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, 10, 7, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("zep", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardZephyr(T / 2 - bodyFlat);
        }

        // ---- tier 5: tapa de aluminio con la pantalla OLED real
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, 10, 3, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, 10, 3, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 22), W / 2 - 22}) {
            slabR(lidPivot, "hinge", -1, 28, 9, lidT + 1.2, 2.0, 2.0, hx, -(lidT + 1.2) / 2 + 0.6, -4.5, Color.rgb(54, 55, 59));
        }
        if (!ghost) {
            cylinderY(lidPivot, "lid", -1, 1.7, 0.3, 0, 0.12, -Dl + 3.2, Color.rgb(30, 30, 34));
            cylinderY(lidPivot, "lid", -1, 0.9, 0.4, 0, 0.18, -Dl + 3.2, Color.rgb(6, 8, 16));
            decal(lidPivot, "display", -1, quad(Textures.resource("zep", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("zep", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    /** Teclado 3D de la Zephyrus segun la foto cenital (sin numpad, altavoces a los lados). */
    private void addKeyboardZephyr(double topY) {
        double keyH = 2.0;
        java.util.Map<String, Image> cache = new java.util.HashMap<>();
        java.util.List<Object[]> ks = new java.util.ArrayList<>();
        double yHot = 343, yF = 376, yN = 410, yQ = 451, yA = 491, yZ = 532, yB = 573;
        double[] hx = {279, 322, 364, 406};
        String[] hl = {"vol-", "vol+", "mic", "rog"};
        for (int i = 0; i < 4; i++) {
            ks.add(new Object[]{hl[i], hx[i], yHot, 36.0, 20.0});
        }
        ks.add(new Object[]{"esc", 221.0, yF, 36.0, 26.0});
        for (int i = 0; i < 12; i++) {
            ks.add(new Object[]{"F" + (i + 1), 279 + 42.4 * i, yF, 38.0, 26.0});
        }
        ks.add(new Object[]{"del", 787.0, yF, 36.0, 26.0});
        ks.add(new Object[]{"`", 218.0, yN, 26.0, 34.0});
        String[] r1 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "="};
        for (int i = 0; i < 12; i++) {
            ks.add(new Object[]{r1[i], 255 + 42.4 * i, yN, 38.0, 34.0});
        }
        ks.add(new Object[]{"back", 775.0, yN, 62.0, 34.0});
        ks.add(new Object[]{"tab", 227.0, yQ, 50.0, 34.0});
        String[] r2 = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]"};
        for (int i = 0; i < 12; i++) {
            ks.add(new Object[]{r2[i], 275 + 42.4 * i, yQ, 38.0, 34.0});
        }
        ks.add(new Object[]{"\\", 786.0, yQ, 38.0, 34.0});
        ks.add(new Object[]{"caps", 232.0, yA, 62.0, 34.0});
        String[] r3 = {"A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'"};
        for (int i = 0; i < 11; i++) {
            ks.add(new Object[]{r3[i], 286 + 42.4 * i, yA, 38.0, 34.0});
        }
        ks.add(new Object[]{"enter", 757.0, yA, 78.0, 34.0});
        ks.add(new Object[]{"shift", 229.0, yZ, 76.0, 34.0});
        String[] r4 = {"Z", "X", "C", "V", "B", "N", "M", ",", ".", "/"};
        for (int i = 0; i < 10; i++) {
            ks.add(new Object[]{r4[i], 307 + 42.4 * i, yZ, 38.0, 34.0});
        }
        ks.add(new Object[]{"shift", 758.0, yZ, 90.0, 34.0});
        ks.add(new Object[]{"ctrl", 222.0, yB, 38.0, 34.0});
        ks.add(new Object[]{"fn", 265.0, yB, 38.0, 34.0});
        ks.add(new Object[]{"WIN", 308.0, yB, 38.0, 34.0});
        ks.add(new Object[]{"alt", 350.0, yB, 38.0, 34.0});
        ks.add(new Object[]{"", 478.0, yB, 208.0, 34.0});
        ks.add(new Object[]{"alt", 604.0, yB, 38.0, 34.0});
        ks.add(new Object[]{"cop", 647.0, yB, 38.0, 34.0});
        ks.add(new Object[]{"\u25C0", 692.0, yB + 6, 40.0, 20.0});
        ks.add(new Object[]{"\u25B2", 738.0, yB - 9, 40.0, 16.0});
        ks.add(new Object[]{"\u25BC", 738.0, yB + 9, 40.0, 16.0});
        ks.add(new Object[]{"\u25B6", 783.0, yB + 6, 40.0, 20.0});
        double k = 2.237;
        for (Object[] kd : ks) {
            String label = (String) kd[0];
            double kx = ((Double) kd[1] - 504.0) / k;
            double zf = (822.0 - (Double) kd[2]) / k;
            double kw = (Double) kd[3] / k, kh = (Double) kd[4] / k;
            int wpx = Math.max(16, (int) Math.round(kw * 9)), hpx = Math.max(16, (int) Math.round(kh * 9));
            String main = switch (label) {
                case "WIN" -> "\u229E";
                case "cop" -> "\u25C9";
                default -> label;
            };
            Image tile = cache.computeIfAbsent(main + "|" + wpx + "x" + hpx, t -> Textures.keycap(main, "", Color.rgb(244, 245, 248),
                    Color.rgb(104, 124, 214), wpx, hpx));
            double z = -D / 2 + zf;
            partItem(shellRoot, Shapes.keycapBody(kw, kh, keyH, 0.8, 2.2), "deck", 4, false, Color.rgb(160, 162, 170), null, kx, topY - keyH / 2, z);
            partItem(shellRoot, Shapes.keycapTop(kw, kh, keyH, 0.8, 2.2), "deck", 4, false, Color.rgb(176, 178, 186), tile, kx, topY - keyH / 2, z);
        }
        // boton de encendido hexagonal (arriba a la derecha)
        double bx = (745.0 - 504.0) / k, bz = -D / 2 + (822.0 - 340.0) / k;
        double[][] hex = new double[6][2];
        for (int i = 0; i < 6; i++) {
            hex[i][0] = 7.2 * Math.cos(Math.toRadians(60 * i + 30));
            hex[i][1] = 7.2 * Math.sin(Math.toRadians(60 * i + 30));
        }
        partItem(shellRoot, Shapes.prism(hex, 1.4), "deck", 4, false, Color.rgb(214, 216, 222), null, bx, topY - 0.7, bz);
        Image pw = Textures.keycap("PWR", "", Color.rgb(236, 238, 242), Color.rgb(90, 104, 190), 128, 128);
        cylinderY(shellRoot, "deck", 4, 4.2, 0.2, bx, topY - 1.45, bz, Color.rgb(236, 238, 242));
        partItem(shellRoot, Shapes.slabCap(8, 8, 0.2, 3.5, 3.5, 0.05, true, Shapes.UV_SCREEN), "deck", 4, false, Color.WHITE, pw, bx, topY - 1.55, bz);
        // borde del touchpad (150 x 105 mm aprox.)
        partItem(shellRoot, Shapes.ring(150.0, 107.0, 0.5, 3.0, 3.0, 0.6), "deck", 4, false, Color.rgb(190, 192, 198), null,
                0, topY - 0.25, -D / 2 + 58.0);
    }

    /** Puertos 3D de la Zephyrus segun las fotos laterales. */
    private void addPortsZephyr(double deckTop, double bodyFlat) {
        double yc = deckTop + bodyFlat * 0.40;
        // izquierdo (desde atras): DC, HDMI, Thunderbolt 4, USB-A y audio
        tufRect(-1, D / 2 - 28.8, yc, 7.5, 4.2, null);
        tufRect(-1, D / 2 - 47.8, yc, 15.0, 5.4, null);
        tufStadium(-1, D / 2 - 67.6, yc, 9.0, 3.6);
        tufRect(-1, D / 2 - 86.7, yc, 13.2, 5.2, Color.rgb(60, 62, 70));
        double jz = D / 2 - 104.2;
        cylinderX(shellRoot, "base", 0, false, 2.9, 0.5, -(W / 2 + 0.25), yc, jz, Color.rgb(78, 80, 86));
        cylinderX(shellRoot, "base", 0, false, 2.2, 0.25, -(W / 2 + 0.42), yc, jz, Color.rgb(3, 3, 5));
        // derecho (desde delante): lector SD, USB-A y USB-C
        tufRect(1, -D / 2 + 157.8, yc, 22.0, 2.4, null);
        tufRect(1, -D / 2 + 184.6, yc, 13.2, 5.2, Color.rgb(60, 62, 70));
        tufStadium(1, -D / 2 + 204.0, yc, 9.0, 3.6);
    }

    /* ------------------------------------------------------------------ ASUS ROG Strix SCAR 18 (2026) G835 */

    private static boolean isScar(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("SCAR 18");
    }

    private void fanAt(String name, double fx, double fz, double dia, double hB, boolean ghost) {
        comp("cooling", 3, dia, 8, dia, fx, hB + 0.4, fz, Color.rgb(24, 26, 30));
        if (ghost) {
            return;
        }
        double topY = y(hB + 0.4, 8) - 4.1;
        double r = dia * 0.46;
        Group g = new Group();
        MeshView blades = quad(Textures.blades(), flat(-r, -r, r, -r, r, r, -r, r, 0));
        Rotate spin = new Rotate(0, Rotate.Y_AXIS);
        g.getTransforms().add(spin);
        g.getChildren().add(blades);
        fanRotates.add(spin);
        Group holder = new Group(g);
        holder.setTranslateX(fx);
        holder.setTranslateZ(fz);
        holder.setTranslateY(topY);
        holder.setUserData("cooling");
        g.setUserData("cooling");
        blades.setUserData("cooling");
        innerRoot.getChildren().add(holder);
        items.add(new Item(holder, "cooling", 3, topY, false, true, null, (PhongMaterial) blades.getMaterial(), null));
    }

    private void buildScar() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.0, hD = 3.0, bodyFlat = 17.0, hZ = bodyFlat - hB - hD;
        double lidT = 6.5, shelf = 28.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#27282c");
        Color baseCol = Color.web("#202125");

        // cuna: 23,5 mm delante y 35 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 11.5 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta D, paredes, fotos de base/flancos/trasera y resplandor RGB
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            double gw = W + 44, gd = D + 44;
            decal(shellRoot, "base", 0, quad(Textures.resource("scar/photo", "glow.png"),
                    flat(-gw / 2, -gd / 2, gw / 2, -gd / 2, gw / 2, gd / 2, -gw / 2, gd / 2, 0)), yBot + 0.04, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("scar/photo", "bottom.png"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("scar/photo", "rear.png"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("scar/photo", "right.png"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("scar/photo", "left.png"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost && !loadParts("in/scar18").isEmpty()) {
            // interior por piezas (parts.csv): foto cenital del G835 + manual de servicio ASUS
            partsDir = "in/scar18";
            partsFloor = hB;
            partsCeil = hB + hZ;
            partsWall = wt;
            legion10Interior();
        } else if (!ghost) {
            // ---- tier 1: bateria de 90 Wh (4 celdas) en el frente y altavoces
            double batW = 303, batD = 66, batH = 7.0;
            double batZ = -D / 2 + wt + 6 + batD / 2, batX = -18;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, batX, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.0f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            innerRoot.getChildren().get(innerRoot.getChildren().size() - 1).setTranslateX(batX);
            comp("pcb", 1, 40, 8, 14, -168, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 168, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU, VRAM, memoria, SSD y Wi-Fi
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 327, pcbH, 190, -4, pcbZ0, 40, Color.rgb(26, 44, 62));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 40, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 46, cH, 46, -28, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}, {0, -1.6}, {0, 1.6}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -28 + v[0] * 36, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{8, -26}) {
                comp("ram", 2, 69.6, 3.0, 30, 16, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 22, cH, 80, -139, top, -10, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, 108, top, -6, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 64, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes, radiadores y 3 ventiladores (GPU, CPU y VRAM)
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 190, 1.6, 100, 6, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 250, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 250, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 250, 0, pipeY, 98, pipe);
            comp("cooling", 3, 76, 9, 12, 126, hB + 0.4, 118, copper);
            comp("cooling", 3, 76, 9, 12, -134, hB + 0.4, 118, copper);
            fanAt("fan_gpu", 124, 60, 82, hB, ghost);
            fanAt("fan_cpu", -132, 62, 82, hB, ghost);
            fanAt("fan_vram", 32, -30, 62, hB, ghost);
        }

        // ---- tier 4: cubierta C con teclado RGB, trackpad grande y repisa trasera
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("scar/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardAtlas("scar/photo", deckLen, T / 2 - bodyFlat, 2.2);
        }

        // ---- tier 5: tapa con pantalla Mini LED
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 22), W / 2 - 22}) {
            box(lidPivot, "hinge", -1, false, 28, 9, 16, hx, -4.5, -4, Color.rgb(30, 31, 34));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("scar/photo", "screen.png"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("scar/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    /* ------------------------------------------------------------------ Alienware 18 Area-51 (AA18250) */

    private static boolean isAlienware(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("AREA-51");
    }

    private void buildAlienware() {
        boolean ghost = tint != null;
        photoLum = 0.82;
        double wt = 2.6;
        double hB = 3.2, hD = 3.2, bodyFlat = 21.0, hZ = bodyFlat - hB - hD;
        double lidT = 7.5, shelf = 30.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#3b4448");
        Color baseCol = Color.web("#2f3a3e");

        // cuna: 24,72 mm delante y 30,5 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 5.8 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior, paredes y fotos de flancos y trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("aw/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("aw/photo", "rear.jpg"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("aw/photo", "right.jpg"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("aw/photo", "left.jpg"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost) {
            // ---- tier 1: bateria de 96 Wh (294,9 x 77,5 x 7,71 mm) y altavoces
            double batW = 294.9, batD = 77.5, batH = 7.7;
            double batZ = -D / 2 + wt + 14 + batD / 2, batX = 8;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, batX, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.0f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            innerRoot.getChildren().get(innerRoot.getChildren().size() - 1).setTranslateX(batX);
            comp("pcb", 1, 52, 11, 62, -176, hB + 0.4, -D / 2 + 46, Color.rgb(16, 18, 20));
            comp("pcb", 1, 52, 11, 62, 176, hB + 0.4, -D / 2 + 46, Color.rgb(16, 18, 20));

            // ---- tier 2: placa base, CPU, GPU, VRAM, memoria y 3 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 372, pcbH, 190, 0, pcbZ0, 52, Color.rgb(20, 56, 70));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.4;
            comp("cpu", 2, 34, cH, 34, 44, top, 70, Color.rgb(176, 180, 186));
            comp("gpu", 2, 50, cH, 50, -34, top, 70, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}, {0, -1.5}, {0, 1.5}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -34 + v[0] * 38, top, 70 + v[1] * 26, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{22, -12}) {
                comp("ram", 2, 69.6, 3.0, 30, 118, top, rz, Color.rgb(36, 112, 70));
            }
            for (int i = 0; i < 3; i++) {
                comp("ssd", 2, 80, cH, 22, -126, top, 28 - i * 26, Color.rgb(47, 58, 72));
            }
            comp("pcb", 2, 30, cH * 0.7, 22, 20, top, 8, Color.rgb(36, 112, 70));

            // ---- tier 3: disipador de vapor con heatpipes, 4 ventiladores
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 210, 1.8, 110, 4, cool0, 74, alu);
            double pipeY = y(cool0 + 1.8, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.2, 290, 0, pipeY, 54, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.2, 290, 0, pipeY, 74, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.2, 290, 0, pipeY, 94, pipe);
            fanAt("fan_1", -146, 92, 86, hB, ghost);
            fanAt("fan_2", 146, 92, 86, hB, ghost);
            fanAt("fan_3", -62, 30, 70, hB, ghost);
            fanAt("fan_4", 62, 30, 70, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el panel tactil iluminado
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("aw/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardAtlas("aw/photo", deckLen, T / 2 - bodyFlat, 2.2);
        }

        // ---- tier 5: tapa y pantalla WQXGA de 300 Hz
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 40), W / 2 - 40}) {
            box(lidPivot, "hinge", -1, false, 30, 10, 18, hx, -5, -5, Color.rgb(34, 38, 41));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("aw/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("aw/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    /* ------------------------------------------------------------------ Lenovo Legion Pro 7i Gen 10 */

    private static boolean isLegion(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("LEGION PRO 7I GEN");
    }

    private void buildLegion() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.0, hD = 3.0, bodyFlat = 17.0, hZ = bodyFlat - hB - hD;
        double lidT = 5.5, shelf = 31.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#2a2b2f");
        Color baseCol = Color.web("#232428");

        // cuna: 21,9 mm delante y 26,6 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 4.7 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior con la foto de las rejillas, flancos y trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("lg/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("lg/photo", "rear.jpg"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("lg/photo", "right.jpg"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("lg/photo", "left.jpg"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost && isLegionGen10()) {
            legion10Interior();
        } else if (!ghost) {
            // ---- tier 1: bateria de 99,9 Wh y altavoces
            double batW = 292, batD = 74, batH = 7.0;
            double batZ = -D / 2 + wt + 8 + batD / 2;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 40, 8, 14, -160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU y VRAM, 2 modulos de RAM y 2 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 330, pcbH, 150, 0, pcbZ0, 52, Color.rgb(24, 40, 56));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 36, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 44, cH, 44, -30, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -30 + v[0] * 34, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{34, 0}) {
                comp("ram", 2, 69.6, 3.0, 30, 100, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -108, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -108, top, -8, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 0, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes y 2 ventiladores grandes
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 170, 1.6, 100, 4, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 98, pipe);
            comp("cooling", 3, 74, 9, 12, 128, hB + 0.4, 118, copper);
            comp("cooling", 3, 74, 9, 12, -128, hB + 0.4, 118, copper);
            fanAt("fan_right", 122, 68, 86, hB, ghost);
            fanAt("fan_left", -122, 68, 86, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el trackpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("lg/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardAtlas("lg/photo", deckLen, T / 2 - bodyFlat, 2.2);
        }

        // ---- tier 5: tapa con pantalla OLED 16:10
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        boolean g10 = isLegionGen10();
        double ct = g10 ? L10_COVER_TIER : -1;      // en el Gen 10 la cubierta de la tapa es una capa propia del despiece
        int lidFrom = items.size();
        slabR(lidPivot, "lid", ct, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, ct, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            box(lidPivot, "hinge", ct, false, 30, 9, 16, hx, -4.5, -4, Color.rgb(30, 31, 34));
        }
        if (g10) {
            legion10Lid(ghost, Dl, lidT);
        } else if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("lg/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
        }
        if (!ghost) {
            decal(lidPivot, "lid", ct, quad(Textures.resource("lg/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
        if (g10) {
            tagFrom(lidFrom, "lcd_cover");
        }
    }

    /** Capas de la tapa del Gen 10 en el despiece (indice de separacion hacia atras): bisel y tapas de bisagra no se mueven. */
    static final double L10_COVER_TIER = 0.75, L10_CAM_TIER = 0.5, L10_PANEL_TIER = 0.25;
    /** Panel de 16" 16:10 (zona activa, mm) y marco visible derivado de la relacion pantalla/cuerpo del 92 % (PSREF). */
    static final double L10_AW = 344.6, L10_AH = 215.4, L10_BW = 359.5, L10_BH = 224.7, L10_TOP = 1.8, L10_HUMP = 5.0;

    /**
     * Tapa del Legion Pro 7i Gen 10 por capas, segun la figura 2 y la tabla 5 del manual (HMM): cubierta LCD con la joroba de la
     * camara, panel OLED, bisel, 2 tapas de bisagra, soporte de camara (2 tornillos M1.6 x 2.5, tabla 23), camara y gomas de microfono.
     * CONFIRMADO: lista de piezas, tornillos del soporte, panel 16" 16:10 (344,6 x 215,4 mm), camara 5 MP con E-shutter (PSREF).
     * DERIVADO: marco visible 359,5 x 224,7 mm (relacion pantalla/cuerpo 92 % del PSREF). ESTIMADO (sin dato oficial): joroba,
     * panel completo, soporte y placa de camara, tapas de bisagra, grosores. NO modelados: cable eDP, cable de camara, 4 gomas y cintas.
     */
    private void legion10Lid(boolean ghost, double Dl, double lt) {
        final double aw = L10_AW, ah = L10_AH, bw = L10_BW, bh = L10_BH, hump = L10_HUMP;
        final double sideB = (bw - aw) / 2, tbB = (bh - ah) / 2, ztop = -Dl + L10_TOP;
        Color dark = Color.rgb(24, 25, 28), black = Color.rgb(9, 10, 13);
        int f = items.size();
        // cubierta: joroba superior de la camara y ejes de bisagra (todo con la capa de la cubierta)
        partItem(lidPivot, Shapes.prism(new double[][]{{-50, 0.3}, {50, 0.3}, {46, -hump}, {-46, -hump}}, lt), "lid", L10_COVER_TIER, true,
                Color.rgb(30, 31, 35), null, 0, -lt / 2, -Dl);
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            cylinderX(lidPivot, "hinge", L10_COVER_TIER, false, 4.4, 34, hx, -4.4, 1.5, dark);
            cylinderX(lidPivot, "hinge", L10_COVER_TIER, false, 2.2, 38, hx, -4.4, 1.5, Color.rgb(96, 99, 106));
        }
        tagFrom(f, "lcd_cover");
        if (ghost) {
            return;
        }
        // bisel: marco de 4 tiras (el hueco es la zona activa) + tira bajo la joroba; no se mueve en el despiece
        f = items.size();
        double zb0 = ztop, zb1 = ztop + bh;
        box(lidPivot, "lid", -1, false, bw, 0.5, tbB, 0, -0.05, zb0 + tbB / 2, black);
        box(lidPivot, "lid", -1, false, bw, 0.5, tbB, 0, -0.05, zb1 - tbB / 2, black);
        box(lidPivot, "lid", -1, false, sideB, 0.5, bh - 2 * tbB, -(bw - sideB) / 2, -0.05, (zb0 + zb1) / 2, black);
        box(lidPivot, "lid", -1, false, sideB, 0.5, bh - 2 * tbB, (bw - sideB) / 2, -0.05, (zb0 + zb1) / 2, black);
        box(lidPivot, "lid", -1, false, 90, 0.5, hump + L10_TOP - 0.3, 0, -0.05, -Dl - hump / 2 + (L10_TOP - 0.3) / 2 - 0.15, black);
        tagFrom(f, "lcd_bezel");
        // tapas de bisagra (2 piezas negras en las esquinas inferiores, fig. 52); tamano estimado
        f = items.size();
        for (double sx : new double[]{-1, 1}) {
            box(lidPivot, "hinge", -1, false, 28, 0.5, 12, sx * (W / 2 - 15.8), -0.05, -7.0, dark);
        }
        tagFrom(f, "hinge_caps");
        // panel OLED completo (zona activa + 2 mm bajo el bisel por lado) con la imagen de la foto oficial
        f = items.size();
        box(lidPivot, "lid", L10_PANEL_TIER, false, aw + 4, 1.0, ah + 4, 0, -0.4, ztop + tbB + ah / 2, black);
        Image scr = cropFrac(Textures.resource("lg/photo", "screen.jpg"),
                new double[]{70 / 1620.0, 185 / 1012.0, 1550 / 1620.0, 880 / 1012.0});
        double zt = ztop + tbB, zb = zt + ah;
        decal(lidPivot, "display", L10_PANEL_TIER, quad(scr, flat(-aw / 2, zt, aw / 2, zt, aw / 2, zb, -aw / 2, zb, 0)), 0.14, 0);
        tagFrom(f, "oled_panel");
        // camara 5 MP (E-shutter): lente en la joroba, placa, soporte con 2 tornillos M1.6 x 2.5 y 2 gomas de microfono
        f = items.size();
        double zc = -Dl - 1.9;
        cylinderY(lidPivot, "lid", L10_CAM_TIER, 2.5, 0.3, 0, 0.2, zc, Color.rgb(40, 41, 45));
        cylinderY(lidPivot, "lid", L10_CAM_TIER, 1.7, 0.4, 0, 0.28, zc, Color.rgb(5, 6, 12));
        cylinderY(lidPivot, "lid", L10_CAM_TIER, 0.6, 0.45, 0.4, 0.32, zc - 0.4, Color.rgb(46, 66, 118));
        box(lidPivot, "lid", L10_CAM_TIER, false, 14, 0.8, 4.2, 0, -0.5, zc, Color.rgb(34, 70, 56));
        box(lidPivot, "lid", L10_CAM_TIER, false, 44, 0.5, 4.0, 0, -1.2, zc + 0.2, Color.rgb(120, 123, 130));
        for (double sx : new double[]{-20, 20}) {
            cylinderY(lidPivot, "lid", L10_CAM_TIER, 1.4, 0.5, sx, -0.7, zc + 0.2, Color.rgb(150, 152, 158));
        }
        for (double mx : new double[]{-26, 26}) {
            cylinderY(lidPivot, "lid", L10_CAM_TIER, 0.9, 0.4, mx, 0.15, zc, Color.rgb(2, 2, 3));
        }
        tagFrom(f, "camera");
    }

    /** Control de calidad de la tapa del Gen 10: piezas dentro de la tapa, panel dentro del hueco del bisel, camara en la joroba y despiece reversible. */
    public String qcLidGen10() {
        StringBuilder out = new StringBuilder();
        java.util.Map<String, double[]> a = qcBounds(true);
        String[] ids = {"lcd_cover", "lcd_bezel", "hinge_caps", "oled_panel", "camera"};
        int bad = 0;
        for (String id : ids) {
            double[] b = a.get(id);
            if (b == null) {
                out.append("SIN GEOMETRIA ").append(id).append('\n');
                bad++;
                continue;
            }
            // dentro de la huella de la tapa (la joroba sobresale por arriba hasta -Dl - hump)
            if (b[0] < -W / 2 - 0.5 || b[1] > W / 2 + 0.5 || b[5] > 6.5 || b[4] < -lidLen - L10_HUMP - 0.5 || b[3] > 0.6 || b[2] < -9.5) {
                out.append(String.format(Locale.US, "FUERA DE LA TAPA %s x[%.1f,%.1f] y[%.2f,%.2f] z[%.1f,%.1f]%n", id, b[0], b[1], b[2], b[3], b[4], b[5]));
                bad++;
            }
        }
        double[] pn = a.get("oled_panel"), bz = a.get("lcd_bezel");
        if (pn != null && bz != null) {
            if (pn[0] < bz[0] || pn[1] > bz[1] || pn[4] < bz[4] || pn[5] > bz[5] || pn[1] - pn[0] < L10_AW) {
                out.append("PANEL FUERA DEL BISEL\n");
                bad++;
            }
            if (pn[3] - bz[3] > 0.2) {
                out.append("PANEL SOBRESALE DEL BISEL\n");
                bad++;
            }
        }
        double[] cam = a.get("camera");
        if (cam != null && (cam[0] < -50 || cam[1] > 50)) {
            out.append("CAMARA FUERA DE LA JOROBA\n");
            bad++;
        }
        double save = explode;
        setExplode(1.0);
        java.util.Map<String, double[]> ex = qcBounds(true);
        double cy = ex.get("lcd_cover")[3], py = ex.get("oled_panel")[3], by = ex.get("lcd_bezel")[3], kmy = ex.get("camera")[3];
        // al separar, la cubierta debe quedar detras (y menor) de la camara y del panel, y el bisel delante de todo
        if (!(cy < kmy && kmy < py && py < by + 0.01)) {
            out.append(String.format(Locale.US, "ORDEN DE CAPAS EN EL DESPIECE cubierta=%.1f camara=%.1f panel=%.1f bisel=%.1f%n", cy, kmy, py, by));
            bad++;
        }
        setExplode(0);
        java.util.Map<String, double[]> back = qcBounds(true);
        for (String id : ids) {
            double[] x = a.get(id), y = back.get(id);
            if (x != null && y != null) {
                for (int i = 0; i < 6; i++) {
                    if (Math.abs(x[i] - y[i]) > 1e-6) {
                        out.append("NO VUELVE ").append(id).append('\n');
                        bad++;
                        break;
                    }
                }
            }
        }
        setExplode(save);
        out.append(String.format(Locale.US, "RESUMEN_TAPA capas=%d problemas=%d", ids.length, bad));
        return out.toString();
    }

    /* ------------------------------------------------------------------ HyperX OMEN MAX 16 */

    private static boolean isOmen(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("OMEN MAX 16");
    }

    private void buildOmen() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.0, hD = 3.0, bodyFlat = 17.0, hZ = bodyFlat - hB - hD;
        double lidT = 6.0, shelf = 26.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#3a3b3f");
        Color baseCol = Color.web("#2e2f33");

        // cuna: 26,15 mm delante y 28,75 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 2.6 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior con la foto de las rejillas, flancos y trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("om/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("om/photo", "rear.jpg"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("om/photo", "right.jpg"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("om/photo", "left.jpg"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost && !loadParts("in/omenmax").isEmpty()) {
            // interior por piezas (parts.csv): foto cenital del OMEN MAX 16 + manual HP
            partsDir = "in/omenmax";
            partsFloor = hB;
            partsCeil = hB + hZ;
            partsWall = wt;
            legion10Interior();
        } else if (!ghost) {
            // ---- tier 1: bateria de 99,9 Wh y altavoces
            double batW = 270, batD = 70, batH = 7.0;
            double batZ = -D / 2 + wt + 8 + batD / 2;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 40, 8, 14, -160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU y VRAM, 2 modulos de RAM y 2 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 330, pcbH, 150, 0, pcbZ0, 52, Color.rgb(24, 40, 56));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 36, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 44, cH, 44, -30, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -30 + v[0] * 34, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{34, 0}) {
                comp("ram", 2, 69.6, 3.0, 30, 100, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -108, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -108, top, -8, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 0, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes y 3 ventiladores
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 170, 1.6, 100, 4, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 98, pipe);
            comp("cooling", 3, 74, 9, 12, 128, hB + 0.4, 118, copper);
            comp("cooling", 3, 74, 9, 12, -128, hB + 0.4, 118, copper);
            fanAt("fan_right", 118, 66, 80, hB, ghost);
            fanAt("fan_left", -118, 66, 80, hB, ghost);
            fanAt("fan_third", 0, 8, 56, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el trackpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("om/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardAtlas("om/photo", deckLen, T / 2 - bodyFlat, 2.2);
        }

        // ---- tier 5: tapa con pantalla OLED 16:10
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            box(lidPivot, "hinge", -1, false, 30, 9, 16, hx, -4.5, -4, Color.rgb(30, 31, 34));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("om/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("om/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    /* ------------------------------------------------------------------ MacBook Pro 16 M5 Pro / M5 Max */

    private static boolean isMbp(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("MACBOOK PRO 16 (M");
    }

    private void buildMbp() {
        boolean ghost = tint != null;
        photoLum = 0.90;
        double wt = 2.0;
        double hB = 2.4, hD = 2.0, bodyFlat = 11.8, hZ = bodyFlat - hB - hD;
        double lidT = 5.0, shelf = 8.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#2a2b2e");
        Color baseCol = Color.web("#232427");

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior de una sola pieza con las fotos de flancos
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("mb/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("mb/photo", "rear.jpg"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("mb/photo", "right.jpg"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("mb/photo", "left.jpg"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost) {
            // ---- tier 1: 6 celdas de bateria (100 Wh) y altavoces, segun la foto interior
            double bz = hB + 0.3, bh = 5.8;
            box(innerRoot, "battery", 1, false, 77, bh, 86, -114, y(bz, bh), -65, Color.rgb(24, 25, 28));
            box(innerRoot, "battery", 1, false, 77, bh, 86, 114, y(bz, bh), -65, Color.rgb(24, 25, 28));
            box(innerRoot, "battery", 1, false, 64, bh, 70, -39, y(bz, bh), -59, Color.rgb(24, 25, 28));
            box(innerRoot, "battery", 1, false, 64, bh, 70, 44, y(bz, bh), -59, Color.rgb(24, 25, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.0f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(24, 25, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-26, 8, 26, 8, 26, -8, -26, -8, 0)), y(bz, bh) - bh / 2 - 0.08, -59);
            comp("pcb", 1, 22, 5, 30, -166, hB + 0.3, -88, Color.rgb(15, 16, 18));
            comp("pcb", 1, 22, 5, 30, 166, hB + 0.3, -88, Color.rgb(15, 16, 18));

            // ---- tier 2: placa en dos alas con el chip M5 y la memoria en el mismo encapsulado
            double pcbZ0 = hB + 0.3, pcbH = 1.2;
            comp("pcb", 2, 332, pcbH, 130, 0, pcbZ0, 52, Color.rgb(20, 24, 30));
            double top = pcbZ0 + pcbH + 0.05;
            comp("cpu", 2, 33, 2.4, 33, 0, top, 50, Color.rgb(176, 180, 186));
            comp("ram", 2, 14, 1.4, 22, -24, top, 50, Color.rgb(40, 44, 52));
            comp("ram", 2, 14, 1.4, 22, 24, top, 50, Color.rgb(40, 44, 52));
            comp("gpu", 2, 33, 0.6, 33, 0, top + 2.4, 50, Color.rgb(120, 124, 132));
            comp("ssd", 2, 20, 1.2, 14, -52, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 20, 1.2, 14, 52, top, 20, Color.rgb(47, 58, 72));

            // ---- tier 3: disipador de cobre y 2 ventiladores
            double cool0 = top + 2.5;
            comp("cooling", 3, 150, 1.0, 14, 0, cool0, 84, Color.rgb(176, 112, 66));
            fanAt("fan_left", -110, 39, 62, hB - 1.2, ghost);
            fanAt("fan_right", 110, 39, 62, hB - 1.2, ghost);
        }

        // ---- tier 4: cubierta C (teclado, altavoces y panel tactil)
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("mb/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardAtlas("mb/photo", deckLen, T / 2 - bodyFlat, 2.0);
        }

        // ---- tier 5: tapa Liquid Retina XDR con muesca de camara
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("mb/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("mb/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    /* ------------------------------------------------------------------ HP ZBook Fury 16 G11 */

    private static boolean isZbook(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("ZBOOK FURY 16");
    }

    private void buildZbook() {
        boolean ghost = tint != null;
        photoLum = 0.92;
        double wt = 2.4;
        double hB = 3.2, hD = 3.0, bodyFlat = 22.0, hZ = bodyFlat - hB - hD;
        double lidT = 5.7, shelf = 9.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#a4a8ae");
        Color baseCol = Color.web("#8d9197");

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior de aluminio con las fotos de los flancos
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("zb/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("zb/photo", "rear.jpg"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("zb/photo", "right.jpg"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("zb/photo", "left.jpg"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost && !loadParts("in/zbook").isEmpty()) {
            // interior por piezas (parts.csv): foto cenital del ZBook Fury G10/G11 + manual HP
            partsDir = "in/zbook";
            partsFloor = hB;
            partsCeil = hB + hZ;
            partsWall = wt;
            legion10Interior();
        } else if (!ghost) {
            // posiciones tomadas de la foto del interior (cubierta inferior retirada)
            // ---- tier 1: bateria de 95 Wh (8 celdas) y altavoces
            double bh = 7.2, bz = hB + 0.4;
            box(innerRoot, "battery", 1, false, 256, bh, 56, -41, y(bz, bh), -88, Color.rgb(22, 24, 28));
            box(innerRoot, "battery", 1, false, 76, bh, 54, 125, y(bz, bh), -83, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.0f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-34, 12, 34, 12, 34, -12, -34, -12, 0)), y(bz, bh) - bh / 2 - 0.08, -88);
            innerRoot.getChildren().get(innerRoot.getChildren().size() - 1).setTranslateX(-41);
            comp("pcb", 1, 40, 6, 12, -150, hB + 0.4, -122, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 6, 12, 150, hB + 0.4, -122, Color.rgb(15, 16, 18));

            // ---- tier 2: placa, procesador, GPU, 4 SODIMM, 4 SSD y WLAN
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 326, pcbH, 150, 0, pcbZ0, 35, Color.rgb(20, 70, 120));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.4;
            comp("cpu", 2, 36, cH, 36, -40, top, 66, Color.rgb(176, 180, 186));
            comp("gpu", 2, 48, cH, 48, 14, top, 66, Color.rgb(46, 49, 56));
            double[][] vr = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vr) {
                comp("gpu", 2, 13, cH * 0.8, 14, 14 + v[0] * 36, top, 66 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{22, -10}) {
                comp("ram", 2, 69.6, 3.0, 30, 35, top, rz, Color.rgb(36, 112, 70));
                comp("ram", 2, 69.6, 3.0, 30, 35, top + 3.2, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -64, top, -8, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -64, top, -32, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -64, top + 2.6, -8, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -64, top + 2.6, -32, Color.rgb(47, 58, 72));
            comp("pcb", 2, 22, cH * 0.7, 30, -152, top, -40, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor con heatpipes y 2 ventiladores
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(120, 126, 134), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 140, 1.8, 66, -14, cool0, 68, alu);
            double pipeY = y(cool0 + 1.8, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 250, 0, pipeY, 54, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 250, 0, pipeY, 72, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 250, 0, pipeY, 90, pipe);
            fanAt("fan_left", -133, 43, 64, hB, ghost);
            fanAt("fan_right", 127, 38, 64, hB, ghost);
        }

        // ---- tier 4: cubierta C con teclado, trackpad con 3 botones y lector de huellas
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("zb/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardAtlas("zb/photo", deckLen, T / 2 - bodyFlat, 2.0);
        }

        // ---- tier 5: tapa con pantalla 16:10 y camara de 5 MP
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 26), W / 2 - 26}) {
            box(lidPivot, "hinge", -1, false, 30, 9, 16, hx, -4.5, -4, Color.rgb(90, 93, 98));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("zb/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("zb/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    /* ------------------------------------------------------------------ Acer Predator Helios 18 (PH18-71) */

    private static boolean isHelios(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("HELIOS 18");
    }

    private void buildHelios() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.2, hD = 3.0, bodyFlat = 22.0, hZ = bodyFlat - hB - hD;
        double lidT = 6.0, shelf = 28.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#1c2036");
        Color baseCol = Color.web("#151829");

        // cuna: 26,9 mm delante y 28,9 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 2.0 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior con la foto de las rejillas, flancos y trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("ac/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("ac/photo", "rear.png"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("ac/photo", "right.png"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("ac/photo", "left.png"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost && !loadParts("in/helios18").isEmpty()) {
            // interior por piezas (parts.csv): foto cenital del PH18-71 + manual Acer
            partsDir = "in/helios18";
            partsFloor = hB;
            partsCeil = hB + hZ;
            partsWall = wt;
            legion10Interior();
        } else if (!ghost) {
            // ---- tier 1: bateria de 99,9 Wh y altavoces
            double batW = 300, batD = 76, batH = 7.0;
            double batZ = -D / 2 + wt + 8 + batD / 2;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 40, 8, 14, -160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU y VRAM, 2 modulos de RAM y 2 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 360, pcbH, 170, 0, pcbZ0, 52, Color.rgb(24, 60, 54));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 36, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 44, cH, 44, -30, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -30 + v[0] * 34, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{34, 0}) {
                comp("ram", 2, 69.6, 3.0, 30, 100, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -108, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -108, top, -8, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 0, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes y 2 ventiladores grandes
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 170, 1.6, 100, 4, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 98, pipe);
            comp("cooling", 3, 74, 9, 12, 160, hB + 0.4, 126, copper);
            comp("cooling", 3, 74, 9, 12, -160, hB + 0.4, 126, copper);
            fanAt("fan_right", 134, 70, 92, hB, ghost);
            fanAt("fan_left", -134, 70, 92, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el trackpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("ac/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardAtlas("ac/photo", deckLen, T / 2 - bodyFlat, 2.2);
        }

        // ---- tier 5: tapa con pantalla OLED 16:10
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            box(lidPivot, "hinge", -1, false, 30, 9, 16, hx, -4.5, -4, Color.rgb(30, 31, 34));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("ac/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("ac/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    /* ------------------------------------------------------------------ Acer Predator Helios 16 AI (PH16-73) */

    private static boolean isHelios16(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("HELIOS 16 AI");
    }

    private void buildHelios16() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.2, hD = 3.0, bodyFlat = 17.0, hZ = bodyFlat - hB - hD;
        double lidT = 6.0, shelf = 30.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#1b1c21");
        Color baseCol = Color.web("#141519");

        // cuna: la ficha da 15,9 mm delante y 27,7 mm detras; aqui 23 mm delante y 27,7 mm detras para que quepa el interior
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 4.7 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior con la foto de las rejillas, flancos y trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("h16/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("h16/photo", "front.jpg"),
                    pts(-W / 2, yTop + 4, -D / 2 - 0.1, W / 2, yTop + 4, -D / 2 - 0.1, W / 2, yBot - 2, -D / 2 - 0.1, -W / 2, yBot - 2, -D / 2 - 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("h16/photo", "rear.png"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("h16/photo", "right.png"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("h16/photo", "left.png"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost && !loadParts("in/helios16ai").isEmpty()) {
            // interior por piezas (parts.csv): foto cenital del PH16-73 + manual Acer (FRU y despiece)
            partsDir = "in/helios16ai";
            partsFloor = hB;
            partsCeil = hB + hZ;
            partsWall = wt;
            legion10Interior();
        } else if (!ghost) {
            // ---- tier 1: bateria de 99,9 Wh y altavoces
            double batW = 292, batD = 74, batH = 7.0;
            double batZ = -D / 2 + wt + 8 + batD / 2;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 40, 8, 14, -160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU y VRAM, 2 modulos de RAM y 2 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 330, pcbH, 150, 0, pcbZ0, 52, Color.rgb(24, 40, 56));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 36, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 44, cH, 44, -30, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -30 + v[0] * 34, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{34, 0}) {
                comp("ram", 2, 69.6, 3.0, 30, 100, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -108, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -108, top, -8, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 0, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes y 2 ventiladores grandes
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 170, 1.6, 100, 4, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 98, pipe);
            comp("cooling", 3, 74, 9, 12, 128, hB + 0.4, 118, copper);
            comp("cooling", 3, 74, 9, 12, -128, hB + 0.4, 118, copper);
            fanAt("fan_right", 122, 68, 86, hB, ghost);
            fanAt("fan_left", -122, 68, 86, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el trackpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("h16/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardAtlas("h16/photo", deckLen, T / 2 - bodyFlat, 2.0);
        }

        // ---- tier 5: tapa con pantalla OLED 16:10
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            box(lidPivot, "hinge", -1, false, 30, 9, 16, hx, -4.5, -4, Color.rgb(30, 31, 34));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("h16/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("h16/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    /* ------------------------------------------------------------------ HP Victus 16 */

    private static boolean isVictus(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("VICTUS 16");
    }

    private void buildVictus() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.0, hD = 3.0, bodyFlat = 18.4, hZ = bodyFlat - hB - hD;
        double lidT = 5.5, shelf = 12.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#4a4b50");
        Color baseCol = Color.web("#3c3d42");

        // cuna: 21,9 mm delante y 26,6 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 1.0 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior con la foto de las rejillas, flancos y trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("vc/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("vc/photo", "rear.jpg"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("vc/photo", "right.png"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("vc/photo", "left.png"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost) {
            for (double fx : new double[]{-W / 2 + 28, W / 2 - 28}) {
                for (double fz : new double[]{-D / 2 + 24, D / 2 - 22}) {
                    slabR(shellRoot, "base", 0, 22, 16, 2.6, 6, 6, fx, T / 2 + 1.3, fz, Color.rgb(20, 20, 22));
                }
            }
        }
        if (!ghost && !loadParts("in/victus").isEmpty()) {
            // interior por piezas (parts.csv): foto cenital del Victus 16 + manual HP
            partsDir = "in/victus";
            partsFloor = hB;
            partsCeil = hB + hZ;
            partsWall = wt;
            legion10Interior();
        } else if (!ghost) {
            // ---- tier 1: bateria de 99,9 Wh y altavoces
            double batW = 270, batD = 70, batH = 7.0;
            double batZ = -D / 2 + wt + 8 + batD / 2;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 40, 8, 14, -160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU y VRAM, 2 modulos de RAM y 2 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 330, pcbH, 150, 0, pcbZ0, 52, Color.rgb(24, 40, 56));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 36, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 44, cH, 44, -30, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -30 + v[0] * 34, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{34, 0}) {
                comp("ram", 2, 69.6, 3.0, 30, 100, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -108, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -108, top, -8, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 0, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes y 2 ventiladores grandes
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 170, 1.6, 100, 4, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 98, pipe);
            comp("cooling", 3, 74, 9, 12, 128, hB + 0.4, 118, copper);
            comp("cooling", 3, 74, 9, 12, -128, hB + 0.4, 118, copper);
            fanAt("fan_right", 118, 66, 80, hB, ghost);
            fanAt("fan_left", -118, 66, 80, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el trackpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("vc/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboard(deckCz, deckLen, T / 2 - bodyFlat);
        }

        // ---- tier 5: tapa con pantalla OLED 16:10
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            box(lidPivot, "hinge", -1, false, 30, 9, 16, hx, -4.5, -4, Color.rgb(30, 31, 34));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("vc/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("vc/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    private void addKeyboard(double deckCz, double deckLen, double topY) {
        String[][] rows = {
                {"ESC", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "INS", "DEL"},
                {"`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BKSP"},
                {"TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\"},
                {"CAPS", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "ENTER"},
                {"SHIFT", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "SHIFT"},
                {"CTRL", "FN", "WIN", "ALT", "", "ALT", "CTRL", "<", "v", ">"}};
        double[][] wts = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2},
                {1.5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1.5},
                {1.8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.2},
                {2.3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.7},
                {1.3, 1, 1.2, 1.2, 5.2, 1.2, 1.2, 1, 1, 1}};
        String[][] num = {{"NUM", "/", "*", "-"}, {"7", "8", "9", "+"}, {"4", "5", "6", ""}, {"1", "2", "3", "ENT"}, {"0", "", ".", ""}};
        double pitch = 18.4, gap = 1.6, keyH = 2.2;
        double mainW = 15 * pitch, numW = 4 * pitch;
        double x0 = -(mainW + 8 + numW) / 2;
        double zBack = deckLen / 2 - 14;
        java.util.Map<String, javafx.scene.image.Image> cache = new java.util.HashMap<>();
        Color keyCol = Color.rgb(34, 35, 39);
        for (int r = 0; r < rows.length; r++) {
            double rowH = r == 0 ? 11 : pitch - gap;
            double zc = zBack - (r == 0 ? 6 : 14 + (r - 1) * pitch + (pitch - gap) / 2);
            double total = 0;
            for (double w : wts[r]) {
                total += w;
            }
            double unit = mainW / total;
            double x = x0;
            for (int k = 0; k < rows[r].length; k++) {
                double kw = wts[r][k] * unit - gap;
                key(x + kw / 2 + gap / 2, zc, kw, rowH, rows[r][k], deckCz, topY, keyH, keyCol, cache);
                x += wts[r][k] * unit;
            }
            if (r > 0 && r - 1 < num.length) {
                double nx = x0 + mainW + 8;
                for (int k = 0; k < 4; k++) {
                    if (num[r - 1][k].isEmpty()) {
                        continue;
                    }
                    key(nx + k * pitch + (pitch - gap) / 2, zc, pitch - gap, rowH, num[r - 1][k], deckCz, topY, keyH, keyCol, cache);
                }
            }
        }
        // panel tactil
        box(shellRoot, "deck", 4, false, 112, 0.7, 70, -22, topY - 0.35, deckCz - deckLen / 2 + 60, Color.rgb(44, 45, 49));
    }

    private void key(double x, double zc, double w, double h, String label, double deckCz, double topY, double keyH,
                     Color col, java.util.Map<String, javafx.scene.image.Image> cache) {
        box(shellRoot, "deck", 4, false, w, keyH, h, x, topY - keyH / 2, deckCz + zc, col);
        if (label.isEmpty() || label.length() > 4) {
            return;
        }
        javafx.scene.image.Image img = cache.computeIfAbsent(label, t -> Textures.label(t, Color.rgb(34, 35, 39),
                Color.rgb(190, 192, 198), 96, 96, t.length() > 2 ? 26 : 44));
        double s = Math.min(w, h) * 0.8;
        Item it = decal(shellRoot, "deck", 4, quad(img, flat(-s / 2, s / 2, s / 2, s / 2, s / 2, -s / 2, -s / 2, -s / 2, 0)),
                topY - keyH - 0.06, deckCz + zc);
        it.node.setTranslateX(x);
    }
    /* ------------------------------------------------------------------ Dell G16 7630 */

    private static boolean isDellG16(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("G16 7630");
    }

    private void buildDellG16() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.0, hD = 3.0, bodyFlat = 18.9, hZ = bodyFlat - hB - hD;
        double lidT = 6.0, shelf = 22.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#3a3a42");
        Color baseCol = Color.web("#2e2e36");

        // cuna: 21,9 mm delante y 26,6 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 4.7 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior con la foto de las rejillas, flancos y trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("dg/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("dg/photo", "rear.png"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("dg/photo", "right.png"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("dg/photo", "left.png"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost) {
            for (double fx : new double[]{-W / 2 + 28, W / 2 - 28}) {
                for (double fz : new double[]{-D / 2 + 24, D / 2 - 22}) {
                    slabR(shellRoot, "base", 0, 22, 16, 2.6, 6, 6, fx, T / 2 + 1.3, fz, Color.rgb(20, 20, 22));
                }
            }
        }
        if (!ghost) {
            // ---- tier 1: bateria de 99,9 Wh y altavoces
            double batW = 290, batD = 72, batH = 7.0;
            double batZ = -D / 2 + wt + 8 + batD / 2;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 40, 8, 14, -160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU y VRAM, 2 modulos de RAM y 2 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 330, pcbH, 150, 0, pcbZ0, 52, Color.rgb(24, 40, 56));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 36, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 44, cH, 44, -30, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -30 + v[0] * 34, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{34, 0}) {
                comp("ram", 2, 69.6, 3.0, 30, 100, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -108, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -108, top, -8, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 0, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes y 2 ventiladores grandes
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 170, 1.6, 100, 4, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 98, pipe);
            comp("cooling", 3, 74, 9, 12, 128, hB + 0.4, 118, copper);
            comp("cooling", 3, 74, 9, 12, -128, hB + 0.4, 118, copper);
            fanAt("fan_right", 118, 66, 80, hB, ghost);
            fanAt("fan_left", -118, 66, 80, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el trackpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("dg/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardDell(deckCz, deckLen, T / 2 - bodyFlat);
        }

        // ---- tier 5: tapa con pantalla OLED 16:10
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            box(lidPivot, "hinge", -1, false, 30, 9, 16, hx, -4.5, -4, Color.rgb(30, 31, 34));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("dg/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("dg/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    private void addKeyboardDell(double deckCz, double deckLen, double topY) {
        String[][] rows = {
                {"ESC", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "INS", "DEL"},
                {"`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BKSP"},
                {"TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\"},
                {"CAPS", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "ENTER"},
                {"SHIFT", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "SHIFT"},
                {"CTRL", "FN", "WIN", "ALT", "", "ALT", "CTRL", "<", "v", ">"}};
        double[][] wts = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2},
                {1.5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1.5},
                {1.8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.2},
                {2.3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.7},
                {1.3, 1, 1.2, 1.2, 5.2, 1.2, 1.2, 1, 1, 1}};
        String[][] num = {};
        double pitch = 18.4, gap = 1.6, keyH = 2.2;
        double mainW = W * 0.86, numW = 0;
        double x0 = -mainW / 2;
        double zBack = deckLen / 2 - deckLen * 0.255;
        java.util.Map<String, javafx.scene.image.Image> cache = new java.util.HashMap<>();
        Color keyCol = Color.rgb(34, 35, 39);
        for (int r = 0; r < rows.length; r++) {
            double rowH = r == 0 ? 11 : pitch - gap;
            double zc = zBack - (r == 0 ? 6 : 14 + (r - 1) * pitch + (pitch - gap) / 2);
            double total = 0;
            for (double w : wts[r]) {
                total += w;
            }
            double unit = mainW / total;
            double x = x0;
            for (int k = 0; k < rows[r].length; k++) {
                double kw = wts[r][k] * unit - gap;
                keyDell(x + kw / 2 + gap / 2, zc, kw, rowH, rows[r][k], deckCz, topY, keyH, keyCol, cache);
                x += wts[r][k] * unit;
            }
            if (r > 0 && r - 1 < num.length) {
                double nx = x0 + mainW + 8;
                for (int k = 0; k < 4; k++) {
                    if (num[r - 1][k].isEmpty()) {
                        continue;
                    }
                    keyDell(nx + k * pitch + (pitch - gap) / 2, zc, pitch - gap, rowH, num[r - 1][k], deckCz, topY, keyH, keyCol, cache);
                }
            }
        }
        // panel tactil
        box(shellRoot, "deck", 4, false, 105, 0.7, 66, -24, topY - 0.35, deckCz + deckLen / 2 - 0.84 * deckLen, Color.rgb(46, 46, 52));
    }

    private void keyDell(double x, double zc, double w, double h, String label, double deckCz, double topY, double keyH,
                     Color col, java.util.Map<String, javafx.scene.image.Image> cache) {
        box(shellRoot, "deck", 4, false, w, keyH, h, x, topY - keyH / 2, deckCz + zc, col);
        if (label.isEmpty() || label.length() > 4) {
            return;
        }
        javafx.scene.image.Image img = cache.computeIfAbsent(label, t -> Textures.label(t, Color.rgb(34, 35, 39),
                Color.rgb(190, 192, 198), 96, 96, t.length() > 2 ? 26 : 44));
        double s = Math.min(w, h) * 0.8;
        Item it = decal(shellRoot, "deck", 4, quad(img, flat(-s / 2, s / 2, s / 2, s / 2, s / 2, -s / 2, -s / 2, -s / 2, 0)),
                topY - keyH - 0.06, deckCz + zc);
        it.node.setTranslateX(x);
    }
    /* ------------------------------------------------------------------ Gigabyte AORUS 17X */

    private static boolean isAorus(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("AORUS 17X");
    }

    private void buildAorus() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.0, hD = 3.0, bodyFlat = 16.3, hZ = bodyFlat - hB - hD;
        double lidT = 5.5, shelf = 32.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#35363b");
        Color baseCol = Color.web("#292a2f");

        // cuna: 21,9 mm delante y 26,6 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 1.5 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior con la foto de las rejillas, flancos y trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("ao/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("ao/photo", "rear.png"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("ao/photo", "right.png"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("ao/photo", "left.png"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost) {
            for (double fx : new double[]{-W / 2 + 28, W / 2 - 28}) {
                for (double fz : new double[]{-D / 2 + 24, D / 2 - 22}) {
                    slabR(shellRoot, "base", 0, 22, 16, 2.6, 6, 6, fx, T / 2 + 1.3, fz, Color.rgb(20, 20, 22));
                }
            }
        }
        if (!ghost) {
            // ---- tier 1: bateria de 99,9 Wh y altavoces
            double batW = 300, batD = 76, batH = 7.0;
            double batZ = -D / 2 + wt + 8 + batD / 2;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 40, 8, 14, -160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU y VRAM, 2 modulos de RAM y 2 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 330, pcbH, 150, 0, pcbZ0, 52, Color.rgb(24, 40, 56));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 36, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 44, cH, 44, -30, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -30 + v[0] * 34, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{34, 0}) {
                comp("ram", 2, 69.6, 3.0, 30, 100, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -108, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -108, top, -8, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 0, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes y 2 ventiladores grandes
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 170, 1.6, 100, 4, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 98, pipe);
            comp("cooling", 3, 74, 9, 12, 128, hB + 0.4, 118, copper);
            comp("cooling", 3, 74, 9, 12, -128, hB + 0.4, 118, copper);
            fanAt("fan_right", 112, 68, 86, hB, ghost);
            fanAt("fan_left", -112, 68, 86, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el trackpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("ao/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardAorus(deckCz, deckLen, T / 2 - bodyFlat);
        }

        // ---- tier 5: tapa con pantalla OLED 16:10
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            box(lidPivot, "hinge", -1, false, 30, 9, 16, hx, -4.5, -4, Color.rgb(30, 31, 34));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("ao/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("ao/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    private void addKeyboardAorus(double deckCz, double deckLen, double topY) {
        String[][] rows = {
                {"ESC", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "INS", "DEL"},
                {"`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BKSP"},
                {"TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\"},
                {"CAPS", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "ENTER"},
                {"SHIFT", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "SHIFT"},
                {"CTRL", "FN", "WIN", "ALT", "", "ALT", "CTRL", "<", "v", ">"}};
        double[][] wts = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2},
                {1.5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1.5},
                {1.8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.2},
                {2.3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.7},
                {1.3, 1, 1.2, 1.2, 5.2, 1.2, 1.2, 1, 1, 1}};
        String[][] num = {{"NUM", "/", "*", "-"}, {"7", "8", "9", "+"}, {"4", "5", "6", ""}, {"1", "2", "3", "ENT"}, {"0", "", ".", ""}};
        double pitch = 18.9, gap = 1.6, keyH = 2.2;
        double mainW = 15 * pitch, numW = 4 * pitch;
        double x0 = -(mainW + 8 + numW) / 2;
        double zBack = deckLen / 2 - deckLen * 0.165;
        java.util.Map<String, javafx.scene.image.Image> cache = new java.util.HashMap<>();
        Color keyCol = Color.rgb(34, 35, 39);
        for (int r = 0; r < rows.length; r++) {
            double rowH = r == 0 ? 11 : pitch - gap;
            double zc = zBack - (r == 0 ? 6 : 14 + (r - 1) * pitch + (pitch - gap) / 2);
            double total = 0;
            for (double w : wts[r]) {
                total += w;
            }
            double unit = mainW / total;
            double x = x0;
            for (int k = 0; k < rows[r].length; k++) {
                double kw = wts[r][k] * unit - gap;
                keyAorus(x + kw / 2 + gap / 2, zc, kw, rowH, rows[r][k], deckCz, topY, keyH, keyCol, cache);
                x += wts[r][k] * unit;
            }
            if (r > 0 && r - 1 < num.length) {
                double nx = x0 + mainW + 8;
                for (int k = 0; k < 4; k++) {
                    if (num[r - 1][k].isEmpty()) {
                        continue;
                    }
                    keyAorus(nx + k * pitch + (pitch - gap) / 2, zc, pitch - gap, rowH, num[r - 1][k], deckCz, topY, keyH, keyCol, cache);
                }
            }
        }
        // panel tactil
        box(shellRoot, "deck", 4, false, 120, 0.7, 74, -21, topY - 0.35, deckCz + deckLen / 2 - 0.805 * deckLen, Color.rgb(48, 49, 54));
    }

    private void keyAorus(double x, double zc, double w, double h, String label, double deckCz, double topY, double keyH,
                     Color col, java.util.Map<String, javafx.scene.image.Image> cache) {
        box(shellRoot, "deck", 4, false, w, keyH, h, x, topY - keyH / 2, deckCz + zc, col);
        if (label.isEmpty() || label.length() > 4) {
            return;
        }
        javafx.scene.image.Image img = cache.computeIfAbsent(label, t -> Textures.label(t, Color.rgb(34, 35, 39),
                Color.rgb(190, 192, 198), 96, 96, t.length() > 2 ? 26 : 44));
        double s = Math.min(w, h) * 0.8;
        Item it = decal(shellRoot, "deck", 4, quad(img, flat(-s / 2, s / 2, s / 2, s / 2, s / 2, -s / 2, -s / 2, -s / 2, 0)),
                topY - keyH - 0.06, deckCz + zc);
        it.node.setTranslateX(x);
    }
    /* ------------------------------------------------------------------ Razer Blade 18 (2024) */

    private static boolean isBlade18(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("BLADE 1");
    }

    private void buildBlade18() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.0, hD = 3.0, bodyFlat = 17.0, hZ = bodyFlat - hB - hD;
        double lidT = 5.0, shelf = 24.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#1f1f22");
        Color baseCol = Color.web("#17171a");

        // cuna: 21,9 mm delante y 26,6 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 0.6 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior con la foto de las rejillas, flancos y trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("rz/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("rz/photo", "rear.jpg"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("rz/photo", "right.png"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("rz/photo", "left.png"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost) {
            for (double fx : new double[]{-W / 2 + 28, W / 2 - 28}) {
                for (double fz : new double[]{-D / 2 + 24, D / 2 - 22}) {
                    slabR(shellRoot, "base", 0, 22, 16, 2.6, 6, 6, fx, T / 2 + 1.3, fz, Color.rgb(20, 20, 22));
                }
            }
        }
        if (!ghost) {
            // ---- tier 1: bateria de 99,9 Wh y altavoces
            double batW = 330, batD = 58, batH = 7.0;
            double batZ = -D / 2 + wt + 8 + batD / 2;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 40, 8, 14, -160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU y VRAM, 2 modulos de RAM y 2 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 330, pcbH, 150, 0, pcbZ0, 52, Color.rgb(24, 40, 56));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 36, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 44, cH, 44, -30, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -30 + v[0] * 34, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{34, 0}) {
                comp("ram", 2, 69.6, 3.0, 30, 100, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -108, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -108, top, -8, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 0, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes y 2 ventiladores grandes
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 170, 1.6, 100, 4, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 98, pipe);
            comp("cooling", 3, 74, 9, 12, 128, hB + 0.4, 118, copper);
            comp("cooling", 3, 74, 9, 12, -128, hB + 0.4, 118, copper);
            fanAt("fan_right", 118, 70, 84, hB, ghost);
            fanAt("fan_left", -118, 70, 84, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el trackpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("rz/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardBlade(deckCz, deckLen, T / 2 - bodyFlat);
        }

        // ---- tier 5: tapa con pantalla OLED 16:10
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            box(lidPivot, "hinge", -1, false, 30, 9, 16, hx, -4.5, -4, Color.rgb(30, 31, 34));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("rz/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("rz/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    private void addKeyboardBlade(double deckCz, double deckLen, double topY) {
        String[][] rows = {
                {"ESC", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "INS", "DEL"},
                {"`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BKSP"},
                {"TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\"},
                {"CAPS", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "ENTER"},
                {"SHIFT", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "SHIFT"},
                {"CTRL", "FN", "WIN", "ALT", "", "ALT", "CTRL", "<", "v", ">"}};
        double[][] wts = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2},
                {1.5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1.5},
                {1.8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.2},
                {2.3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.7},
                {1.3, 1, 1.2, 1.2, 5.2, 1.2, 1.2, 1, 1, 1}};
        String[][] num = {};
        double pitch = 19.4, gap = 1.6, keyH = 2.2;
        double mainW = 287, numW = 0;
        double x0 = -mainW / 2;
        double zBack = deckLen / 2 - deckLen * 0.14;
        java.util.Map<String, javafx.scene.image.Image> cache = new java.util.HashMap<>();
        Color keyCol = Color.rgb(34, 35, 39);
        for (int r = 0; r < rows.length; r++) {
            double rowH = r == 0 ? 11 : pitch - gap;
            double zc = zBack - (r == 0 ? 6 : 14 + (r - 1) * pitch + (pitch - gap) / 2);
            double total = 0;
            for (double w : wts[r]) {
                total += w;
            }
            double unit = mainW / total;
            double x = x0;
            for (int k = 0; k < rows[r].length; k++) {
                double kw = wts[r][k] * unit - gap;
                keyBlade(x + kw / 2 + gap / 2, zc, kw, rowH, rows[r][k], deckCz, topY, keyH, keyCol, cache);
                x += wts[r][k] * unit;
            }
            if (r > 0 && r - 1 < num.length) {
                double nx = x0 + mainW + 8;
                for (int k = 0; k < 4; k++) {
                    if (num[r - 1][k].isEmpty()) {
                        continue;
                    }
                    keyBlade(nx + k * pitch + (pitch - gap) / 2, zc, pitch - gap, rowH, num[r - 1][k], deckCz, topY, keyH, keyCol, cache);
                }
            }
        }
        // panel tactil
        box(shellRoot, "deck", 4, false, 169, 0.7, 93, 0, topY - 0.35, deckCz + deckLen / 2 - 0.80 * deckLen, Color.rgb(34, 34, 37));
    }

    private void keyBlade(double x, double zc, double w, double h, String label, double deckCz, double topY, double keyH,
                     Color col, java.util.Map<String, javafx.scene.image.Image> cache) {
        box(shellRoot, "deck", 4, false, w, keyH, h, x, topY - keyH / 2, deckCz + zc, col);
        if (label.isEmpty() || label.length() > 4) {
            return;
        }
        javafx.scene.image.Image img = cache.computeIfAbsent(label, t -> Textures.label(t, Color.rgb(34, 35, 39),
                Color.rgb(190, 192, 198), 96, 96, t.length() > 2 ? 26 : 44));
        double s = Math.min(w, h) * 0.8;
        Item it = decal(shellRoot, "deck", 4, quad(img, flat(-s / 2, s / 2, s / 2, s / 2, s / 2, -s / 2, -s / 2, -s / 2, 0)),
                topY - keyH - 0.06, deckCz + zc);
        it.node.setTranslateX(x);
    }
    /* ------------------------------------------------------------------ Alienware m18 R2 */

    private static boolean isM18(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("M18 R2");
    }

    private void buildM18() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.0, hD = 3.0, bodyFlat = 18.6, hZ = bodyFlat - hB - hD;
        double lidT = 6.5, shelf = 30.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#585a63");
        Color baseCol = Color.web("#3e3f46");

        // cuna: 21,9 mm delante y 26,6 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 1.0 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior con la foto de las rejillas, flancos y trasera
        slabR(shellRoot, "base", 0, W, D, hB, RF, RB, 0, y(0, hB), 0, baseCol);
        wallR(shellRoot, "base", 0, W, D, hZ, RF, RB, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, RF, RB, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            decal(shellRoot, "base", 0, quad(Textures.resource("am/photo", "bottom.jpg"),
                    flat(W / 2, D / 2, -W / 2, D / 2, -W / 2, -D / 2, W / 2, -D / 2, 0)), yBot + 0.08, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("am/photo", "rear.png"),
                    pts(W / 2, yTop, D / 2 + 0.1, -W / 2, yTop, D / 2 + 0.1, -W / 2, yBot, D / 2 + 0.1, W / 2, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("am/photo", "right.png"),
                    pts(W / 2 + 0.1, yTop, -D / 2, W / 2 + 0.1, yTop, D / 2, W / 2 + 0.1, yBot, D / 2, W / 2 + 0.1, yBot, -D / 2)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("am/photo", "left.png"),
                    pts(-W / 2 - 0.1, yTop, D / 2, -W / 2 - 0.1, yTop, -D / 2, -W / 2 - 0.1, yBot, -D / 2, -W / 2 - 0.1, yBot, D / 2)), 0, 0);
        }

        if (!ghost) {
            for (double fx : new double[]{-W / 2 + 28, W / 2 - 28}) {
                for (double fz : new double[]{-D / 2 + 24, D / 2 - 22}) {
                    slabR(shellRoot, "base", 0, 22, 16, 2.6, 6, 6, fx, T / 2 + 1.3, fz, Color.rgb(20, 20, 22));
                }
            }
        }
        if (!ghost) {
            // ---- tier 1: bateria de 99,9 Wh y altavoces
            double batW = 295, batD = 78, batH = 7.7;
            double batZ = -D / 2 + wt + 8 + batD / 2;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 40, 8, 14, -160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU y VRAM, 2 modulos de RAM y 2 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 330, pcbH, 150, 0, pcbZ0, 52, Color.rgb(24, 40, 56));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 36, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 44, cH, 44, -30, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -30 + v[0] * 34, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{34, 0}) {
                comp("ram", 2, 69.6, 3.0, 30, 100, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -108, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -108, top, -8, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 0, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes y 2 ventiladores grandes
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 170, 1.6, 100, 4, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 98, pipe);
            comp("cooling", 3, 74, 9, 12, 128, hB + 0.4, 118, copper);
            comp("cooling", 3, 74, 9, 12, -128, hB + 0.4, 118, copper);
            fanAt("fan_right", 112, 68, 86, hB, ghost);
            fanAt("fan_left", -112, 68, 86, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el trackpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        slabR(shellRoot, "deck", 4, W, deckLen, hD, RF, 1.5, 0, deckY, deckCz, shell);
        slabR(shellRoot, "deck", 4, W, shelf, hD, 1.5, RB, 0, deckY, D / 2 - shelf / 2, shell);
        addWireOutline(shellRoot, 4, W, D, RF, RB, hD, 0, deckY, 0);
        if (!ghost) {
            decal(shellRoot, "deck", 4, quad(Textures.resource("am/photo", "deck.jpg"),
                    flat(-W / 2, deckLen / 2, W / 2, deckLen / 2, W / 2, -deckLen / 2, -W / 2, -deckLen / 2, 0)),
                    T / 2 - bodyFlat - 0.08, deckCz);
            addKeyboardM18(deckCz, deckLen, T / 2 - bodyFlat);
        }

        // ---- tier 5: tapa con pantalla OLED 16:10
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        slabR(lidPivot, "lid", -1, W, Dl, lidT, RLT, RLB, 0, -lidT / 2, -Dl / 2, shell);
        addWireOutline(lidPivot, -1, W, Dl, RLT, RLB, lidT, 0, -lidT / 2, -Dl / 2);
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            box(lidPivot, "hinge", -1, false, 30, 9, 16, hx, -4.5, -4, Color.rgb(30, 31, 34));
        }
        if (!ghost) {
            decal(lidPivot, "display", -1, quad(Textures.resource("am/photo", "screen.jpg"),
                    flat(-W / 2, -Dl, W / 2, -Dl, W / 2, 0, -W / 2, 0, 0)), 0.08, 0);
            decal(lidPivot, "lid", -1, quad(Textures.resource("am/photo", "lid.jpg"),
                    flat(W / 2, -Dl, -W / 2, -Dl, -W / 2, 0, W / 2, 0, 0)), -lidT - 0.08, 0);
        }
    }

    private void addKeyboardM18(double deckCz, double deckLen, double topY) {
        String[][] rows = {
                {"ESC", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "INS", "DEL"},
                {"`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BKSP"},
                {"TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\"},
                {"CAPS", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "ENTER"},
                {"SHIFT", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "SHIFT"},
                {"CTRL", "FN", "WIN", "ALT", "", "ALT", "CTRL", "<", "v", ">"}};
        double[][] wts = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2},
                {1.5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1.5},
                {1.8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.2},
                {2.3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.7},
                {1.3, 1, 1.2, 1.2, 5.2, 1.2, 1.2, 1, 1, 1}};
        String[][] num = {{"NUM", "/", "*", "-"}, {"7", "8", "9", "+"}, {"4", "5", "6", ""}, {"1", "2", "3", "ENT"}, {"0", "", ".", ""}};
        double pitch = 19.0, gap = 1.6, keyH = 2.2;
        double mainW = 15 * pitch, numW = 4 * pitch;
        double x0 = -(mainW + 8 + numW) / 2;
        double zBack = deckLen / 2 - deckLen * 0.215;
        java.util.Map<String, javafx.scene.image.Image> cache = new java.util.HashMap<>();
        Color keyCol = Color.rgb(34, 35, 39);
        for (int r = 0; r < rows.length; r++) {
            double rowH = r == 0 ? 11 : pitch - gap;
            double zc = zBack - (r == 0 ? 6 : 14 + (r - 1) * pitch + (pitch - gap) / 2);
            double total = 0;
            for (double w : wts[r]) {
                total += w;
            }
            double unit = mainW / total;
            double x = x0;
            for (int k = 0; k < rows[r].length; k++) {
                double kw = wts[r][k] * unit - gap;
                keyM18(x + kw / 2 + gap / 2, zc, kw, rowH, rows[r][k], deckCz, topY, keyH, keyCol, cache);
                x += wts[r][k] * unit;
            }
            if (r > 0 && r - 1 < num.length) {
                double nx = x0 + mainW + 8;
                for (int k = 0; k < 4; k++) {
                    if (num[r - 1][k].isEmpty()) {
                        continue;
                    }
                    keyM18(nx + k * pitch + (pitch - gap) / 2, zc, pitch - gap, rowH, num[r - 1][k], deckCz, topY, keyH, keyCol, cache);
                }
            }
        }
        // panel tactil
        box(shellRoot, "deck", 4, false, 124, 0.7, 76, 0, topY - 0.35, deckCz + deckLen / 2 - 0.795 * deckLen, Color.rgb(64, 65, 72));
    }

    private void keyM18(double x, double zc, double w, double h, String label, double deckCz, double topY, double keyH,
                     Color col, java.util.Map<String, javafx.scene.image.Image> cache) {
        box(shellRoot, "deck", 4, false, w, keyH, h, x, topY - keyH / 2, deckCz + zc, col);
        if (label.isEmpty() || label.length() > 4) {
            return;
        }
        javafx.scene.image.Image img = cache.computeIfAbsent(label, t -> Textures.label(t, Color.rgb(34, 35, 39),
                Color.rgb(190, 192, 198), 96, 96, t.length() > 2 ? 26 : 44));
        double s = Math.min(w, h) * 0.8;
        Item it = decal(shellRoot, "deck", 4, quad(img, flat(-s / 2, s / 2, s / 2, s / 2, s / 2, -s / 2, -s / 2, -s / 2, 0)),
                topY - keyH - 0.06, deckCz + zc);
        it.node.setTranslateX(x);
    }
    /* ------------------------------------------------------------------ ORIGIN PC EON18-X */

    private static boolean isEon18(Laptop l) {
        return l.getModel() != null && l.getModel().toUpperCase(Locale.ROOT).contains("EON18-X");
    }

    private void buildEon18() {
        boolean ghost = tint != null;
        photoLum = 0.80;
        double wt = 2.4;
        double hB = 3.0, hD = 3.0, bodyFlat = 22.0, hZ = bodyFlat - hB - hD;
        double lidT = 6.0, shelf = 26.0;
        hingeZ = D / 2 - shelf;
        double Dl = D - shelf;
        lidLen = Dl;
        Color shell = Color.web("#26262a");
        Color baseCol = Color.web("#1c1c1f");

        // cuna: 21,9 mm delante y 26,6 mm detras
        Rotate tilt = new Rotate(Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, 0.5 / D)))), 0, T / 2, -D / 2, Rotate.X_AXIS);
        innerRoot.getTransforms().add(tilt);
        shellRoot.getTransforms().add(tilt);

        if (first && !ghost) {
            double sw = W * 1.30, sd = D * 1.26;
            shadow = quad(Textures.shadow(), flat(-sw / 2, sd / 2, sw / 2, sd / 2, sw / 2, -sd / 2, -sw / 2, -sd / 2, T / 2 + 0.3));
            shadow.setMouseTransparent(true);
        }

        // ---- tier 0: cubierta inferior de una pieza con esquinas redondeadas y paredes huecas
        double rFo = 16, rBo = 9;
        Image imgBottom = ghost ? null : Textures.resource("op/photo", "bottom.jpg");
        slab(shellRoot, "base", 0, W, D, hB, rFo, rBo, 1.8, 0, y(0, hB), 0, baseCol, null, Shapes.UV_BOTTOM, imgBottom, Shapes.UV_BOTTOM);
        wall(shellRoot, "base", 0, W, D, hZ, rFo, rBo, wt, 0, y(hB, hZ), 0, shell);
        addWireOutline(shellRoot, 0, W, D, rFo, rBo, hB + hZ, 0, y(0, hB + hZ), 0);
        double yBot = T / 2, yTop = T / 2 - bodyFlat;
        if (!ghost) {
            double xr = W / 2 - rBo, zf = -D / 2 + rFo, zb = D / 2 - rBo;
            decal(shellRoot, "base", 0, quad(Textures.resource("op/photo", "rear.jpg"),
                    pts(xr, yTop, D / 2 + 0.1, -xr, yTop, D / 2 + 0.1, -xr, yBot, D / 2 + 0.1, xr, yBot, D / 2 + 0.1)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("op/photo", "right.jpg"),
                    pts(W / 2 + 0.1, yTop, zf, W / 2 + 0.1, yTop, zb, W / 2 + 0.1, yBot, zb, W / 2 + 0.1, yBot, zf)), 0, 0);
            decal(shellRoot, "base", 0, quad(Textures.resource("op/photo", "left.jpg"),
                    pts(-W / 2 - 0.1, yTop, zb, -W / 2 - 0.1, yTop, zf, -W / 2 - 0.1, yBot, zf, -W / 2 - 0.1, yBot, zb)), 0, 0);
        }

        if (!ghost) {
            for (double fx : new double[]{-W / 2 + 28, W / 2 - 28}) {
                for (double fz : new double[]{-D / 2 + 24, D / 2 - 22}) {
                    slab(shellRoot, "base", 0, 22, 16, 2.6, 6, 6, 0.7, fx, T / 2 + 1.3, fz, Color.rgb(20, 20, 22), null, 0, null, 0);
                }
            }
        }
        if (!ghost) {
            // ---- tier 1: bateria de 99,9 Wh y altavoces
            double batW = 310, batD = 80, batH = 8.0;
            double batZ = -D / 2 + wt + 8 + batD / 2;
            double batY = y(hB + 0.4, batH);
            box(innerRoot, "battery", 1, false, batW, batH, batD, 0, batY, batZ, Color.rgb(22, 24, 28));
            decal(innerRoot, "battery", 1, quad(Textures.label(String.format(Locale.US, "%.1f Wh", nz(d.getBatteryWh(), 0)),
                    Color.rgb(22, 24, 28), Color.rgb(150, 158, 170), 640, 200, 64),
                    flat(-70, 22, 70, 22, 70, -22, -70, -22, 0)), batY - batH / 2 - 0.08, batZ);
            comp("pcb", 1, 40, 8, 14, -160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));
            comp("pcb", 1, 40, 8, 14, 160, hB + 0.4, -D / 2 + 20, Color.rgb(15, 16, 18));

            // ---- tier 2: placa base, CPU, GPU y VRAM, 2 modulos de RAM y 2 SSD
            double pcbZ0 = hB + 0.4, pcbH = 1.4;
            comp("pcb", 2, 330, pcbH, 150, 0, pcbZ0, 52, Color.rgb(24, 40, 56));
            double top = pcbZ0 + pcbH + 0.05;
            double cH = 2.2;
            comp("cpu", 2, 32, cH, 32, 36, top, 84, Color.rgb(176, 180, 186));
            comp("gpu", 2, 44, cH, 44, -30, top, 84, Color.rgb(46, 49, 56));
            double[][] vram = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}};
            for (double[] v : vram) {
                comp("gpu", 2, 13, cH * 0.8, 14, -30 + v[0] * 34, top, 84 + v[1] * 24, Color.rgb(26, 28, 32));
            }
            for (double rz : new double[]{34, 0}) {
                comp("ram", 2, 69.6, 3.0, 30, 100, top, rz, Color.rgb(36, 112, 70));
            }
            comp("ssd", 2, 80, cH, 22, -108, top, 20, Color.rgb(47, 58, 72));
            comp("ssd", 2, 80, cH, 22, -108, top, -8, Color.rgb(47, 58, 72));
            comp("pcb", 2, 30, cH * 0.7, 22, 0, top, 20, Color.rgb(36, 112, 70));

            // ---- tier 3: camara de vapor, heatpipes y 2 ventiladores grandes
            double cool0 = top + cH + 0.1;
            Color alu = Color.rgb(104, 110, 118), copper = Color.rgb(190, 122, 72), pipe = Color.rgb(176, 112, 66);
            comp("cooling", 3, 170, 1.6, 100, 4, cool0, 82, alu);
            double pipeY = y(cool0 + 1.6, 6);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 66, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 82, pipe);
            cylinderX(innerRoot, "cooling", 3, false, 3.0, 240, 0, pipeY, 98, pipe);
            comp("cooling", 3, 74, 9, 12, 128, hB + 0.4, 118, copper);
            comp("cooling", 3, 74, 9, 12, -128, hB + 0.4, 118, copper);
            fanAt("fan_right", 112, 68, 86, hB, ghost);
            fanAt("fan_left", -112, 68, 86, hB, ghost);
        }

        // ---- tier 4: cubierta C con la foto del teclado y el trackpad
        double deckLen = Dl;
        double deckCz = -D / 2 + deckLen / 2;
        double deckY = y(hB + hZ, hD);
        Image imgDeck = ghost ? null : Textures.resource("op/photo", "deck.jpg");
        slab(shellRoot, "deck", 4, W, deckLen, hD, rFo, 1.5, 1.4, 0, deckY, deckCz, shell, imgDeck, Shapes.UV_DECK, null, 0);
        slab(shellRoot, "deck", 4, W, shelf, hD, 1.5, rBo, 1.4, 0, deckY, D / 2 - shelf / 2, shell, null, 0, null, 0);
        addWireOutline(shellRoot, 4, W, D, rFo, rBo, hD, 0, deckY, 0);
        if (!ghost) {
            addKeyboardEon(deckCz, deckLen, T / 2 - bodyFlat);
            addPowerButtonEon(deckCz, deckLen, T / 2 - bodyFlat);
        }

        // ---- tier 5: tapa con pantalla OLED 16:10
        lidPivot.getTransforms().add(lidRot);
        lidPivot.setTranslateY(yHinge);
        lidPivot.setTranslateZ(hingeZ);
        shellRoot.getChildren().add(lidPivot);
        Image imgScreen = ghost ? null : Textures.resource("op/photo", "screen.jpg");
        Image imgLid = ghost ? null : Textures.resource("op/photo", "lid.jpg");
        slab(lidPivot, "lid", -1, W, Dl, lidT, 14, 3, 1.3, 0, -lidT / 2, -Dl / 2, shell, imgLid, Shapes.UV_LID, imgScreen, Shapes.UV_SCREEN);
        addWireOutline(lidPivot, -1, W, Dl, 14, 3, lidT, 0, -lidT / 2, -Dl / 2);
        if (!ghost) {
            // la pantalla mira hacia el interior: cara del panel en y = -lidT + algo ... el modulo se apoya sobre ella
            addWebcamEon(-Dl + 4.6, 0.0);
        }
        for (double hx : new double[]{-(W / 2 - 24), W / 2 - 24}) {
            cylinderX(lidPivot, "hinge", -1, false, 4.8, 32, hx, -4.5, -3, Color.rgb(36, 37, 41));
        }
    }

    private void addKeyboardEon(double deckCz, double deckLen, double topY) {
        String[][] rows = {
                {"ESC", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "INS", "DEL"},
                {"`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BKSP"},
                {"TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\"},
                {"CAPS", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "ENTER"},
                {"SHIFT", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "SHIFT"},
                {"CTRL", "FN", "WIN", "ALT", "", "ALT", "CTRL", "<", "v", ">"}};
        double[][] wts = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2},
                {1.5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1.5},
                {1.8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.2},
                {2.3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.7},
                {1.3, 1, 1.2, 1.2, 5.2, 1.2, 1.2, 1, 1, 1}};
        String[][] num = {{"NUM", "/", "*", "-"}, {"7", "8", "9", "+"}, {"4", "5", "6", ""}, {"1", "2", "3", "ENT"}, {"0", "", ".", ""}};
        double pitch = 17.8, gap = 1.5, keyH = 2.2;
        double[] rowZ = {17.6, 35.6, 58.2, 80.6, 101.4, 123.2};
        double mainW = 15 * pitch, numW = 4 * 15.5;
        double x0 = -168;
        double zBack = deckLen / 2;
        java.util.Map<String, javafx.scene.image.Image> cache = new java.util.HashMap<>();
        Color keyCol = Color.rgb(34, 35, 39);
        for (int r = 0; r < rows.length; r++) {
            double rowH = r == 0 ? 11 : 19;
            double zc = zBack - rowZ[r];
            double total = 0;
            for (double w : wts[r]) {
                total += w;
            }
            double unit = mainW / total;
            double x = x0;
            for (int k = 0; k < rows[r].length; k++) {
                double kw = wts[r][k] * unit - gap;
                keyEon(x + kw / 2 + gap / 2, zc, kw, rowH, rows[r][k], deckCz, topY, keyH, keyCol, cache);
                x += wts[r][k] * unit;
            }
            if (r > 0 && r - 1 < num.length) {
                double nx = x0 + mainW + 8;
                for (int k = 0; k < 4; k++) {
                    if (num[r - 1][k].isEmpty()) {
                        continue;
                    }
                    keyEon(nx + k * 15.5 + (15.5 - gap) / 2, zc, 15.5 - gap, rowH, num[r - 1][k], deckCz, topY, keyH, keyCol, cache);
                }
            }
        }
        // panel tactil
        box(shellRoot, "deck", 4, false, 154, 0.7, 107, -31.4, topY - 0.35, deckCz + deckLen / 2 - 0.738 * deckLen, Color.rgb(40, 40, 44));
    }

    private void keyEon(double x, double zc, double w, double h, String label, double deckCz, double topY, double keyH,
                     Color col, java.util.Map<String, javafx.scene.image.Image> cache) {
        partItem(shellRoot, Shapes.slabBody(w, h, keyH, 2.4, 2.4, 0.7), "deck", 4, false, col, null, x, topY - keyH / 2, deckCz + zc);
        String sub = switch (label) {
            case "1" -> "!";
            case "2" -> "@";
            case "3" -> "#";
            case "4" -> "$";
            case "5" -> "%";
            case "6" -> "^";
            case "7" -> "&";
            case "8" -> "*";
            case "9" -> "(";
            case "0" -> ")";
            case "-" -> "_";
            case "=" -> "+";
            case "[" -> "{";
            case "]" -> "}";
            case "\\" -> "|";
            case ";" -> ":";
            case "'" -> "\"";
            case "," -> "<";
            case "." -> ">";
            case "/" -> "?";
            case "`" -> "~";
            default -> "";
        };
        String main = switch (label) {
            case "v" -> "▼";
            case "<" -> "◀";
            case ">" -> "▶";
            case "WIN" -> "⊞";
            case "BKSP" -> "⌫";
            case "ENTER" -> "ENTER";
            default -> label;
        };
        String key = main + "|" + sub + "|" + (int) w + "x" + (int) h;
        Image img = cache.computeIfAbsent(key, t -> Textures.keycap(main, sub, Color.rgb(30, 31, 35),
                Color.rgb(232, 234, 240), 128, (int) Math.max(40, Math.round(128 * h / w))));
        partItem(shellRoot, Shapes.slabCap(w, h, keyH, 2.4, 2.4, 0.7, true, Shapes.UV_SCREEN), "deck", 4, false, col, img,
                x, topY - keyH / 2, deckCz + zc);
    }

    /** Boton de encendido (arriba a la derecha de la cubierta C) con su aro luminoso. */
    private void addPowerButtonEon(double deckCz, double deckLen, double topY) {
        double zc = deckCz + deckLen / 2 - 8.5;
        double x = 160;
        partItem(shellRoot, Shapes.slabBody(15, 8, 1.6, 2.4, 2.4, 0.5), "deck", 4, false, Color.rgb(20, 20, 23), null, x, topY - 0.8, zc);
        Image ic = Textures.keycap("PWR", "", Color.rgb(20, 20, 23), Color.rgb(232, 234, 240), 128, 68);
        partItem(shellRoot, Shapes.slabCap(15, 8, 1.6, 2.4, 2.4, 0.5, true, Shapes.UV_SCREEN), "deck", 4, false, Color.rgb(20, 20, 23), ic,
                x, topY - 0.8, zc);
    }

    /** Modulo de camara en el marco superior de la tapa: carcasa, dos lentes, microfonos, LED y cubierta de privacidad. */
    private void addWebcamEon(double topEdgeZ, double faceY) {
        double cw = 40, ch = 5.2;
        partItem(lidPivot, Shapes.slabBody(cw, ch, 0.9, 2.2, 2.2, 0.3), "lid", -1, false, Color.rgb(8, 8, 9), null, 0, faceY + 0.45, topEdgeZ);
        double[] lx = {-6, 6};
        double ty = faceY + 0.9;
        for (double l : lx) {
            cylinderY(lidPivot, "lid", -1, 1.35, 0.3, l, ty + 0.05, topEdgeZ, Color.rgb(3, 4, 8));
            cylinderY(lidPivot, "lid", -1, 0.55, 0.3, l, ty + 0.12, topEdgeZ, Color.rgb(36, 60, 110));
        }
        for (double mx : new double[]{-14, 14}) {
            cylinderY(lidPivot, "lid", -1, 0.35, 0.3, mx, ty + 0.05, topEdgeZ, Color.rgb(30, 30, 34));
        }
        cylinderY(lidPivot, "lid", -1, 0.4, 0.3, 0, ty + 0.05, topEdgeZ, Color.rgb(70, 90, 70));
    }

    /* ------------------------------------------------------------------ cubiertas con forma (esquinas redondeadas y biseles) */

    private void partItem(Group parent, javafx.scene.shape.MeshView mv, String name, double tier, boolean shell, Color base,
                          Image map, double x, double yy, double z) {
        PhongMaterial m = new PhongMaterial(map != null ? Color.WHITE : base);
        Color spec = shell ? Color.gray(0.40) : Color.gray(0.16);
        m.setSpecularColor(spec);
        m.setSpecularPower(shell ? 30 : 12);
        if (map != null) {
            m.setDiffuseMap(map);
        }
        mv.setMaterial(m);
        mv.setTranslateX(x);
        mv.setTranslateY(yy);
        mv.setTranslateZ(z);
        mv.setUserData(name);
        parent.getChildren().add(mv);
        Item it = new Item(mv, name, tier, yy, shell, false, map != null ? Color.WHITE : base, m, spec);
        it.map = map;
        items.add(it);
    }

    private void slab(Group parent, String name, double tier, double w, double dd, double h, double rF, double rB, double b,
                      double x, double yy, double z, Color body, Image top, int topUv, Image bottom, int bottomUv) {
        partItem(parent, Shapes.slabBody(w, dd, h, rF, rB, b), name, tier, true, body, null, x, yy, z);
        partItem(parent, Shapes.slabCap(w, dd, h, rF, rB, b, true, topUv), name, tier, true, body, top, x, yy, z);
        partItem(parent, Shapes.slabCap(w, dd, h, rF, rB, b, false, bottomUv), name, tier, true, body, bottom, x, yy, z);
    }

    private void wall(Group parent, String name, double tier, double w, double dd, double h, double rF, double rB, double t,
                      double x, double yy, double z, Color col) {
        partItem(parent, Shapes.ring(w, dd, h, rF, rB, t), name, tier, true, col, null, x, yy, z);
    }

    private void addWireOutline(Group parent, double tier, double w, double dd, double rF, double rB, double h,
                                double x, double yy, double z) {
        double[][] o = Shapes.outline(w, dd, rF, rB);
        Group wg = new Group();
        for (int layer = 0; layer < 2; layer++) {
            double ly = layer == 0 ? -h / 2 : h / 2;
            for (int i = 0; i < o.length; i++) {
                double[] a = o[i], b = o[(i + 1) % o.length];
                double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
                if (len < 1e-3) {
                    continue;
                }
                Cylinder c = new Cylinder(0.35, len, 4);
                c.setRotationAxis(Rotate.Z_AXIS);
                c.setRotate(90);
                c.setMaterial(wireMat);
                Group seg = new Group(c);
                seg.setTranslateX((a[0] + b[0]) / 2);
                seg.setTranslateY(ly);
                seg.setTranslateZ((a[1] + b[1]) / 2);
                seg.setRotationAxis(Rotate.Y_AXIS);
                seg.setRotate(-Math.toDegrees(Math.atan2(b[1] - a[1], b[0] - a[0])));
                wg.getChildren().add(seg);
            }
        }
        wg.setTranslateX(x);
        wg.setTranslateY(yy);
        wg.setTranslateZ(z);
        wg.setVisible(false);
        wg.setMouseTransparent(true);
        parent.getChildren().add(wg);
        items.add(new Item(wg, "wire", tier, yy, false, true, null, wireMat, null, true));
    }

    private void chip(String name, double w, double dd, double x, double z, Color c, double top, double h) {
        box(innerRoot, name, 2, false, w, h, dd, x, y(top, h), z, c);
    }

    private void coolBox(double w, double dd, double x, double z, Color c, double cy0, double h) {
        box(innerRoot, "cooling", 3, false, w, h, dd, x, y(cy0, h), z, c);
    }

    private static int nzI(Integer v) {
        return v == null ? 0 : v;
    }

    /* ------------------------------------------------------------------ API */

    public Group getRoot() {
        return root;
    }

    public double width() {
        return W;
    }

    public double depth() {
        return D;
    }

    /** Desplazamiento vertical que centra el conjunto segun el despiece y la apertura de la tapa. */
    public double centerShift() {
        double bottom = T / 2;
        double top = yHinge - explode * 5 * GAP - lidLen * Math.max(0, Math.sin(Math.toRadians(lidDeg)));
        return -(top + bottom) / 2;
    }

    public void setExplode(double e) {
        this.explode = Math.max(0, Math.min(1, e));
        for (Item it : items) {
            if (it.tier > 0) {
                it.node.setTranslateY(it.baseY - explode * it.tier * GAP);
                it.node.setTranslateZ(it.baseZ + explode * it.tier * ZSTEP);
            }
        }
        lidPivot.setTranslateY(yHinge - explode * 5 * GAP);
        lidPivot.setTranslateZ(hingeZ + explode * 5 * ZSTEP);
    }

    public void setLid(double deg) {
        this.lidDeg = Math.max(0, Math.min(180, deg));
        lidRot.setAngle(-lidDeg);
    }

    public void setMode(Mode m) {
        this.mode = m;
        repaint();
    }

    public void setStress(boolean on) {
        this.stress = on;
        if (!on) {
            refreshScreen();
        }
    }

    /** La camara esta por debajo del equipo: la sombra de contacto taparia la vista. */
    public void setBelow(boolean b) {
        if (b != below) {
            below = b;
            repaint();
        }
    }

    public void select(String part) {
        this.selected = part;
        repaint();
    }

    private final java.util.Set<String> hiddenParts = new java.util.HashSet<>();

    /** Oculta o muestra todas las piezas de una categoria (cpu, gpu, ram, ssd, battery, cooling, pcb...). */
    public void setPartHidden(String part, boolean hidden) {
        if (hidden ? hiddenParts.add(part) : hiddenParts.remove(part)) {
            repaint();
        }
    }

    /** Carpetas de interior reconstruido pieza por pieza (parts.csv), segun el nombre del modelo. */
    private static final String[][] RECONSTRUCTED = {
            {"LEGION PRO 7I GEN 10", "in/legion10"}, {"LOQ", "in/loq15"}, {"SCAR 18", "in/scar18"},
            {"TUF GAMING A18", "in/tuf18"}, {"HELIOS 16 AI", "in/helios16ai"}, {"HELIOS 18", "in/helios18"},
            {"OMEN MAX 16", "in/omenmax"}, {"ZBOOK FURY 16", "in/zbook"}, {"VICTUS 16", "in/victus"}};

    /**
     * Estado del interior de un modelo. {@code reconstructed} es verdadero si existe un parts.csv propio;
     * {@code counts} da el numero de piezas por categoria y {@code sources} las fuentes citadas en esa tabla.
     */
    public record InteriorInfo(boolean reconstructed, java.util.Map<String, Integer> counts,
                               java.util.Map<String, java.util.Set<String>> sources) {
    }

    public static InteriorInfo interiorInfo(Laptop l) {
        String m = l == null || l.getModel() == null ? "" : l.getModel().toUpperCase(Locale.ROOT);
        for (String[] e : RECONSTRUCTED) {
            if (!m.contains(e[0])) {
                continue;
            }
            java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            java.util.Map<String, java.util.Set<String>> sources = new java.util.LinkedHashMap<>();
            for (PartSpec p : loadParts(e[1])) {
                String cat = switch (p.type) {
                    case "fan", "fins", "pipe", "plate" -> "cooling";
                    case "battery", "pcb", "ram", "ssd", "cpu", "gpu" -> p.type;
                    default -> null;
                };
                if (cat != null) {
                    counts.merge(cat, 1, Integer::sum);
                    sources.computeIfAbsent(cat, k -> new java.util.LinkedHashSet<>()).add(p.src);
                }
            }
            return new InteriorInfo(!counts.isEmpty(), counts, sources);
        }
        return new InteriorInfo(false, java.util.Map.of(), java.util.Map.of());
    }

    public void key(KeyCode code, boolean pressed) {
        if (keyboardMat == null) {
            return;
        }
        boolean changed = pressed ? down.add(code) : down.remove(code);
        if (changed) {
            Item ki = keyItems.get(code);
            if (ki != null) {
                ki.base = pressed ? accent : (ki.map != null ? Color.WHITE : Color.rgb(30, 33, 40));
                ki.mat.setDiffuseColor(ki.base);
            } else {
                keyboardMat.setDiffuseMap(Textures.keyboard(W, D, deckColor, accent, down));
            }
        }
    }

    public double cpuTemp() {
        return tCpu;
    }

    public double gpuTemp() {
        return tGpu;
    }

    public double stressWatts() {
        double w = nzI(d.getCpuBoostWatts()) + nzI(d.getGpuTgpWatts());
        if (w == 0) {
            w = "Apple".equalsIgnoreCase(d.getBrand()) ? 90 : 35;
        }
        return w;
    }

    public double noiseDba() {
        return stress ? Math.max(29, Math.min(52, 27 + stressWatts() * 0.08)) : 26;
    }

    /** Avanza la simulacion (ventiladores, temperaturas y animacion de la pantalla). */
    public void tick(double dt) {
        double speed = stress ? 1500 : 90;
        fanAngle = (fanAngle + speed * dt) % 360;
        for (Rotate r : fanRotates) {
            r.setAngle(fanAngle);
        }
        double w = stressWatts();
        double g = nzI(d.getGpuTgpWatts()) > 0 ? nzI(d.getGpuTgpWatts()) : w * 0.4;
        double[] target = stress
                ? new double[]{clamp(46 + w * 0.17, 52, 94), clamp(44 + g * 0.28, 50, 90), clamp(38 + w * 0.05, 40, 58),
                clamp(37 + w * 0.04, 38, 55), clamp(33 + w * 0.02, 34, 42)}
                : new double[]{44, 41, 38, 37, 33};
        double k = 1 - Math.exp(-dt / 2.2);
        double before = tCpu + tGpu;
        tCpu += (target[0] - tCpu) * k;
        tGpu += (target[1] - tGpu) * k;
        tRam += (target[2] - tRam) * k;
        tSsd += (target[3] - tSsd) * k;
        tBat += (target[4] - tBat) * k;
        if (mode == Mode.THERMAL && Math.abs(tCpu + tGpu - before) > 0.002) {
            repaint();
        }
        if (stress) {
            screenClock += dt;
            screenPhase += dt;
            if (screenClock > 0.14) {
                screenClock = 0;
                refreshScreen();
            }
        }
    }

    private void refreshScreen() {
        if (screenMat != null && tint == null) {
            double sw = W * 0.97, sd = D * 0.97;
            screenMat.setDiffuseMap(Textures.screen(d, sw, sd, accent, stress, screenPhase));
        }
    }

    private static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    /* ------------------------------------------------------------------ pintado por modo */

    private Color heatFor(Item it) {
        double t;
        switch (it.name) {
            case "cpu" -> t = tCpu;
            case "gpu" -> t = tGpu;
            case "ram" -> t = tRam;
            case "ssd" -> t = tSsd;
            case "battery" -> t = tBat;
            case "cooling" -> t = tCpu - 26;
            case "pcb" -> t = (tCpu + tGpu) / 2 - 14;
            default -> {
                return it.shell ? Color.rgb(18, 22, 28, 0.03) : Color.rgb(36, 40, 46);
            }
        }
        return Heat.at(t);
    }

    private void repaint() {
        boolean ghost = tint != null;
        Color acc = ghost ? tint : accent;
        if (shadow != null) {
            shadow.setVisible(mode == Mode.STUDIO && !below);
        }
        for (Item it : items) {
            if (it.wire) {
                boolean on = ghost || mode == Mode.XRAY;
                it.node.setVisible(on);
                wireMat.setDiffuseColor(Color.color(acc.getRed(), acc.getGreen(), acc.getBlue(), ghost ? 0.95 : 0.80));
                wireMat.setSpecularColor(Color.TRANSPARENT);
                continue;
            }
            if (it.decal) {
                boolean outerPhoto = "deck".equals(it.name) || "base".equals(it.name) || "lid".equals(it.name);
                it.node.setVisible(!ghost && !(outerPhoto && mode != Mode.STUDIO));
                boolean screen = "display".equals(it.name);
                double op = mode == Mode.XRAY ? (screen ? 0.40 : 0.07) : mode == Mode.THERMAL ? (screen ? 0.22 : 0.06) : 1.0;
                if (it.photo) {
                    op = mode == Mode.THERMAL ? 0.28 : 1.0;
                }
                if (it.mat != null) {
                    double lum = it.photo ? 1.0 : photoLum;
                    it.mat.setDiffuseColor(Color.color(lum, lum, lum, op));
                    it.mat.setSpecularColor(op < 1.0 ? Color.TRANSPARENT : Color.gray(0.12));
                }
                continue;
            }
            if (it.zone) {
                boolean sel = !ghost && selected != null && selected.equals(it.name);
                Color zc;
                if (sel) {
                    zc = Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.55);
                } else if (!ghost && mode == Mode.THERMAL) {
                    Color hc = heatFor(it);
                    zc = Color.color(hc.getRed(), hc.getGreen(), hc.getBlue(), 0.80);
                } else {
                    zc = Color.color(0, 0, 0, 0.02);
                }
                it.node.setVisible(!ghost);
                it.mat.setDiffuseColor(zc);
                it.mat.setSpecularColor(Color.TRANSPARENT);
                continue;
            }
            Color c;
            boolean translucent = false;
            it.node.setVisible(true);
            if (insidePhoto && mode == Mode.XRAY && "deck".equals(it.name)) {
                it.node.setVisible(false);       // cubierta, teclas y touchpad: dejan ver la placa real (queda el contorno)
                continue;
            }
            if (it.photo && !ghost && mode != Mode.THERMAL) {
                c = Color.gray(0.86);            // foto del interior: sin aclarar para no quemarla
                if (selected != null && selected.equals(it.name)) {
                    c = c.interpolate(accent, 0.22);
                }
                it.mat.setDiffuseColor(c);
                it.mat.setSpecularColor(Color.TRANSPARENT);
                continue;
            }
            if (ghost) {
                it.node.setVisible(it.shell);
                c = Color.color(tint.getRed() * 0.8, tint.getGreen() * 0.8, tint.getBlue() * 0.8, 0.07);
                translucent = true;
            } else if (mode == Mode.XRAY) {
                if (it.shell) {
                    c = Color.color(acc.getRed() * 0.8, acc.getGreen() * 0.8, acc.getBlue() * 0.8, 0.015);
                    translucent = true;
                } else {
                    c = it.base.interpolate(Color.WHITE, 0.16);
                    if (it.xray < 1.0) {
                        c = Color.color(c.getRed(), c.getGreen(), c.getBlue(), it.xray);
                        translucent = true;
                    }
                }
            } else if (mode == Mode.THERMAL) {
                c = heatFor(it);
                translucent = it.shell;
            } else {
                c = it.base;
            }
            if (!ghost && selected != null && selected.equals(it.name)) {
                c = c.interpolate(accent, it.map != null && "pcb".equals(it.name) ? 0.22 : 0.55);
            }
            if (it.map != null) {
                it.mat.setDiffuseMap(mode == Mode.THERMAL && !it.shell ? null : it.map);
            }
            it.mat.setDiffuseColor(c);
            it.mat.setSpecularColor(translucent ? Color.TRANSPARENT : it.spec);
            if (it.mats != null) {
                for (PhongMaterial pm : it.mats) {
                    pm.setDiffuseColor(c);
                    pm.setSpecularColor(translucent ? Color.TRANSPARENT : it.spec);
                }
            }
        }
        if (!hiddenParts.isEmpty()) {
            for (Item it : items) {
                if (hiddenParts.contains(it.name)) {
                    it.node.setVisible(false);
                }
            }
        }
    }
}
