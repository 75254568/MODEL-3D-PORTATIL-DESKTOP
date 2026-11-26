package com.laptopexplorer.controller;

import com.laptopexplorer.model.AMJLaptop;
import com.laptopexplorer.ui.AMJIcons;
import com.laptopexplorer.ui.AMJPartInspector;
import com.laptopexplorer.ui.AMJRefreshable;
import com.laptopexplorer.ui.AMJSelectionState;
import com.laptopexplorer.viewer.AMJLaptopModel;
import com.laptopexplorer.viewer.AMJLaptopViewer;
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
public class AMJViewerController implements AMJRefreshable {

    private record AMJPart(String id, String label, boolean interior) {
    }

    private static final List<AMJPart> PARTS = List.of(
            new AMJPart("battery", "Batería", true), new AMJPart("pcb", "Placa base", true),
            new AMJPart("cooling", "Refrigeración", true), new AMJPart("ram", "Memoria RAM", true),
            new AMJPart("ssd", "Almacenamiento", true), new AMJPart("cpu", "Procesador", true),
            new AMJPart("gpu", "Gráficos", true), new AMJPart("display", "Pantalla", false),
            new AMJPart("base", "Chasis", false));

    private final AMJSelectionState selection;
    private final com.laptopexplorer.ui.AMJNavigationService navigation;

    @FXML private HBox pnlVisores;
    @FXML private VBox pnlA;
    @FXML private VBox pnlB;
    @FXML private StackPane pnlVisorA;
    @FXML private StackPane pnlVisorB;
    @FXML private Label lblNameA;
    @FXML private Label lblNameB;
    @FXML private Label lblModelos;
    @FXML private VBox pnlSpecA;
    @FXML private VBox pnlSpecB;
    @FXML private HBox pnlCam;
    @FXML private ToggleButton tglSplit;
    @FXML private ToggleButton tglGhost;
    @FXML private ToggleButton tglEstudio;
    @FXML private ToggleButton tglXray;
    @FXML private ToggleButton tglTermico;
    @FXML private ToggleButton tglReposo;
    @FXML private ToggleButton tglCarga;
    @FXML private CheckBox chkSync;
    @FXML private Slider sldExplode;
    @FXML private Slider sldLid;
    @FXML private Label lblExplode;
    @FXML private Label lblLid;
    @FXML private ListView<AMJPart> lstPartes;
    @FXML private Label lblPieza;
    @FXML private VBox pnlDetalle;

    private AMJLaptopViewer viewerA;
    private AMJLaptopViewer viewerB;
    private boolean ghost;
    private final Set<String> ocultas = new HashSet<>();
    private EventHandler<KeyEvent> keyDown;
    private EventHandler<KeyEvent> keyUp;
    private Scene keyScene;

    @FXML
    public void initialize() {
        AMJLaptopViewer.AMJListener listener = new AMJLaptopViewer.AMJListener() {
            @Override
            public void amjOnPart(String side, String part) {
                amjElegirPieza(part, true);
            }

            @Override
            public void amjOnCamera(String side, double rx, double ry, double zoom) {
                AMJLaptopViewer other = "A".equals(side) ? viewerB : viewerA;
                other.amjSetCamera(rx, ry, zoom);
            }
        };
        viewerA = new AMJLaptopViewer("A", listener);
        viewerB = new AMJLaptopViewer("B", listener);
        pnlVisorA.getChildren().add(viewerA);
        pnlVisorB.getChildren().add(viewerB);

        amjConstruirCamara();
        amjConstruirLista();
        amjCablearBarra();

        navigation.amjImmersiveProperty().addListener((o, antes, on) -> amjInmersivo(on));
        selection.amjAProperty().addListener((o, a, b) -> amjRefrescar());
        selection.amjBProperty().addListener((o, a, b) -> amjRefrescar());
        amjRefrescar();
        amjMostrarDetalle(null);
    }

    /* ------------------------------------------------------------------ construcción */

    private void amjConstruirCamara() {
        String[][] presets = {{"3D", "iso"}, {"Frente", "front"}, {"Perfil", "side"}, {"Superior", "top"}, {"Restablecer", "reset"}};
        for (int i = 0; i < presets.length; i++) {
            Button b = new Button(presets[i][0]);
            b.getStyleClass().add(i == 0 ? "seg-first" : i == presets.length - 1 ? "seg-last" : "seg-mid");
            String id = presets[i][1];
            b.setOnAction(e -> {
                viewerA.amjPreset(id);
                viewerB.amjPreset(id);
            });
            pnlCam.getChildren().add(b);
        }
    }

    private void amjConstruirLista() {
        lstPartes.setItems(FXCollections.observableArrayList(PARTS));
        lstPartes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AMJPart p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean oculta = ocultas.contains(p.id());
                Button eye = new Button();
                eye.setGraphic(AMJIcons.amjOf(oculta ? AMJIcons.EYE_OFF : AMJIcons.EYE, 16));
                eye.getStyleClass().add("icon-only");
                eye.setAccessibleText((oculta ? "Mostrar " : "Ocultar ") + p.label());
                eye.setOnAction(e -> {
                    e.consume();
                    amjAlternarVisible(p.id());
                });
                Label name = new Label(p.label());
                name.setOpacity(oculta ? 0.45 : 1.0);
                Region dot = new Region();
                dot.getStyleClass().add("dot");
                String estado = amjEstadoPieza(p);
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
                amjElegirPieza(p.id(), false);
            }
        });
    }

    /** "ok" si los modelos mostrados tienen esa pieza modelada, "warn" si solo uno, vacío si ninguno. */
    private String amjEstadoPieza(AMJPart p) {
        List<AMJLaptop> mostrados = new java.util.ArrayList<>();
        if (selection.amjGetA() != null) {
            mostrados.add(selection.amjGetA());
        }
        if (selection.amjGetB() != null && !ghost) {
            mostrados.add(selection.amjGetB());
        } else if (selection.amjGetB() != null) {
            mostrados.add(selection.amjGetB());
        }
        if (mostrados.isEmpty()) {
            return "";
        }
        long ok = mostrados.stream().filter(l -> {
            if (!p.interior()) {
                return true;
            }
            AMJLaptopModel.AMJInteriorInfo info = AMJLaptopModel.amjInteriorInfo(l);
            return info.reconstructed() && info.counts().containsKey(p.id());
        }).count();
        return ok == mostrados.size() ? "ok" : ok > 0 ? "warn" : "";
    }

    private void amjAlternarVisible(String id) {
        boolean ocultar = ocultas.add(id);
        if (!ocultar) {
            ocultas.remove(id);
        }
        viewerA.amjSetPartHidden(id, ocultar);
        viewerB.amjSetPartHidden(id, ocultar);
        lstPartes.refresh();
        selection.amjSay((ocultar ? "Oculto: " : "Visible: ") + PARTS.stream().filter(p -> p.id().equals(id)).findFirst().get().label());
    }

    private void amjGroup(ToggleButton... buttons) {
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

    private void amjCablearBarra() {
        amjGroup(tglSplit, tglGhost);
        amjGroup(tglEstudio, tglXray, tglTermico);
        amjGroup(tglReposo, tglCarga);
        tglSplit.setOnAction(e -> amjSetGhost(false));
        tglGhost.setOnAction(e -> amjSetGhost(true));
        tglEstudio.setOnAction(e -> amjSetMode(AMJLaptopModel.AMJMode.STUDIO));
        tglXray.setOnAction(e -> amjSetMode(AMJLaptopModel.AMJMode.XRAY));
        tglTermico.setOnAction(e -> amjSetMode(AMJLaptopModel.AMJMode.THERMAL));
        tglReposo.setOnAction(e -> amjSetStress(false));
        tglCarga.setOnAction(e -> amjSetStress(true));
        sldExplode.valueProperty().addListener((o, a, v) -> {
            viewerA.amjSetExplode(v.doubleValue() / 100.0);
            viewerB.amjSetExplode(v.doubleValue() / 100.0);
            lblExplode.setText(Math.round(v.doubleValue()) + " %");
        });
        sldLid.valueProperty().addListener((o, a, v) -> {
            viewerA.amjSetLid(v.doubleValue());
            viewerB.amjSetLid(v.doubleValue());
            lblLid.setText(Math.round(v.doubleValue()) + "°");
        });
        lblExplode.setText("0 %");
        lblLid.setText("105°");
        chkSync.selectedProperty().addListener((o, a, on) -> {
            viewerA.amjSetSync(on);
            viewerB.amjSetSync(on);
        });
        tglSplit.setTooltip(new javafx.scene.control.Tooltip("Cada laptop en su propio visor"));
        tglGhost.setTooltip(new javafx.scene.control.Tooltip("Ambos laptops en el mismo visor, uno sobre otro"));
        tglXray.setTooltip(new javafx.scene.control.Tooltip("Carcasa semitransparente para ver el interior"));
        tglTermico.setTooltip(new javafx.scene.control.Tooltip("Colores por temperatura. Valores estimados, no medidos"));
        tglCarga.setTooltip(new javafx.scene.control.Tooltip("Simula carga máxima en la vista térmica. Valores estimados"));
    }

    /* ------------------------------------------------------------------ acciones */

    private void amjSetGhost(boolean on) {
        ghost = on;
        amjRefrescar();
    }

    private void amjSetMode(AMJLaptopModel.AMJMode m) {
        viewerA.amjSetMode(m);
        viewerB.amjSetMode(m);
    }

    private void amjSetStress(boolean on) {
        viewerA.amjSetStress(on);
        viewerB.amjSetStress(on);
    }

    private void amjRefrescar() {
        AMJLaptop a = selection.amjGetA();
        AMJLaptop b = selection.amjGetB();
        lblNameA.setText(a == null ? "Sin laptop" : a.amjGetDisplayName());
        lblNameB.setText(b == null ? "Sin laptop" : b.amjGetDisplayName());
        lblModelos.setText(a == null && b == null ? "No hay laptops cargados. Marca laptops en el catálogo y pulsa «Abrir en visor 3D»."
                : (a == null ? "" : "A · " + a.getModel()) + (a != null && b != null ? "   |   " : "") + (b == null ? "" : "B · " + b.getModel()));
        boolean overlay = ghost && a != null && b != null;
        amjLlenarFicha(pnlSpecA, a);
        amjLlenarFicha(pnlSpecB, b);
        pnlB.setVisible(!overlay);
        pnlB.setManaged(!overlay);
        if (overlay) {
            viewerA.amjSetLaptops(a, b);
            lblNameA.setText("A y B superpuestos");
        } else {
            viewerA.amjSetLaptops(a, null);
            viewerB.amjSetLaptops(b, null);
        }
        if (lstPartes != null) {
            lstPartes.refresh();
        }
        AMJPart p = lstPartes == null ? null : lstPartes.getSelectionModel().getSelectedItem();
        if (p != null) {
            amjMostrarDetalle(p.id());
        }
    }

    /** Resumen de compra del laptop (procesador, RAM, SSD, GPU, pantalla, precio) sobre su visor, sin salir del 3D. */
    private void amjLlenarFicha(VBox box, AMJLaptop l) {
        box.getChildren().clear();
        box.setVisible(l != null && !enInmersivo);
        box.setManaged(l != null && !enInmersivo);
        if (l == null) {
            return;
        }
        String[][] s = com.laptopexplorer.ui.AMJSpecSheet.amjSummary(l);
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
        mas.setOnAction(e -> com.laptopexplorer.ui.AMJSpecSheet.amjShow(box, l));
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
    private void amjOnPantallaCompleta() {
        navigation.amjImmersiveProperty().set(true);
    }

    /**
     * Modo inmersivo: quedan solo los dos visores y una barra mínima con las vistas de cámara y el botón de salir.
     * No se reconstruye ningún modelo: los mismos visores cambian de tamaño y la cámara se reajusta sola.
     */
    private void amjInmersivo(boolean on) {
        javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) pnlVisores.getParent();
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
                    viewerA.amjPreset(p[1]);
                    viewerB.amjPreset(p[1]);
                });
                barra.getChildren().add(b);
            }
            Button salir = new Button("Salir de pantalla completa (Esc)");
            salir.getStyleClass().addAll("immersive-btn", "immersive-exit");
            salir.setOnAction(e -> navigation.amjImmersiveProperty().set(false));
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
        pnlSpecA.setVisible(!on && selection.amjGetA() != null);
        pnlSpecA.setManaged(pnlSpecA.isVisible());
        pnlSpecB.setVisible(!on && selection.amjGetB() != null);
        pnlSpecB.setManaged(pnlSpecB.isVisible());
    }

    private void amjElegirPieza(String part, boolean desdeVisor) {
        AMJPart match = PARTS.stream().filter(p -> p.id().equals(part)).findFirst().orElse(null);
        String id = match != null ? match.id() : "base";
        viewerA.amjSelect(id);
        viewerB.amjSelect(id);
        if (desdeVisor) {
            AMJPart m = PARTS.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
            if (m != null && lstPartes.getSelectionModel().getSelectedItem() != m) {
                lstPartes.getSelectionModel().select(m);
                return;
            }
        }
        amjMostrarDetalle(id);
    }

    private void amjMostrarDetalle(String part) {
        pnlDetalle.getChildren().clear();
        if (part == null) {
            lblPieza.setText("Sin selección");
            Label hint = new Label("Elige un componente en la lista o haz clic en una pieza del modelo para ver su ficha.");
            hint.setWrapText(true);
            hint.getStyleClass().add("muted");
            hint.setPadding(new javafx.geometry.Insets(12));
            pnlDetalle.getChildren().add(hint);
            return;
        }
        AMJPartInspector.AMJResult r = AMJPartInspector.amjInspect(part, selection.amjGetA(), selection.amjGetB());
        lblPieza.setText(r.title());
        for (AMJPartInspector.AMJRow row : r.rows()) {
            VBox box = new VBox(2);
            box.getStyleClass().add("inspect-row");
            box.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
            Label k = new Label(row.label());
            k.getStyleClass().addAll("field-label");
            k.setWrapText(true);
            box.getChildren().add(k);
            boolean sameValue = row.a().equals(row.b());
            if (selection.amjGetA() != null && selection.amjGetB() != null && sameValue) {
                box.getChildren().add(amjValor("A y B", row.a(), "muted"));
            } else {
                if (selection.amjGetA() != null) {
                    box.getChildren().add(amjValor("A", row.a(), "viewer-tag-a"));
                }
                if (selection.amjGetB() != null) {
                    box.getChildren().add(amjValor("B", row.b(), "viewer-tag-b"));
                }
            }
            pnlDetalle.getChildren().add(box);
        }
    }

    private HBox amjValor(String lado, String texto, String estilo) {
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

    /* ------------------------------------------------------------------ teclado físico */

    @Override
    public void amjOnShow() {
        viewerA.amjSetActive(true);
        viewerB.amjSetActive(true);
        // por si cambio el modelo 3D importado o la moneda mientras la pantalla estaba oculta
        viewerA.amjInvalidateIfModelChanged(selection.amjGetA());
        viewerB.amjInvalidateIfModelChanged(selection.amjGetB());
        amjRefrescar();
        Scene scene = pnlVisores.getScene();
        if (scene == null || keyScene == scene) {
            return;
        }
        keyScene = scene;
        keyDown = e -> amjForwardKey(e, true);
        keyUp = e -> amjForwardKey(e, false);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyDown);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, keyUp);
    }

    /** Con la pantalla oculta los visores dejan de animar: no gastan CPU ni GPU. */
    @Override
    public void amjOnHide() {
        navigation.amjImmersiveProperty().set(false);
        viewerA.amjSetActive(false);
        viewerB.amjSetActive(false);
    }

    private void amjForwardKey(KeyEvent e, boolean down) {
        if (!pnlVisores.isVisible() || !pnlVisores.getParent().isVisible() || e.getTarget() instanceof TextInputControl) {
            return;
        }
        viewerA.amjKey(e.getCode(), down);
        viewerB.amjKey(e.getCode(), down);
    }
}
