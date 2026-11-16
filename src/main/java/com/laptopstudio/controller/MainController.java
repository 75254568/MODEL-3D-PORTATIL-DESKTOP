package com.laptopstudio.controller;

import com.laptopstudio.service.LaptopService;
import com.laptopstudio.ui.Icons;
import com.laptopstudio.ui.Money;
import com.laptopstudio.ui.NavigationService;
import com.laptopstudio.ui.Refreshable;
import com.laptopstudio.ui.Screen;
import com.laptopstudio.ui.SelectionState;
import com.laptopstudio.ui.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/** Contenedor principal: riel de navegacion, pantalla activa y barra de estado. */
@Component
@RequiredArgsConstructor
public class MainController {

    private final ViewLoader loader;
    private final NavigationService navigation;
    private final LaptopService service;
    private final SelectionState selection;
    private final com.laptopstudio.service.AppConfig config;

    @FXML private StackPane content;
    @FXML private javafx.scene.layout.VBox rail;
    @FXML private javafx.scene.layout.HBox topbar;
    @FXML private ToggleButton tbUsd;
    @FXML private ToggleButton tbPen;
    @FXML private ToggleButton btnBuscar;
    @FXML private ToggleButton btnCatalogo;
    @FXML private ToggleButton btnComparar;
    @FXML private ToggleButton btnVisor;
    @FXML private ToggleButton btnComponentes;
    @FXML private ToggleButton btnAjustes;
    @FXML private Label lblDb;
    @FXML private Label lblSel;
    @FXML private Label lblMsg;
    @FXML private javafx.scene.image.ImageView logo;
    @FXML private javafx.scene.layout.HBox topSearch;
    @FXML private javafx.scene.control.TextField txtGlobal;

    private final Map<Screen, Node> views = new EnumMap<>(Screen.class);
    private final Map<Screen, Object> controllers = new EnumMap<>(Screen.class);
    private final Map<Screen, ToggleButton> buttons = new EnumMap<>(Screen.class);
    private Screen current;

    @FXML
    public void initialize() {
        buttons.put(Screen.BUSCAR, btnBuscar);
        buttons.put(Screen.CATALOGO, btnCatalogo);
        buttons.put(Screen.COMPARAR, btnComparar);
        buttons.put(Screen.VISOR, btnVisor);
        buttons.put(Screen.COMPONENTES, btnComponentes);
        buttons.put(Screen.AJUSTES, btnAjustes);
        btnBuscar.setGraphic(Icons.of(Icons.SEARCH, 18));
        btnCatalogo.setGraphic(Icons.of(Icons.LIST, 18));
        btnComparar.setGraphic(Icons.of(Icons.COMPARE, 18));
        btnVisor.setGraphic(Icons.of(Icons.CUBE, 18));
        btnComponentes.setGraphic(Icons.of(Icons.CHIP, 18));
        btnAjustes.setGraphic(Icons.of(Icons.SETTINGS, 18));

        // solo la laptop del logo, recortada con fondo transparente (ver /img)
        logo.setImage(new javafx.scene.image.Image(getClass().getResource("/img/laptop.png").toExternalForm()));
        topSearch.getChildren().add(0, Icons.of(Icons.SEARCH, 16));

        ToggleGroup group = new ToggleGroup();
        buttons.values().forEach(b -> b.setToggleGroup(group));
        group.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });

        // El visor parte con dos modelos reconstruidos para que no se abra vacio
        if (selection.getA() == null) {
            service.buscarPorModelo("LOQ 15AHP10").ifPresent(selection::setA);
        }
        if (selection.getB() == null) {
            service.buscarPorModelo("TUF Gaming A18").ifPresent(selection::setB);
        }

        iniciarMoneda();
        navigation.immersiveProperty().addListener((o, antes, on) -> inmersivo(on));
        navigation.register(this::show);
        lblMsg.textProperty().bind(selection.messageProperty());
        selection.getCompare().addListener((javafx.collections.ListChangeListener<Object>) c -> updateStatus());
        updateStatus();
        show(Screen.BUSCAR);
    }

    /** Carga la moneda y el tipo de cambio guardados y conecta el selector USD/PEN de la barra superior. */
    private void iniciarMoneda() {
        try {
            Money.rateProperty().set(Double.parseDouble(config.get(
                    com.laptopstudio.service.AppConfig.PEN_RATE, String.valueOf(Money.DEFAULT_PEN_RATE))));
        } catch (NumberFormatException e) {
            Money.rateProperty().set(Money.DEFAULT_PEN_RATE);
        }
        boolean pen = "PEN".equals(config.get(com.laptopstudio.service.AppConfig.CURRENCY, "USD"));
        Money.currencyProperty().set(pen ? Money.Currency.PEN : Money.Currency.USD);
        ToggleGroup g = new ToggleGroup();
        tbUsd.setToggleGroup(g);
        tbPen.setToggleGroup(g);
        (pen ? tbPen : tbUsd).setSelected(true);
        g.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null) {
                old.setSelected(true);                   // siempre hay una moneda elegida
                return;
            }
            Money.Currency c = now == tbPen ? Money.Currency.PEN : Money.Currency.USD;
            Money.currencyProperty().set(c);
            config.set(com.laptopstudio.service.AppConfig.CURRENCY, c.name());
        });
        Runnable alCambiar = () -> {
            javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(String.format(java.util.Locale.US,
                    "1 USD = %.3f PEN (%s). Se cambia en Ajustes.", Money.rateProperty().get(),
                    config.get(com.laptopstudio.service.AppConfig.PEN_RATE_NOTE, Money.DEFAULT_PEN_NOTE)));
            tbUsd.setTooltip(tip);
            tbPen.setTooltip(tip);
            // la pantalla visible vuelve a pintar sus precios; las demas lo hacen al mostrarse
            if (current != null && current != Screen.AJUSTES && controllers.get(current) instanceof Refreshable r) {
                r.onShow();
            }
        };
        Money.currencyProperty().addListener((o, a, b) -> alCambiar.run());
        Money.rateProperty().addListener((o, a, b) -> alCambiar.run());
        alCambiar.run();
    }

    /** Entra o sale del modo inmersivo: oculta barra lateral y superior y pone la ventana a pantalla completa. */
    private void inmersivo(boolean on) {
        rail.setVisible(!on);
        rail.setManaged(!on);
        topbar.setVisible(!on);
        topbar.setManaged(!on);
        javafx.scene.Parent root = content.getScene().getRoot();
        root.getStyleClass().remove("immersive");
        if (on) {
            root.getStyleClass().add("immersive");
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) content.getScene().getWindow();
        if (stage.getProperties().putIfAbsent("immersive.listener", Boolean.TRUE) == null) {
            stage.setFullScreenExitHint("Pulsa Esc para salir de la pantalla completa");
            // Esc lo gestiona JavaFX: al salir de pantalla completa se restaura la interfaz
            stage.fullScreenProperty().addListener((o, a, full) -> {
                if (!full) {
                    navigation.immersiveProperty().set(false);
                }
            });
        }
        stage.setFullScreen(on);
    }

    private void updateStatus() {
        try {
            lblDb.setText(service.contar() + " laptops en la base de datos");
        } catch (RuntimeException e) {
            lblDb.setText("Base de datos no disponible");
        }
        int n = selection.getCompare().size();
        lblSel.setText(n == 0 ? "" : n + (n == 1 ? " laptop marcado" : " laptops marcados") + " para comparar");
        lblSel.setVisible(n > 0);
        lblSel.setManaged(n > 0);
    }

    @FXML
    private void onBuscar() {
        show(Screen.BUSCAR);
    }

    @FXML
    private void onCatalogo() {
        show(Screen.CATALOGO);
    }

    @FXML
    private void onComparar() {
        show(Screen.COMPARAR);
    }

    @FXML
    private void onVisor() {
        show(Screen.VISOR);
    }

    @FXML
    private void onComponentes() {
        show(Screen.COMPONENTES);
    }

    @FXML
    private void onAjustes() {
        show(Screen.AJUSTES);
    }

    private void show(Screen screen) {
        if (current != null && controllers.get(current) instanceof Refreshable r) {
            r.onHide();
        }
        if (!views.containsKey(screen)) {
            String fxml = switch (screen) {
                case BUSCAR -> "/view/search-view.fxml";
                case CATALOGO -> "/view/catalog-view.fxml";
                case COMPARAR -> "/view/compare-view.fxml";
                case VISOR -> "/view/viewer-view.fxml";
                case COMPONENTES -> "/view/components-view.fxml";
                case AJUSTES -> "/view/settings-view.fxml";
            };
            ViewLoader.Loaded loaded = loader.load(fxml);
            views.put(screen, loaded.root());
            controllers.put(screen, loaded.controller());
            content.getChildren().add(loaded.root());
        }
        views.forEach((s, node) -> {
            boolean on = s == screen;
            node.setVisible(on);
            node.setManaged(on);
        });
        boolean cambio = current != screen;
        current = screen;
        buttons.forEach((s, b) -> b.setSelected(s == screen));
        updateStatus();
        if (controllers.get(screen) instanceof Refreshable r) {
            r.onShow();
        }
        if (cambio) {
            // transicion de pantalla: la nueva aparece con fundido y sube unos pixeles
            com.laptopstudio.ui.Motion.enter(views.get(screen));
        }
    }

    /** Busqueda de la barra superior: abre la pantalla Buscar con el texto escrito. */
    @FXML
    private void onBuscarGlobal() {
        show(Screen.BUSCAR);
        if (controllers.get(Screen.BUSCAR) instanceof SearchController sc) {
            sc.buscarTexto(txtGlobal.getText());
        }
    }
}
