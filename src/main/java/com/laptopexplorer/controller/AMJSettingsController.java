package com.laptopexplorer.controller;

import com.laptopexplorer.service.AMJAppConfig;
import com.laptopexplorer.service.AMJLaptopService;
import com.laptopexplorer.ui.AMJMoney;
import com.laptopexplorer.ui.AMJRefreshable;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Locale;

/** Ajustes: tipo de cambio USD/PEN, clave para la consulta en internet y datos de la instalación. */
@Component
@RequiredArgsConstructor
public class AMJSettingsController implements AMJRefreshable {

    private final AMJLaptopService service;
    private final AMJAppConfig config;

    @FXML private Label lblRuta;
    @FXML private Label lblTotal;
    @FXML private Label lblJava;
    @FXML private Label lblFx;
    @FXML private Label lblMem;
    @FXML private TextField txtRate;
    @FXML private TextField txtRateNote;
    @FXML private Label lblRateMsg;
    @FXML private PasswordField txtKey;
    @FXML private Label lblKeyMsg;

    @FXML
    public void initialize() {
        lblRuta.setText(Path.of(System.getProperty("user.home"), ".laptop-explorer", "laptops.db").toString());
        lblJava.setText(String.valueOf(Runtime.version()));
        lblFx.setText(System.getProperty("javafx.runtime.version", "21"));
        amjMostrarCambio();
        amjMostrarClave();
    }

    @Override
    public void amjOnShow() {
        lblTotal.setText(String.valueOf(service.amjContar()));
        Runtime rt = Runtime.getRuntime();
        lblMem.setText((rt.totalMemory() - rt.freeMemory()) / (1024 * 1024) + " MB");
    }

    /* ------------------------------------------------------------------ tipo de cambio */

    private void amjMostrarCambio() {
        txtRate.setText(String.format(Locale.US, "%.3f", AMJMoney.amjRateProperty().get()));
        txtRateNote.setText(config.amjGet(AMJAppConfig.PEN_RATE_NOTE, AMJMoney.DEFAULT_PEN_NOTE));
        lblRateMsg.setText("En uso: 1 USD = " + txtRate.getText() + " PEN (" + txtRateNote.getText() + ").");
    }

    @FXML
    private void amjOnGuardarCambio() {
        double rate;
        try {
            rate = Double.parseDouble(txtRate.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            rate = -1;
        }
        if (rate < 1 || rate > 10) {
            lblRateMsg.setText("Escribe un valor entre 1 y 10, por ejemplo 3.433.");
            return;
        }
        String nota = txtRateNote.getText() == null || txtRateNote.getText().isBlank()
                ? "definido por el usuario" : txtRateNote.getText().trim();
        config.amjSet(AMJAppConfig.PEN_RATE, String.format(Locale.US, "%.4f", rate));
        config.amjSet(AMJAppConfig.PEN_RATE_NOTE, nota);
        AMJMoney.amjRateProperty().set(rate);
        amjMostrarCambio();
    }

    @FXML
    private void amjOnRestablecerCambio() {
        config.amjSet(AMJAppConfig.PEN_RATE, null);
        config.amjSet(AMJAppConfig.PEN_RATE_NOTE, null);
        AMJMoney.amjRateProperty().set(AMJMoney.DEFAULT_PEN_RATE);
        amjMostrarCambio();
    }

    /* ------------------------------------------------------------------ clave de la API */

    private void amjMostrarClave() {
        boolean env = System.getenv("ANTHROPIC_API_KEY") != null && !System.getenv("ANTHROPIC_API_KEY").isBlank();
        boolean guardada = config.amjGet(AMJAppConfig.API_KEY, null) != null;
        txtKey.clear();
        lblKeyMsg.setText(env ? "Se usa la clave de la variable de entorno ANTHROPIC_API_KEY."
                : guardada ? "Hay una clave guardada en este equipo (config.properties, sin cifrar)."
                : "Sin clave: la consulta en internet está desactivada. El resto de la aplicación no la necesita.");
    }

    @FXML
    private void amjOnGuardarClave() {
        String k = txtKey.getText() == null ? "" : txtKey.getText().trim();
        if (k.length() < 20) {
            lblKeyMsg.setText("Esa clave parece incompleta.");
            return;
        }
        config.amjSet(AMJAppConfig.API_KEY, k);
        amjMostrarClave();
    }

    @FXML
    private void amjOnQuitarClave() {
        config.amjSet(AMJAppConfig.API_KEY, null);
        amjMostrarClave();
    }
}
