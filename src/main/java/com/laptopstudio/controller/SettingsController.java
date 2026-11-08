package com.laptopstudio.controller;

import com.laptopstudio.service.LaptopService;
import com.laptopstudio.ui.Refreshable;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/** Informacion de solo lectura: donde se guardan los datos y con que versiones de Java se ejecuta. */
@Component
@RequiredArgsConstructor
public class SettingsController implements Refreshable {

    private final LaptopService service;

    @FXML private Label lblRuta;
    @FXML private Label lblTotal;
    @FXML private Label lblJava;
    @FXML private Label lblFx;
    @FXML private Label lblMem;

    @FXML
    public void initialize() {
        lblRuta.setText(Path.of(System.getProperty("user.home"), ".laptop-comparator", "laptops.db").toString());
        lblJava.setText(String.valueOf(Runtime.version()));
        lblFx.setText(System.getProperty("javafx.runtime.version", "21"));
    }

    @Override
    public void onShow() {
        lblTotal.setText(String.valueOf(service.contar()));
        Runtime rt = Runtime.getRuntime();
        lblMem.setText((rt.totalMemory() - rt.freeMemory()) / (1024 * 1024) + " MB");
    }
}
