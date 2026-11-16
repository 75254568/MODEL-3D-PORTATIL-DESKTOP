package com.laptopstudio.controller;

import com.laptopstudio.model.Laptop;
import com.laptopstudio.service.SearchService;
import com.laptopstudio.service.SearchService.Campo;
import com.laptopstudio.service.SearchService.Criteria;
import com.laptopstudio.service.SearchService.Hit;
import com.laptopstudio.ui.LaptopCard;
import com.laptopstudio.ui.Money;
import com.laptopstudio.ui.Motion;
import com.laptopstudio.ui.NavigationService;
import com.laptopstudio.ui.OnlineLookupDialog;
import com.laptopstudio.ui.Refreshable;
import com.laptopstudio.ui.Screen;
import com.laptopstudio.ui.SelectionState;
import com.laptopstudio.ui.Suggest;
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
 * Busqueda inteligente: asistente con sugerencias en cada campo, resultados que se actualizan mientras se
 * escribe, alternativas cercanas, laptops parecidos y acceso a la consulta en internet.
 */
@Component
@RequiredArgsConstructor
public class SearchController implements Refreshable {

    private static final String TODAS = "Cualquiera";

    private final SearchService search;
    private final SelectionState selection;
    private final NavigationService navigation;
    private final OnlineLookupDialog onlineLookup;

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
    @FXML private VBox boxResultados;

    private boolean listo;
    private boolean cargando;
    private Laptop similaresDe;
    /** Espera a que el usuario deje de escribir antes de relanzar la busqueda. */
    private final PauseTransition espera = new PauseTransition(Duration.millis(260));

    @FXML
    public void initialize() {
        cargarCombos();
        espera.setOnFinished(e -> onBuscar());

        Suggest.attach(txtTexto, () -> search.sugerencias(Campo.TEXTO, criteriosTolerantes()), this::onBuscar);
        Suggest.attach(txtCpu, () -> search.sugerencias(Campo.CPU, criteriosTolerantes()), this::onBuscar);
        Suggest.attach(txtGpu, () -> search.sugerencias(Campo.GPU, criteriosTolerantes()), this::onBuscar);
        Suggest.attach(txtRam, () -> search.sugerencias(Campo.RAM, criteriosTolerantes()), this::onBuscar);
        Suggest.attach(txtSsd, () -> search.sugerencias(Campo.ALMACENAMIENTO, criteriosTolerantes()), this::onBuscar);
        Suggest.attach(txtPantMin, () -> search.sugerencias(Campo.PANTALLA_MIN, criteriosTolerantes()), this::onBuscar);
        Suggest.attach(txtPantMax, () -> search.sugerencias(Campo.PANTALLA_MAX, criteriosTolerantes()), this::onBuscar);

        for (TextField t : campos()) {
            t.textProperty().addListener((o, a, b) -> programar());
        }
        cmbMarca.valueProperty().addListener((o, a, b) -> programar());
        cmbCategoria.valueProperty().addListener((o, a, b) -> programar());
        Money.currencyProperty().addListener((o, a, b) -> alCambiarMoneda());
        Money.rateProperty().addListener((o, a, b) -> alCambiarMoneda());
        alCambiarMoneda();

        listo = true;
        onBuscar();
    }

    @Override
    public void onShow() {
        if (listo) {
            cargarCombos();
            refrescar();
        }
    }

    private List<TextField> campos() {
        return List.of(txtTexto, txtMin, txtMax, txtCpu, txtGpu, txtRam, txtSsd, txtPantMin, txtPantMax, txtPeso);
    }

    private void programar() {
        if (!cargando && listo) {
            espera.playFromStart();
        }
    }

    private void alCambiarMoneda() {
        lblPresupuesto.setText("PRESUPUESTO (" + Money.code() + ")");
        if (listo) {
            refrescar();
        }
    }

    private void refrescar() {
        if (similaresDe != null) {
            mostrarSimilares(similaresDe);
        } else {
            onBuscar();
        }
    }

    private void cargarCombos() {
        cargando = true;
        recargar(cmbMarca, search.marcas());
        recargar(cmbCategoria, search.categorias());
        cargando = false;
    }

    private static void recargar(ComboBox<String> cmb, List<String> valores) {
        String actual = cmb.getValue();
        List<String> items = new ArrayList<>();
        items.add(TODAS);
        items.addAll(valores);
        cmb.setItems(FXCollections.observableArrayList(items));
        cmb.setValue(actual != null && items.contains(actual) ? actual : TODAS);
    }

    /* ------------------------------------------------------------------ criterios */

    /** Lee los campos. Lanza IllegalArgumentException con un mensaje para el usuario si un numero no es valido. */
    private Criteria criterios() {
        Integer min = monto(txtMin, "Precio mínimo");
        Integer max = monto(txtMax, "Precio máximo");
        return new Criteria(txt(txtTexto), min, max, combo(cmbMarca), txt(txtCpu), entero(txtRam, "RAM"),
                entero(txtSsd, "Almacenamiento"), txt(txtGpu), decimal(txtPantMin, "Pantalla mínima"),
                decimal(txtPantMax, "Pantalla máxima"), decimal(txtPeso, "Peso máximo"), combo(cmbCategoria));
    }

    /** Criterios para calcular sugerencias: si algun numero esta a medio escribir, se ignoran los numeros. */
    private Criteria criteriosTolerantes() {
        try {
            return criterios();
        } catch (IllegalArgumentException e) {
            return new Criteria(txt(txtTexto), null, null, combo(cmbMarca), txt(txtCpu), null, null, txt(txtGpu),
                    null, null, null, combo(cmbCategoria));
        }
    }

    /* ------------------------------------------------------------------ acciones */

    @FXML
    private void onBuscar() {
        espera.stop();
        similaresDe = null;
        lblError.setText("");
        Criteria c;
        try {
            c = criterios();
        } catch (IllegalArgumentException e) {
            lblError.setText(e.getMessage());
            return;
        }
        if (c.minPrice() != null && c.maxPrice() != null && c.minPrice() > c.maxPrice()) {
            lblError.setText("El precio mínimo es mayor que el máximo.");
            return;
        }
        mostrar(search.buscar(c));
    }

    @FXML
    private void onLimpiar() {
        cargando = true;
        campos().forEach(TextField::clear);
        cmbMarca.setValue(TODAS);
        cmbCategoria.setValue(TODAS);
        cargando = false;
        onBuscar();
    }

    /** Consulta en internet un laptop que no esta en el catalogo; al incorporarlo, se refresca la busqueda. */
    @FXML
    private void onInternet() {
        String pista = String.join(" ", List.of(combo(cmbMarca) == null ? "" : combo(cmbMarca),
                txt(txtTexto) == null ? "" : txt(txtTexto))).trim();
        onlineLookup.open(boxResultados, pista, guardado -> {
            cargarCombos();
            buscarTexto(guardado.getModel());
        });
    }

    /** Abre la busqueda con un texto libre (lo usa la barra superior). */
    public void buscarTexto(String texto) {
        cargando = true;
        txtTexto.setText(texto == null ? "" : texto.trim());
        cargando = false;
        onBuscar();
    }

    /* ------------------------------------------------------------------ resultados */

    private void mostrar(SearchService.Result r) {
        boxResultados.getChildren().clear();
        int n = r.matches().size();
        Motion.countUp(lblResumen, n, v -> v + (v == 1 ? " coincidencia" : " coincidencias"));
        List<Node> animar = new ArrayList<>();
        if (r.matches().isEmpty()) {
            boxResultados.getChildren().add(aviso(r.alternatives().isEmpty()
                    ? "Ningún laptop del catálogo cumple esos criterios. Amplía el presupuesto, quita algún filtro o búscalo en internet."
                    : "Ningún laptop cumple todos los criterios. Estas son las alternativas más cercanas:"));
            if (r.alternatives().isEmpty()) {
                Button web = new Button("Buscar en internet");
                web.getStyleClass().add("primary");
                web.setOnAction(e -> onInternet());
                boxResultados.getChildren().add(web);
            }
        } else {
            boxResultados.getChildren().add(rejilla(r.matches(), false, animar));
        }
        if (!r.alternatives().isEmpty()) {
            if (!r.matches().isEmpty()) {
                boxResultados.getChildren().add(titulo("Alternativas que casi cumplen"));
            }
            boxResultados.getChildren().add(rejilla(r.alternatives(), false, animar));
        }
        Motion.stagger(animar);
    }

    private void mostrarSimilares(Laptop ref) {
        similaresDe = ref;
        boxResultados.getChildren().clear();
        lblResumen.setText("Parecidos a " + ref.getDisplayName());
        Button volver = new Button("Volver a los resultados");
        volver.setOnAction(e -> onBuscar());
        List<Node> animar = new ArrayList<>();
        boxResultados.getChildren().addAll(volver, titulo("Laptops parecidos a " + ref.getDisplayName()),
                rejilla(search.similares(ref, 6), true, animar));
        Motion.stagger(animar);
    }

    /** Rejilla de tarjetas que se reparte en columnas segun el ancho disponible. */
    private FlowPane rejilla(List<Hit> hits, boolean similar, List<Node> animar) {
        FlowPane grid = new FlowPane(12, 12);
        LaptopCard.Actions acciones = new LaptopCard.Actions(this::verEn3d, this::mostrarSimilares, null);
        for (Hit h : hits) {
            VBox card = LaptopCard.build(h.laptop(), LaptopCard.note(similar ? "Se parece en: " : "No cumple: ", textos(h.notes())),
                    similar, selection, acciones);
            grid.getChildren().add(card);
            animar.add(card);
        }
        return grid;
    }

    /** Notas listas para mostrar: los importes llegan en USD y se escriben en la moneda elegida. */
    private static List<String> textos(List<SearchService.Note> notas) {
        return notas.stream().map(n -> n.usd() == null ? n.text() : Money.format(n.usd(), null) + " " + n.text()).toList();
    }

    private void verEn3d(Laptop l) {
        selection.setA(l);
        selection.say("Mostrando " + l.getDisplayName() + " en el visor 3D");
        navigation.go(Screen.VISOR);
    }

    private static Label titulo(String t) {
        Label l = new Label(t.toUpperCase());
        l.getStyleClass().add("section-label");
        l.setPadding(new Insets(8, 0, 0, 0));
        return l;
    }

    private static Label aviso(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }

    /* ------------------------------------------------------------------ lectura de campos */

    private static String txt(TextField f) {
        String s = f.getText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String combo(ComboBox<String> c) {
        return c.getValue() == null || TODAS.equals(c.getValue()) ? null : c.getValue();
    }

    private static Integer entero(TextField f, String etiqueta) {
        Double d = numero(f, etiqueta);
        return d == null ? null : (int) Math.round(d);
    }

    /** Importe escrito en la moneda elegida, devuelto en USD (la moneda en que se guardan los precios). */
    private static Integer monto(TextField f, String etiqueta) {
        Double d = numero(f, etiqueta);
        return d == null ? null : Money.toUsd(d);
    }

    private static Double decimal(TextField f, String etiqueta) {
        return numero(f, etiqueta);
    }

    private static Double numero(TextField f, String etiqueta) {
        String s = txt(f);
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
