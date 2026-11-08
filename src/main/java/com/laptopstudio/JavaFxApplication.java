package com.laptopstudio;

import atlantafx.base.theme.PrimerDark;
import com.laptopstudio.ui.NavigationService;
import com.laptopstudio.ui.SelfCheck;
import com.laptopstudio.ui.ViewLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        try {
            // La carpeta de datos debe existir antes de que SQLite abra el archivo
            Files.createDirectories(Path.of(System.getProperty("user.home"), ".laptop-comparator"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.context = new SpringApplicationBuilder(LaptopStudioApplication.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        ViewLoader.Loaded main = context.getBean(ViewLoader.class).load("/view/main-view.fxml");
        double w = 1440;
        double h = 900;
        String size = System.getProperty("studio.size");     // solo para pruebas: -Dstudio.size=1100x720
        if (size != null && size.matches("[0-9]+x[0-9]+")) {
            w = Double.parseDouble(size.split("x")[0]);
            h = Double.parseDouble(size.split("x")[1]);
        }
        Scene scene = new Scene((Parent) main.root(), w, h);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());

        stage.setTitle("Laptop Studio 3D");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(720);
        stage.show();

        if (com.laptopstudio.ui.FunctionalCheck.enabled()) {
            com.laptopstudio.ui.FunctionalCheck.run(scene, context, context.getBean(NavigationService.class));
        } else if (com.laptopstudio.ui.InteractionCheck.enabled()) {
            com.laptopstudio.ui.InteractionCheck.run(scene, context.getBean(NavigationService.class));
        } else if (SelfCheck.enabled()) {
            // para que la captura de "Comparar" tenga contenido: marca tres laptops del catalogo
            var service = context.getBean(com.laptopstudio.service.LaptopService.class);
            var seleccion = context.getBean(com.laptopstudio.ui.SelectionState.class);
            for (String m : new String[]{"LOQ 15AHP10", "TUF Gaming A18", "Legion Pro 7i Gen 10"}) {
                service.buscarPorModelo(m).ifPresent(l -> seleccion.setMarked(l, true));
            }
            SelfCheck.run(scene, context.getBean(NavigationService.class));
        }
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }
}
