package com.laptopstudio.controller;

import com.laptopstudio.service.LaptopService;
import com.laptopstudio.ui.Icons;
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

    @FXML private StackPane content;
    @FXML private ToggleButton btnCatalogo;
    @FXML private ToggleButton btnComparar;
    @FXML private ToggleButton btnVisor;
    @FXML private ToggleButton btnAjustes;
    @FXML private Label lblDb;
    @FXML private Label lblSel;
    @FXML private Label lblMsg;

    private final Map<Screen, Node> views = new EnumMap<>(Screen.class);
    private final Map<Screen, Object> controllers = new EnumMap<>(Screen.class);
    private final Map<Screen, ToggleButton> buttons = new EnumMap<>(Screen.class);
    private Screen current;

    @FXML
    public void initialize() {
        buttons.put(Screen.CATALOGO, btnCatalogo);
        buttons.put(Screen.COMPARAR, btnComparar);
        buttons.put(Screen.VISOR, btnVisor);
        buttons.put(Screen.AJUSTES, btnAjustes);
        btnCatalogo.setGraphic(Icons.of(Icons.LIST, 20));
        btnComparar.setGraphic(Icons.of(Icons.COMPARE, 20));
        btnVisor.setGraphic(Icons.of(Icons.CUBE, 20));
        btnAjustes.setGraphic(Icons.of(Icons.SETTINGS, 20));

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

        navigation.register(this::show);
        lblMsg.textProperty().bind(selection.messageProperty());
        selection.getCompare().addListener((javafx.collections.ListChangeListener<Object>) c -> updateStatus());
        updateStatus();
        show(Screen.CATALOGO);
    }

    private void updateStatus() {
        try {
            lblDb.setText(service.contar() + " laptops en la base de datos");
        } catch (RuntimeException e) {
            lblDb.setText("Base de datos no disponible");
        }
        int n = selection.getCompare().size();
        lblSel.setText(n == 0 ? "" : n + (n == 1 ? " laptop marcado" : " laptops marcados") + " para comparar");
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
    private void onAjustes() {
        show(Screen.AJUSTES);
    }

    private void show(Screen screen) {
        if (current != null && controllers.get(current) instanceof Refreshable r) {
            r.onHide();
        }
        if (!views.containsKey(screen)) {
            String fxml = switch (screen) {
                case CATALOGO -> "/view/catalog-view.fxml";
                case COMPARAR -> "/view/compare-view.fxml";
                case VISOR -> "/view/viewer-view.fxml";
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
        current = screen;
        buttons.forEach((s, b) -> b.setSelected(s == screen));
        updateStatus();
        if (controllers.get(screen) instanceof Refreshable r) {
            r.onShow();
        }
    }
}
