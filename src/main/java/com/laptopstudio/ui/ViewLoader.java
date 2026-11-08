package com.laptopstudio.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Carga archivos FXML haciendo que Spring construya los controladores (con sus servicios inyectados). */
@Component
@RequiredArgsConstructor
public class ViewLoader {

    public record Loaded(Node root, Object controller) {
    }

    private final ApplicationContext context;

    public Loaded load(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(context::getBean);
            Node root = loader.load();
            return new Loaded(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + fxml, e);
        }
    }
}
