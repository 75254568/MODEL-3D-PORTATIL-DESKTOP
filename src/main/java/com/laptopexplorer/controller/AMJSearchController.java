package com.laptopexplorer.controller;

import com.laptopexplorer.model.AMJLaptop;
import com.laptopexplorer.service.AMJSearchService;
import com.laptopexplorer.service.AMJSearchService.AMJCampo;
import com.laptopexplorer.service.AMJSearchService.AMJCriteria;
import com.laptopexplorer.service.AMJSearchService.AMJHit;
import com.laptopexplorer.ui.AMJLaptopCard;
import com.laptopexplorer.ui.AMJMoney;
import com.laptopexplorer.ui.AMJMotion;
import com.laptopexplorer.ui.AMJNavigationService;
import com.laptopexplorer.ui.AMJOnlineLookupDialog;
import com.laptopexplorer.ui.AMJRefreshable;
import com.laptopexplorer.ui.AMJScreen;
import com.laptopexplorer.ui.AMJSelectionState;
import com.laptopexplorer.ui.AMJSuggest;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Búsqueda inteligente: asistente con sugerencias en cada campo, resultados que se actualizan mientras se
 * escribe, alternativas cercanas, laptops parecidos y acceso a la consulta en internet.
 */
@Component
@RequiredArgsConstructor
public class AMJSearchController implements AMJRefreshable {

    private static final String TODAS = "Cualquiera";

    private final AMJSearchService search;
    private final AMJSelectionState selection;
    private final AMJNavigationService navigation;
    private final AMJOnlineLookupDialog onlineLookup;
    private final com.laptopexplorer.ui.AMJLaptopThumbs thumbs;

    @FXML private TextField txtTexto;
    @FXML private TextField txtMin;
    @FXML private TextField txtMax;
    @FXML private ComboBox<String> cmbMarca;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private TextField txtCpu;
    @FXML private TextField txtGpu;
    @FXML private TextField txtRam;
    @FXML private TextField txtSsd;
    @FXML private TextField txtPantMin;
    @FXML private TextField txtPantMax;
    @FXML private TextField txtPeso;
    @FXML private Label lblPresupuesto;
    @FXML private Label lblResumen;
    @FXML private Label lblError;
    @FXML private VBox pnlResultados;

    private boolean listo;
    private boolean cargando;
    private AMJLaptop similaresDe;
    /** Espera a que el usuario deje de escribir antes de relanzar la búsqueda. */
    private final PauseTransition espera = new PauseTransition(Duration.millis(260));

    @FXML
    public void initialize() {
        amjCargarCombos();
        espera.setOnFinished(e -> amjOnBuscar());

        AMJSuggest.amjAttach(txtTexto, () -> search.amjSugerencias(AMJCampo.TEXTO, amjCriteriosTolerantes()), this::amjOnBuscar);
        AMJSuggest.amjAttach(txtCpu, () -> search.amjSugerencias(AMJCampo.CPU, amjCriteriosTolerantes()), this::amjOnBuscar);
        AMJSuggest.amjAttach(txtGpu, () -> search.amjSugerencias(AMJCampo.GPU, amjCriteriosTolerantes()), this::amjOnBuscar);
        AMJSuggest.amjAttach(txtRam, () -> search.amjSugerencias(AMJCampo.RAM, amjCriteriosTolerantes()), this::amjOnBuscar);
        AMJSuggest.amjAttach(txtSsd, () -> search.amjSugerencias(AMJCampo.ALMACENAMIENTO, amjCriteriosTolerantes()), this::amjOnBuscar);
        AMJSuggest.amjAttach(txtPantMin, () -> search.amjSugerencias(AMJCampo.PANTALLA_MIN, amjCriteriosTolerantes()), this::amjOnBuscar);
        AMJSuggest.amjAttach(txtPantMax, () -> search.amjSugerencias(AMJCampo.PANTALLA_MAX, amjCriteriosTolerantes()), this::amjOnBuscar);

        for (TextField t : amjCampos()) {
            t.textProperty().addListener((o, a, b) -> amjProgramar());
        }
        cmbMarca.valueProperty().addListener((o, a, b) -> amjProgramar());
        cmbCategoria.valueProperty().addListener((o, a, b) -> amjProgramar());
        AMJMoney.amjCurrencyProperty().addListener((o, a, b) -> amjAlCambiarMoneda());
        AMJMoney.amjRateProperty().addListener((o, a, b) -> amjAlCambiarMoneda());
        amjAlCambiarMoneda();

        listo = true;
        amjOnBuscar();
    }

    @Override
    public void amjOnShow() {
        if (listo) {
            amjCargarCombos();
            amjRefrescar();
        }
    }

    private List<TextField> amjCampos() {
        return List.of(txtTexto, txtMin, txtMax, txtCpu, txtGpu, txtRam, txtSsd, txtPantMin, txtPantMax, txtPeso);
    }

    private void amjProgramar() {
        if (!cargando && listo) {
            espera.playFromStart();
        }
    }

    private void amjAlCambiarMoneda() {
        lblPresupuesto.setText("PRESUPUESTO (" + AMJMoney.amjCode() + ")");
        if (listo) {
            amjRefrescar();
        }
    }

    private void amjRefrescar() {
        if (similaresDe != null) {
            amjMostrarSimilares(similaresDe);
        } else {
            amjOnBuscar();
        }
    }

    private void amjCargarCombos() {
        cargando = true;
        amjRecargar(cmbMarca, search.amjMarcas());
        amjRecargar(cmbCategoria, search.amjCategorias());
        cargando = false;
    }

    private static void amjRecargar(ComboBox<String> cmb, List<String> valores) {
        String actual = cmb.getValue();
        List<String> items = new ArrayList<>();
        items.add(TODAS);
        items.addAll(valores);
        cmb.setItems(FXCollections.observableArrayList(items));
        cmb.setValue(actual != null && items.contains(actual) ? actual : TODAS);
    }

    /* ------------------------------------------------------------------ criterios */

    /** Lee los campos. Lanza IllegalArgumentException con un mensaje para el usuario si un número no es válido. */
    private AMJCriteria amjCriterios() {
        Integer min = amjMonto(txtMin, "Precio mínimo");
        Integer max = amjMonto(txtMax, "Precio máximo");
        return new AMJCriteria(amjTxt(txtTexto), min, max, amjCombo(cmbMarca), amjTxt(txtCpu), amjEntero(txtRam, "RAM"),
                amjEntero(txtSsd, "Almacenamiento"), amjTxt(txtGpu), amjDecimal(txtPantMin, "Pantalla mínima"),
                amjDecimal(txtPantMax, "Pantalla máxima"), amjDecimal(txtPeso, "Peso máximo"), amjCombo(cmbCategoria));
    }

    /** Criterios para calcular sugerencias: si algún número esta a medio escribir, se ignoran los números. */
    private AMJCriteria amjCriteriosTolerantes() {
        try {
            return amjCriterios();
        } catch (IllegalArgumentException e) {
            return new AMJCriteria(amjTxt(txtTexto), null, null, amjCombo(cmbMarca), amjTxt(txtCpu), null, null, amjTxt(txtGpu),
                    null, null, null, amjCombo(cmbCategoria));
        }
    }

    /* ------------------------------------------------------------------ acciones */

    @FXML
    private void amjOnBuscar() {
        espera.stop();
        similaresDe = null;
        lblError.setText("");
        AMJCriteria c;
        try {
            c = amjCriterios();
        } catch (IllegalArgumentException e) {
            lblError.setText(e.getMessage());
            return;
        }
        if (c.minPrice() != null && c.maxPrice() != null && c.minPrice() > c.maxPrice()) {
            lblError.setText("El precio mínimo es mayor que el máximo.");
            return;
        }
        amjMostrar(search.amjBuscar(c));
    }

    @FXML
    private void amjOnLimpiar() {
        cargando = true;
        amjCampos().forEach(TextField::clear);
        cmbMarca.setValue(TODAS);
        cmbCategoria.setValue(TODAS);
        cargando = false;
        amjOnBuscar();
    }

    /** Consulta en internet un laptop que no esta en el catálogo; al incorporarlo, se refresca la búsqueda. */
    @FXML
    private void amjOnInternet() {
        String pista = String.join(" ", List.of(amjCombo(cmbMarca) == null ? "" : amjCombo(cmbMarca),
                amjTxt(txtTexto) == null ? "" : amjTxt(txtTexto))).trim();
        onlineLookup.amjOpen(pnlResultados, pista, guardado -> {
            amjCargarCombos();
            amjBuscarTexto(guardado.getModel());
        });
    }

    /** Abre la búsqueda con un texto libre (lo usa la barra superior). */
    public void amjBuscarTexto(String texto) {
        cargando = true;
        txtTexto.setText(texto == null ? "" : texto.trim());
        cargando = false;
        amjOnBuscar();
    }

    /* ------------------------------------------------------------------ resultados */

    private void amjMostrar(AMJSearchService.AMJResult r) {
        pnlResultados.getChildren().clear();
        int n = r.matches().size();
        AMJMotion.amjCountUp(lblResumen, n, v -> v + (v == 1 ? " coincidencia" : " coincidencias"));
        List<Node> animar = new ArrayList<>();
        if (r.matches().isEmpty()) {
            pnlResultados.getChildren().add(amjAviso(r.alternatives().isEmpty()
                    ? "Ningún laptop del catálogo cumple esos criterios. Amplía el presupuesto, quita algún filtro o búscalo en internet."
                    : "Ningún laptop cumple todos los criterios. Estas son las alternativas más cercanas:"));
            if (r.alternatives().isEmpty()) {
                Button web = new Button("Buscar en internet");
                web.getStyleClass().add("primary");
                web.setOnAction(e -> amjOnInternet());
                pnlResultados.getChildren().add(web);
            }
        } else {
            pnlResultados.getChildren().add(amjRejilla(r.matches(), false, animar));
        }
        if (!r.alternatives().isEmpty()) {
            if (!r.matches().isEmpty()) {
                pnlResultados.getChildren().add(amjTitulo("Alternativas que casi cumplen"));
            }
            pnlResultados.getChildren().add(amjRejilla(r.alternatives(), false, animar));
        }
        AMJMotion.amjStagger(animar);
    }

    private void amjMostrarSimilares(AMJLaptop ref) {
        similaresDe = ref;
        pnlResultados.getChildren().clear();
        lblResumen.setText("Parecidos a " + ref.amjGetDisplayName());
        Button volver = new Button("Volver a los resultados");
        volver.setOnAction(e -> amjOnBuscar());
        List<Node> animar = new ArrayList<>();
        pnlResultados.getChildren().addAll(volver, amjTitulo("Laptops parecidos a " + ref.amjGetDisplayName()),
                amjRejilla(search.amjSimilares(ref, 6), true, animar));
        AMJMotion.amjStagger(animar);
    }

    /** Rejilla de tarjetas que se reparte en columnas según el ancho disponible. */
    private FlowPane amjRejilla(List<AMJHit> hits, boolean similar, List<Node> animar) {
        FlowPane grid = new FlowPane(12, 12);
        AMJLaptopCard.AMJActions acciones = new AMJLaptopCard.AMJActions(this::amjVerEn3d, this::amjMostrarSimilares, null);
        for (AMJHit h : hits) {
            VBox card = AMJLaptopCard.amjBuild(h.laptop(), AMJLaptopCard.amjNote(similar ? "Se parece en: " : "No cumple: ", amjTextos(h.notes())),
                    similar, selection, acciones, thumbs);
            grid.getChildren().add(card);
            animar.add(card);
        }
        return grid;
    }

    /** Notas listas para mostrar: los importes llegan en USD y se escriben en la moneda elegida. */
    private static List<String> amjTextos(List<AMJSearchService.AMJNote> notas) {
        return notas.stream().map(n -> n.usd() == null ? n.text() : AMJMoney.amjFormat(n.usd(), null) + " " + n.text()).toList();
    }

    private void amjVerEn3d(AMJLaptop l) {
        selection.amjSetA(l);
        selection.amjSay("Mostrando " + l.amjGetDisplayName() + " en el visor 3D");
        navigation.amjGo(AMJScreen.VISOR);
    }

    private static Label amjTitulo(String t) {
        Label l = new Label(t.toUpperCase());
        l.getStyleClass().add("section-label");
        l.setPadding(new Insets(8, 0, 0, 0));
        return l;
    }

    private static Label amjAviso(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }

    /* ------------------------------------------------------------------ lectura de campos */

    private static String amjTxt(TextField f) {
        String s = f.getText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String amjCombo(ComboBox<String> c) {
        return c.getValue() == null || TODAS.equals(c.getValue()) ? null : c.getValue();
    }

    private static Integer amjEntero(TextField f, String etiqueta) {
        Double d = amjNumero(f, etiqueta);
        return d == null ? null : (int) Math.round(d);
    }

    /** Importe escrito en la moneda elegida, devuelto en USD (la moneda en que se guardan los precios). */
    private static Integer amjMonto(TextField f, String etiqueta) {
        Double d = amjNumero(f, etiqueta);
        return d == null ? null : AMJMoney.amjToUsd(d);
    }

    private static Double amjDecimal(TextField f, String etiqueta) {
        return amjNumero(f, etiqueta);
    }

    private static Double amjNumero(TextField f, String etiqueta) {
        String s = amjTxt(f);
        if (s == null) {
            return null;
        }
        try {
            double v = Double.parseDouble(s.replace("$", "").replace("S/", "").replace(" ", "").replace(',', '.'));
            if (v < 0) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(etiqueta + ": escribe un número.");
        }
    }
}
