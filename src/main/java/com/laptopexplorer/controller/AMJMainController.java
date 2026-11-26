package com.laptopexplorer.controller;

import com.laptopexplorer.service.AMJLaptopService;
import com.laptopexplorer.ui.AMJIcons;
import com.laptopexplorer.ui.AMJMoney;
import com.laptopexplorer.ui.AMJNavigationService;
import com.laptopexplorer.ui.AMJRefreshable;
import com.laptopexplorer.ui.AMJScreen;
import com.laptopexplorer.ui.AMJSelectionState;
import com.laptopexplorer.ui.AMJViewLoader;
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

/** Contenedor principal: riel de navegación, pantalla activa y barra de estado. */
@Component
@RequiredArgsConstructor
public class AMJMainController {

    private final AMJViewLoader loader;
    private final AMJNavigationService navigation;
    private final AMJLaptopService service;
    private final AMJSelectionState selection;
    private final com.laptopexplorer.service.AMJAppConfig config;

    @FXML private StackPane pnlContenido;
    @FXML private javafx.scene.layout.VBox pnlRiel;
    @FXML private javafx.scene.layout.HBox pnlBarraSuperior;
    @FXML private ToggleButton tglUsd;
    @FXML private ToggleButton tglPen;
    @FXML private ToggleButton tglBuscar;
    @FXML private ToggleButton tglCatalogo;
    @FXML private ToggleButton tglComparar;
    @FXML private ToggleButton tglVisor;
    @FXML private ToggleButton tglComponentes;
    @FXML private ToggleButton tglAjustes;
    @FXML private Label lblDb;
    @FXML private Label lblSel;
    @FXML private Label lblMsg;
    @FXML private javafx.scene.image.ImageView imgLogo;
    @FXML private javafx.scene.layout.HBox pnlBusquedaGlobal;
    @FXML private javafx.scene.control.TextField txtGlobal;

    private final Map<AMJScreen, Node> views = new EnumMap<>(AMJScreen.class);
    private final Map<AMJScreen, Object> controllers = new EnumMap<>(AMJScreen.class);
    private final Map<AMJScreen, ToggleButton> buttons = new EnumMap<>(AMJScreen.class);
    private AMJScreen current;

    @FXML
    public void initialize() {
        buttons.put(AMJScreen.BUSCAR, tglBuscar);
        buttons.put(AMJScreen.CATALOGO, tglCatalogo);
        buttons.put(AMJScreen.COMPARAR, tglComparar);
        buttons.put(AMJScreen.VISOR, tglVisor);
        buttons.put(AMJScreen.COMPONENTES, tglComponentes);
        buttons.put(AMJScreen.AJUSTES, tglAjustes);
        tglBuscar.setGraphic(AMJIcons.amjOf(AMJIcons.SEARCH, 18));
        tglCatalogo.setGraphic(AMJIcons.amjOf(AMJIcons.LIST, 18));
        tglComparar.setGraphic(AMJIcons.amjOf(AMJIcons.COMPARE, 18));
        tglVisor.setGraphic(AMJIcons.amjOf(AMJIcons.CUBE, 18));
        tglComponentes.setGraphic(AMJIcons.amjOf(AMJIcons.CHIP, 18));
        tglAjustes.setGraphic(AMJIcons.amjOf(AMJIcons.SETTINGS, 18));

        // solo la laptop del logo, recortada con fondo transparente (ver /img)
        imgLogo.setImage(new javafx.scene.image.Image(getClass().getResource("/img/laptop.png").toExternalForm()));
        pnlBusquedaGlobal.getChildren().add(0, AMJIcons.amjOf(AMJIcons.SEARCH, 16));

        ToggleGroup group = new ToggleGroup();
        buttons.values().forEach(b -> b.setToggleGroup(group));
        group.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });

        // El visor parte con dos modelos reconstruidos para que no se abra vacío
        if (selection.amjGetA() == null) {
            service.amjBuscarPorModelo("LOQ 15AHP10").ifPresent(selection::amjSetA);
        }
        if (selection.amjGetB() == null) {
            service.amjBuscarPorModelo("TUF Gaming A18").ifPresent(selection::amjSetB);
        }

        amjIniciarMoneda();
        navigation.amjImmersiveProperty().addListener((o, antes, on) -> amjInmersivo(on));
        navigation.amjRegister(this::amjShow);
        lblMsg.textProperty().bind(selection.amjMessageProperty());
        selection.amjGetCompare().addListener((javafx.collections.ListChangeListener<Object>) c -> amjUpdateStatus());
        amjUpdateStatus();
        amjShow(AMJScreen.BUSCAR);
    }

    /** Carga la moneda y el tipo de cambio guardados y conecta el selector USD/PEN de la barra superior. */
    private void amjIniciarMoneda() {
        try {
            AMJMoney.amjRateProperty().set(Double.parseDouble(config.amjGet(
                    com.laptopexplorer.service.AMJAppConfig.PEN_RATE, String.valueOf(AMJMoney.DEFAULT_PEN_RATE))));
        } catch (NumberFormatException e) {
            AMJMoney.amjRateProperty().set(AMJMoney.DEFAULT_PEN_RATE);
        }
        boolean pen = "PEN".equals(config.amjGet(com.laptopexplorer.service.AMJAppConfig.CURRENCY, "USD"));
        AMJMoney.amjCurrencyProperty().set(pen ? AMJMoney.AMJCurrency.PEN : AMJMoney.AMJCurrency.USD);
        ToggleGroup g = new ToggleGroup();
        tglUsd.setToggleGroup(g);
        tglPen.setToggleGroup(g);
        (pen ? tglPen : tglUsd).setSelected(true);
        g.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null) {
                old.setSelected(true);                   // siempre hay una moneda elegida
                return;
            }
            AMJMoney.AMJCurrency c = now == tglPen ? AMJMoney.AMJCurrency.PEN : AMJMoney.AMJCurrency.USD;
            AMJMoney.amjCurrencyProperty().set(c);
            config.amjSet(com.laptopexplorer.service.AMJAppConfig.CURRENCY, c.name());
        });
        Runnable alCambiar = () -> {
            javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(String.format(java.util.Locale.US,
                    "1 USD = %.3f PEN (%s). Se cambia en Ajustes.", AMJMoney.amjRateProperty().get(),
                    config.amjGet(com.laptopexplorer.service.AMJAppConfig.PEN_RATE_NOTE, AMJMoney.DEFAULT_PEN_NOTE)));
            tglUsd.setTooltip(tip);
            tglPen.setTooltip(tip);
            // la pantalla visible vuelve a pintar sus precios; las demás lo hacen al mostrarse
            if (current != null && current != AMJScreen.AJUSTES && controllers.get(current) instanceof AMJRefreshable r) {
                r.amjOnShow();
            }
        };
        AMJMoney.amjCurrencyProperty().addListener((o, a, b) -> alCambiar.run());
        AMJMoney.amjRateProperty().addListener((o, a, b) -> alCambiar.run());
        alCambiar.run();
    }

    /** Entra o sale del modo inmersivo: oculta barra lateral y superior y pone la ventana a pantalla completa. */
    private void amjInmersivo(boolean on) {
        pnlRiel.setVisible(!on);
        pnlRiel.setManaged(!on);
        pnlBarraSuperior.setVisible(!on);
        pnlBarraSuperior.setManaged(!on);
        javafx.scene.Parent root = pnlContenido.getScene().getRoot();
        root.getStyleClass().remove("immersive");
        if (on) {
            root.getStyleClass().add("immersive");
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) pnlContenido.getScene().getWindow();
        if (stage.getProperties().putIfAbsent("immersive.listener", Boolean.TRUE) == null) {
            stage.setFullScreenExitHint("Pulsa Esc para salir de la pantalla completa");
            // Esc lo gestiona JavaFX: al salir de pantalla completa se restaura la interfaz
            stage.fullScreenProperty().addListener((o, a, full) -> {
                if (!full) {
                    navigation.amjImmersiveProperty().set(false);
                }
            });
        }
        stage.setFullScreen(on);
    }

    private void amjUpdateStatus() {
        try {
            lblDb.setText(service.amjContar() + " laptops en la base de datos");
        } catch (RuntimeException e) {
            lblDb.setText("Base de datos no disponible");
        }
        int n = selection.amjGetCompare().size();
        lblSel.setText(n == 0 ? "" : n + (n == 1 ? " laptop marcado" : " laptops marcados") + " para comparar");
        lblSel.setVisible(n > 0);
        lblSel.setManaged(n > 0);
    }

    @FXML
    private void amjOnBuscar() {
        amjShow(AMJScreen.BUSCAR);
    }

    @FXML
    private void amjOnCatalogo() {
        amjShow(AMJScreen.CATALOGO);
    }

    @FXML
    private void amjOnComparar() {
        amjShow(AMJScreen.COMPARAR);
    }

    @FXML
    private void amjOnVisor() {
        amjShow(AMJScreen.VISOR);
    }

    @FXML
    private void amjOnComponentes() {
        amjShow(AMJScreen.COMPONENTES);
    }

    @FXML
    private void amjOnAjustes() {
        amjShow(AMJScreen.AJUSTES);
    }

    private void amjShow(AMJScreen screen) {
        if (current != null && controllers.get(current) instanceof AMJRefreshable r) {
            r.amjOnHide();
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
            AMJViewLoader.AMJLoaded loaded = loader.amjLoad(fxml);
            views.put(screen, loaded.root());
            controllers.put(screen, loaded.controller());
            pnlContenido.getChildren().add(loaded.root());
        }
        views.forEach((s, node) -> {
            boolean on = s == screen;
            node.setVisible(on);
            node.setManaged(on);
        });
        boolean cambio = current != screen;
        current = screen;
        buttons.forEach((s, b) -> b.setSelected(s == screen));
        amjUpdateStatus();
        if (controllers.get(screen) instanceof AMJRefreshable r) {
            r.amjOnShow();
        }
        if (cambio) {
            // transición de pantalla: la nueva aparece con fundido y sube unos píxeles
            com.laptopexplorer.ui.AMJMotion.amjEnter(views.get(screen));
        }
    }

    /** Búsqueda de la barra superior: abre la pantalla Buscar con el texto escrito. */
    @FXML
    private void amjOnBuscarGlobal() {
        amjShow(AMJScreen.BUSCAR);
        if (controllers.get(AMJScreen.BUSCAR) instanceof AMJSearchController sc) {
            sc.amjBuscarTexto(txtGlobal.getText());
        }
    }
}
