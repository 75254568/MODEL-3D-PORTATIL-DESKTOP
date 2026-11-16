package com.laptopstudio.viewer;

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
 * Visor 3D de un componente de la biblioteca (procesador, grafica, memoria, SSD): un SubScene con su camara y
 * luces, giro con el raton y zoom con la rueda. Sobre el modelo puede dibujar las cotas de su caja envolvente,
 * separarlo en capas (despiece), resaltar u ocultar las piezas de un material y hacer girar los ventiladores.
 * El modelo llega ya centrado, apoyado en y = 0 y escalado por {@link ExternalModel}.
 */
public final class ComponentViewer extends StackPane {

    /** Avisa de la pieza elegida con un clic; {@code material} es null si se hizo clic fuera del modelo. */
    public interface Listener {
        void onPick(String material);
    }

    private static final Color DIMMED = Color.rgb(22, 32, 44);
    private static final double FIELD_OF_VIEW = 30;
    /** Los trazos de referencia de una cota sobresalen de la linea de medida en esta proporcion. */
    private static final double TICK_REACH = 1.35;

    private final Listener listener;
    private final Group rig = new Group();          // gira y hace zoom
    private final Group holder = new Group();       // contiene el modelo; se desplaza para centrarlo en altura
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene sub;
    private final Scale scale = new Scale(1, 1, 1);
    private final Rotate rotX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotY = new Rotate(0, Rotate.Y_AXIS);
    private final Pane overlay = new Pane();        // cotas: lineas y rotulos 2D colocados sobre el modelo
    private final Label status = new Label();
    private final AnimationTimer timer;

    private ExternalModel.Loaded model;
    private List<ExternalModel.Part> layers = List.of();            // piezas de abajo arriba, para el despiece
    private final Map<Node, ExternalModel.Part> byNode = new HashMap<>();
    private final Map<ExternalModel.Part, Color> specular = new HashMap<>();
    private final List<Rotate> rotors = new ArrayList<>();
    private final List<Dimension> dimensions = new ArrayList<>();
    private final Set<String> hidden = new HashSet<>();
    private String highlighted;

    // camara: valor actual y valor objetivo (los cambios de vista se interpolan)
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

    public ComponentViewer(Listener listener) {
        this.listener = listener;
        rig.getTransforms().addAll(scale, rotX, rotY);
        rig.getChildren().add(holder);
        Group sceneRoot = new Group(rig, camera);
        sceneRoot.getChildren().addAll(lights());
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
        sceneProperty().addListener((o, a, b) -> updateTimer());
    }

    /**
     * Las mismas luces para el visor y para las miniaturas: ambiente, principal, relleno y contraluz. La suma
     * sobre una cara que mira hacia arriba queda por debajo de 1: con mas luz, los metales claros se ven blancos.
     */
    private static List<Node> lights() {
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

    /** Pausa o reanuda la animacion; la pantalla lo llama al ocultarse y al mostrarse para no gastar CPU ni GPU. */
    public void setActive(boolean on) {
        active = on;
        updateTimer();
    }

    private void updateTimer() {
        if (active && getScene() != null) {
            timer.start();
        } else {
            timer.stop();
            lastNanos = 0;
        }
    }

    /* ------------------------------------------------------------------ contenido */

    /** Texto centrado sobre el visor ("Cargando…", un error); vacio para quitarlo. */
    public void setStatus(String text) {
        status.setText(text == null ? "" : text);
    }

    public void clear() {
        model = null;
        layers = List.of();
        byNode.clear();
        specular.clear();
        rotors.clear();
        hidden.clear();
        highlighted = null;
        holder.getChildren().clear();
        clearDimensions();
    }

    /** Muestra un modelo. Conserva el angulo de camara: asi se comparan dos componentes desde el mismo punto de vista. */
    public void show(ExternalModel.Loaded loaded) {
        clear();
        model = loaded;
        holder.getChildren().setAll(loaded.root());
        List<ExternalModel.Part> sorted = new ArrayList<>(loaded.parts());
        // y = 0 es la base y hacia arriba es negativo: el centro mas alto en Y es la pieza mas baja
        sorted.sort(Comparator.comparingDouble((ExternalModel.Part p) -> -(p.min()[1] + p.max()[1]) / 2));
        layers = sorted;
        for (ExternalModel.Part p : loaded.parts()) {
            byNode.put(p.view(), p);
            specular.put(p, p.phong().getSpecularColor());
        }
        findRotors(loaded);
        buildDimensions();
        tZoom = 1;
        applyExplode();
        applyLook();
        setStatus("");
    }

    /** Verdadero si el modelo trae ventiladores como objetos aparte ("Fan_Rotor_1"...), que se pueden hacer girar. */
    public boolean hasFans() {
        return !rotors.isEmpty();
    }

    /**
     * Los ventiladores vienen como objetos cuyo nombre contiene "rotor". Cada uno gira sobre el eje en el que es
     * mas delgado (un ventilador es un disco), alrededor del centro de su caja envolvente.
     */
    private void findRotors(ExternalModel.Loaded loaded) {
        Map<String, List<ExternalModel.Part>> byObject = new LinkedHashMap<>();
        for (ExternalModel.Part p : loaded.parts()) {
            if (p.object().toLowerCase(Locale.ROOT).contains("rotor")) {
                byObject.computeIfAbsent(p.object(), k -> new ArrayList<>()).add(p);
            }
        }
        for (List<ExternalModel.Part> group : byObject.values()) {
            double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
            double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
            for (ExternalModel.Part p : group) {
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

    /** Vista de camara: "iso", "top", "front", "side", "bottom" o "back". */
    public void preset(String name) {
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
        // se gira por el camino mas corto, sin dar la vuelta completa
        double dy = ((((nry - ry) % 360) + 540) % 360) - 180;
        tRx = nrx;
        tRy = ry + dy;
        tZoom = 1;
    }

    /** Despiece: 0 = montado, 1 = capas separadas del todo. */
    public void setExplode(double value) {
        explode = Math.max(0, Math.min(1, value));
        applyExplode();
    }

    public void setShowDimensions(boolean on) {
        showDimensions = on;
    }

    public void setAutoRotate(boolean on) {
        autoRotate = on;
    }

    public void setSpinFans(boolean on) {
        spinFans = on;
    }

    /** Resalta las piezas de un material y atenua el resto; null devuelve el modelo a su aspecto normal. */
    public void highlight(String material) {
        highlighted = material;
        applyLook();
    }

    public void setHidden(String material, boolean hide) {
        if (hide ? hidden.add(material) : hidden.remove(material)) {
            applyLook();
        }
    }

    private void applyLook() {
        for (ExternalModel.Part p : layers) {
            p.view().setVisible(!hidden.contains(p.material()));
            boolean dim = highlighted != null && !highlighted.equals(p.material());
            p.phong().setDiffuseColor(dim ? p.color().interpolate(DIMMED, 0.82) : p.color());
            p.phong().setSpecularColor(dim ? Color.gray(0.04) : specular.get(p));
        }
    }

    /** Alto extra, en unidades del visor, que anade el despiece al conjunto. */
    private double spread() {
        return model == null ? 0 : explode * Math.max(model.width(), model.depth()) * 0.85;
    }

    private void applyExplode() {
        if (model == null) {
            return;
        }
        int n = layers.size();
        double spread = spread();
        for (int i = 0; i < n; i++) {
            layers.get(i).view().setTranslateY(n > 1 ? -spread * i / (n - 1) : 0);
        }
        // el centro del conjunto queda en el origen, que es el punto alrededor del cual gira la camara
        holder.setTranslateY((model.height() + spread) / 2);
        layoutCamera();
    }

    /** Aleja la camara lo justo para que la esfera que envuelve al modelo quepa entera en el visor. */
    private void layoutCamera() {
        if (model == null || getWidth() < 10 || getHeight() < 10) {
            return;
        }
        camera.setTranslateZ(-distance(model.width(), model.height() + spread(), model.depth(), getWidth() / getHeight()));
        camera.setNearClip(Math.max(1, -camera.getTranslateZ() / 400));
        camera.setFarClip(-camera.getTranslateZ() * 8);
    }

    private static double distance(double w, double h, double d, double aspect) {
        double radius = Math.sqrt(w * w + h * h + d * d) / 2;
        double half = Math.toRadians(FIELD_OF_VIEW / 2);
        // el campo de vision es vertical: en un visor mas alto que ancho manda el horizontal, que es menor
        double limiting = aspect >= 1 ? half : Math.atan(Math.tan(half) * aspect);
        return 1.12 * radius / Math.sin(limiting);
    }

    /* ------------------------------------------------------------------ cotas */

    /** Una cota: la linea de medida entre dos puntos del modelo, sus dos trazos de referencia y el rotulo. */
    private static final class Dimension {
        final Point3D a, b, a0, b0;
        final Line main = line(true), tickA = line(false), tickB = line(false);
        final Label label = new Label();

        Dimension(Point3D a, Point3D b, Point3D a0, Point3D b0, String text) {
            this.a = a;
            this.b = b;
            this.a0 = a0;
            this.b0 = b0;
            label.setText(text);
            label.getStyleClass().add("dim-label");
            label.setManaged(false);
        }

        private static Line line(boolean dashed) {
            Line l = new Line();
            l.getStyleClass().add("dim-line");
            l.setManaged(false);
            if (dashed) {
                l.getStrokeDashArray().addAll(5.0, 4.0);
            }
            return l;
        }
    }

    private void clearDimensions() {
        dimensions.clear();
        overlay.getChildren().clear();
    }

    /**
     * Cotas de la caja envolvente (ancho, fondo y alto), en las unidades reales del archivo. Las lineas se
     * definen en el espacio del modelo y cada fotograma se proyectan a la pantalla: giran con el, pero el trazo
     * y el texto se dibujan en 2D, siempre legibles.
     */
    private void buildDimensions() {
        double w = model.width(), h = model.height(), d = model.depth();
        double m = Math.max(w, d) * 0.07;       // separacion entre el modelo y la linea de cota
        double x0 = -w / 2, x1 = w / 2, z0 = -d / 2, z1 = d / 2;
        dimensions.add(new Dimension(new Point3D(x0, 0, z0 - m), new Point3D(x1, 0, z0 - m),
                new Point3D(x0, 0, z0), new Point3D(x1, 0, z0), mm(w)));
        dimensions.add(new Dimension(new Point3D(x1 + m, 0, z0), new Point3D(x1 + m, 0, z1),
                new Point3D(x1, 0, z0), new Point3D(x1, 0, z1), mm(d)));
        dimensions.add(new Dimension(new Point3D(x0 - m, 0, z1), new Point3D(x0 - m, -h, z1),
                new Point3D(x0, 0, z1), new Point3D(x0, -h, z1), mm(h)));
        for (Dimension dim : dimensions) {
            overlay.getChildren().addAll(dim.main, dim.tickA, dim.tickB, dim.label);
        }
    }

    private String mm(double units) {
        return String.format(Locale.US, "%.2f mm", units / model.scale());
    }

    /** Punto del modelo a coordenadas de la capa de cotas, pasando por la camara del visor. */
    private Point2D project(Point3D p) {
        Point3D inScene = holder.localToScene(p, true);
        return inScene == null ? null : overlay.sceneToLocal(inScene.getX(), inScene.getY());
    }

    private void updateDimensions() {
        boolean on = showDimensions && model != null && explode < 0.01;
        overlay.setVisible(on);
        if (!on) {
            return;
        }
        for (Dimension dim : dimensions) {
            Point2D a = project(dim.a), b = project(dim.b), a0 = project(dim.a0), b0 = project(dim.b0);
            // vista de frente a una cota (el alto, mirando desde arriba): queda reducida a un punto y no se dibuja
            boolean readable = a != null && b != null && a0 != null && b0 != null && a.distance(b) >= 4;
            for (Node n : List.of(dim.main, dim.tickA, dim.tickB, dim.label)) {
                n.setVisible(readable);
            }
            if (!readable) {
                continue;
            }
            set(dim.main, a, b);
            set(dim.tickA, a0, a0.add(a.subtract(a0).multiply(TICK_REACH)));
            set(dim.tickB, b0, b0.add(b.subtract(b0).multiply(TICK_REACH)));
            // el rotulo va en el punto medio, desplazado hacia fuera del modelo para no tapar la linea
            Point2D mid = a.midpoint(b);
            Point2D out = mid.subtract(a0.midpoint(b0));
            Point2D off = out.magnitude() < 1e-6 ? Point2D.ZERO : out.normalize().multiply(16);
            double lw = dim.label.prefWidth(-1), lh = dim.label.prefHeight(-1);
            dim.label.resizeRelocate(mid.getX() + off.getX() - lw / 2, mid.getY() + off.getY() - lh / 2, lw, lh);
        }
    }

    private static void set(Line l, Point2D from, Point2D to) {
        l.setStartX(from.getX());
        l.setStartY(from.getY());
        l.setEndX(to.getX());
        l.setEndY(to.getY());
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
                pick(e);
            }
            dragging = false;
        });
        sub.setOnScroll(e -> {
            tZoom = Math.max(0.5, Math.min(4.0, tZoom * Math.exp(e.getDeltaY() * 0.0015)));
            e.consume();
        });
    }

    private void pick(MouseEvent e) {
        PickResult pr = e.getPickResult();
        ExternalModel.Part part = pr == null ? null : byNode.get(pr.getIntersectedNode());
        listener.onPick(part == null ? null : part.material());
    }

    /* ------------------------------------------------------------------ animacion */

    private void step(double dt) {
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
        updateDimensions();
    }

    /* ------------------------------------------------------------------ miniaturas */

    /**
     * Miniatura del modelo en vista isometrica, con fondo transparente. Usa una escena aparte, asi que el modelo
     * no puede estar a la vez en un visor. Debe llamarse en el hilo de JavaFX. Se dibuja al doble de tamano para
     * que, al reducirla, los bordes queden suaves.
     */
    public static Image thumbnail(ExternalModel.Loaded loaded, int width, int height) {
        Group hold = new Group(loaded.root());
        hold.setTranslateY(loaded.height() / 2);
        Group turn = new Group(hold);
        turn.getTransforms().addAll(new Rotate(32, Rotate.X_AXIS), new Rotate(34, Rotate.Y_AXIS));
        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setFieldOfView(FIELD_OF_VIEW);
        double dist = distance(loaded.width(), loaded.height(), loaded.depth(), (double) width / height) * 0.9;
        cam.setTranslateZ(-dist);
        cam.setNearClip(Math.max(1, dist / 400));
        cam.setFarClip(dist * 8);
        Group root = new Group(turn);
        root.getChildren().addAll(lights());
        // Se fotografia el grupo con la camara como parametro. Un SubScene que no esta en una ventana no llega a
        // dibujar su contenido y la foto sale vacia.
        SnapshotParameters params = new SnapshotParameters();
        params.setCamera(cam);
        params.setDepthBuffer(true);
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(0, 0, width * 2.0, height * 2.0));
        return root.snapshot(params, null);
    }
}
