package com.laptopexplorer.viewer;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Visor 3D de un componente de la biblioteca (procesador, gráfica, memoria, SSD): un SubScene con su cámara y
 * luces, giro con el ratón y zoom con la rueda. Sobre el modelo puede dibujar las cotas de su caja envolvente,
 * separarlo en capas (despiece), resaltar u ocultar las piezas de un material y hacer girar los ventiladores.
 * El modelo llega ya centrado, apoyado en y = 0 y escalado por {@link AMJExternalModel}.
 */
public final class AMJComponentViewer extends StackPane {

    /** Avisa de la pieza elegida con un clic; {@code material} es null si se hizo clic fuera del modelo. */
    public interface AMJListener {
        void amjOnPick(String material);
    }

    private static final Color DIMMED = Color.rgb(22, 32, 44);
    private static final double FIELD_OF_VIEW = 30;
    /** Los trazos de referencia de una cota sobresalen de la línea de medida en esta proporción. */
    private static final double TICK_REACH = 1.35;

    private final AMJListener listener;
    private final Group rig = new Group();          // gira y hace zoom
    private final Group holder = new Group();       // contiene el modelo; se desplaza para centrarlo en altura
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene sub;
    private final Scale scale = new Scale(1, 1, 1);
    private final Rotate rotX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotY = new Rotate(0, Rotate.Y_AXIS);
    private final Pane overlay = new Pane();        // cotas: líneas y rótulos 2D colocados sobre el modelo
    private final Label status = new Label();
    private final AnimationTimer timer;

    private AMJExternalModel.AMJLoaded model;
    private List<AMJExternalModel.AMJPart> layers = List.of();            // piezas de abajo arriba, para el despiece
    private final Map<Node, AMJExternalModel.AMJPart> byNode = new HashMap<>();
    private final Map<AMJExternalModel.AMJPart, Color> specular = new HashMap<>();
    private final List<Rotate> rotors = new ArrayList<>();
    private final List<AMJDimension> dimensions = new ArrayList<>();
    private final Set<String> hidden = new HashSet<>();
    private String highlighted;

    // cámara: valor actual y valor objetivo (los cambios de vista se interpolan)
    private double rx = 28, ry = 34, zoom = 1;
    private double tRx = 28, tRy = 34, tZoom = 1;
    private double explode;
    private boolean showDimensions = true;
    private boolean autoRotate;
    private boolean spinFans;
    private boolean active = true;
    private boolean dragging;
    private double lastX, lastY, moved;
    private long lastNanos;

    public AMJComponentViewer(AMJListener listener) {
        this.listener = listener;
        rig.getTransforms().addAll(scale, rotX, rotY);
        rig.getChildren().add(holder);
        Group sceneRoot = new Group(rig, camera);
        sceneRoot.getChildren().addAll(amjLights());
        camera.setFieldOfView(FIELD_OF_VIEW);

        sub = new SubScene(sceneRoot, 10, 10, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.TRANSPARENT);
        sub.setManaged(false);
        sub.relocate(0, 0);
        sub.widthProperty().bind(widthProperty());
        sub.heightProperty().bind(heightProperty());
        setStyle("-fx-background-color: radial-gradient(center 50% 70%, radius 90%, #1b4a66 0%, #0c2a40 55%, #071a2b 100%); -fx-background-radius: 14;");

        overlay.setMouseTransparent(true);
        status.getStyleClass().add("viewer-status");
        status.setMouseTransparent(true);
        status.setWrapText(true);
        StackPane.setAlignment(status, Pos.CENTER);
        getChildren().addAll(sub, overlay, status);
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

    /**
     * Las mismas luces para el visor y para las miniaturas: ambiente, principal, relleno y contraluz. La suma
     * sobre una cara que mira hacia arriba queda por debajo de 1: con más luz, los metales claros se ven blancos.
     */
    private static List<Node> amjLights() {
        AmbientLight ambient = new AmbientLight(Color.gray(0.32));
        PointLight key = new PointLight(Color.gray(0.70));
        key.setTranslateX(-700);
        key.setTranslateY(-1100);
        key.setTranslateZ(-1100);
        PointLight fill = new PointLight(Color.gray(0.28));
        fill.setTranslateX(900);
        fill.setTranslateY(-300);
        fill.setTranslateZ(-500);
        PointLight rim = new PointLight(Color.gray(0.20));
        rim.setTranslateY(-500);
        rim.setTranslateZ(1200);
        return List.of(ambient, key, fill, rim);
    }

    /** Pausa o reanuda la animación; la pantalla lo llama al ocultarse y al mostrarse para no gastar CPU ni GPU. */
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

    /* ------------------------------------------------------------------ contenido */

    /** Texto centrado sobre el visor ("Cargando…", un error); vacío para quitarlo. */
    public void amjSetStatus(String text) {
        status.setText(text == null ? "" : text);
    }

    public void amjClear() {
        model = null;
        layers = List.of();
        byNode.clear();
        specular.clear();
        rotors.clear();
        hidden.clear();
        highlighted = null;
        holder.getChildren().clear();
        amjClearDimensions();
    }

    /** Muestra un modelo. Conserva el ángulo de cámara: así se comparan dos componentes desde el mismo punto de vista. */
    public void amjShow(AMJExternalModel.AMJLoaded loaded) {
        amjClear();
        model = loaded;
        holder.getChildren().setAll(loaded.root());
        List<AMJExternalModel.AMJPart> sorted = new ArrayList<>(loaded.parts());
        // y = 0 es la base y hacia arriba es negativo: el centro más alto en Y es la pieza más baja
        sorted.sort(Comparator.comparingDouble((AMJExternalModel.AMJPart p) -> -(p.min()[1] + p.max()[1]) / 2));
        layers = sorted;
        for (AMJExternalModel.AMJPart p : loaded.parts()) {
            byNode.put(p.view(), p);
            specular.put(p, p.phong().getSpecularColor());
        }
        amjFindRotors(loaded);
        amjBuildDimensions();
        tZoom = 1;
        amjApplyExplode();
        amjApplyLook();
        amjSetStatus("");
    }

    /** Verdadero si el modelo trae ventiladores como objetos aparte ("Fan_Rotor_1"...), que se pueden hacer girar. */
    public boolean amjHasFans() {
        return !rotors.isEmpty();
    }

    /**
     * Los ventiladores vienen como objetos cuyo nombre contiene "rotor". Cada uno gira sobre el eje en el que es
     * más delgado (un ventilador es un disco), alrededor del centro de su caja envolvente.
     */
    private void amjFindRotors(AMJExternalModel.AMJLoaded loaded) {
        Map<String, List<AMJExternalModel.AMJPart>> byObject = new LinkedHashMap<>();
        for (AMJExternalModel.AMJPart p : loaded.parts()) {
            if (p.object().toLowerCase(Locale.ROOT).contains("rotor")) {
                byObject.computeIfAbsent(p.object(), k -> new ArrayList<>()).add(p);
            }
        }
        for (List<AMJExternalModel.AMJPart> group : byObject.values()) {
            double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
            double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
            for (AMJExternalModel.AMJPart p : group) {
                for (int i = 0; i < 3; i++) {
                    min[i] = Math.min(min[i], p.min()[i]);
                    max[i] = Math.max(max[i], p.max()[i]);
                }
            }
            int thin = 0;
            for (int i = 1; i < 3; i++) {
                if (max[i] - min[i] < max[thin] - min[thin]) {
                    thin = i;
                }
            }
            Point3D axis = thin == 0 ? Rotate.X_AXIS : thin == 1 ? Rotate.Y_AXIS : Rotate.Z_AXIS;
            Rotate spin = new Rotate(0, (min[0] + max[0]) / 2, (min[1] + max[1]) / 2, (min[2] + max[2]) / 2, axis);
            group.forEach(p -> p.view().getTransforms().add(spin));
            rotors.add(spin);
        }
    }

    /* ------------------------------------------------------------------ controles publicos */

    /** Vista de cámara: "iso", "top", "front", "side", "bottom" o "back". */
    public void amjPreset(String name) {
        double nrx;
        double nry;
        switch (name) {
            case "top" -> { nrx = 88; nry = 0; }
            case "bottom" -> { nrx = -88; nry = 0; }
            case "front" -> { nrx = 4; nry = 0; }
            case "side" -> { nrx = 4; nry = 90; }
            case "back" -> { nrx = 4; nry = 180; }
            default -> { nrx = 28; nry = 34; }
        }
        // se gira por el camino más corto, sin dar la vuelta completa
        double dy = ((((nry - ry) % 360) + 540) % 360) - 180;
        tRx = nrx;
        tRy = ry + dy;
        tZoom = 1;
    }

    /** Despiece: 0 = montado, 1 = capas separadas del todo. */
    public void amjSetExplode(double value) {
        explode = Math.max(0, Math.min(1, value));
        amjApplyExplode();
    }

    public void amjSetShowDimensions(boolean on) {
        showDimensions = on;
    }

    public void amjSetAutoRotate(boolean on) {
        autoRotate = on;
    }

    public void amjSetSpinFans(boolean on) {
        spinFans = on;
    }

    /** Resalta las piezas de un material y atenua el resto; null devuelve el modelo a su aspecto normal. */
    public void amjHighlight(String material) {
        highlighted = material;
        amjApplyLook();
    }

    public void amjSetHidden(String material, boolean hide) {
        if (hide ? hidden.add(material) : hidden.remove(material)) {
            amjApplyLook();
        }
    }

    private void amjApplyLook() {
        for (AMJExternalModel.AMJPart p : layers) {
            p.view().setVisible(!hidden.contains(p.material()));
            boolean dim = highlighted != null && !highlighted.equals(p.material());
            p.phong().setDiffuseColor(dim ? p.color().interpolate(DIMMED, 0.82) : p.color());
            p.phong().setSpecularColor(dim ? Color.gray(0.04) : specular.get(p));
        }
    }

    /** Alto extra, en unidades del visor, que añade el despiece al conjunto. */
    private double amjSpread() {
        return model == null ? 0 : explode * Math.max(model.width(), model.depth()) * 0.85;
    }

    private void amjApplyExplode() {
        if (model == null) {
            return;
        }
        int n = layers.size();
        double spread = amjSpread();
        for (int i = 0; i < n; i++) {
            layers.get(i).view().setTranslateY(n > 1 ? -spread * i / (n - 1) : 0);
        }
        // el centro del conjunto queda en el origen, que es el punto alrededor del cual gira la cámara
        holder.setTranslateY((model.height() + spread) / 2);
        amjLayoutCamera();
    }

    /** Aleja la cámara lo justo para que la esfera que envuelve al modelo quepa entera en el visor. */
    private void amjLayoutCamera() {
        if (model == null || getWidth() < 10 || getHeight() < 10) {
            return;
        }
        camera.setTranslateZ(-amjDistance(model.width(), model.height() + amjSpread(), model.depth(), getWidth() / getHeight()));
        camera.setNearClip(Math.max(1, -camera.getTranslateZ() / 400));
        camera.setFarClip(-camera.getTranslateZ() * 8);
    }

    private static double amjDistance(double w, double h, double d, double aspect) {
        double radius = Math.sqrt(w * w + h * h + d * d) / 2;
        double half = Math.toRadians(FIELD_OF_VIEW / 2);
        // el campo de visión es vertical: en un visor más alto que ancho manda el horizontal, que es menor
        double limiting = aspect >= 1 ? half : Math.atan(Math.tan(half) * aspect);
        return 1.12 * radius / Math.sin(limiting);
    }

    /* ------------------------------------------------------------------ cotas */

    /** Una cota: la línea de medida entre dos puntos del modelo, sus dos trazos de referencia y el rótulo. */
    private static final class AMJDimension {
        final Point3D a, b, a0, b0;
        final Line main = amjLine(true), tickA = amjLine(false), tickB = amjLine(false);
        final Label label = new Label();

        AMJDimension(Point3D a, Point3D b, Point3D a0, Point3D b0, String text) {
            this.a = a;
            this.b = b;
            this.a0 = a0;
            this.b0 = b0;
            label.setText(text);
            label.getStyleClass().add("dim-label");
            label.setManaged(false);
        }

        private static Line amjLine(boolean dashed) {
            Line l = new Line();
            l.getStyleClass().add("dim-line");
            l.setManaged(false);
            if (dashed) {
                l.getStrokeDashArray().addAll(5.0, 4.0);
            }
            return l;
        }
    }

    private void amjClearDimensions() {
        dimensions.clear();
        overlay.getChildren().clear();
    }

    /**
     * Cotas de la caja envolvente (ancho, fondo y alto), en las unidades reales del archivo. Las líneas se
     * definen en el espacio del modelo y cada fotograma se proyectan a la pantalla: giran con el, pero el trazo
     * y el texto se dibujan en 2D, siempre legibles.
     */
    private void amjBuildDimensions() {
        double w = model.width(), h = model.height(), d = model.depth();
        double m = Math.max(w, d) * 0.07;       // separación entre el modelo y la línea de cota
        double x0 = -w / 2, x1 = w / 2, z0 = -d / 2, z1 = d / 2;
        dimensions.add(new AMJDimension(new Point3D(x0, 0, z0 - m), new Point3D(x1, 0, z0 - m),
                new Point3D(x0, 0, z0), new Point3D(x1, 0, z0), amjMm(w)));
        dimensions.add(new AMJDimension(new Point3D(x1 + m, 0, z0), new Point3D(x1 + m, 0, z1),
                new Point3D(x1, 0, z0), new Point3D(x1, 0, z1), amjMm(d)));
        dimensions.add(new AMJDimension(new Point3D(x0 - m, 0, z1), new Point3D(x0 - m, -h, z1),
                new Point3D(x0, 0, z1), new Point3D(x0, -h, z1), amjMm(h)));
        for (AMJDimension dim : dimensions) {
            overlay.getChildren().addAll(dim.main, dim.tickA, dim.tickB, dim.label);
        }
    }

    private String amjMm(double units) {
        return String.format(Locale.US, "%.2f mm", units / model.scale());
    }

    /** Punto del modelo a coordenadas de la capa de cotas, pasando por la cámara del visor. */
    private Point2D amjProject(Point3D p) {
        Point3D inScene = holder.localToScene(p, true);
        return inScene == null ? null : overlay.sceneToLocal(inScene.getX(), inScene.getY());
    }

    private void amjUpdateDimensions() {
        boolean on = showDimensions && model != null && explode < 0.01;
        overlay.setVisible(on);
        if (!on) {
            return;
        }
        for (AMJDimension dim : dimensions) {
            Point2D a = amjProject(dim.a), b = amjProject(dim.b), a0 = amjProject(dim.a0), b0 = amjProject(dim.b0);
            // vista de frente a una cota (el alto, mirando desde arriba): queda reducida a un punto y no se dibuja
            boolean readable = a != null && b != null && a0 != null && b0 != null && a.distance(b) >= 4;
            for (Node n : List.of(dim.main, dim.tickA, dim.tickB, dim.label)) {
                n.setVisible(readable);
            }
            if (!readable) {
                continue;
            }
            amjSet(dim.main, a, b);
            amjSet(dim.tickA, a0, a0.add(a.subtract(a0).multiply(TICK_REACH)));
            amjSet(dim.tickB, b0, b0.add(b.subtract(b0).multiply(TICK_REACH)));
            // el rótulo va en el punto medio, desplazado hacia fuera del modelo para no tapar la línea
            Point2D mid = a.midpoint(b);
            Point2D out = mid.subtract(a0.midpoint(b0));
            Point2D off = out.magnitude() < 1e-6 ? Point2D.ZERO : out.normalize().multiply(16);
            double lw = dim.label.prefWidth(-1), lh = dim.label.prefHeight(-1);
            dim.label.resizeRelocate(mid.getX() + off.getX() - lw / 2, mid.getY() + off.getY() - lh / 2, lw, lh);
        }
    }

    private static void amjSet(Line l, Point2D from, Point2D to) {
        l.setStartX(from.getX());
        l.setStartY(from.getY());
        l.setEndX(to.getX());
        l.setEndY(to.getY());
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
            if (!dragging) {
                return;
            }
            double dx = e.getX() - lastX, dy = e.getY() - lastY;
            lastX = e.getX();
            lastY = e.getY();
            moved += Math.abs(dx) + Math.abs(dy);
            ry -= dx * 0.5;
            rx = Math.max(-89, Math.min(89, rx + dy * 0.4));
            tRx = rx;
            tRy = ry;
        });
        sub.setOnMouseReleased(e -> {
            if (dragging && moved < 5) {
                amjPick(e);
            }
            dragging = false;
        });
        sub.setOnScroll(e -> {
            tZoom = Math.max(0.5, Math.min(4.0, tZoom * Math.exp(e.getDeltaY() * 0.0015)));
            e.consume();
        });
    }

    private void amjPick(MouseEvent e) {
        PickResult pr = e.getPickResult();
        AMJExternalModel.AMJPart part = pr == null ? null : byNode.get(pr.getIntersectedNode());
        listener.amjOnPick(part == null ? null : part.material());
    }

    /* ------------------------------------------------------------------ animación */

    private void amjStep(double dt) {
        if (autoRotate && !dragging) {
            tRy -= 14 * dt;
        }
        double k = 1 - Math.exp(-dt * 9);
        rx += (tRx - rx) * k;
        ry += (tRy - ry) * k;
        zoom += (tZoom - zoom) * k;
        rotX.setAngle(rx);
        rotY.setAngle(ry);
        scale.setX(zoom);
        scale.setY(zoom);
        scale.setZ(zoom);
        if (spinFans) {
            for (Rotate r : rotors) {
                r.setAngle((r.getAngle() + 420 * dt) % 360);
            }
        }
        amjUpdateDimensions();
    }

    /* ------------------------------------------------------------------ miniaturas */

    /**
     * Miniatura del modelo en vista isometrica, con fondo transparente. Usa una escena aparte, así que el modelo
     * no puede estar a la vez en un visor. Debe llamarse en el hilo de JavaFX. Se dibuja al doble de tamaño para
     * que, al reducirla, los bordes queden suaves.
     */
    public static Image amjThumbnail(AMJExternalModel.AMJLoaded loaded, int width, int height) {
        Group hold = new Group(loaded.root());
        hold.setTranslateY(loaded.height() / 2);
        Group turn = new Group(hold);
        turn.getTransforms().addAll(new Rotate(32, Rotate.X_AXIS), new Rotate(34, Rotate.Y_AXIS));
        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setFieldOfView(FIELD_OF_VIEW);
        double dist = amjDistance(loaded.width(), loaded.height(), loaded.depth(), (double) width / height) * 0.9;
        cam.setTranslateZ(-dist);
        cam.setNearClip(Math.max(1, dist / 400));
        cam.setFarClip(dist * 8);
        Group root = new Group(turn);
        root.getChildren().addAll(amjLights());
        // Se fotografía el grupo con la cámara como parametro. Un SubScene que no esta en una ventana no llega a
        // dibujar su contenido y la foto sale vacía.
        SnapshotParameters params = new SnapshotParameters();
        params.setCamera(cam);
        params.setDepthBuffer(true);
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(0, 0, width * 2.0, height * 2.0));
        return root.snapshot(params, null);
    }
}
