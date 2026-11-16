package com.laptopstudio.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Autoprueba opcional: con {@code -Dstudio.snapshot=carpeta} recorre las pantallas, guarda una captura
 * de cada una y cierra la aplicacion. Sirve para comprobar el arranque sin intervencion manual.
 */
public final class SelfCheck {

    private SelfCheck() {
    }

    public static boolean enabled() {
        return System.getProperty("studio.snapshot") != null;
    }

    public static void run(Scene scene, NavigationService nav) {
        Path out = Path.of(System.getProperty("studio.snapshot"));
        try {
            Files.createDirectories(out);
        } catch (Exception e) {
            System.out.println("SELFCHECK no se pudo crear " + out + ": " + e);
            Platform.exit();
            return;
        }
        List<Screen> order = List.of(Screen.BUSCAR, Screen.CATALOGO, Screen.COMPARAR, Screen.VISOR, Screen.COMPONENTES,
                Screen.AJUSTES);
        step(scene, nav, out, order, 0);
    }

    /**
     * Opciones para capturar la pantalla Componentes 3D en un estado concreto:
     * {@code -Dstudio.component=texto} elige el primer componente cuyo nombre lo contiene,
     * {@code -Dstudio.explode=0..100} mueve el despiece y {@code -Dstudio.sheet=0..1} desplaza la ficha.
     */
    private static void prepareComponents(Scene scene) {
        String want = System.getProperty("studio.component");
        if (want != null && scene.lookup(".component-list .list-view") instanceof javafx.scene.control.ListView<?> list) {
            for (int k = 0; k < list.getItems().size(); k++) {
                if (String.valueOf(list.getItems().get(k)).toLowerCase().contains(want.toLowerCase())) {
                    list.getSelectionModel().select(k);
                    list.scrollTo(k);
                    break;
                }
            }
        }
        if (System.getProperty("studio.explode") != null
                && scene.lookup(".component-explode") instanceof javafx.scene.control.Slider slider) {
            slider.setValue(Double.parseDouble(System.getProperty("studio.explode")));
        }
        if (System.getProperty("studio.sheet") != null
                && scene.lookup(".component-sheet") instanceof javafx.scene.control.ScrollPane sheet) {
            double v = Double.parseDouble(System.getProperty("studio.sheet"));
            // la ficha se rellena al cargar el modelo: se desplaza un poco despues
            PauseTransition later = new PauseTransition(Duration.millis(2500));
            later.setOnFinished(e -> sheet.setVvalue(v));
            later.play();
        }
    }

    private static void step(Scene scene, NavigationService nav, Path out, List<Screen> order, int i) {
        if (i >= order.size()) {
            System.out.println("SELFCHECK terminado");
            Platform.exit();
            return;
        }
        Screen s = order.get(i);
        nav.go(s);
        // -Dstudio.immersive=true: captura el visor en su modo de pantalla completa
        boolean inmersivo = s == Screen.VISOR && Boolean.getBoolean("studio.immersive");
        if (inmersivo) {
            nav.immersiveProperty().set(true);
        }
        if (s == Screen.COMPONENTES) {
            prepareComponents(scene);
        }
        // los visores 3D necesitan mas tiempo: cargan modelos (y la biblioteca, ademas, dibuja sus miniaturas)
        int espera = s == Screen.VISOR ? 2500 : s == Screen.COMPONENTES ? 5000 : 1200;
        PauseTransition pt = new PauseTransition(Duration.millis(espera));
        pt.setOnFinished(e -> {
            try {
                WritableImage img = scene.snapshot(null);
                int w = (int) img.getWidth(), h = (int) img.getHeight();
                BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                PixelReader pr = img.getPixelReader();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        bi.setRGB(x, y, pr.getArgb(x, y));
                    }
                }
                ImageIO.write(bi, "png", new File(out.toFile(), s.name().toLowerCase() + ".png"));
                System.out.println("SELFCHECK ok " + s);
            } catch (Exception ex) {
                System.out.println("SELFCHECK error " + s + ": " + ex);
            }
            if (inmersivo) {
                nav.immersiveProperty().set(false);
            }
            step(scene, nav, out, order, i + 1);
        });
        pt.play();
    }
}
