package com.laptopstudio.controller;

import com.laptopstudio.exception.NegocioException;
import com.laptopstudio.model.Laptop;
import com.laptopstudio.service.LaptopService;
import com.laptopstudio.ui.Dialogs;
import com.laptopstudio.ui.Fmt;
import com.laptopstudio.ui.Icons;
import com.laptopstudio.ui.NavigationService;
import com.laptopstudio.ui.Refreshable;
import com.laptopstudio.ui.Screen;
import com.laptopstudio.ui.SelectionState;
import com.laptopstudio.viewer.LaptopModel;
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
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Catalogo de laptops: tabla con busqueda y filtros, ficha agrupada por secciones con edicion validada,
 * marcado de 2 a 4 laptops para comparar y acceso al visor 3D.
 */
@Component
@RequiredArgsConstructor
public class CatalogController implements Refreshable {

    private static final String TODAS_MARCAS = "Todas las marcas";
    private static final String TODAS_CATEGORIAS = "Todas las categorías";
    private static final Pattern RTX50 = Pattern.compile("RTX (serie )?50", Pattern.CASE_INSENSITIVE);
    private static final Pattern RTX40 = Pattern.compile("RTX (serie )?40", Pattern.CASE_INSENSITIVE);

    private final LaptopService service;
    private final SelectionState selection;
    private final NavigationService navigation;

    @FXML private TextField txtBuscar;
    @FXML private HBox searchBox;
    @FXML private ComboBox<String> cmbMarca;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ToggleButton tgRtx50;
    @FXML private ToggleButton tgRtx40;
    @FXML private ToggleButton tgRecon;
    @FXML private Label lblCount;
    @FXML private TableView<Laptop> tabla;
    @FXML private Label lblMarca;
    @FXML private Label lblModelo;
    @FXML private HBox boxEstado3d;
    @FXML private VBox ficha;
    @FXML private Button btnGuardar;
    @FXML private Button btnDescartar;
    @FXML private Button btnEliminar;
    @FXML private Label lblSeleccion;
    @FXML private Button btnVisor;
    @FXML private Button btnComparar;

    private final ObservableList<Laptop> todos = FXCollections.observableArrayList();
    private FilteredList<Laptop> filtrados;
    private final Map<Long, Boolean> reconstruido = new HashMap<>();
    private final Map<String, TextField> campos = new LinkedHashMap<>();
    private final CheckBox chkRam = new CheckBox("RAM ampliable (módulos SO-DIMM)");
    private Laptop actual;
    private boolean cargando;
    private boolean sucio;
    private boolean revirtiendo;

    /* ------------------------------------------------------------------ inicio */

    @FXML
    public void initialize() {
        searchBox.getChildren().add(0, Icons.of(Icons.SEARCH, 16));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Ningún laptop coincide con los filtros."));

        columnaMarca();
        columna("Marca", 100, false, Laptop::getBrand, Comparator.comparing(l -> l.getBrand().toLowerCase(Locale.ROOT)));
        columna("Modelo", 200, false, Laptop::getModel, Comparator.comparing(l -> l.getModel().toLowerCase(Locale.ROOT)));
        columna("Procesador", 170, false, Laptop::getCpu, Comparator.comparing(l -> l.getCpu().toLowerCase(Locale.ROOT)));
        columna("Gráficos", 190, false, Laptop::getGpu, Comparator.comparing(l -> l.getGpu().toLowerCase(Locale.ROOT)));
        columna("RAM", 72, true, l -> Fmt.num(l.getRamGb(), "GB"), numerico(Laptop::getRamGb));
        columna("Pantalla", 86, true, l -> Fmt.num(l.getScreenInches(), "in"), numerico(Laptop::getScreenInches));
        columna("Peso", 84, true, l -> Fmt.num(l.getWeightKg(), "kg"), numerico(Laptop::getWeightKg));
        columna("Batería", 84, true, l -> Fmt.num(l.getBatteryWh(), "Wh"), numerico(Laptop::getBatteryWh));
        columnaInterior();

        filtrados = new FilteredList<>(todos, l -> true);
        SortedList<Laptop> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);
        tabla.getSelectionModel().selectedItemProperty().addListener((o, antes, ahora) -> alCambiarFila(antes, ahora));

        construirFicha();
        cmbMarca.valueProperty().addListener((o, a, b) -> filtrar());
        cmbCategoria.valueProperty().addListener((o, a, b) -> filtrar());
        txtBuscar.textProperty().addListener((o, a, b) -> filtrar());
        for (ToggleButton t : List.of(tgRtx50, tgRtx40, tgRecon)) {
            t.selectedProperty().addListener((o, a, b) -> {
                if (b && t == tgRtx50) {
                    tgRtx40.setSelected(false);
                } else if (b && t == tgRtx40) {
                    tgRtx50.setSelected(false);
                }
                filtrar();
            });
        }
        selection.getCompare().addListener((ListChangeListener<Laptop>) c -> {
            tabla.refresh();
            actualizarBarra();
        });
        cargar(null);
        mostrar(null);
        tabla.getSelectionModel().selectFirst();
    }

    @Override
    public void onShow() {
        if (sucio) {
            tabla.refresh();
            return;
        }
        cargar(actual == null ? null : actual.getId());
    }

    /* ------------------------------------------------------------------ tabla */

    private void columnaMarca() {
        TableColumn<Laptop, Laptop> c = new TableColumn<>("");
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
                    Laptop l = getItem();
                    if (l == null) {
                        return;
                    }
                    if (!selection.setMarked(l, box.isSelected())) {
                        box.setSelected(false);
                        selection.say("La comparación admite como máximo " + SelectionState.MAX_COMPARE + " laptops.");
                    }
                });
            }

            @Override
            protected void updateItem(Laptop l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) {
                    setGraphic(null);
                } else {
                    box.setSelected(selection.isMarked(l));
                    box.getStyleClass().add("compare-box");
                    box.setAccessibleText("Marcar " + l.getDisplayName() + " para comparar");
                    setGraphic(box);
                }
            }
        });
        tabla.getColumns().add(c);
    }

    private void columna(String titulo, double ancho, boolean numerica, Function<Laptop, String> texto,
                         Comparator<Laptop> orden) {
        TableColumn<Laptop, Laptop> c = new TableColumn<>(titulo);
        c.setPrefWidth(ancho);
        c.setMinWidth(Math.max(56, ancho * 0.55));
        c.setComparator(orden);
        c.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(x.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Laptop l, boolean empty) {
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
                if (Fmt.ND.equals(t)) {
                    getStyleClass().add("nd");
                }
            }
        });
        tabla.getColumns().add(c);
    }

    private void columnaInterior() {
        TableColumn<Laptop, Laptop> c = new TableColumn<>("Interior 3D");
        c.setPrefWidth(118);
        c.setMinWidth(104);
        c.setComparator(Comparator.comparing((Laptop l) -> !esReconstruido(l)));
        c.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(x.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Laptop l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(etiqueta3d(l));
            }
        });
        tabla.getColumns().add(c);
    }

    private static Comparator<Laptop> numerico(Function<Laptop, ? extends Number> f) {
        return Comparator.comparingDouble(l -> {
            Number n = f.apply(l);
            return n == null || n.doubleValue() == 0 ? Double.MAX_VALUE : n.doubleValue();
        });
    }

    private boolean esReconstruido(Laptop l) {
        return reconstruido.computeIfAbsent(l.getId(), id -> LaptopModel.interiorInfo(l).reconstructed());
    }

    private Label etiqueta3d(Laptop l) {
        boolean ok = esReconstruido(l);
        Label t = new Label(ok ? "Reconstruido" : "Aproximado");
        t.getStyleClass().addAll("tag", ok ? "ok" : "off");
        return t;
    }

    private void cargar(Long reseleccionar) {
        reconstruido.clear();
        revirtiendo = true;
        todos.setAll(service.todos());
        String marca = cmbMarca.getValue();
        String categoria = cmbCategoria.getValue();
        cmbMarca.getItems().setAll(prepend(TODAS_MARCAS, todos.stream().map(Laptop::getBrand)));
        cmbCategoria.getItems().setAll(prepend(TODAS_CATEGORIAS, todos.stream().map(Laptop::getCategory)));
        cmbMarca.setValue(marca != null && cmbMarca.getItems().contains(marca) ? marca : TODAS_MARCAS);
        cmbCategoria.setValue(categoria != null && cmbCategoria.getItems().contains(categoria) ? categoria : TODAS_CATEGORIAS);
        filtrar();
        revirtiendo = false;
        Laptop fresco = reseleccionar == null ? null
                : todos.stream().filter(l -> reseleccionar.equals(l.getId())).findFirst().orElse(null);
        if (fresco != null && filtrados.contains(fresco)) {
            tabla.getSelectionModel().select(fresco);
            mostrar(fresco);
        } else if (actual != null) {
            mostrar(null);
        }
    }

    private static List<String> prepend(String first, java.util.stream.Stream<String> rest) {
        List<String> out = new java.util.ArrayList<>();
        out.add(first);
        out.addAll(new TreeSet<>(rest.collect(Collectors.toList())));
        return out;
    }

    private void filtrar() {
        if (filtrados == null) {
            return;
        }
        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        String marca = cmbMarca.getValue();
        String categoria = cmbCategoria.getValue();
        filtrados.setPredicate(l -> {
            String texto = (l.getBrand() + " " + l.getModel() + " " + l.getCpu() + " " + l.getGpu()).toLowerCase(Locale.ROOT);
            return (q.isEmpty() || texto.contains(q))
                    && (marca == null || TODAS_MARCAS.equals(marca) || marca.equals(l.getBrand()))
                    && (categoria == null || TODAS_CATEGORIAS.equals(categoria) || categoria.equals(l.getCategory()))
                    && (!tgRtx50.isSelected() || RTX50.matcher(l.getGpu()).find())
                    && (!tgRtx40.isSelected() || RTX40.matcher(l.getGpu()).find())
                    && (!tgRecon.isSelected() || esReconstruido(l));
        });
        int n = filtrados.size();
        lblCount.setText(n == todos.size() ? n + " laptops" : n + " de " + todos.size() + " laptops");
        actualizarBarra();
    }

    @FXML
    private void onQuitarFiltros() {
        txtBuscar.clear();
        cmbMarca.setValue(TODAS_MARCAS);
        cmbCategoria.setValue(TODAS_CATEGORIAS);
        tgRtx50.setSelected(false);
        tgRtx40.setSelected(false);
        tgRecon.setSelected(false);
    }

    /* ------------------------------------------------------------------ ficha */

    private void construirFicha() {
        String[][][] secciones = {
                {{"Identificación"}, {"Marca *", "brand"}, {"Modelo *", "model"}, {"Categoría *", "category"}},
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
                    chkRam.selectedProperty().addListener((o, a, b) -> marcarSucio());
                    continue;
                }
                Label l = new Label(s[i][0]);
                l.getStyleClass().add("field-label");
                TextField t = new TextField();
                t.textProperty().addListener((o, a, b) -> {
                    t.getStyleClass().remove("invalid");
                    marcarSucio();
                });
                campos.put(s[i][1], t);
                g.add(l, 0, row);
                g.add(t, 1, row++);
            }
            TitledPane tp = new TitledPane(s[0][0].toUpperCase(Locale.ROOT), g);
            tp.setExpanded(n < 3);
            tp.setAnimated(false);
            ficha.getChildren().add(tp);
            n++;
        }
    }

    private void marcarSucio() {
        if (!cargando && actual != null) {
            sucio = true;
            actualizarBotones();
        }
    }

    private void actualizarBotones() {
        btnGuardar.setDisable(actual == null || !sucio);
        btnDescartar.setDisable(actual == null || !sucio);
        btnEliminar.setDisable(actual == null);
    }

    private void alCambiarFila(Laptop antes, Laptop ahora) {
        if (revirtiendo) {
            return;
        }
        if (sucio && actual != null && (ahora == null || !ahora.equals(actual))) {
            boolean descartar = Dialogs.confirm(tabla, "Cambios sin guardar",
                    "Hay cambios sin guardar en " + actual.getDisplayName() + ". ¿Descartarlos?", "Descartar", "Seguir editando");
            if (!descartar) {
                revirtiendo = true;
                if (actual != null && filtrados.contains(actual)) {
                    tabla.getSelectionModel().select(actual);
                } else {
                    tabla.getSelectionModel().clearSelection();
                }
                revirtiendo = false;
                return;
            }
        }
        mostrar(ahora);
    }

    private void mostrar(Laptop l) {
        cargando = true;
        actual = l;
        sucio = false;
        boxEstado3d.getChildren().clear();
        if (l == null) {
            lblMarca.setText("Sin selección");
            lblModelo.setText("Elige un laptop de la tabla para ver su ficha.");
            campos.values().forEach(t -> {
                t.clear();
                t.setDisable(true);
                t.getStyleClass().remove("invalid");
            });
            chkRam.setSelected(false);
            chkRam.setDisable(true);
        } else {
            lblMarca.setText(l.getBrand());
            lblModelo.setText(l.getModel());
            boxEstado3d.getChildren().add(etiqueta3d(l));
            campos.values().forEach(t -> {
                t.setDisable(false);
                t.getStyleClass().remove("invalid");
            });
            chkRam.setDisable(false);
            set("brand", l.getBrand());
            set("model", l.getModel());
            set("category", l.getCategory());
            set("cpu", l.getCpu());
            set("cpuCores", l.getCpuCores());
            set("cpuBaseWatts", l.getCpuBaseWatts());
            set("cpuBoostWatts", l.getCpuBoostWatts());
            set("gpu", l.getGpu());
            set("gpuVramGb", l.getGpuVramGb());
            set("gpuTgpWatts", l.getGpuTgpWatts());
            set("ramGb", l.getRamGb());
            set("ramType", l.getRamType());
            chkRam.setSelected(Boolean.TRUE.equals(l.getRamUpgradable()));
            set("storageGb", l.getStorageGb());
            set("storageSlots", l.getStorageSlots());
            set("screenInches", l.getScreenInches());
            set("resolution", l.getResolution());
            set("refreshRateHz", l.getRefreshRateHz());
            set("panelType", l.getPanelType());
            set("weightKg", l.getWeightKg());
            set("thicknessMm", l.getThicknessMm());
            set("widthMm", l.getWidthMm());
            set("depthMm", l.getDepthMm());
            set("batteryWh", l.getBatteryWh());
            set("colorHex", l.getColorHex());
            set("chassisMaterial", l.getChassisMaterial());
            set("sourceUrl", l.getSourceUrl());
            set("imageUrl", l.getImageUrl());
            set("notes", l.getNotes());
        }
        cargando = false;
        actualizarBotones();
        actualizarBarra();
    }

    private void set(String key, Object v) {
        campos.get(key).setText(v == null ? "" : String.valueOf(v));
    }

    /** Un campo numerico con un valor que no se puede leer. */
    private static final class CampoInvalido extends RuntimeException {
        final String clave;
        final String etiqueta;

        CampoInvalido(String clave, String etiqueta) {
            super(etiqueta);
            this.clave = clave;
            this.etiqueta = etiqueta;
        }
    }

    private String t(String key) {
        String s = campos.get(key).getText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    private Integer i(String key, String etiqueta) {
        String s = t(key);
        try {
            return s == null ? null : Integer.valueOf(s);
        } catch (NumberFormatException e) {
            throw new CampoInvalido(key, etiqueta);
        }
    }

    private Double d(String key, String etiqueta) {
        String s = t(key);
        try {
            return s == null ? null : Double.valueOf(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new CampoInvalido(key, etiqueta);
        }
    }

    private Laptop leerFormulario() {
        return Laptop.builder()
                .id(actual.getId())
                .brand(t("brand")).model(t("model")).category(t("category"))
                .cpu(t("cpu")).cpuCores(i("cpuCores", "Núcleos")).cpuBaseWatts(i("cpuBaseWatts", "Potencia base"))
                .cpuBoostWatts(i("cpuBoostWatts", "Potencia máxima"))
                .gpu(t("gpu")).gpuVramGb(i("gpuVramGb", "VRAM")).gpuTgpWatts(i("gpuTgpWatts", "TGP"))
                .ramGb(i("ramGb", "RAM")).ramType(t("ramType")).ramUpgradable(chkRam.isSelected())
                .storageGb(i("storageGb", "Almacenamiento")).storageSlots(i("storageSlots", "Ranuras M.2"))
                .screenInches(d("screenInches", "Tamaño de pantalla")).resolution(t("resolution"))
                .refreshRateHz(i("refreshRateHz", "Frecuencia")).panelType(t("panelType"))
                .weightKg(d("weightKg", "Peso")).thicknessMm(d("thicknessMm", "Grosor")).widthMm(d("widthMm", "Ancho"))
                .depthMm(d("depthMm", "Fondo")).batteryWh(d("batteryWh", "Batería"))
                .colorHex(t("colorHex")).chassisMaterial(t("chassisMaterial"))
                .sourceUrl(t("sourceUrl")).imageUrl(t("imageUrl")).notes(t("notes"))
                .build();
    }

    /* ------------------------------------------------------------------ acciones */

    @FXML
    private void onGuardar() {
        if (actual == null) {
            return;
        }
        try {
            Laptop guardado = service.guardar(leerFormulario());
            sucio = false;
            selection.refresh(guardado);
            selection.say("Cambios guardados: " + guardado.getDisplayName());
            cargar(guardado.getId());
        } catch (CampoInvalido e) {
            campos.get(e.clave).getStyleClass().add("invalid");
            campos.get(e.clave).requestFocus();
            selection.say("Error al guardar los cambios");
            Dialogs.warn(tabla, "Error al guardar los cambios", "El campo «" + e.etiqueta + "» debe ser un número.");
        } catch (ConstraintViolationException e) {
            String detalle = e.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage).collect(Collectors.joining("\n"));
            selection.say("Error al guardar los cambios");
            Dialogs.warn(tabla, "Error al guardar los cambios", detalle);
        } catch (NegocioException e) {
            selection.say("Error al guardar los cambios");
            Dialogs.warn(tabla, "Error al guardar los cambios", e.getMessage());
        } catch (Exception e) {
            selection.say("Error al guardar los cambios");
            Dialogs.error(tabla, "Error al guardar los cambios", String.valueOf(e.getMessage()));
        }
    }

    @FXML
    private void onDescartar() {
        mostrar(actual == null ? null : service.buscarPorId(actual.getId()).orElse(actual));
        selection.say("Cambios descartados");
    }

    @FXML
    private void onEliminar() {
        if (actual == null) {
            return;
        }
        Laptop objetivo = actual;
        if (!Dialogs.confirm(tabla, "Eliminar laptop",
                "Se eliminará «" + objetivo.getDisplayName() + "» del catálogo. Esta acción no se puede deshacer.",
                "Eliminar", "Cancelar")) {
            return;
        }
        try {
            service.eliminar(objetivo.getId());
            sucio = false;
            selection.forget(objetivo);
            tabla.getSelectionModel().clearSelection();
            cargar(null);
            mostrar(null);
            selection.say("Eliminado: " + objetivo.getDisplayName());
        } catch (NegocioException e) {
            Dialogs.warn(tabla, "No se pudo eliminar", e.getMessage());
        }
    }

    @FXML
    private void onComparar() {
        navigation.go(Screen.COMPARAR);
    }

    @FXML
    private void onVisor() {
        List<Laptop> m = selection.getCompare();
        selection.setA(m.get(0));
        selection.setB(m.size() > 1 ? m.get(1) : null);
        navigation.go(Screen.VISOR);
    }

    private void actualizarBarra() {
        if (lblSeleccion == null) {
            return;
        }
        int n = selection.getCompare().size();
        lblSeleccion.setText(n == 0 ? "Marca de 2 a " + SelectionState.MAX_COMPARE + " laptops para compararlos."
                : n + (n == 1 ? " laptop marcado. Marca al menos uno más para comparar." : " laptops marcados para comparar."));
        btnComparar.setDisable(n < 2);
        btnVisor.setDisable(n < 1 || n > 2);
    }
}
