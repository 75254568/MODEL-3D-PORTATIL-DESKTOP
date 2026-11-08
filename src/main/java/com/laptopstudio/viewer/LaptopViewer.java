package com.laptopstudio.viewer;

import com.laptopstudio.model.Laptop;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Visor 3D independiente (un SubScene con su propia camara, luces y raton).
 * El portatil A y el B usan dos instancias distintas, asi que nunca comparten movimiento.
 */
public final class LaptopViewer extends StackPane {

    public static final Color ACCENT_A = Color.web("#5ea5c7");
    public static final Color ACCENT_B = Color.web("#877fb0");

    public interface Listener {
        void onPart(String side, String part);

        void onCamera(String side, double rx, double ry, double zoom);
    }

    private final String side;
    private final Color accent;
    private final Listener listener;

    private final Group sceneRoot = new Group();
    private final Group rig = new Group();
    private final Group holder = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene sub;
    private final Scale scale = new Scale(1, 1, 1);
    private final Rotate rotX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotY = new Rotate(0, Rotate.Y_AXIS);
    private final Label hudName = new Label();
    private final Label hudTele = new Label();
    private final List<LaptopModel> models = new ArrayList<>();
    private final AnimationTimer timer;

    // camara: valor actual y valor objetivo (los presets se interpolan)
    private double rx = 26, ry = -32, zoom = 1;
    private double tRx = 26, tRy = -32, tZoom = 1;
    private double hx, hy, thx, thy;

    private LaptopModel.Mode mode = LaptopModel.Mode.STUDIO;
    private double explode;
    private double lid = 105;
    private boolean stress;
    private boolean sync;
    private boolean silent;
    private String selected;

    private boolean dragging;
    private double lastX, lastY, moved;
    private long lastNanos;
    private double hudClock;

    public LaptopViewer(String side, Listener listener) {
        this.side = side;
        this.accent = "B".equals(side) ? ACCENT_B : ACCENT_A;
        this.listener = listener;

        rig.getTransforms().addAll(scale, rotX, rotY);
        rig.getChildren().add(holder);
        AmbientLight ambient = new AmbientLight(Color.gray(0.50));
        PointLight key = new PointLight(Color.gray(1.0));
        key.setTranslateX(-700);
        key.setTranslateY(-1100);
        key.setTranslateZ(-1100);
        PointLight fill = new PointLight(Color.gray(0.38));
        fill.setTranslateX(900);
        fill.setTranslateY(-300);
        fill.setTranslateZ(-500);
        PointLight rim = new PointLight(Color.gray(0.28));
        rim.setTranslateX(0);
        rim.setTranslateY(-500);
        rim.setTranslateZ(1200);
        sceneRoot.getChildren().addAll(rig, ambient, key, fill, rim);

        camera.setNearClip(4);
        camera.setFarClip(8000);
        camera.setFieldOfView(30);
        sceneRoot.getChildren().add(camera);

        sub = new SubScene(sceneRoot, 10, 10, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.TRANSPARENT);
        setStyle("-fx-background-color: radial-gradient(center 50% 75%, radius 90%, #18202a 0%, #0d1015 60%, #090b0e 100%);");
        sub.setManaged(false);
        sub.relocate(0, 0);
        sub.widthProperty().bind(widthProperty());
        sub.heightProperty().bind(heightProperty());

        hudName.getStyleClass().add("hud-name");
        hudName.setStyle("-fx-text-fill: " + css(accent) + ";");
        hudTele.getStyleClass().add("hud-tele");
        hudName.setMouseTransparent(true);
        hudTele.setMouseTransparent(true);
        StackPane.setAlignment(hudName, Pos.TOP_LEFT);
        StackPane.setAlignment(hudTele, Pos.BOTTOM_LEFT);
        StackPane.setMargin(hudName, new Insets(10, 14, 0, 14));
        StackPane.setMargin(hudTele, new Insets(0, 14, 10, 14));

        getChildren().addAll(sub, hudName, hudTele);
        setMinSize(0, 0);
        setPrefSize(10, 10);
        widthProperty().addListener((o, a, b) -> layoutCamera());
        heightProperty().addListener((o, a, b) -> layoutCamera());

        installMouse();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = lastNanos == 0 ? 0.016 : Math.min(0.1, (now - lastNanos) / 1e9);
                lastNanos = now;
                step(dt);
            }
        };
        sceneProperty().addListener((o, a, b) -> {
            if (b == null) {
                timer.stop();
                lastNanos = 0;
            } else {
                timer.start();
            }
        });
    }

    private static String css(Color c) {
        return String.format(Locale.ROOT, "rgb(%d,%d,%d)", (int) (c.getRed() * 255), (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    /** Escena 3D interna (usada por las pruebas automaticas). */
    public SubScene getSubScene() {
        return sub;
    }

    /* ------------------------------------------------------------------ contenido */

    /** Muestra el portatil {@code a}; si {@code ghostB} no es nulo, superpone ambos en modo Ghost. */
    public void setLaptops(Laptop a, Laptop ghostB) {
        models.clear();
        holder.getChildren().clear();
        if (a != null) {
            boolean ghost = ghostB != null;
            LaptopModel ma = new LaptopModel(a, ACCENT_A.equals(accent) || ghost ? ACCENT_A : accent,
                    ghost ? ACCENT_A : null, true);
            models.add(ma);
            holder.getChildren().add(ma.getRoot());
            if (ghost) {
                LaptopModel mb = new LaptopModel(ghostB, ACCENT_B, ACCENT_B, false);
                models.add(mb);
                holder.getChildren().add(mb.getRoot());
            }
            hudName.setText(ghost ? "A  " + a.getDisplayName() + "\nB  " + ghostB.getDisplayName() : "");
        } else {
            hudName.setText("");
        }
        applyState();
        layoutCamera();
    }

    private void applyState() {
        for (LaptopModel m : models) {
            m.setExplode(explode);
            m.setLid(lid);
            m.setMode(mode);
            m.setStress(stress);
            m.select(selected);
            hiddenParts.forEach(p -> m.setPartHidden(p, true));
        }
        recenter();
        updateHud();
    }

    private void recenter() {
        double shift = 0;
        for (LaptopModel m : models) {
            shift = m == models.get(0) ? m.centerShift() : Math.max(shift, m.centerShift());
        }
        holder.setTranslateY(shift);
    }

    private void layoutCamera() {
        if (models.isEmpty() || getWidth() < 10 || getHeight() < 10) {
            return;
        }
        double diag = 0;
        for (LaptopModel m : models) {
            diag = Math.max(diag, Math.hypot(m.width(), m.depth()));
        }
        double aspect = getWidth() / getHeight();
        double visibleFactor = 2 * Math.tan(Math.toRadians(15)) * Math.min(1.0, aspect);
        camera.setTranslateZ(-diag / (0.68 * visibleFactor));
    }

    /* ------------------------------------------------------------------ controles publicos */

    public void setMode(LaptopModel.Mode m) {
        this.mode = m;
        models.forEach(x -> x.setMode(m));
        updateHud();
    }

    public void setExplode(double e) {
        this.explode = e;
        models.forEach(x -> x.setExplode(e));
        recenter();
    }

    public void setLid(double deg) {
        this.lid = deg;
        models.forEach(x -> x.setLid(deg));
        recenter();
    }

    public void setStress(boolean on) {
        this.stress = on;
        models.forEach(x -> x.setStress(on));
        updateHud();
    }

    public void setSync(boolean on) {
        this.sync = on;
    }

    public void select(String part) {
        this.selected = part;
        models.forEach(x -> x.select(part));
    }

    private final java.util.Set<String> hiddenParts = new java.util.HashSet<>();

    public void setPartHidden(String part, boolean hidden) {
        if (hidden) {
            hiddenParts.add(part);
        } else {
            hiddenParts.remove(part);
        }
        models.forEach(x -> x.setPartHidden(part, hidden));
    }

    public void key(KeyCode code, boolean down) {
        models.forEach(x -> x.key(code, down));
    }

    public void preset(String name) {
        double nrx;
        double nry;
        switch (name) {
            case "front" -> { nrx = 3; nry = 0; }
            case "side" -> { nrx = 3; nry = 90; }
            case "top" -> { nrx = 88; nry = 0; }
            default -> { nrx = 26; nry = -32; }
        }
        double dy = ((((nry - ry) % 360) + 540) % 360) - 180;
        tRx = nrx;
        tRy = ry + dy;
        if ("reset".equals(name)) {
            tZoom = 1;
        }
    }

    /** Fija la camara desde fuera (sincronizacion). No vuelve a emitir el evento. */
    public void setCamera(double nrx, double nry, double nzoom) {
        silent = true;
        rx = nrx;
        ry = nry;
        zoom = nzoom;
        tRx = nrx;
        tRy = nry;
        tZoom = nzoom;
        silent = false;
    }

    /* ------------------------------------------------------------------ raton */

    private void installMouse() {
        sub.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragging = true;
                lastX = e.getX();
                lastY = e.getY();
                moved = 0;
            }
        });
        sub.setOnMouseDragged(e -> {
            updateHover(e);
            if (!dragging) {
                return;
            }
            double dx = e.getX() - lastX, dy = e.getY() - lastY;
            lastX = e.getX();
            lastY = e.getY();
            moved += Math.abs(dx) + Math.abs(dy);
            ry -= dx * 0.5;
            rx += dy * 0.4;
            tRx = rx;
            tRy = ry;
            emitCamera();
        });
        sub.setOnMouseMoved(this::updateHover);
        sub.setOnMouseReleased(e -> {
            if (dragging && moved < 5) {
                pick(e);
            }
            dragging = false;
        });
        sub.setOnMouseExited(e -> {
            thx = 0;
            thy = 0;
        });
        sub.setOnScroll(e -> {
            zoom = Math.max(0.5, Math.min(3.2, zoom * Math.exp(e.getDeltaY() * 0.0015)));
            tZoom = zoom;
            emitCamera();
            e.consume();
        });
    }

    private void updateHover(MouseEvent e) {
        thx = Math.max(-1, Math.min(1, e.getX() / Math.max(1, getWidth()) * 2 - 1));
        thy = Math.max(-1, Math.min(1, e.getY() / Math.max(1, getHeight()) * 2 - 1));
    }

    private void pick(MouseEvent e) {
        PickResult pr = e.getPickResult();
        Node n = pr == null ? null : pr.getIntersectedNode();
        while (n != null && !(n.getUserData() instanceof String)) {
            n = n.getParent();
        }
        if (n != null) {
            String part = (String) n.getUserData();
            select(part);
            listener.onPart(side, part);
        }
    }

    private void emitCamera() {
        if (sync && !silent) {
            listener.onCamera(side, rx, ry, zoom);
        }
    }

    /* ------------------------------------------------------------------ animacion */

    private void step(double dt) {
        boolean moving = false;
        double k = 1 - Math.exp(-dt * 9);
        if (Math.abs(tRx - rx) > 0.01 || Math.abs(tRy - ry) > 0.01 || Math.abs(tZoom - zoom) > 0.0005) {
            rx += (tRx - rx) * k;
            ry += (tRy - ry) * k;
            zoom += (tZoom - zoom) * k;
            moving = true;
        } else {
            rx = tRx;
            ry = tRy;
            zoom = tZoom;
        }
        double kh = 1 - Math.exp(-dt * 7);
        hx += (thx - hx) * kh;
        hy += (thy - hy) * kh;
        if (moving) {
            emitCamera();
        }
        rotX.setAngle(rx - hy * 24);
        boolean under = rotX.getAngle() < -8;
        for (LaptopModel m : models) {
            m.setBelow(under);
        }
        rotY.setAngle(ry - hx * 35);
        scale.setX(zoom);
        scale.setY(zoom);
        scale.setZ(zoom);

        for (LaptopModel m : models) {
            m.tick(dt);
        }
        hudClock += dt;
        if (hudClock > 0.15) {
            hudClock = 0;
            updateHud();
        }
    }

    private void updateHud() {
        if (models.isEmpty() || (!stress && mode != LaptopModel.Mode.THERMAL)) {
            hudTele.setText("");
            return;
        }
        LaptopModel m = models.get(0);
        hudTele.setText(String.format(Locale.ROOT, "CPU %d C   GPU %d C   %d dBA   [ estimado ]",
                Math.round(m.cpuTemp()), Math.round(m.gpuTemp()), Math.round(m.noiseDba())));
    }
}
