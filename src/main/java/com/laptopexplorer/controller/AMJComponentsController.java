package com.laptopexplorer.controller;

import com.laptopexplorer.component.AMJComponentInfo;
import com.laptopexplorer.component.AMJComponentLibrary;
import com.laptopexplorer.component.AMJSpecDoc;
import com.laptopexplorer.ui.AMJIcons;
import com.laptopexplorer.ui.AMJRefreshable;
import com.laptopexplorer.ui.AMJSelectionState;
import com.laptopexplorer.viewer.AMJComponentViewer;
import com.laptopexplorer.viewer.AMJExternalModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Componentes 3D: biblioteca de procesadores, gráficas, memorias y SSD modelados a escala. A la izquierda, la
 * lista con miniaturas y filtros; en el centro, el visor (vistas de cámara, cotas, despiece, ventiladores); a
 * la derecha, las medidas tomadas del modelo, sus piezas por material y la ficha técnica ya limpia.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AMJComponentsController implements AMJRefreshable {

    private static final String[][] CATEGORIES = {{"", "Todos"}, {"CPU", "Procesadores"}, {"GPU", "Gráficos"},
            {"RAM", "Memoria"}, {"SSD", "Almacenamiento"}};
    private static final String[][] VIEWS = {{"iso", "3D"}, {"top", "Superior"}, {"front", "Frente"}, {"side", "Perfil"},
            {"bottom", "Inferior"}};
    private static final int THUMB_W = 66, THUMB_H = 48;
    /** Ancho del nombre en la lista: lo que queda de la columna (264) tras la miniatura, los márgenes y la barra de desplazamiento. */
    private static final double NAME_W = 150;

    private final AMJComponentLibrary library;
    private final AMJSelectionState selection;

    @FXML private Label lblCount;
    @FXML private HBox pnlBusqueda;
    @FXML private TextField txtBuscar;
    @FXML private HBox pnlCategorias;
    @FXML private ToggleButton tglFicha;
    @FXML private ListView<AMJComponentInfo> lstComponentes;
    @FXML private Label lblNombre;
    @FXML private HBox pnlEtiquetas;
    @FXML private HBox pnlCam;
    @FXML private Slider sldExplode;
    @FXML private Label lblExplode;
    @FXML private ToggleButton tglCotas;
    @FXML private ToggleButton tglGirar;
    @FXML private ToggleButton tglVentiladores;
    @FXML private StackPane pnlVisor;
    @FXML private VBox pnlInspector;
    @FXML private Label lblCategoria;
    @FXML private Label lblTitulo;
    @FXML private FlowPane pnlDestacados;
    @FXML private VBox pnlFicha;

    private AMJComponentViewer viewer;
    private FilteredList<AMJComponentInfo> filtered;
    private String category = "";
    private long ticket;                                   // descarta un modelo que termina de cargar tarde
    private final VBox boxModelo = new VBox();             // medidas y piezas: se rellenan al cargar el modelo
    private final VBox boxSecciones = new VBox();          // ficha técnica: se rellena al elegir el componente
    private final Map<String, HBox> materialRows = new LinkedHashMap<>();
    private String selectedMaterial;
    private final Map<String, Image> thumbs = new HashMap<>();
    private ExecutorService thumbQueue;

    /* ------------------------------------------------------------------ inicio */

    @FXML
    public void initialize() {
        pnlBusqueda.getChildren().add(0, AMJIcons.amjOf(AMJIcons.SEARCH, 16));
        viewer = new AMJComponentViewer(this::amjSelectMaterial);
        pnlVisor.getChildren().add(viewer);
        pnlFicha.getChildren().setAll(boxModelo, boxSecciones);

        amjSegmented(pnlCategorias, CATEGORIES, true, key -> {
            category = key;
            amjFilter();
        });
        amjSegmented(pnlCam, VIEWS, false, viewer::amjPreset);

        filtered = new FilteredList<>(FXCollections.observableArrayList(library.amjAll()), c -> true);
        lstComponentes.setItems(filtered);
        lstComponentes.setCellFactory(lv -> new AMJComponentCell());
        lstComponentes.setPlaceholder(new Label("Ningún componente coincide."));
        lstComponentes.getSelectionModel().selectedItemProperty().addListener((o, a, c) -> amjShow(c));
        txtBuscar.textProperty().addListener((o, a, b) -> amjFilter());

        sldExplode.valueProperty().addListener((o, a, v) -> {
            viewer.amjSetExplode(v.doubleValue() / 100.0);
            lblExplode.setText(Math.round(v.doubleValue()) + " %");
        });
        lblExplode.setText("0 %");
        tglCotas.selectedProperty().addListener((o, a, on) -> viewer.amjSetShowDimensions(on));
        tglGirar.selectedProperty().addListener((o, a, on) -> viewer.amjSetAutoRotate(on));
        tglVentiladores.selectedProperty().addListener((o, a, on) -> viewer.amjSetSpinFans(on));
        tglCotas.setTooltip(new javafx.scene.control.Tooltip("Ancho, fondo y alto medidos en el modelo"));
        tglVentiladores.setTooltip(new javafx.scene.control.Tooltip("Hace girar los ventiladores del modelo"));
        amjVisible(tglVentiladores, false);
        tglFicha.selectedProperty().addListener((o, a, on) -> amjVisible(pnlInspector, on));

        amjFilter();
        lstComponentes.getSelectionModel().selectFirst();
    }

    /**
     * Fila de botones unidos a partir de pares {clave, rótulo}; con {@code exclusive} uno queda siempre elegido,
     * como un selector.
     */
    private void amjSegmented(HBox into, String[][] options, boolean exclusive, java.util.function.Consumer<String> onPick) {
        ToggleGroup group = exclusive ? new ToggleGroup() : null;
        for (int i = 0; i < options.length; i++) {
            String style = i == 0 ? "seg-first" : i == options.length - 1 ? "seg-last" : "seg-mid";
            String key = options[i][0];
            javafx.scene.control.ButtonBase b;
            if (exclusive) {
                ToggleButton t = new ToggleButton(options[i][1]);
                t.setToggleGroup(group);
                t.setSelected(i == 0);
                t.setOnAction(e -> {
                    t.setSelected(true);             // un segundo clic no deja el selector sin opción
                    onPick.accept(key);
                });
                b = t;
            } else {
                Button plain = new Button(options[i][1]);
                plain.setOnAction(e -> onPick.accept(key));
                b = plain;
            }
            b.getStyleClass().add(style);
            b.setMinWidth(Region.USE_PREF_SIZE);
            into.getChildren().add(b);
        }
    }

    private static void amjVisible(Node node, boolean on) {
        node.setVisible(on);
        node.setManaged(on);
    }

    @Override
    public void amjOnShow() {
        viewer.amjSetActive(true);
        amjStartThumbnails();
    }

    /** Con la pantalla oculta el visor deja de animar. */
    @Override
    public void amjOnHide() {
        viewer.amjSetActive(false);
    }

    /* ------------------------------------------------------------------ lista */

    private void amjFilter() {
        String q = amjNorm(txtBuscar.getText());
        AMJComponentInfo before = lstComponentes.getSelectionModel().getSelectedItem();
        filtered.setPredicate(c -> (category.isEmpty() || category.equals(c.category()))
                && (q.isEmpty() || amjNorm(c.name() + " " + c.amjCategoryLabel() + " " + c.segment() + " " + c.id()).contains(q)));
        int n = filtered.size(), total = library.amjAll().size();
        lblCount.setText(n == total ? total + " modelos" : n + " de " + total + " modelos");
        if (before != null && filtered.contains(before)) {
            lstComponentes.getSelectionModel().select(before);
        } else if (!filtered.isEmpty()) {
            lstComponentes.getSelectionModel().selectFirst();
        }
    }

    private static String amjNorm(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }

    private final class AMJComponentCell extends ListCell<AMJComponentInfo> {
        private final ImageView image = new ImageView();
        // Text con ancho fijo y no Label: un Label dentro de una celda abrevia el nombre con puntos en vez de partirlo
        private final Text name = new Text();
        private final Label detail = new Label();
        private final HBox row;

        AMJComponentCell() {
            image.setFitWidth(THUMB_W);
            image.setFitHeight(THUMB_H);
            image.setSmooth(true);
            StackPane thumb = new StackPane(image);
            thumb.getStyleClass().add("thumb");
            thumb.setMinSize(THUMB_W, THUMB_H);
            thumb.setMaxSize(THUMB_W, THUMB_H);
            name.getStyleClass().add("component-name");
            name.setWrappingWidth(NAME_W);
            detail.getStyleClass().addAll("muted", "small");
            VBox text = new VBox(1, name, detail);
            text.setAlignment(Pos.CENTER_LEFT);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);
            row = new HBox(10, thumb, text);
            row.setAlignment(Pos.CENTER_LEFT);
            setPrefWidth(0);                         // el texto se ajusta al ancho de la lista en vez de ensancharla
        }

        @Override
        protected void updateItem(AMJComponentInfo c, boolean empty) {
            super.updateItem(c, empty);
            setText(null);
            if (empty || c == null) {
                setGraphic(null);
                return;
            }
            name.setText(c.name());
            detail.setText(c.amjCategoryLabel() + " · " + c.segment());
            image.setImage(thumbs.get(c.id()));
            setGraphic(row);
        }
    }

    /**
     * Las miniaturas se dibujan con el propio visor, una a una y en segundo plano, la primera vez que se abre la
     * pantalla: leer cada modelo no bloquea la interfaz y la lista se va completando.
     */
    private void amjStartThumbnails() {
        if (thumbQueue != null) {
            return;
        }
        thumbQueue = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "miniaturas-componentes");
            t.setDaemon(true);
            return t;
        });
        for (AMJComponentInfo c : library.amjAll()) {
            thumbQueue.submit(() -> {
                try {
                    AMJExternalModel.AMJLoaded m = library.amjLoad(c);
                    Platform.runLater(() -> {
                        try {
                            thumbs.put(c.id(), AMJComponentViewer.amjThumbnail(m, THUMB_W, THUMB_H));
                            lstComponentes.refresh();
                        } catch (RuntimeException e) {
                            log.warn("No se pudo dibujar la miniatura de {}: {}", c.id(), e.toString());
                        }
                    });
                } catch (Exception e) {
                    log.warn("No se pudo leer el modelo de {}: {}", c.id(), e.getMessage());
                }
            });
        }
        thumbQueue.shutdown();                       // termina el hilo cuando acaba la cola
    }

    /* ------------------------------------------------------------------ componente elegido */

    private void amjShow(AMJComponentInfo c) {
        long mine = ++ticket;
        selectedMaterial = null;
        materialRows.clear();
        boxModelo.getChildren().clear();
        boxSecciones.getChildren().clear();
        pnlEtiquetas.getChildren().clear();
        pnlDestacados.getChildren().clear();
        viewer.amjClear();
        amjVisible(tglVentiladores, false);
        if (c == null) {
            lblNombre.setText("Sin componente");
            lblCategoria.setText("");
            lblTitulo.setText("Elige un componente de la lista.");
            viewer.amjSetStatus("");
            return;
        }
        lblNombre.setText(c.name());
        pnlEtiquetas.getChildren().addAll(amjTag(c.amjCategoryLabel(), null), amjTag(c.segment(), null));
        lblCategoria.setText((c.amjCategoryLabel() + " · " + c.segment()).toUpperCase(Locale.ROOT));
        lblTitulo.setText(c.name());
        AMJSpecDoc doc = library.amjSpec(c);
        for (String h : doc.highlights()) {
            Label chip = amjTag(h, null);
            chip.setMaxWidth(300);                   // un destacado largo se parte en líneas en vez de cortarse
            pnlDestacados.getChildren().add(chip);
        }
        amjRenderSpec(doc);

        viewer.amjSetStatus("Cargando modelo…");
        Task<AMJExternalModel.AMJLoaded> task = new Task<>() {
            @Override
            protected AMJExternalModel.AMJLoaded call() throws Exception {
                return library.amjLoad(c);
            }
        };
        task.setOnSucceeded(e -> {
            if (mine != ticket) {
                return;                              // ya se eligio otro componente mientras cargaba
            }
            AMJExternalModel.AMJLoaded m = task.getValue();
            viewer.amjShow(m);
            amjVisible(tglVentiladores, viewer.amjHasFans());
            amjRenderModel(m);
            selection.amjSay(c.name() + " · " + String.format(Locale.US, "%,d", m.triangles()) + " triángulos");
        });
        task.setOnFailed(e -> {
            if (mine == ticket) {
                log.warn("No se pudo cargar el modelo de {}", c.id(), task.getException());
                viewer.amjSetStatus("No se pudo cargar el modelo 3D de este componente.");
            }
        });
        Thread th = new Thread(task, "carga-componente-3d");
        th.setDaemon(true);
        th.start();
    }

    private static Label amjTag(String text, String kind) {
        Label t = new Label(text);
        t.getStyleClass().add("tag");
        if (kind != null) {
            t.getStyleClass().add(kind);
        }
        t.setWrapText(true);
        return t;
    }

    /* ------------------------------------------------------------------ medidas y piezas del modelo */

    private void amjRenderModel(AMJExternalModel.AMJLoaded m) {
        double k = m.scale();
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.ColumnConstraints col = new javafx.scene.layout.ColumnConstraints();
            col.setPercentWidth(100.0 / 3);
            grid.getColumnConstraints().add(col);
        }
        grid.add(amjMetric(amjMm(m.width() / k), "Ancho"), 0, 0);
        grid.add(amjMetric(amjMm(m.depth() / k), "Fondo"), 1, 0);
        grid.add(amjMetric(amjMm(m.height() / k), "Alto"), 2, 0);
        Map<String, List<AMJExternalModel.AMJPart>> byMaterial = new LinkedHashMap<>();
        for (AMJExternalModel.AMJPart p : m.parts()) {
            byMaterial.computeIfAbsent(p.material(), x -> new java.util.ArrayList<>()).add(p);
        }
        grid.add(amjMetric(String.format(Locale.US, "%,d", m.triangles()), "Triángulos"), 0, 1);
        grid.add(amjMetric(String.valueOf(byMaterial.size()), "Materiales"), 1, 1);
        Label nota = new Label("Medidas tomadas de la geometría del modelo, que está a escala real. Incluyen contactos y salientes.");
        nota.getStyleClass().addAll("muted", "small");
        nota.setWrapText(true);
        VBox medidas = new VBox(8, grid, nota);
        medidas.setPadding(new Insets(8, 2, 12, 2));

        VBox piezas = new VBox(1);
        piezas.setPadding(new Insets(6, 6, 10, 6));
        byMaterial.forEach((material, parts) -> {
            HBox row = amjMaterialRow(material, parts);
            materialRows.put(material, row);
            piezas.getChildren().add(row);
        });
        Label ayuda = new Label("Haz clic en una pieza, aquí o en el modelo, para resaltarla.");
        ayuda.getStyleClass().addAll("muted", "small");
        ayuda.setWrapText(true);
        ayuda.setPadding(new Insets(6, 8, 0, 8));
        piezas.getChildren().add(ayuda);

        boxModelo.getChildren().setAll(amjPane("Medidas del modelo 3D", medidas, true),
                amjPane("Piezas y materiales", piezas, true));
    }

    private static String amjMm(double v) {
        return String.format(Locale.US, "%.2f mm", v);
    }

    private static VBox amjMetric(String value, String caption) {
        Label v = new Label(value);
        v.getStyleClass().add("metric-value");
        Label c = new Label(caption);
        c.getStyleClass().addAll("muted", "small");
        VBox box = new VBox(0, v, c);
        box.getStyleClass().add("metric");
        return box;
    }

    private HBox amjMaterialRow(String material, List<AMJExternalModel.AMJPart> parts) {
        Region swatch = new Region();
        swatch.getStyleClass().add("swatch");
        Color c = parts.get(0).color();
        swatch.setStyle(String.format(Locale.ROOT, "-fx-background-color: rgb(%d,%d,%d);", (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255), (int) (c.getBlue() * 255)));
        Label name = new Label(library.amjMaterialLabel(material));
        name.setWrapText(true);
        name.setMinWidth(0);
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);
        Button eye = new Button();
        eye.setGraphic(AMJIcons.amjOf(AMJIcons.EYE, 16));
        eye.getStyleClass().add("icon-only");
        eye.setAccessibleText("Ocultar " + name.getText());
        boolean[] hiddenNow = {false};
        eye.setOnAction(e -> {
            hiddenNow[0] = !hiddenNow[0];
            viewer.amjSetHidden(material, hiddenNow[0]);
            eye.setGraphic(AMJIcons.amjOf(hiddenNow[0] ? AMJIcons.EYE_OFF : AMJIcons.EYE, 16));
            eye.setAccessibleText((hiddenNow[0] ? "Mostrar " : "Ocultar ") + name.getText());
            name.setOpacity(hiddenNow[0] ? 0.45 : 1.0);
            e.consume();
        });
        HBox row = new HBox(9, swatch, name, eye);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("material-row");
        row.setOnMouseClicked(e -> amjSelectMaterial(material.equals(selectedMaterial) ? null : material));
        return row;
    }

    /** Resalta un material en el modelo y en la lista; null (o un clic fuera del modelo) quita el resaltado. */
    private void amjSelectMaterial(String material) {
        selectedMaterial = material;
        viewer.amjHighlight(material);
        materialRows.forEach((m, row) -> {
            row.getStyleClass().remove("selected");
            if (m.equals(material)) {
                row.getStyleClass().add("selected");
            }
        });
        if (material != null) {
            selection.amjSay("Resaltado: " + library.amjMaterialLabel(material));
        }
    }

    /* ------------------------------------------------------------------ ficha técnica */

    private void amjRenderSpec(AMJSpecDoc doc) {
        if (doc.sections().isEmpty()) {
            Label none = new Label("Este componente no trae ficha técnica.");
            none.getStyleClass().add("muted");
            none.setPadding(new Insets(12));
            boxSecciones.getChildren().setAll(none);
            return;
        }
        int n = 0;
        for (AMJSpecDoc.AMJSection s : doc.sections()) {
            VBox body = new VBox(8);
            body.setPadding(new Insets(8, 14, 12, 14));
            s.blocks().forEach(b -> body.getChildren().add(amjBlock(b)));
            boxSecciones.getChildren().add(amjPane(s.title(), body, n++ == 0));
        }
    }

    private static TitledPane amjPane(String title, Node content, boolean expanded) {
        TitledPane tp = new TitledPane(title.toUpperCase(Locale.ROOT), content);
        tp.setExpanded(expanded);
        tp.setAnimated(false);
        // un título largo no debe ensanchar el panel (y con el, todo su contenido): se abrevia y se lee entero al pasar el ratón
        tp.setTooltip(new javafx.scene.control.Tooltip(title));
        tp.setWrapText(true);
        tp.setMinWidth(0);
        tp.setPrefWidth(0);
        tp.setMaxWidth(Double.MAX_VALUE);
        return tp;
    }

    private Node amjBlock(AMJSpecDoc.AMJBlock b) {
        if (b instanceof AMJSpecDoc.AMJParagraph p) {
            return amjFlow(p.text(), "spec-run");
        }
        if (b instanceof AMJSpecDoc.AMJSubheading h) {
            Label l = new Label(h.text());
            l.getStyleClass().add("spec-subhead");
            l.setWrapText(true);
            return l;
        }
        if (b instanceof AMJSpecDoc.AMJBullets list) {
            VBox box = new VBox(4);
            for (AMJSpecDoc.AMJItem it : list.items()) {
                Label marker = new Label(it.marker());
                marker.getStyleClass().add("muted");
                marker.setMinWidth(it.marker().length() > 1 ? 18 : 10);
                TextFlow text = amjFlow(it.text(), "spec-run");
                HBox.setHgrow(text, Priority.ALWAYS);
                HBox row = new HBox(6, marker, text);
                row.setAlignment(Pos.TOP_LEFT);
                row.setPadding(new Insets(0, 0, 0, 14.0 * it.level()));
                box.getChildren().add(row);
            }
            return box;
        }
        if (b instanceof AMJSpecDoc.AMJTable t) {
            return amjTable(t);
        }
        AMJSpecDoc.AMJNote note = (AMJSpecDoc.AMJNote) b;
        VBox box = new VBox(6);
        box.getStyleClass().add("spec-note");
        note.blocks().forEach(x -> box.getChildren().add(amjBlock(x)));
        return box;
    }

    /**
     * Una tabla se muestra por filas apiladas: el nombre del dato, su valor destacado (con la tolerancia al lado
     * si la tabla la trae) y debajo el resto de columnas. Una tabla ancha no cabe en un panel lateral.
     */
    private Node amjTable(AMJSpecDoc.AMJTable t) {
        int tol = -1;
        for (int i = 0; i < t.headers().size(); i++) {
            if (amjNorm(t.headers().get(i)).contains("tolerancia")) {
                tol = i;
            }
        }
        VBox box = new VBox();
        for (List<AMJSpecDoc.AMJRich> row : t.rows()) {
            VBox r = new VBox(2);
            r.getStyleClass().add("inspect-row");
            r.setPadding(new Insets(7, 0, 7, 0));
            r.getChildren().add(amjFlow(row.get(0), "spec-muted"));
            if (row.size() > 1) {
                TextFlow value = amjFlow(row.get(1), "spec-value");
                if (tol > 1 && tol < row.size() && !row.get(tol).amjPlain().isBlank()) {
                    Label tolerance = amjTag(row.get(tol).amjPlain(), null);
                    tolerance.setTranslateY(-1);
                    value.getChildren().addAll(new Text("  "), tolerance);
                }
                r.getChildren().add(value);
            }
            for (int i = 2; i < row.size(); i++) {
                if (i != tol && !row.get(i).amjPlain().isBlank()) {
                    r.getChildren().add(amjFlow(row.get(i), "spec-muted"));
                }
            }
            box.getChildren().add(r);
        }
        return box;
    }

    /** Texto con sus tramos resaltados; si a la ficha original le faltan cifras, lo dice en vez de disimularlo. */
    private static TextFlow amjFlow(AMJSpecDoc.AMJRich rich, String style) {
        TextFlow f = new TextFlow();
        for (AMJSpecDoc.AMJRun run : rich.runs()) {
            Text t = new Text(run.text());
            t.getStyleClass().add(style);
            if (run.bold()) {
                t.getStyleClass().add("spec-bold");
            }
            f.getChildren().add(t);
        }
        if (rich.incomplete()) {
            Label warn = amjTag("cifra incompleta en la ficha original", "warn");
            warn.setWrapText(false);
            f.getChildren().addAll(new Text("  "), warn);
        }
        f.setMinWidth(0);
        f.setPrefWidth(0);                           // toma el ancho del panel y parte las líneas ahi
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }
}
