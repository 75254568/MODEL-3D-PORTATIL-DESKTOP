package com.laptopstudio.controller;

import com.laptopstudio.model.Laptop;
import com.laptopstudio.ui.CompareMatrix;
import com.laptopstudio.ui.Fmt;
import com.laptopstudio.ui.Icons;
import com.laptopstudio.ui.NavigationService;
import com.laptopstudio.ui.Refreshable;
import com.laptopstudio.ui.Screen;
import com.laptopstudio.ui.SelectionState;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Comparacion tecnica de 2 a 4 laptops marcados en el catalogo. */
@Component
@RequiredArgsConstructor
public class CompareController implements Refreshable {

    private final SelectionState selection;
    private final NavigationService navigation;

    @FXML private Label lblCount;
    @FXML private FlowPane chips;
    @FXML private CheckBox chkDiff;
    @FXML private TableView<CompareMatrix.Row> tabla;
    @FXML private javafx.scene.control.ScrollPane scrollColumnas;
    @FXML private HBox columnas;
    @FXML private CheckBox chkTabla;
    @FXML private Label lblVacio;
    @FXML private Button btnVisor;

    @FXML
    public void initialize() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label(""));
        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(CompareMatrix.Row r, boolean empty) {
                super.updateItem(r, empty);
                getStyleClass().remove("group-row");
                if (!empty && r != null && r.group()) {
                    getStyleClass().add("group-row");
                }
            }
        });
        chkDiff.selectedProperty().addListener((o, a, b) -> rebuild());
        chkTabla.selectedProperty().addListener((o, a, b) -> rebuild());
        selection.getCompare().addListener((ListChangeListener<Laptop>) c -> rebuild());
        rebuild();
    }

    @Override
    public void onShow() {
        rebuild();
    }

    private void rebuild() {
        List<Laptop> sel = new ArrayList<>(selection.getCompare());
        lblCount.setText(sel.isEmpty() ? "" : sel.size() + (sel.size() == 1 ? " laptop" : " laptops"));
        chips.getChildren().clear();
        // en pantalla dividida cada columna tiene su propio boton de quitar; las etiquetas solo acompanan a la tabla
        chips.setVisible(!sel.isEmpty() && chkTabla.isSelected());
        chips.setManaged(chips.isVisible());
        for (Laptop l : sel) {
            Label name = new Label(l.getDisplayName());
            Button x = new Button();
            x.setGraphic(Icons.of(Icons.CLOSE, 12));
            x.getStyleClass().addAll("icon-only");
            x.setAccessibleText("Quitar " + l.getDisplayName());
            x.setOnAction(e -> selection.setMarked(l, false));
            HBox chip = new HBox(6, name, x);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("tag");
            chip.setStyle("-fx-padding: 3 4 3 9; -fx-text-fill: -color-fg-default;");
            name.setStyle("-fx-text-fill: -color-fg-default;");
            chips.getChildren().add(chip);
        }
        boolean ready = sel.size() >= 2;
        btnVisor.setDisable(sel.isEmpty() || sel.size() > 2);
        lblVacio.setVisible(!ready);
        lblVacio.setManaged(!ready);
        lblVacio.setText(sel.isEmpty()
                ? "No hay laptops para comparar. Marca de 2 a " + SelectionState.MAX_COMPARE + " en el catálogo."
                : "Marca al menos un laptop más en el catálogo para compararlos.");
        boolean comoTabla = chkTabla.isSelected();
        tabla.setVisible(ready && comoTabla);
        tabla.setManaged(ready && comoTabla);
        scrollColumnas.setVisible(ready && !comoTabla);
        scrollColumnas.setManaged(ready && !comoTabla);
        construirColumnas(ready ? sel : List.of());
        tabla.getColumns().clear();
        if (!ready) {
            tabla.setItems(FXCollections.emptyObservableList());
            return;
        }
        TableColumn<CompareMatrix.Row, CompareMatrix.Row> k = new TableColumn<>("");
        k.setPrefWidth(210);
        k.setSortable(false);
        k.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(x.getValue()));
        k.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(CompareMatrix.Row r, boolean empty) {
                super.updateItem(r, empty);
                getStyleClass().remove("key");
                setText(empty || r == null ? null : r.group() ? r.label().toUpperCase() : r.label());
                if (!empty && r != null && !r.group()) {
                    getStyleClass().add("key");
                }
            }
        });
        tabla.getColumns().add(k);
        for (int i = 0; i < sel.size(); i++) {
            final int idx = i;
            TableColumn<CompareMatrix.Row, CompareMatrix.Row> c = new TableColumn<>(sel.get(i).getDisplayName());
            c.setPrefWidth(Math.max(200, 760.0 / sel.size()));
            c.setSortable(false);
            c.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(x.getValue()));
            c.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(CompareMatrix.Row r, boolean empty) {
                    super.updateItem(r, empty);
                    getStyleClass().removeAll("best", "nd");
                    if (empty || r == null || r.group()) {
                        setText(null);
                        return;
                    }
                    String v = r.values().get(idx);
                    setText((r.best().contains(idx) ? "●  " : "") + v);
                    if (Fmt.ND.equals(v)) {
                        getStyleClass().add("nd");
                    }
                    if (r.best().contains(idx)) {
                        getStyleClass().add("best");
                    }
                }
            });
            tabla.getColumns().add(c);
        }
        tabla.setItems(FXCollections.observableArrayList(CompareMatrix.build(sel, chkDiff.isSelected())));
    }

    /**
     * Pantalla dividida: una columna por laptop, con las mismas filas a la misma altura para leer en horizontal.
     * Las filas tienen alto fijo; un valor largo se corta y se ve completo al pasar el raton.
     */
    private void construirColumnas(List<Laptop> sel) {
        columnas.getChildren().clear();
        if (sel.isEmpty()) {
            return;
        }
        List<CompareMatrix.Row> rows = CompareMatrix.build(sel, chkDiff.isSelected());
        List<javafx.scene.Node> animar = new ArrayList<>();
        for (int i = 0; i < sel.size(); i++) {
            final int idx = i;
            Laptop l = sel.get(i);
            long gana = rows.stream().filter(r -> !r.group() && r.best().contains(idx)).count();

            Label marca = new Label(l.getBrand().toUpperCase() + "  ·  " + l.getCategory());
            marca.getStyleClass().add("section-label");
            Label modelo = new Label(l.getModel());
            modelo.getStyleClass().add("model-name");
            modelo.setMinHeight(44);
            modelo.setMaxHeight(44);
            modelo.setWrapText(true);
            Label precio = new Label(Fmt.price(l.getPriceUsd(), l.getPriceEstimated()));
            precio.getStyleClass().add("price");
            Label wins = new Label(gana == 0 ? "Sin ventajas medibles" : "Destaca en " + gana + (gana == 1 ? " apartado" : " apartados"));
            wins.getStyleClass().add(gana == 0 ? "muted" : "compare-wins");
            Button ver = new Button("Ver en 3D");
            ver.getStyleClass().addAll("primary", "compact");
            ver.setOnAction(e -> {
                selection.setA(l);
                navigation.go(Screen.VISOR);
            });
            Button ficha = new Button("Ficha");
            ficha.getStyleClass().add("compact");
            ficha.setOnAction(e -> com.laptopstudio.ui.SpecSheet.show(columnas, l));
            Button quitar = new Button("Quitar");
            quitar.getStyleClass().add("compact");
            quitar.setOnAction(e -> selection.setMarked(l, false));
            javafx.scene.layout.VBox head = new javafx.scene.layout.VBox(4, marca, modelo, precio, wins, new HBox(6, ver, ficha, quitar));
            head.getStyleClass().add("compare-head");

            javafx.scene.layout.VBox col = new javafx.scene.layout.VBox(head);
            col.getStyleClass().addAll("glass-card", "compare-col");
            for (CompareMatrix.Row r : rows) {
                if (r.group()) {
                    Label g = new Label(r.label().toUpperCase());
                    g.getStyleClass().add("compare-group");
                    g.setMinHeight(30);
                    g.setMaxHeight(30);
                    g.setMaxWidth(Double.MAX_VALUE);
                    col.getChildren().add(g);
                    continue;
                }
                String v = r.values().get(idx);
                boolean best = r.best().contains(idx);
                Label k = new Label(r.label());
                k.getStyleClass().addAll("muted", "small");
                Label val = new Label(v);
                val.getStyleClass().add("value");
                if (Fmt.ND.equals(v)) {
                    val.getStyleClass().add("nd");
                }
                val.setTooltip(new javafx.scene.control.Tooltip(v));
                javafx.scene.layout.VBox cell = new javafx.scene.layout.VBox(0, k, val);
                cell.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(cell, javafx.scene.layout.Priority.ALWAYS);
                HBox row = new HBox(8, cell);
                row.setAlignment(Pos.CENTER_LEFT);
                if (best) {
                    Label tag = new Label("Mejor");
                    tag.getStyleClass().addAll("tag", "ok");
                    tag.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    row.getChildren().add(tag);
                    row.getStyleClass().add("best");
                }
                row.getStyleClass().add("compare-row");
                row.setMinHeight(46);
                row.setMaxHeight(46);
                col.getChildren().add(row);
            }
            col.setMinWidth(0);
            col.setPrefWidth(10);
            HBox.setHgrow(col, javafx.scene.layout.Priority.ALWAYS);
            columnas.getChildren().add(col);
            animar.add(col);
        }
        com.laptopstudio.ui.Motion.stagger(animar);
    }

    @FXML
    private void onCatalogo() {
        navigation.go(Screen.CATALOGO);
    }

    @FXML
    private void onVisor() {
        List<Laptop> m = selection.getCompare();
        selection.setA(m.get(0));
        selection.setB(m.size() > 1 ? m.get(1) : null);
        navigation.go(Screen.VISOR);
    }
}
