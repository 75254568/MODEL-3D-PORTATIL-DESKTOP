package com.laptopstudio.controller;

import com.laptopstudio.service.AppConfig;
import com.laptopstudio.service.LaptopService;
import com.laptopstudio.ui.Money;
import com.laptopstudio.ui.Refreshable;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Locale;

/** Ajustes: tipo de cambio USD/PEN, clave para la consulta en internet y datos de la instalacion. */
@Component
@RequiredArgsConstructor
public class SettingsController implements Refreshable {

    private final LaptopService service;
    private final AppConfig config;

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
        lblRuta.setText(Path.of(System.getProperty("user.home"), ".laptop-comparator", "laptops.db").toString());
        lblJava.setText(String.valueOf(Runtime.version()));
        lblFx.setText(System.getProperty("javafx.runtime.version", "21"));
        mostrarCambio();
        mostrarClave();
    }

    @Override
    public void onShow() {
        lblTotal.setText(String.valueOf(service.contar()));
        Runtime rt = Runtime.getRuntime();
        lblMem.setText((rt.totalMemory() - rt.freeMemory()) / (1024 * 1024) + " MB");
    }

    /* ------------------------------------------------------------------ tipo de cambio */

    private void mostrarCambio() {
        txtRate.setText(String.format(Locale.US, "%.3f", Money.rateProperty().get()));
        txtRateNote.setText(config.get(AppConfig.PEN_RATE_NOTE, Money.DEFAULT_PEN_NOTE));
        lblRateMsg.setText("En uso: 1 USD = " + txtRate.getText() + " PEN (" + txtRateNote.getText() + ").");
    }

    @FXML
    private void onGuardarCambio() {
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
        config.set(AppConfig.PEN_RATE, String.format(Locale.US, "%.4f", rate));
        config.set(AppConfig.PEN_RATE_NOTE, nota);
        Money.rateProperty().set(rate);
        mostrarCambio();
    }

    @FXML
    private void onRestablecerCambio() {
        config.set(AppConfig.PEN_RATE, null);
        config.set(AppConfig.PEN_RATE_NOTE, null);
        Money.rateProperty().set(Money.DEFAULT_PEN_RATE);
        mostrarCambio();
    }

    /* ------------------------------------------------------------------ clave de la API */

    private void mostrarClave() {
        boolean env = System.getenv("ANTHROPIC_API_KEY") != null && !System.getenv("ANTHROPIC_API_KEY").isBlank();
        boolean guardada = config.get(AppConfig.API_KEY, null) != null;
        txtKey.clear();
        lblKeyMsg.setText(env ? "Se usa la clave de la variable de entorno ANTHROPIC_API_KEY."
                : guardada ? "Hay una clave guardada en este equipo (config.properties, sin cifrar)."
                : "Sin clave: la consulta en internet está desactivada. El resto de la aplicación no la necesita.");
    }

    @FXML
    private void onGuardarClave() {
        String k = txtKey.getText() == null ? "" : txtKey.getText().trim();
        if (k.length() < 20) {
            lblKeyMsg.setText("Esa clave parece incompleta.");
            return;
        }
        config.set(AppConfig.API_KEY, k);
        mostrarClave();
    }

    @FXML
    private void onQuitarClave() {
        config.set(AppConfig.API_KEY, null);
        mostrarClave();
    }
}
