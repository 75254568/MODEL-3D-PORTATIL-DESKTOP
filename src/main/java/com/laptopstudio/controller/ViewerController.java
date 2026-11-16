package com.laptopstudio.controller;

import com.laptopstudio.model.Laptop;
import com.laptopstudio.ui.Icons;
import com.laptopstudio.ui.PartInspector;
import com.laptopstudio.ui.Refreshable;
import com.laptopstudio.ui.SelectionState;
import com.laptopstudio.viewer.LaptopModel;
import com.laptopstudio.viewer.LaptopViewer;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Visor 3D: dos visores independientes, controles agrupados, lista de componentes y ficha de la pieza elegida. */
@Component
@RequiredArgsConstructor
public class ViewerController implements Refreshable {

    private record Part(String id, String label, boolean interior) {
    }

    private static final List<Part> PARTS = List.of(
            new Part("battery", "Batería", true), new Part("pcb", "Placa base", true),
            new Part("cooling", "Refrigeración", true), new Part("ram", "Memoria RAM", true),
            new Part("ssd", "Almacenamiento", true), new Part("cpu", "Procesador", true),
            new Part("gpu", "Gráficos", true), new Part("display", "Pantalla", false),
            new Part("base", "Chasis", false));

    private final SelectionState selection;
    private final com.laptopstudio.ui.NavigationService navigation;

    @FXML private HBox viewers;
    @FXML private VBox boxA;
    @FXML private VBox boxB;
    @FXML private StackPane holderA;
    @FXML private StackPane holderB;
    @FXML private Label lblNameA;
    @FXML private Label lblNameB;
    @FXML private Label lblModelos;
    @FXML private VBox specA;
    @FXML private VBox specB;
    @FXML private HBox boxCam;
    @FXML private ToggleButton tbSplit;
    @FXML private ToggleButton tbGhost;
    @FXML private ToggleButton tbEstudio;
    @FXML private ToggleButton tbXray;
    @FXML private ToggleButton tbTermico;
    @FXML private ToggleButton tbReposo;
    @FXML private ToggleButton tbCarga;
    @FXML private CheckBox chkSync;
    @FXML private Slider slExplode;
    @FXML private Slider slLid;
    @FXML private Label lblExplode;
    @FXML private Label lblLid;
    @FXML private ListView<Part> lstPartes;
    @FXML private Label lblPieza;
    @FXML private VBox boxDetalle;

    private LaptopViewer viewerA;
    private LaptopViewer viewerB;
    private boolean ghost;
    private final Set<String> ocultas = new HashSet<>();
    private EventHandler<KeyEvent> keyDown;
    private EventHandler<KeyEvent> keyUp;
    private Scene keyScene;

    @FXML
    public void initialize() {
        LaptopViewer.Listener listener = new LaptopViewer.Listener() {
            @Override
            public void onPart(String side, String part) {
                elegirPieza(part, true);
            }

            @Override
            public void onCamera(String side, double rx, double ry, double zoom) {
                LaptopViewer other = "A".equals(side) ? viewerB : viewerA;
                other.setCamera(rx, ry, zoom);
            }
        };
        viewerA = new LaptopViewer("A", listener);
        viewerB = new LaptopViewer("B", listener);
        holderA.getChildren().add(viewerA);
        holderB.getChildren().add(viewerB);

        construirCamara();
        construirLista();
        cablearBarra();

        navigation.immersiveProperty().addListener((o, antes, on) -> inmersivo(on));
        selection.aProperty().addListener((o, a, b) -> refrescar());
        selection.bProperty().addListener((o, a, b) -> refrescar());
        refrescar();
        mostrarDetalle(null);
    }

    /* ------------------------------------------------------------------ construccion */

    private void construirCamara() {
        String[][] presets = {{"3D", "iso"}, {"Frente", "front"}, {"Perfil", "side"}, {"Superior", "top"}, {"Restablecer", "reset"}};
        for (int i = 0; i < presets.length; i++) {
            Button b = new Button(presets[i][0]);
            b.getStyleClass().add(i == 0 ? "seg-first" : i == presets.length - 1 ? "seg-last" : "seg-mid");
            String id = presets[i][1];
            b.setOnAction(e -> {
                viewerA.preset(id);
                viewerB.preset(id);
            });
            boxCam.getChildren().add(b);
        }
    }

    private void construirLista() {
        lstPartes.setItems(FXCollections.observableArrayList(PARTS));
        lstPartes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Part p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean oculta = ocultas.contains(p.id());
                Button eye = new Button();
                eye.setGraphic(Icons.of(oculta ? Icons.EYE_OFF : Icons.EYE, 16));
                eye.getStyleClass().add("icon-only");
                eye.setAccessibleText((oculta ? "Mostrar " : "Ocultar ") + p.label());
                eye.setOnAction(e -> {
                    e.consume();
                    alternarVisible(p.id());
                });
                Label name = new Label(p.label());
                name.setOpacity(oculta ? 0.45 : 1.0);
                Region dot = new Region();
                dot.getStyleClass().add("dot");
                String estado = estadoPieza(p);
                if (!estado.isEmpty()) {
                    dot.getStyleClass().add(estado);
                }
                Region grow = new Region();
                HBox.setHgrow(grow, Priority.ALWAYS);
                HBox row = new HBox(7, eye, name, grow, dot);
                row.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(row);
            }
        });
        lstPartes.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> {
            if (p != null) {
                elegirPieza(p.id(), false);
            }
        });
    }

    /** "ok" si los modelos mostrados tienen esa pieza modelada, "warn" si solo uno, vacio si ninguno. */
    private String estadoPieza(Part p) {
        List<Laptop> mostrados = new java.util.ArrayList<>();
        if (selection.getA() != null) {
            mostrados.add(selection.getA());
        }
        if (selection.getB() != null && !ghost) {
            mostrados.add(selection.getB());
        } else if (selection.getB() != null) {
            mostrados.add(selection.getB());
        }
        if (mostrados.isEmpty()) {
            return "";
        }
        long ok = mostrados.stream().filter(l -> {
            if (!p.interior()) {
                return true;
            }
            LaptopModel.InteriorInfo info = LaptopModel.interiorInfo(l);
            return info.reconstructed() && info.counts().containsKey(p.id());
        }).count();
        return ok == mostrados.size() ? "ok" : ok > 0 ? "warn" : "";
    }

    private void alternarVisible(String id) {
        boolean ocultar = ocultas.add(id);
        if (!ocultar) {
            ocultas.remove(id);
        }
        viewerA.setPartHidden(id, ocultar);
        viewerB.setPartHidden(id, ocultar);
        lstPartes.refresh();
        selection.say((ocultar ? "Oculto: " : "Visible: ") + PARTS.stream().filter(p -> p.id().equals(id)).findFirst().get().label());
    }

    private void group(ToggleButton... buttons) {
        ToggleGroup g = new ToggleGroup();
        for (ToggleButton b : buttons) {
            b.setToggleGroup(g);
        }
        g.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });
    }

    private void cablearBarra() {
        group(tbSplit, tbGhost);
        group(tbEstudio, tbXray, tbTermico);
        group(tbReposo, tbCarga);
        tbSplit.setOnAction(e -> setGhost(false));
        tbGhost.setOnAction(e -> setGhost(true));
        tbEstudio.setOnAction(e -> setMode(LaptopModel.Mode.STUDIO));
        tbXray.setOnAction(e -> setMode(LaptopModel.Mode.XRAY));
        tbTermico.setOnAction(e -> setMode(LaptopModel.Mode.THERMAL));
        tbReposo.setOnAction(e -> setStress(false));
        tbCarga.setOnAction(e -> setStress(true));
        slExplode.valueProperty().addListener((o, a, v) -> {
            viewerA.setExplode(v.doubleValue() / 100.0);
            viewerB.setExplode(v.doubleValue() / 100.0);
            lblExplode.setText(Math.round(v.doubleValue()) + " %");
        });
        slLid.valueProperty().addListener((o, a, v) -> {
            viewerA.setLid(v.doubleValue());
            viewerB.setLid(v.doubleValue());
            lblLid.setText(Math.round(v.doubleValue()) + "°");
        });
        lblExplode.setText("0 %");
        lblLid.setText("105°");
        chkSync.selectedProperty().addListener((o, a, on) -> {
            viewerA.setSync(on);
            viewerB.setSync(on);
        });
        tbSplit.setTooltip(new javafx.scene.control.Tooltip("Cada laptop en su propio visor"));
        tbGhost.setTooltip(new javafx.scene.control.Tooltip("Ambos laptops en el mismo visor, uno sobre otro"));
        tbXray.setTooltip(new javafx.scene.control.Tooltip("Carcasa semitransparente para ver el interior"));
        tbTermico.setTooltip(new javafx.scene.control.Tooltip("Colores por temperatura. Valores estimados, no medidos"));
        tbCarga.setTooltip(new javafx.scene.control.Tooltip("Simula carga máxima en la vista térmica. Valores estimados"));
    }

    /* ------------------------------------------------------------------ acciones */

    private void setGhost(boolean on) {
        ghost = on;
        refrescar();
    }

    private void setMode(LaptopModel.Mode m) {
        viewerA.setMode(m);
        viewerB.setMode(m);
    }

    private void setStress(boolean on) {
        viewerA.setStress(on);
        viewerB.setStress(on);
    }

    private void refrescar() {
        Laptop a = selection.getA();
        Laptop b = selection.getB();
        lblNameA.setText(a == null ? "Sin laptop" : a.getDisplayName());
        lblNameB.setText(b == null ? "Sin laptop" : b.getDisplayName());
        lblModelos.setText(a == null && b == null ? "No hay laptops cargados. Marca laptops en el catálogo y pulsa «Abrir en visor 3D»."
                : (a == null ? "" : "A · " + a.getModel()) + (a != null && b != null ? "   |   " : "") + (b == null ? "" : "B · " + b.getModel()));
        boolean overlay = ghost && a != null && b != null;
        llenarFicha(specA, a);
        llenarFicha(specB, b);
        boxB.setVisible(!overlay);
        boxB.setManaged(!overlay);
        if (overlay) {
            viewerA.setLaptops(a, b);
            lblNameA.setText("A y B superpuestos");
        } else {
            viewerA.setLaptops(a, null);
            viewerB.setLaptops(b, null);
        }
        if (lstPartes != null) {
            lstPartes.refresh();
        }
        Part p = lstPartes == null ? null : lstPartes.getSelectionModel().getSelectedItem();
        if (p != null) {
            mostrarDetalle(p.id());
        }
    }

    /** Resumen de compra del laptop (procesador, RAM, SSD, GPU, pantalla, precio) sobre su visor, sin salir del 3D. */
    private void llenarFicha(VBox box, Laptop l) {
        box.getChildren().clear();
        box.setVisible(l != null && !enInmersivo);
        box.setManaged(l != null && !enInmersivo);
        if (l == null) {
            return;
        }
        String[][] s = com.laptopstudio.ui.SpecSheet.summary(l);
        javafx.scene.layout.GridPane g = new javafx.scene.layout.GridPane();
        g.setHgap(14);
        g.setVgap(3);
        // s[0] es marca y modelo, que ya esta en la cabecera del visor
        for (int i = 1; i < s.length; i++) {
            VBox cell = new VBox(0);
            Label k = new Label(s[i][0].toUpperCase());
            k.getStyleClass().addAll("muted", "small");
            Label v = new Label(s[i][1]);
            v.getStyleClass().add(i == s.length - 1 ? "value-strong" : "small");
            v.setMaxWidth(150);
            v.setTooltip(new javafx.scene.control.Tooltip(s[i][1]));
            cell.getChildren().addAll(k, v);
            g.add(cell, (i - 1) % 2, (i - 1) / 2);
        }
        Button mas = new Button("Ficha completa");
        mas.getStyleClass().add("quiet");
        mas.setOnAction(e -> com.laptopstudio.ui.SpecSheet.show(box, l));
        VBox fila = new VBox(4, g, mas);
        fila.setAlignment(Pos.CENTER_LEFT);
        mas.getStyleClass().add("compact");
        box.setPadding(new javafx.geometry.Insets(4, 10, 6, 10));
        box.getChildren().add(fila);
    }

    /* ------------------------------------------------------------------ pantalla completa */

    private javafx.scene.Node topNormal;
    private javafx.scene.Node leftNormal;
    private javafx.scene.Node rightNormal;
    private javafx.scene.Node bottomNormal;
    private boolean enInmersivo;

    @FXML
    private void onPantallaCompleta() {
        navigation.immersiveProperty().set(true);
    }

    /**
     * Modo inmersivo: quedan solo los dos visores y una barra minima con las vistas de camara y el boton de salir.
     * No se reconstruye ningun modelo: los mismos visores cambian de tamano y la camara se reajusta sola.
     */
    private void inmersivo(boolean on) {
        javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) viewers.getParent();
        if (on && !enInmersivo) {
            topNormal = root.getTop();
            leftNormal = root.getLeft();
            rightNormal = root.getRight();
            bottomNormal = root.getBottom();
            HBox barra = new HBox(8);
            barra.setAlignment(Pos.CENTER_LEFT);
            barra.setPadding(new javafx.geometry.Insets(10, 14, 0, 14));
            Label ayuda = new Label("Arrastra para girar · Rueda para acercar");
            ayuda.getStyleClass().add("immersive-hint");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            barra.getChildren().addAll(ayuda, sp);
            String[][] presets = {{"3D", "iso"}, {"Frente", "front"}, {"Perfil", "side"}, {"Superior", "top"}, {"Restablecer", "reset"}};
            for (String[] p : presets) {
                Button b = new Button(p[0]);
                b.getStyleClass().add("immersive-btn");
                b.setOnAction(e -> {
                    viewerA.preset(p[1]);
                    viewerB.preset(p[1]);
                });
                barra.getChildren().add(b);
            }
            Button salir = new Button("Salir de pantalla completa (Esc)");
            salir.getStyleClass().addAll("immersive-btn", "immersive-exit");
            salir.setOnAction(e -> navigation.immersiveProperty().set(false));
            barra.getChildren().add(salir);
            root.setTop(barra);
            root.setLeft(null);
            root.setRight(null);
            root.setBottom(null);
        } else if (!on && enInmersivo) {
            root.setTop(topNormal);
            root.setLeft(leftNormal);
            root.setRight(rightNormal);
            root.setBottom(bottomNormal);
        }
        enInmersivo = on;
        // la ficha resumida se oculta para dejar todo el alto al modelo
        specA.setVisible(!on && selection.getA() != null);
        specA.setManaged(specA.isVisible());
        specB.setVisible(!on && selection.getB() != null);
        specB.setManaged(specB.isVisible());
    }

    private void elegirPieza(String part, boolean desdeVisor) {
        Part match = PARTS.stream().filter(p -> p.id().equals(part)).findFirst().orElse(null);
        String id = match != null ? match.id() : "base";
        viewerA.select(id);
        viewerB.select(id);
        if (desdeVisor) {
            Part m = PARTS.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
            if (m != null && lstPartes.getSelectionModel().getSelectedItem() != m) {
                lstPartes.getSelectionModel().select(m);
                return;
            }
        }
        mostrarDetalle(id);
    }

    private void mostrarDetalle(String part) {
        boxDetalle.getChildren().clear();
        if (part == null) {
            lblPieza.setText("Sin selección");
            Label hint = new Label("Elige un componente en la lista o haz clic en una pieza del modelo para ver su ficha.");
            hint.setWrapText(true);
            hint.getStyleClass().add("muted");
            hint.setPadding(new javafx.geometry.Insets(12));
            boxDetalle.getChildren().add(hint);
            return;
        }
        PartInspector.Result r = PartInspector.inspect(part, selection.getA(), selection.getB());
        lblPieza.setText(r.title());
        for (PartInspector.Row row : r.rows()) {
            VBox box = new VBox(2);
            box.getStyleClass().add("inspect-row");
            box.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
            Label k = new Label(row.label());
            k.getStyleClass().addAll("field-label");
            k.setWrapText(true);
            box.getChildren().add(k);
            boolean sameValue = row.a().equals(row.b());
            if (selection.getA() != null && selection.getB() != null && sameValue) {
                box.getChildren().add(valor("A y B", row.a(), "muted"));
            } else {
                if (selection.getA() != null) {
                    box.getChildren().add(valor("A", row.a(), "viewer-tag-a"));
                }
                if (selection.getB() != null) {
                    box.getChildren().add(valor("B", row.b(), "viewer-tag-b"));
                }
            }
            boxDetalle.getChildren().add(box);
        }
    }

    private HBox valor(String lado, String texto, String estilo) {
        Label l = new Label(lado);
        l.getStyleClass().add(estilo);
        l.setMinWidth(30);
        Label v = new Label(texto);
        v.setWrapText(true);
        v.setMaxWidth(210);
        if ("Sin dato".equals(texto)) {
            v.getStyleClass().add("faint");
        }
        HBox h = new HBox(6, l, v);
        h.setAlignment(Pos.TOP_LEFT);
        return h;
    }

    /* ------------------------------------------------------------------ teclado fisico */

    @Override
    public void onShow() {
        viewerA.setActive(true);
        viewerB.setActive(true);
        // por si cambio el modelo 3D importado o la moneda mientras la pantalla estaba oculta
        viewerA.invalidateIfModelChanged(selection.getA());
        viewerB.invalidateIfModelChanged(selection.getB());
        refrescar();
        Scene scene = viewers.getScene();
        if (scene == null || keyScene == scene) {
            return;
        }
        keyScene = scene;
        keyDown = e -> forwardKey(e, true);
        keyUp = e -> forwardKey(e, false);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyDown);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, keyUp);
    }

    /** Con la pantalla oculta los visores dejan de animar: no gastan CPU ni GPU. */
    @Override
    public void onHide() {
        navigation.immersiveProperty().set(false);
        viewerA.setActive(false);
        viewerB.setActive(false);
    }

    private void forwardKey(KeyEvent e, boolean down) {
        if (!viewers.isVisible() || !viewers.getParent().isVisible() || e.getTarget() instanceof TextInputControl) {
            return;
        }
        viewerA.key(e.getCode(), down);
        viewerB.key(e.getCode(), down);
    }
}
