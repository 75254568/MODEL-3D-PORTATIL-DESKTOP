package com.laptopstudio;

import atlantafx.base.theme.PrimerLight;
import com.laptopstudio.ui.Dialogs;
import com.laptopstudio.ui.NavigationService;
import com.laptopstudio.ui.SelfCheck;
import com.laptopstudio.ui.ViewLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaFxApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(JavaFxApplication.class);

    private ConfigurableApplicationContext context;
    /** Fallo al preparar los datos o arrancar Spring. Se muestra en {@link #start}: en init() aun no hay interfaz. */
    private Throwable startupError;
    /** Evita apilar dialogos si el mismo error se repite (por ejemplo, dentro de una animacion). */
    private final AtomicBoolean avisando = new AtomicBoolean();

    private static Path dataDir() {
        return Path.of(System.getProperty("user.home"), ".laptop-comparator");
    }

    /** Archivo de registro; la misma ruta que {@code logging.file.name} en application.properties. */
    private static Path logFile() {
        return dataDir().resolve("logs").resolve("laptop-explorer.log");
    }

    @Override
    public void init() {
        try {
            // La carpeta de datos debe existir antes de que SQLite abra el archivo
            Files.createDirectories(dataDir());
            this.context = new SpringApplicationBuilder(LaptopStudioApplication.class)
                    .web(WebApplicationType.NONE)
                    .run();
        } catch (Exception e) {
            log.error("La aplicación no pudo iniciarse", e);
            startupError = e;
        }
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        // En las pruebas automaticas no se abren dialogos de error: las dejarian esperando a que alguien los cierre
        boolean prueba = com.laptopstudio.ui.FunctionalCheck.enabled()
                || com.laptopstudio.ui.InteractionCheck.enabled() || SelfCheck.enabled();
        if (startupError != null) {
            if (prueba) {
                System.out.println("ARRANQUE FALLO  " + describe(startupError));
            } else {
                // Sin esto la aplicacion instalada (sin consola) se cerraba sin decir nada
                showStartupError(startupError);
            }
            Platform.exit();
            return;
        }

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

        stage.setTitle("Laptop Explorer");
        // icono de la ventana y de la barra de tareas, en varios tamanos para que Windows elija el mas nitido
        for (int px : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            stage.getIcons().add(new javafx.scene.image.Image(
                    Objects.requireNonNull(getClass().getResource("/img/icon-" + px + ".png")).toExternalForm()));
        }
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(720);
        stage.show();

        installErrorHandler(scene, prueba);

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

    /**
     * Cualquier excepcion que nadie capture (en la interfaz o en un hilo de trabajo) queda en el registro y se
     * avisa al usuario. En las pruebas automaticas solo se registra: un dialogo las dejaria esperando.
     */
    private void installErrorHandler(Scene scene, boolean silent) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            log.error("Error no controlado en el hilo {}", thread.getName(), error);
            if (silent || !avisando.compareAndSet(false, true)) {
                return;
            }
            // runLater: un dialogo modal no se puede abrir en mitad de una animacion o de un calculo de disposicion
            Platform.runLater(() -> {
                try {
                    Dialogs.error(scene.getRoot(), "Se produjo un error inesperado",
                            describe(error) + "\n\nPuedes seguir usando la aplicación. El detalle quedó en:\n" + logFile());
                } finally {
                    avisando.set(false);
                }
            });
        });
    }

    private static void showStartupError(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, describe(error) + "\n\nEl detalle quedó en:\n" + logFile(), ButtonType.CLOSE);
        alert.setTitle("Laptop Explorer");
        alert.setHeaderText("Laptop Explorer no pudo iniciarse");
        alert.getDialogPane().setMinWidth(520);
        alert.showAndWait();
    }

    /** Mensaje de la causa original, que es la que dice que fallo (base bloqueada, migracion, archivo...). */
    private static String describe(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage() == null || root.getMessage().isBlank() ? root.getClass().getSimpleName() : root.getMessage().trim();
        return msg.length() > 600 ? msg.substring(0, 600) + "…" : msg;
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }
}
