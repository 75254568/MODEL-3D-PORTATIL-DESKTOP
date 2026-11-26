package com.laptopexplorer.ui;

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
public class AMJViewLoader {

    public record AMJLoaded(Node root, Object controller) {
    }

    private final ApplicationContext context;

    public AMJLoaded amjLoad(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(context::getBean);
            Node root = loader.load();
            return new AMJLoaded(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + fxml, e);
        }
    }
}
