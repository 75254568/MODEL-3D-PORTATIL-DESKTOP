package com.laptopexplorer.controller;

import com.laptopexplorer.exception.AMJNegocioException;
import com.laptopexplorer.model.AMJLaptop;
import com.laptopexplorer.service.AMJLaptopService;
import com.laptopexplorer.ui.AMJDialogs;
import com.laptopexplorer.ui.AMJFmt;
import com.laptopexplorer.ui.AMJIcons;
import com.laptopexplorer.ui.AMJNavigationService;
import com.laptopexplorer.ui.AMJRefreshable;
import com.laptopexplorer.ui.AMJScreen;
import com.laptopexplorer.ui.AMJSelectionState;
import com.laptopexplorer.viewer.AMJLaptopModel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Catálogo de laptops: tabla con búsqueda y filtros, ficha agrupada por secciones con edición validada,
 * marcado de 2 a 4 laptops para comparar y acceso al visor 3D.
 */
@Component
@RequiredArgsConstructor
public class AMJCatalogController implements AMJRefreshable {

    private static final String TODAS_MARCAS = "Todas las marcas";
    private static final String TODAS_CATEGORIAS = "Todas las categorías";
    private static final Pattern RTX50 = Pattern.compile("RTX (serie )?50", Pattern.CASE_INSENSITIVE);
    private static final Pattern RTX40 = Pattern.compile("RTX (serie )?40", Pattern.CASE_INSENSITIVE);

    private final AMJLaptopService service;
    private final AMJSelectionState selection;
    private final AMJNavigationService navigation;
    private final com.laptopexplorer.service.AMJModelStore modelStore;
    private final com.laptopexplorer.ui.AMJLaptopThumbs thumbs;
    private final com.laptopexplorer.service.AMJAppConfig config;

    @FXML private ToggleButton tglTabla;
    @FXML private ToggleButton tglTarjetas;
    @FXML private javafx.scene.control.ScrollPane scrTarjetas;
    @FXML private javafx.scene.layout.FlowPane pnlRejilla;
    @FXML private Button btnModelo3d;
    @FXML private TextField txtBuscar;
    @FXML private HBox pnlBusqueda;
    @FXML private ComboBox<String> cmbMarca;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ToggleButton tglRtx50;
    @FXML private ToggleButton tglRtx40;
    @FXML private ToggleButton tglRecon;
    @FXML private ToggleButton tgl2026;
    @FXML private Label lblCount;
    @FXML private TableView<AMJLaptop> tblLaptops;
    @FXML private Label lblMarca;
    @FXML private Label lblModelo;
    @FXML private HBox pnlEstado3d;
    @FXML private VBox pnlFicha;
    @FXML private Button btnGuardar;
    @FXML private Button btnDescartar;
    @FXML private Button btnEliminar;
    @FXML private Label lblSeleccion;
    @FXML private Button btnVisor;
    @FXML private Button btnComparar;

    private final ObservableList<AMJLaptop> todos = FXCollections.observableArrayList();
    private FilteredList<AMJLaptop> filtrados;
    private final Map<Long, Boolean> reconstruido = new HashMap<>();
    private final Map<String, TextField> campos = new LinkedHashMap<>();
    private static final String RAM_TEXTO = "RAM ampliable (módulos SO-DIMM)";
    private final CheckBox chkRam = new CheckBox(RAM_TEXTO);
    private AMJLaptop actual;
    private boolean cargando;
    private boolean sucio;
    private boolean revirtiendo;

    /* ------------------------------------------------------------------ inicio */

    @FXML
    public void initialize() {
        pnlBusqueda.getChildren().add(0, AMJIcons.amjOf(AMJIcons.SEARCH, 16));
        tblLaptops.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblLaptops.setPlaceholder(new Label("Ningún laptop coincide con los filtros."));

        amjColumnaMarca();
        amjColumna("Marca", 100, false, AMJLaptop::getBrand, Comparator.comparing(l -> l.getBrand().toLowerCase(Locale.ROOT)));
        amjColumna("Modelo", 200, false, AMJLaptop::getModel, Comparator.comparing(l -> l.getModel().toLowerCase(Locale.ROOT)));
        amjColumna("Procesador", 170, false, AMJLaptop::getCpu, Comparator.comparing(l -> l.getCpu().toLowerCase(Locale.ROOT)));
        amjColumna("Gráficos", 190, false, AMJLaptop::getGpu, Comparator.comparing(l -> l.getGpu().toLowerCase(Locale.ROOT)));
        amjColumna("RAM", 72, true, l -> AMJFmt.amjNum(l.getRamGb(), "GB"), amjNumerico(AMJLaptop::getRamGb));
        amjColumna("Pantalla", 86, true, l -> AMJFmt.amjNum(l.getScreenInches(), "in"), amjNumerico(AMJLaptop::getScreenInches));
        amjColumna("Peso", 84, true, l -> AMJFmt.amjNum(l.getWeightKg(), "kg"), amjNumerico(AMJLaptop::getWeightKg));
        amjColumna("Batería", 84, true, l -> AMJFmt.amjNum(l.getBatteryWh(), "Wh"), amjNumerico(AMJLaptop::getBatteryWh));
        amjColumna("Precio", 120, true, l -> AMJFmt.amjPrice(l.getPriceUsd(), l.getPriceEstimated()).replace("aprox. ", "~"), amjNumerico(AMJLaptop::getPriceUsd));
        amjColumnaInterior();

        filtrados = new FilteredList<>(todos, l -> true);
        SortedList<AMJLaptop> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tblLaptops.comparatorProperty());
        tblLaptops.setItems(ordenados);
        tblLaptops.getSelectionModel().selectedItemProperty().addListener((o, antes, ahora) -> amjAlCambiarFila(antes, ahora));

        amjConstruirFicha();
        cmbMarca.valueProperty().addListener((o, a, b) -> amjFiltrar());
        cmbCategoria.valueProperty().addListener((o, a, b) -> amjFiltrar());
        txtBuscar.textProperty().addListener((o, a, b) -> amjFiltrar());
        for (ToggleButton t : List.of(tglRtx50, tglRtx40, tglRecon, tgl2026)) {
            t.selectedProperty().addListener((o, a, b) -> {
                if (b && t == tglRtx50) {
                    tglRtx40.setSelected(false);
                } else if (b && t == tglRtx40) {
                    tglRtx50.setSelected(false);
                }
                amjFiltrar();
            });
        }
        selection.amjGetCompare().addListener((ListChangeListener<AMJLaptop>) c -> {
            tblLaptops.refresh();
            amjActualizarBarra();
        });
        // Vista de tarjetas por defecto, como una tienda; la tabla sigue a un clic. La elección se recuerda.
        javafx.scene.control.ToggleGroup vista = new javafx.scene.control.ToggleGroup();
        tglTabla.setToggleGroup(vista);
        tglTarjetas.setToggleGroup(vista);
        (VISTA_TABLA.equals(config.amjGet(VISTA, VISTA_TARJETAS)) ? tglTabla : tglTarjetas).setSelected(true);
        vista.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) {
                antes.setSelected(true);
                return;
            }
            config.amjSet(VISTA, ahora == tglTarjetas ? VISTA_TARJETAS : VISTA_TABLA);
            amjAplicarVista();
        });
        ordenados.addListener((ListChangeListener<AMJLaptop>) c -> amjPintarTarjetas());
        amjCargar(null);
        amjMostrar(null);
        amjAplicarVista();
        tblLaptops.getSelectionModel().selectFirst();
    }

    private static final String VISTA = "catalogo_vista", VISTA_TABLA = "tabla", VISTA_TARJETAS = "tarjetas";

    private void amjAplicarVista() {
        boolean tarjetas = tglTarjetas.isSelected();
        tblLaptops.setVisible(!tarjetas);
        tblLaptops.setManaged(!tarjetas);
        scrTarjetas.setVisible(tarjetas);
        scrTarjetas.setManaged(tarjetas);
        amjPintarTarjetas();
    }

    /**
     * Vista de tarjetas: mismos laptops, filtros y orden que la tabla. Solo se construye mientras se ve. Un clic
     * en una tarjeta la elige, como elegir su fila: la ficha de la derecha pasa a ser la suya.
     */
    private void amjPintarTarjetas() {
        if (!scrTarjetas.isVisible()) {
            pnlRejilla.getChildren().clear();
            return;
        }
        com.laptopexplorer.ui.AMJLaptopCard.AMJActions acciones = new com.laptopexplorer.ui.AMJLaptopCard.AMJActions(l -> {
            selection.amjSetA(l);
            navigation.amjGo(AMJScreen.VISOR);
        }, null, l -> tblLaptops.getSelectionModel().select(l));
        List<javafx.scene.Node> tarjetas = new java.util.ArrayList<>();
        for (AMJLaptop l : tblLaptops.getItems()) {
            javafx.scene.layout.VBox card = com.laptopexplorer.ui.AMJLaptopCard.amjBuild(l, null, true, selection, acciones, thumbs);
            card.getProperties().put(TARJETA, l);
            tarjetas.add(card);
        }
        pnlRejilla.getChildren().setAll(tarjetas);
        amjMarcarTarjeta();
    }

    private static final String TARJETA = "catalogo.laptop";

    /** Resalta la tarjeta del laptop cuya ficha se esta viendo. */
    private void amjMarcarTarjeta() {
        for (javafx.scene.Node n : pnlRejilla.getChildren()) {
            n.getStyleClass().remove("selected");
            if (actual != null && actual.equals(n.getProperties().get(TARJETA))) {
                n.getStyleClass().add("selected");
            }
        }
    }

    /* ------------------------------------------------------------------ modelo 3D externo */

    @FXML
    private void amjOnModelo3d() {
        if (actual == null) {
            return;
        }
        AMJLaptop l = actual;
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem archivo = new javafx.scene.control.MenuItem("Importar archivo .obj…");
        archivo.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Modelo 3D de " + l.amjGetDisplayName());
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Wavefront OBJ", "*.obj"));
            java.io.File f = fc.showOpenDialog(tblLaptops.getScene().getWindow());
            if (f != null) {
                amjImportarModelo(l, () -> modelStore.amjImportFile(l, f.toPath()));
            }
        });
        javafx.scene.control.MenuItem enlace = new javafx.scene.control.MenuItem("Descargar desde un enlace .obj…");
        enlace.setOnAction(e -> {
            javafx.scene.control.TextInputDialog d = new javafx.scene.control.TextInputDialog();
            d.setTitle("Descargar modelo 3D");
            d.setHeaderText("Enlace directo (https) a un archivo .obj de " + l.amjGetDisplayName());
            d.initOwner(tblLaptops.getScene().getWindow());
            d.getDialogPane().getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            d.getDialogPane().setMinWidth(520);
            d.showAndWait().filter(s -> !s.isBlank()).ifPresent(url -> amjImportarModelo(l, () -> modelStore.amjImportUrl(l, url)));
        });
        javafx.scene.control.MenuItem quitar = new javafx.scene.control.MenuItem("Quitar modelo importado");
        quitar.setDisable(!modelStore.amjHas(l));
        quitar.setOnAction(e -> {
            modelStore.amjRemove(l);
            selection.amjSay("Modelo 3D importado quitado de " + l.amjGetDisplayName());
            amjRefrescarEstado3d(l);
        });
        menu.getItems().addAll(archivo, enlace, quitar);
        menu.show(btnModelo3d, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    /** Importa en segundo plano (lee disco o red) y avisa si las proporciones no cuadran con la ficha. */
    private void amjImportarModelo(AMJLaptop l, java.util.function.Supplier<com.laptopexplorer.service.AMJModelStore.AMJReport> trabajo) {
        btnModelo3d.setDisable(true);
        selection.amjSay("Importando modelo 3D de " + l.amjGetDisplayName() + "…");
        javafx.concurrent.Task<com.laptopexplorer.service.AMJModelStore.AMJReport> t = new javafx.concurrent.Task<>() {
            @Override
            protected com.laptopexplorer.service.AMJModelStore.AMJReport call() {
                return trabajo.get();
            }
        };
        t.setOnSucceeded(e -> {
            btnModelo3d.setDisable(false);
            com.laptopexplorer.service.AMJModelStore.AMJReport r = t.getValue();
            selection.amjSay("Modelo 3D importado: " + String.format(Locale.US, "%,d", r.info().triangles()) + " triángulos");
            amjRefrescarEstado3d(l);
            if (r.warning() != null) {
                AMJDialogs.amjWarn(tblLaptops, "Revisa el modelo 3D", r.warning());
            }
        });
        t.setOnFailed(e -> {
            btnModelo3d.setDisable(false);
            selection.amjSay("No se importó el modelo 3D");
            AMJDialogs.amjWarn(tblLaptops, "No se pudo importar el modelo 3D", String.valueOf(t.getException().getMessage()));
        });
        Thread th = new Thread(t, "importar-modelo-3d");
        th.setDaemon(true);
        th.start();
    }

    /** Etiquetas de estado 3D de la ficha: interior reconstruido o aproximado y, si lo hay, modelo importado. */
    private void amjRefrescarEstado3d(AMJLaptop l) {
        if (actual != null && actual.equals(l)) {
            // de donde sale el exterior del 3D (fotos propias, de la familia o rasgos de la serie) y el interior
            AMJLaptopModel.AMJExteriorInfo origen = AMJLaptopModel.amjExteriorInfo(l);
            Label exterior = new Label("Exterior: " + origen.amjLabel().toLowerCase(Locale.ROOT));
            exterior.getStyleClass().addAll("tag", origen.kind() == AMJLaptopModel.AMJExterior.PHOTOS || origen.kind() == AMJLaptopModel.AMJExterior.BLENDER ? "ok"
                    : origen.kind() == AMJLaptopModel.AMJExterior.GENERIC ? "off" : "warn");
            exterior.setTooltip(new javafx.scene.control.Tooltip(origen.amjDetail()));
            pnlEstado3d.getChildren().setAll(exterior, amjEtiqueta3d(l));
            if (modelStore.amjHas(l)) {
                Label ext = new Label("Modelo 3D importado");
                ext.getStyleClass().addAll("tag", "ok");
                pnlEstado3d.getChildren().add(ext);
            }
        }
    }

    @Override
    public void amjOnShow() {
        if (sucio) {
            tblLaptops.refresh();
            return;
        }
        amjCargar(actual == null ? null : actual.getId());
    }

    /* ------------------------------------------------------------------ tabla */

    private void amjColumnaMarca() {
        TableColumn<AMJLaptop, AMJLaptop> c = new TableColumn<>("");
        c.setPrefWidth(38);
        c.setMinWidth(38);
        c.setMaxWidth(38);
        c.setSortable(false);
        c.setResizable(false);
        c.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(x.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            private final CheckBox box = new CheckBox();

            {
                setAlignment(Pos.CENTER);
                box.setOnAction(e -> {
                    AMJLaptop l = getItem();
                    if (l == null) {
                        return;
                    }
                    if (!selection.amjSetMarked(l, box.isSelected())) {
                        box.setSelected(false);
                        selection.amjSay("La comparación admite como máximo " + AMJSelectionState.MAX_COMPARE + " laptops.");
                    }
                });
            }

            @Override
            protected void updateItem(AMJLaptop l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) {
                    setGraphic(null);
                } else {
                    box.setSelected(selection.amjIsMarked(l));
                    box.getStyleClass().add("compare-box");
                    box.setAccessibleText("Marcar " + l.amjGetDisplayName() + " para comparar");
                    setGraphic(box);
                }
            }
        });
        tblLaptops.getColumns().add(c);
    }

    private void amjColumna(String titulo, double ancho, boolean numerica, Function<AMJLaptop, String> texto,
                         Comparator<AMJLaptop> orden) {
        TableColumn<AMJLaptop, AMJLaptop> c = new TableColumn<>(titulo);
        c.setPrefWidth(ancho);
        c.setMinWidth(Math.max(56, ancho * 0.55));
        c.setComparator(orden);
        c.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(x.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AMJLaptop l, boolean empty) {
                super.updateItem(l, empty);
                getStyleClass().removeAll("num", "nd");
                if (empty || l == null) {
                    setText(null);
                    return;
                }
                String t = texto.apply(l);
                setText(t);
                if (numerica) {
                    getStyleClass().add("num");
                }
                if (AMJFmt.ND.equals(t)) {
                    getStyleClass().add("nd");
                }
            }
        });
        tblLaptops.getColumns().add(c);
    }

    private void amjColumnaInterior() {
        TableColumn<AMJLaptop, AMJLaptop> c = new TableColumn<>("Interior 3D");
        c.setPrefWidth(118);
        c.setComparator(Comparator.comparing((AMJLaptop l) -> !amjEsReconstruido(l)));
        c.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(x.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AMJLaptop l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(amjEtiqueta3d(l));
            }
        });
        tblLaptops.getColumns().add(c);
    }

    private static Comparator<AMJLaptop> amjNumerico(Function<AMJLaptop, ? extends Number> f) {
        return Comparator.comparingDouble(l -> {
            Number n = f.apply(l);
            return n == null || n.doubleValue() == 0 ? Double.MAX_VALUE : n.doubleValue();
        });
    }

    private boolean amjEsReconstruido(AMJLaptop l) {
        return reconstruido.computeIfAbsent(l.getId(), id -> AMJLaptopModel.amjInteriorInfo(l).reconstructed());
    }

    private Label amjEtiqueta3d(AMJLaptop l) {
        boolean ok = amjEsReconstruido(l);
        Label t = new Label(ok ? "Reconstruido" : "Aproximado");
        t.getStyleClass().addAll("tag", ok ? "ok" : "off");
        return t;
    }

    private void amjCargar(Long reseleccionar) {
        reconstruido.clear();
        revirtiendo = true;
        todos.setAll(service.amjTodos());
        String marca = cmbMarca.getValue();
        String categoria = cmbCategoria.getValue();
        cmbMarca.getItems().setAll(amjPrepend(TODAS_MARCAS, todos.stream().map(AMJLaptop::getBrand)));
        cmbCategoria.getItems().setAll(amjPrepend(TODAS_CATEGORIAS, todos.stream().map(AMJLaptop::getCategory)));
        cmbMarca.setValue(marca != null && cmbMarca.getItems().contains(marca) ? marca : TODAS_MARCAS);
        cmbCategoria.setValue(categoria != null && cmbCategoria.getItems().contains(categoria) ? categoria : TODAS_CATEGORIAS);
        amjFiltrar();
        revirtiendo = false;
        AMJLaptop fresco = reseleccionar == null ? null
                : todos.stream().filter(l -> reseleccionar.equals(l.getId())).findFirst().orElse(null);
        if (fresco != null && filtrados.contains(fresco)) {
            tblLaptops.getSelectionModel().select(fresco);
            amjMostrar(fresco);
        } else if (actual != null) {
            amjMostrar(null);
        }
    }

    private static List<String> amjPrepend(String first, java.util.stream.Stream<String> rest) {
        List<String> out = new java.util.ArrayList<>();
        out.add(first);
        out.addAll(new TreeSet<>(rest.collect(Collectors.toList())));
        return out;
    }

    private void amjFiltrar() {
        if (filtrados == null) {
            return;
        }
        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        String marca = cmbMarca.getValue();
        String categoria = cmbCategoria.getValue();
        filtrados.setPredicate(l -> {
            String texto = (l.getBrand() + " " + l.getModel() + " " + l.getCpu() + " " + l.getGpu() + " "
                    + (l.getReleaseYear() == null ? "" : l.getReleaseYear()) + " "
                    + (l.getAvailability() == null ? "" : l.getAvailability())).toLowerCase(Locale.ROOT);
            return (q.isEmpty() || texto.contains(q))
                    && (marca == null || TODAS_MARCAS.equals(marca) || marca.equals(l.getBrand()))
                    && (categoria == null || TODAS_CATEGORIAS.equals(categoria) || categoria.equals(l.getCategory()))
                    && (!tglRtx50.isSelected() || RTX50.matcher(l.getGpu()).find())
                    && (!tglRtx40.isSelected() || RTX40.matcher(l.getGpu()).find())
                    && (!tgl2026.isSelected() || Integer.valueOf(2026).equals(l.getReleaseYear()))
                    && (!tglRecon.isSelected() || amjEsReconstruido(l));
        });
        int n = filtrados.size();
        lblCount.setText(n == todos.size() ? n + " laptops" : n + " de " + todos.size() + " laptops");
        amjActualizarBarra();
    }

    @FXML
    private void amjOnQuitarFiltros() {
        txtBuscar.clear();
        cmbMarca.setValue(TODAS_MARCAS);
        cmbCategoria.setValue(TODAS_CATEGORIAS);
        tglRtx50.setSelected(false);
        tglRtx40.setSelected(false);
        tglRecon.setSelected(false);
        tgl2026.setSelected(false);
    }

    /* ------------------------------------------------------------------ ficha */

    private void amjConstruirFicha() {
        String[][][] secciones = {
                {{"Identificación"}, {"Marca *", "brand"}, {"Modelo *", "model"}, {"Categoría *", "category"}, {"Precio (USD)", "priceUsd"}, {"Año", "releaseYear"}, {"Disponibilidad", "availability"}},
                {{"Procesador"}, {"Modelo *", "cpu"}, {"Núcleos", "cpuCores"}, {"Potencia base (W)", "cpuBaseWatts"}, {"Potencia máxima (W)", "cpuBoostWatts"}},
                {{"Gráficos"}, {"Modelo *", "gpu"}, {"VRAM (GB)", "gpuVramGb"}, {"TGP (W)", "gpuTgpWatts"}},
                {{"Memoria y almacenamiento"}, {"RAM (GB)", "ramGb"}, {"Tipo de RAM", "ramType"}, {"@ram"}, {"Almacenamiento (GB)", "storageGb"}, {"Ranuras M.2", "storageSlots"}},
                {{"Pantalla"}, {"Tamaño (pulgadas)", "screenInches"}, {"Resolución", "resolution"}, {"Frecuencia (Hz)", "refreshRateHz"}, {"Panel", "panelType"}},
                {{"Chasis y batería"}, {"Ancho (mm)", "widthMm"}, {"Fondo (mm)", "depthMm"}, {"Grosor (mm)", "thicknessMm"}, {"Peso (kg)", "weightKg"}, {"Batería (Wh)", "batteryWh"}, {"Material", "chassisMaterial"}, {"Color (#rrggbb)", "colorHex"}},
                {{"Origen de los datos"}, {"URL de la fuente", "sourceUrl"}, {"URL de imagen", "imageUrl"}, {"Notas", "notes"}}};
        int n = 0;
        for (String[][] s : secciones) {
            GridPane g = new GridPane();
            g.setHgap(8);
            g.setVgap(6);
            g.setPadding(new Insets(6, 14, 10, 14));
            ColumnConstraints c0 = new ColumnConstraints(112);
            ColumnConstraints c1 = new ColumnConstraints();
            c1.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().addAll(c0, c1);
            int row = 0;
            for (int i = 1; i < s.length; i++) {
                if ("@ram".equals(s[i][0])) {
                    g.add(chkRam, 1, row++);
                    // tres estados: sí, no y sin dato (un dato desconocido no debe guardarse como "no")
                    chkRam.setAllowIndeterminate(true);
                    chkRam.selectedProperty().addListener((o, a, b) -> amjAlCambiarRam());
                    chkRam.indeterminateProperty().addListener((o, a, b) -> amjAlCambiarRam());
                    amjAlCambiarRam();
                    continue;
                }
                Label l = new Label(s[i][0]);
                l.getStyleClass().add("field-label");
                TextField t = new TextField();
                t.textProperty().addListener((o, a, b) -> {
                    t.getStyleClass().remove("invalid");
                    amjMarcarSucio();
                });
                campos.put(s[i][1], t);
                g.add(l, 0, row);
                g.add(t, 1, row++);
            }
            TitledPane tp = new TitledPane(s[0][0].toUpperCase(Locale.ROOT), g);
            tp.setExpanded(n < 3);
            tp.setAnimated(false);
            pnlFicha.getChildren().add(tp);
            n++;
        }
    }

    /** El estado de la casilla se escribe con palabras: el guion del estado intermedio no se entiende solo. */
    private void amjAlCambiarRam() {
        chkRam.setText(RAM_TEXTO + (chkRam.isIndeterminate() ? ": sin dato" : chkRam.isSelected() ? ": sí" : ": no"));
        amjMarcarSucio();
    }

    private void amjSetRam(Boolean ampliable) {
        chkRam.setIndeterminate(ampliable == null);
        chkRam.setSelected(Boolean.TRUE.equals(ampliable));
    }

    private Boolean amjRam() {
        return chkRam.isIndeterminate() ? null : chkRam.isSelected();
    }

    private void amjMarcarSucio() {
        if (!cargando && actual != null) {
            sucio = true;
            amjActualizarBotones();
        }
    }

    private void amjActualizarBotones() {
        btnGuardar.setDisable(actual == null || !sucio);
        btnDescartar.setDisable(actual == null || !sucio);
        btnEliminar.setDisable(actual == null);
        btnModelo3d.setDisable(actual == null);
    }

    private void amjAlCambiarFila(AMJLaptop antes, AMJLaptop ahora) {
        if (revirtiendo) {
            return;
        }
        if (sucio && actual != null && (ahora == null || !ahora.equals(actual))) {
            boolean descartar = AMJDialogs.amjConfirm(tblLaptops, "Cambios sin guardar",
                    "Hay cambios sin guardar en " + actual.amjGetDisplayName() + ". ¿Descartarlos?", "Descartar", "Seguir editando");
            if (!descartar) {
                revirtiendo = true;
                if (actual != null && filtrados.contains(actual)) {
                    tblLaptops.getSelectionModel().select(actual);
                } else {
                    tblLaptops.getSelectionModel().clearSelection();
                }
                revirtiendo = false;
                return;
            }
        }
        amjMostrar(ahora);
    }

    private void amjMostrar(AMJLaptop l) {
        cargando = true;
        actual = l;
        sucio = false;
        pnlEstado3d.getChildren().clear();
        if (l == null) {
            lblMarca.setText("Sin selección");
            lblModelo.setText("Elige un laptop de la tabla para ver su ficha.");
            campos.values().forEach(t -> {
                t.clear();
                t.setDisable(true);
                t.getStyleClass().remove("invalid");
            });
            amjSetRam(null);
            chkRam.setDisable(true);
        } else {
            lblMarca.setText(l.getBrand());
            lblModelo.setText(l.getModel());
            amjRefrescarEstado3d(l);
            campos.values().forEach(t -> {
                t.setDisable(false);
                t.getStyleClass().remove("invalid");
            });
            chkRam.setDisable(false);
            amjSet("brand", l.getBrand());
            amjSet("model", l.getModel());
            amjSet("category", l.getCategory());
            amjSet("cpu", l.getCpu());
            amjSet("cpuCores", l.getCpuCores());
            amjSet("cpuBaseWatts", l.getCpuBaseWatts());
            amjSet("cpuBoostWatts", l.getCpuBoostWatts());
            amjSet("priceUsd", l.getPriceUsd());
            amjSet("releaseYear", l.getReleaseYear());
            amjSet("availability", l.getAvailability());
            amjSet("gpu", l.getGpu());
            amjSet("gpuVramGb", l.getGpuVramGb());
            amjSet("gpuTgpWatts", l.getGpuTgpWatts());
            amjSet("ramGb", l.getRamGb());
            amjSet("ramType", l.getRamType());
            amjSetRam(l.getRamUpgradable());
            amjSet("storageGb", l.getStorageGb());
            amjSet("storageSlots", l.getStorageSlots());
            amjSet("screenInches", l.getScreenInches());
            amjSet("resolution", l.getResolution());
            amjSet("refreshRateHz", l.getRefreshRateHz());
            amjSet("panelType", l.getPanelType());
            amjSet("weightKg", l.getWeightKg());
            amjSet("thicknessMm", l.getThicknessMm());
            amjSet("widthMm", l.getWidthMm());
            amjSet("depthMm", l.getDepthMm());
            amjSet("batteryWh", l.getBatteryWh());
            amjSet("colorHex", l.getColorHex());
            amjSet("chassisMaterial", l.getChassisMaterial());
            amjSet("sourceUrl", l.getSourceUrl());
            amjSet("imageUrl", l.getImageUrl());
            amjSet("notes", l.getNotes());
        }
        cargando = false;
        amjActualizarBotones();
        amjActualizarBarra();
        amjMarcarTarjeta();
    }

    private void amjSet(String key, Object v) {
        campos.get(key).setText(v == null ? "" : String.valueOf(v));
    }

    /** Un campo numérico con un valor que no se puede leer. */
    private static final class AMJCampoInvalido extends RuntimeException {
        final String clave;
        final String etiqueta;

        AMJCampoInvalido(String clave, String etiqueta) {
            super(etiqueta);
            this.clave = clave;
            this.etiqueta = etiqueta;
        }
    }

    private String amjT(String key) {
        String s = campos.get(key).getText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    private Integer amjI(String key, String etiqueta) {
        String s = amjT(key);
        try {
            return s == null ? null : Integer.valueOf(s);
        } catch (NumberFormatException e) {
            throw new AMJCampoInvalido(key, etiqueta);
        }
    }

    private Double amjD(String key, String etiqueta) {
        String s = amjT(key);
        try {
            return s == null ? null : Double.valueOf(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new AMJCampoInvalido(key, etiqueta);
        }
    }

    private AMJLaptop amjLeerFormulario() {
        return AMJLaptop.builder()
                .id(actual.getId())
                .brand(amjT("brand")).model(amjT("model")).category(amjT("category"))
                .priceUsd(amjI("priceUsd", "Precio"))
                .releaseYear(amjI("releaseYear", "Año")).availability(amjT("availability"))
                // un precio editado a mano deja de ser una estimación; si no cambia, conserva su marca
                .priceEstimated(Objects.equals(amjI("priceUsd", "Precio"), actual.getPriceUsd()) ? actual.getPriceEstimated() : Boolean.FALSE)
                .cpu(amjT("cpu")).cpuCores(amjI("cpuCores", "Núcleos")).cpuBaseWatts(amjI("cpuBaseWatts", "Potencia base"))
                .cpuBoostWatts(amjI("cpuBoostWatts", "Potencia máxima"))
                .gpu(amjT("gpu")).gpuVramGb(amjI("gpuVramGb", "VRAM")).gpuTgpWatts(amjI("gpuTgpWatts", "TGP"))
                .ramGb(amjI("ramGb", "RAM")).ramType(amjT("ramType")).ramUpgradable(amjRam())
                .storageGb(amjI("storageGb", "Almacenamiento")).storageSlots(amjI("storageSlots", "Ranuras M.2"))
                .screenInches(amjD("screenInches", "Tamaño de pantalla")).resolution(amjT("resolution"))
                .refreshRateHz(amjI("refreshRateHz", "Frecuencia")).panelType(amjT("panelType"))
                .weightKg(amjD("weightKg", "Peso")).thicknessMm(amjD("thicknessMm", "Grosor")).widthMm(amjD("widthMm", "Ancho"))
                .depthMm(amjD("depthMm", "Fondo")).batteryWh(amjD("batteryWh", "Batería"))
                .colorHex(amjT("colorHex")).chassisMaterial(amjT("chassisMaterial"))
                .sourceUrl(amjT("sourceUrl")).imageUrl(amjT("imageUrl")).notes(amjT("notes"))
                .build();
    }

    /* ------------------------------------------------------------------ acciones */

    @FXML
    private void amjOnGuardar() {
        if (actual == null) {
            return;
        }
        try {
            AMJLaptop guardado = service.amjGuardar(amjLeerFormulario());
            sucio = false;
            selection.amjRefresh(guardado);
            selection.amjSay("Cambios guardados: " + guardado.amjGetDisplayName());
            amjCargar(guardado.getId());
        } catch (AMJCampoInvalido e) {
            campos.get(e.clave).getStyleClass().add("invalid");
            campos.get(e.clave).requestFocus();
            selection.amjSay("Error al guardar los cambios");
            AMJDialogs.amjWarn(tblLaptops, "Error al guardar los cambios", "El campo «" + e.etiqueta + "» debe ser un número.");
        } catch (ConstraintViolationException e) {
            String detalle = e.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage).collect(Collectors.joining("\n"));
            selection.amjSay("Error al guardar los cambios");
            AMJDialogs.amjWarn(tblLaptops, "Error al guardar los cambios", detalle);
        } catch (AMJNegocioException e) {
            selection.amjSay("Error al guardar los cambios");
            AMJDialogs.amjWarn(tblLaptops, "Error al guardar los cambios", e.getMessage());
        } catch (Exception e) {
            selection.amjSay("Error al guardar los cambios");
            AMJDialogs.amjError(tblLaptops, "Error al guardar los cambios", String.valueOf(e.getMessage()));
        }
    }

    @FXML
    private void amjOnDescartar() {
        amjMostrar(actual == null ? null : service.amjBuscarPorId(actual.getId()).orElse(actual));
        selection.amjSay("Cambios descartados");
    }

    @FXML
    private void amjOnEliminar() {
        if (actual == null) {
            return;
        }
        AMJLaptop objetivo = actual;
        if (!AMJDialogs.amjConfirm(tblLaptops, "Eliminar laptop",
                "Se eliminará «" + objetivo.amjGetDisplayName() + "» del catálogo. Esta acción no se puede deshacer.",
                "Eliminar", "Cancelar")) {
            return;
        }
        try {
            service.amjEliminar(objetivo.getId());
            modelStore.amjRemove(objetivo);
            sucio = false;
            selection.amjForget(objetivo);
            tblLaptops.getSelectionModel().clearSelection();
            amjCargar(null);
            amjMostrar(null);
            selection.amjSay("Eliminado: " + objetivo.amjGetDisplayName());
        } catch (AMJNegocioException e) {
            AMJDialogs.amjWarn(tblLaptops, "No se pudo eliminar", e.getMessage());
        }
    }

    @FXML
    private void amjOnComparar() {
        navigation.amjGo(AMJScreen.COMPARAR);
    }

    @FXML
    private void amjOnVisor() {
        List<AMJLaptop> m = selection.amjGetCompare();
        selection.amjSetA(m.get(0));
        selection.amjSetB(m.size() > 1 ? m.get(1) : null);
        navigation.amjGo(AMJScreen.VISOR);
    }

    private void amjActualizarBarra() {
        if (lblSeleccion == null) {
            return;
        }
        int n = selection.amjGetCompare().size();
        lblSeleccion.setText(n == 0 ? "Marca de 2 a " + AMJSelectionState.MAX_COMPARE + " laptops para compararlos."
                : n + (n == 1 ? " laptop marcado. Marca al menos uno más para comparar." : " laptops marcados para comparar."));
        btnComparar.setDisable(n < 2);
        btnVisor.setDisable(n < 1 || n > 2);
    }
}
