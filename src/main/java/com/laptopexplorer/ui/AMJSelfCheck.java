package com.laptopexplorer.ui;

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
 * Autoprueba opcional: con {@code -Dexplorer.snapshot=carpeta} recorre las pantallas, guarda una captura
 * de cada una y cierra la aplicación. Sirve para comprobar el arranque sin intervención manual.
 */
public final class AMJSelfCheck {

    private AMJSelfCheck() {
    }

    public static boolean amjEnabled() {
        return System.getProperty("explorer.snapshot") != null;
    }

    public static void amjRun(Scene scene, AMJNavigationService nav) {
        Path out = Path.of(System.getProperty("explorer.snapshot"));
        try {
            Files.createDirectories(out);
        } catch (Exception e) {
            System.out.println("SELFCHECK no se pudo crear " + out + ": " + e);
            Platform.exit();
            return;
        }
        List<AMJScreen> order = List.of(AMJScreen.BUSCAR, AMJScreen.CATALOGO, AMJScreen.COMPARAR, AMJScreen.VISOR, AMJScreen.COMPONENTES,
                AMJScreen.AJUSTES);
        amjStep(scene, nav, out, order, 0);
    }

    /**
     * Opciones para capturar la pantalla Componentes 3D en un estado concreto:
     * {@code -Dexplorer.component=texto} elige el primer componente cuyo nombre lo contiene,
     * {@code -Dexplorer.explode=0..100} mueve el despiece y {@code -Dexplorer.sheet=0..1} desplaza la ficha.
     */
    private static void amjPrepareComponents(Scene scene) {
        String want = System.getProperty("explorer.component");
        if (want != null && scene.lookup(".component-list .list-view") instanceof javafx.scene.control.ListView<?> list) {
            for (int k = 0; k < list.getItems().size(); k++) {
                if (String.valueOf(list.getItems().get(k)).toLowerCase().contains(want.toLowerCase())) {
                    list.getSelectionModel().select(k);
                    list.scrollTo(k);
                    break;
                }
            }
        }
        if (System.getProperty("explorer.explode") != null
                && scene.lookup(".component-explode") instanceof javafx.scene.control.Slider slider) {
            slider.setValue(Double.parseDouble(System.getProperty("explorer.explode")));
        }
        if (System.getProperty("explorer.sheet") != null
                && scene.lookup(".component-sheet") instanceof javafx.scene.control.ScrollPane sheet) {
            double v = Double.parseDouble(System.getProperty("explorer.sheet"));
            // la ficha se rellena al cargar el modelo: se desplaza un poco después
            PauseTransition later = new PauseTransition(Duration.millis(2500));
            later.setOnFinished(e -> sheet.setVvalue(v));
            later.play();
        }
    }

    private static void amjStep(Scene scene, AMJNavigationService nav, Path out, List<AMJScreen> order, int i) {
        if (i >= order.size()) {
            System.out.println("SELFCHECK terminado");
            Platform.exit();
            return;
        }
        AMJScreen s = order.get(i);
        nav.amjGo(s);
        // -Dexplorer.immersive=true: captura el visor en su modo de pantalla completa
        boolean inmersivo = s == AMJScreen.VISOR && Boolean.getBoolean("explorer.immersive");
        if (inmersivo) {
            nav.amjImmersiveProperty().set(true);
        }
        if (s == AMJScreen.COMPONENTES) {
            amjPrepareComponents(scene);
        }
        // los visores 3D necesitan más tiempo: cargan modelos (y la biblioteca, además, dibuja sus miniaturas)
        int espera = s == AMJScreen.VISOR ? 2500 : s == AMJScreen.COMPONENTES ? 5000 : 1200;
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
                nav.amjImmersiveProperty().set(false);
            }
            amjStep(scene, nav, out, order, i + 1);
        });
        pt.play();
    }
}
