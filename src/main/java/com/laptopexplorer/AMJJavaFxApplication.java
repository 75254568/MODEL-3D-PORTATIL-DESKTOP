package com.laptopexplorer;

import atlantafx.base.theme.PrimerLight;
import com.laptopexplorer.ui.AMJDialogs;
import com.laptopexplorer.ui.AMJNavigationService;
import com.laptopexplorer.ui.AMJSelfCheck;
import com.laptopexplorer.ui.AMJViewLoader;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class AMJJavaFxApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(AMJJavaFxApplication.class);

    private ConfigurableApplicationContext context;
    /** Fallo al preparar los datos o arrancar Spring. Se muestra en {@link #start}: en init() aun no hay interfaz. */
    private Throwable startupError;
    /** Evita apilar diálogos si el mismo error se repite (por ejemplo, dentro de una animación). */
    private final AtomicBoolean avisando = new AtomicBoolean();

    private static Path amjDataDir() {
        return Path.of(System.getProperty("user.home"), ".laptop-explorer");
    }

    /**
     * Las versiones anteriores guardaban los datos en ~/.laptop-comparator. Si esa carpeta existe y la nueva
     * todavía no, se renombra para conservar el catálogo, la configuración y los modelos importados.
     */
    private static void amjMigrarDatos() {
        Path anterior = Path.of(System.getProperty("user.home"), ".laptop-comparator");
        Path actual = amjDataDir();
        if (Files.exists(actual) || !Files.isDirectory(anterior)) {
            return;
        }
        try {
            Files.move(anterior, actual);
            log.info("Datos trasladados de {} a {}", anterior, actual);
        } catch (IOException e) {
            log.warn("No se pudo trasladar la carpeta de datos {}: {}", anterior, e.toString());
        }
    }

    /** Archivo de registro; la misma ruta que {@code logging.file.name} en application.properties. */
    private static Path amjLogFile() {
        return amjDataDir().resolve("logs").resolve("laptop-explorer.log");
    }

    @Override
    public void init() {
        try {
            // La carpeta de datos debe existir antes de que SQLite abra el archivo
            amjMigrarDatos();
            Files.createDirectories(amjDataDir());
            this.context = new SpringApplicationBuilder(AMJLaptopExplorerApplication.class)
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
        // En las pruebas automáticas no se abren diálogos de error: las dejarian esperando a que alguien los cierre
        boolean prueba = com.laptopexplorer.ui.AMJFunctionalCheck.amjEnabled()
                || com.laptopexplorer.ui.AMJInteractionCheck.amjEnabled() || AMJSelfCheck.amjEnabled();
        if (startupError != null) {
            if (prueba) {
                System.out.println("ARRANQUE FALLO  " + amjDescribe(startupError));
            } else {
                // Sin esto la aplicación instalada (sin consola) se cerraba sin decir nada
                amjShowStartupError(startupError);
            }
            Platform.exit();
            return;
        }

        AMJViewLoader.AMJLoaded main = context.getBean(AMJViewLoader.class).amjLoad("/view/main-view.fxml");
        double w = 1440;
        double h = 900;
        String size = System.getProperty("explorer.size");     // solo para pruebas: -Dexplorer.size=1100x720
        if (size != null && size.matches("[0-9]+x[0-9]+")) {
            w = Double.parseDouble(size.split("x")[0]);
            h = Double.parseDouble(size.split("x")[1]);
        }
        Scene scene = new Scene((Parent) main.root(), w, h);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());

        stage.setTitle("Laptop Explorer");
        // icono de la ventana y de la barra de tareas, en varios tamaños para que Windows elija el más nitido
        for (int px : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            stage.getIcons().add(new javafx.scene.image.Image(
                    Objects.requireNonNull(getClass().getResource("/img/icon-" + px + ".png")).toExternalForm()));
        }
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(720);
        stage.show();

        amjInstallErrorHandler(scene, prueba);

        if (com.laptopexplorer.ui.AMJFunctionalCheck.amjEnabled()) {
            com.laptopexplorer.ui.AMJFunctionalCheck.amjRun(scene, context, context.getBean(AMJNavigationService.class));
        } else if (com.laptopexplorer.ui.AMJInteractionCheck.amjEnabled()) {
            com.laptopexplorer.ui.AMJInteractionCheck.amjRun(scene, context.getBean(AMJNavigationService.class));
        } else if (AMJSelfCheck.amjEnabled()) {
            // para que la captura de "Comparar" tenga contenido: marca tres laptops del catálogo
            var service = context.getBean(com.laptopexplorer.service.AMJLaptopService.class);
            var seleccion = context.getBean(com.laptopexplorer.ui.AMJSelectionState.class);
            for (String m : new String[]{"LOQ 15AHP10", "TUF Gaming A18", "Legion Pro 7i Gen 10"}) {
                service.amjBuscarPorModelo(m).ifPresent(l -> seleccion.amjSetMarked(l, true));
            }
            AMJSelfCheck.amjRun(scene, context.getBean(AMJNavigationService.class));
        }
    }

    /**
     * Cualquier excepción que nadie capture (en la interfaz o en un hilo de trabajo) queda en el registro y se
     * avisa al usuario. En las pruebas automáticas solo se registra: un diálogo las dejaria esperando.
     */
    private void amjInstallErrorHandler(Scene scene, boolean silent) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            log.error("Error no controlado en el hilo {}", thread.getName(), error);
            if (silent || !avisando.compareAndSet(false, true)) {
                return;
            }
            // runLater: un diálogo modal no se puede abrir en mitad de una animación o de un calculo de disposición
            Platform.runLater(() -> {
                try {
                    AMJDialogs.amjError(scene.getRoot(), "Se produjo un error inesperado",
                            amjDescribe(error) + "\n\nPuedes seguir usando la aplicación. El detalle quedó en:\n" + amjLogFile());
                } finally {
                    avisando.set(false);
                }
            });
        });
    }

    private static void amjShowStartupError(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, amjDescribe(error) + "\n\nEl detalle quedó en:\n" + amjLogFile(), ButtonType.CLOSE);
        alert.setTitle("Laptop Explorer");
        alert.setHeaderText("Laptop Explorer no pudo iniciarse");
        alert.getDialogPane().setMinWidth(520);
        alert.showAndWait();
    }

    /** Mensaje de la causa original, que es la que dice que fallo (base bloqueada, migración, archivo...). */
    private static String amjDescribe(Throwable error) {
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
