package com.laptopexplorer.viewer;

import com.laptopexplorer.model.AMJLaptop;
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
 * Visor 3D independiente (un SubScene con su propia cámara, luces y ratón).
 * El portátil A y el B usan dos instancias distintas, así que nunca comparten movimiento.
 */
public final class AMJLaptopViewer extends StackPane {

    public static final Color ACCENT_A = Color.web("#5ea5c7");
    public static final Color ACCENT_B = Color.web("#877fb0");

    public interface AMJListener {
        void amjOnPart(String side, String part);

        void amjOnCamera(String side, double rx, double ry, double zoom);
    }

    private final String side;
    private final Color accent;
    private final AMJListener listener;

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
    private final List<AMJLaptopModel> models = new ArrayList<>();
    private final AnimationTimer timer;

    // cámara: valor actual y valor objetivo (los presets se interpolan)
    private double rx = 26, ry = -32, zoom = 1;
    private double tRx = 26, tRy = -32, tZoom = 1;
    private double hx, hy, thx, thy;

    private AMJLaptopModel.AMJMode mode = AMJLaptopModel.AMJMode.STUDIO;
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

    public AMJLaptopViewer(String side, AMJListener listener) {
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
        setStyle("-fx-background-color: radial-gradient(center 50% 75%, radius 90%, #1b4a66 0%, #0c2a40 55%, #071a2b 100%); -fx-background-radius: 14;");
        sub.setManaged(false);
        sub.relocate(0, 0);
        sub.widthProperty().bind(widthProperty());
        sub.heightProperty().bind(heightProperty());

        hudName.getStyleClass().add("hud-name");
        hudName.setStyle("-fx-text-fill: " + amjCss(accent) + ";");
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
        widthProperty().addListener((o, a, b) -> amjLayoutCamera());
        heightProperty().addListener((o, a, b) -> amjLayoutCamera());

        amjInstallMouse();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = lastNanos == 0 ? 0.016 : Math.min(0.1, (now - lastNanos) / 1e9);
                lastNanos = now;
                amjStep(dt);
            }
        };
        sceneProperty().addListener((o, a, b) -> amjUpdateTimer());
    }

    private boolean active = true;

    /**
     * Pausa o reanuda la animación. La pantalla del visor lo llama al ocultarse y al mostrarse: sin esto el
     * temporizador seguia moviendo ventiladores y recalculando la cámara 60 veces por segundo con el visor oculto.
     */
    public void amjSetActive(boolean on) {
        active = on;
        amjUpdateTimer();
    }

    private void amjUpdateTimer() {
        if (active && getScene() != null) {
            timer.start();
        } else {
            timer.stop();
            lastNanos = 0;
        }
    }

    private AMJLaptop shownA;
    private AMJLaptop shownB;
    private boolean dirty = true;
    private double externalDiag;
    private long loadTicket;

    /** Obliga a reconstruir en la próxima llamada a {@link #amjSetLaptops} (el modelo 3D del laptop cambio en disco). */
    public void amjInvalidate() {
        dirty = true;
    }

    /** Marca para reconstruir si el laptop gano o perdio su modelo importado desde la última vez. */
    public void amjInvalidateIfModelChanged(AMJLaptop current) {
        boolean has = com.laptopexplorer.service.AMJModelStore.amjFileFor(current) != null;
        if (current == shownA && shownB == null && has != (externalDiag > 0)) {
            dirty = true;
        }
    }

    /** Verdadero si lo que se muestra es un modelo OBJ importado y no la reconstrucción interna. */
    public boolean amjIsExternal() {
        return externalDiag > 0;
    }

    private void amjShowExternal(AMJLaptop a, java.nio.file.Path obj) {
        long ticket = ++loadTicket;
        hudName.setText("Cargando modelo 3D importado…");
        double target = a.getWidthMm() != null && a.getWidthMm() > 0 ? a.getWidthMm() : 360;
        externalDiag = target * 1.25;
        javafx.concurrent.Task<AMJExternalModel.AMJLoaded> task = new javafx.concurrent.Task<>() {
            @Override
            protected AMJExternalModel.AMJLoaded call() throws Exception {
                return AMJExternalModel.amjLoad(obj, target);
            }
        };
        task.setOnSucceeded(e -> {
            if (ticket != loadTicket) {
                return;                                  // el usuario ya cambio de laptop mientras cargaba
            }
            AMJExternalModel.AMJLoaded m = task.getValue();
            holder.getChildren().setAll(m.root());
            holder.setTranslateY(m.height() / 2);
            externalDiag = Math.hypot(m.width(), Math.max(m.depth(), m.height()));
            hudName.setText("Modelo 3D importado · " + String.format(Locale.US, "%,d", m.triangles()) + " triángulos");
            amjLayoutCamera();
        });
        task.setOnFailed(e -> {
            if (ticket == loadTicket) {
                externalDiag = 0;
                hudName.setText("No se pudo leer el modelo 3D importado: " + task.getException().getMessage());
            }
        });
        Thread t = new Thread(task, "carga-modelo-3d");
        t.setDaemon(true);
        t.start();
    }

    private static String amjCss(Color c) {
        return String.format(Locale.ROOT, "rgb(%d,%d,%d)", (int) (c.getRed() * 255), (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    /** Escena 3D interna (usada por las pruebas automáticas). */
    public SubScene amjGetSubScene() {
        return sub;
    }

    /* ------------------------------------------------------------------ contenido */

    /** Muestra el portátil {@code a}; si {@code ghostB} no es nulo, superpone ambos en modo Ghost. */
    public void amjSetLaptops(AMJLaptop a, AMJLaptop ghostB) {
        // Reconstruir un modelo cuesta decenas de milisegundos: no se repite si se pide lo que ya se muestra
        if (!dirty && a == shownA && ghostB == shownB) {
            return;
        }
        dirty = false;
        shownA = a;
        shownB = ghostB;
        loadTicket++;
        externalDiag = 0;
        models.clear();
        holder.getChildren().clear();
        holder.setTranslateY(0);
        java.nio.file.Path external = ghostB == null ? com.laptopexplorer.service.AMJModelStore.amjFileFor(a) : null;
        if (external != null) {
            amjShowExternal(a, external);
            amjUpdateHud();
            amjLayoutCamera();
            return;
        }
        if (a != null) {
            boolean ghost = ghostB != null;
            AMJLaptopModel ma = new AMJLaptopModel(a, ACCENT_A.equals(accent) || ghost ? ACCENT_A : accent,
                    ghost ? ACCENT_A : null, true);
            models.add(ma);
            holder.getChildren().add(ma.amjGetRoot());
            if (ghost) {
                AMJLaptopModel mb = new AMJLaptopModel(ghostB, ACCENT_B, ACCENT_B, false);
                models.add(mb);
                holder.getChildren().add(mb.amjGetRoot());
            }
            hudName.setText(ghost ? "A  " + a.amjGetDisplayName() + "\nB  " + ghostB.amjGetDisplayName() : "");
        } else {
            hudName.setText("");
        }
        amjApplyState();
        amjLayoutCamera();
    }

    private void amjApplyState() {
        for (AMJLaptopModel m : models) {
            m.amjSetExplode(explode);
            m.amjSetLid(lid);
            m.amjSetMode(mode);
            m.amjSetStress(stress);
            m.amjSelect(selected);
            hiddenParts.forEach(p -> m.amjSetPartHidden(p, true));
        }
        amjRecenter();
        amjUpdateHud();
    }

    private void amjRecenter() {
        if (externalDiag > 0) {
            return;                                      // el modelo importado ya esta centrado
        }
        double shift = 0;
        for (AMJLaptopModel m : models) {
            shift = m == models.get(0) ? m.amjCenterShift() : Math.max(shift, m.amjCenterShift());
        }
        holder.setTranslateY(shift);
    }

    private void amjLayoutCamera() {
        if ((models.isEmpty() && externalDiag <= 0) || getWidth() < 10 || getHeight() < 10) {
            return;
        }
        double diag = externalDiag;
        for (AMJLaptopModel m : models) {
            diag = Math.max(diag, Math.hypot(m.amjWidth(), m.amjDepth()));
        }
        double aspect = getWidth() / getHeight();
        double visibleFactor = 2 * Math.tan(Math.toRadians(15)) * Math.min(1.0, aspect);
        camera.setTranslateZ(-diag / (0.68 * visibleFactor));
    }

    /* ------------------------------------------------------------------ controles publicos */

    public void amjSetMode(AMJLaptopModel.AMJMode m) {
        this.mode = m;
        models.forEach(x -> x.amjSetMode(m));
        amjUpdateHud();
    }

    public void amjSetExplode(double e) {
        this.explode = e;
        models.forEach(x -> x.amjSetExplode(e));
        amjRecenter();
    }

    public void amjSetLid(double deg) {
        this.lid = deg;
        models.forEach(x -> x.amjSetLid(deg));
        amjRecenter();
    }

    public void amjSetStress(boolean on) {
        this.stress = on;
        models.forEach(x -> x.amjSetStress(on));
        amjUpdateHud();
    }

    public void amjSetSync(boolean on) {
        this.sync = on;
    }

    public void amjSelect(String part) {
        this.selected = part;
        models.forEach(x -> x.amjSelect(part));
    }

    private final java.util.Set<String> hiddenParts = new java.util.HashSet<>();

    public void amjSetPartHidden(String part, boolean hidden) {
        if (hidden) {
            hiddenParts.add(part);
        } else {
            hiddenParts.remove(part);
        }
        models.forEach(x -> x.amjSetPartHidden(part, hidden));
    }

    public void amjKey(KeyCode code, boolean down) {
        models.forEach(x -> x.amjKey(code, down));
    }

    public void amjPreset(String name) {
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

    /** Fija la cámara desde fuera (sincronización). No vuelve a emitir el evento. */
    public void amjSetCamera(double nrx, double nry, double nzoom) {
        silent = true;
        rx = nrx;
        ry = nry;
        zoom = nzoom;
        tRx = nrx;
        tRy = nry;
        tZoom = nzoom;
        silent = false;
    }

    /* ------------------------------------------------------------------ ratón */

    private void amjInstallMouse() {
        sub.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragging = true;
                lastX = e.getX();
                lastY = e.getY();
                moved = 0;
            }
        });
        sub.setOnMouseDragged(e -> {
            amjUpdateHover(e);
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
            amjEmitCamera();
        });
        sub.setOnMouseMoved(this::amjUpdateHover);
        sub.setOnMouseReleased(e -> {
            if (dragging && moved < 5) {
                amjPick(e);
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
            amjEmitCamera();
            e.consume();
        });
    }

    private void amjUpdateHover(MouseEvent e) {
        thx = Math.max(-1, Math.min(1, e.getX() / Math.max(1, getWidth()) * 2 - 1));
        thy = Math.max(-1, Math.min(1, e.getY() / Math.max(1, getHeight()) * 2 - 1));
    }

    private void amjPick(MouseEvent e) {
        PickResult pr = e.getPickResult();
        Node n = pr == null ? null : pr.getIntersectedNode();
        while (n != null && !(n.getUserData() instanceof String)) {
            n = n.getParent();
        }
        if (n != null) {
            String part = (String) n.getUserData();
            amjSelect(part);
            listener.amjOnPart(side, part);
        }
    }

    private void amjEmitCamera() {
        if (sync && !silent) {
            listener.amjOnCamera(side, rx, ry, zoom);
        }
    }

    /* ------------------------------------------------------------------ animación */

    private void amjStep(double dt) {
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
            amjEmitCamera();
        }
        rotX.setAngle(rx - hy * 24);
        boolean under = rotX.getAngle() < -8;
        for (AMJLaptopModel m : models) {
            m.amjSetBelow(under);
        }
        rotY.setAngle(ry - hx * 35);
        scale.setX(zoom);
        scale.setY(zoom);
        scale.setZ(zoom);

        for (AMJLaptopModel m : models) {
            m.amjTick(dt);
        }
        hudClock += dt;
        if (hudClock > 0.15) {
            hudClock = 0;
            amjUpdateHud();
        }
    }

    private void amjUpdateHud() {
        if (models.isEmpty() || (!stress && mode != AMJLaptopModel.AMJMode.THERMAL)) {
            hudTele.setText("");
            return;
        }
        AMJLaptopModel m = models.get(0);
        hudTele.setText(String.format(Locale.ROOT, "CPU %d C   GPU %d C   %d dBA   [ estimado ]",
                Math.round(m.amjCpuTemp()), Math.round(m.amjGpuTemp()), Math.round(m.amjNoiseDba())));
    }
}
