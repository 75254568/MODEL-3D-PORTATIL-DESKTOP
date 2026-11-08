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
        List<Screen> order = List.of(Screen.CATALOGO, Screen.COMPARAR, Screen.VISOR, Screen.AJUSTES);
        step(scene, nav, out, order, 0);
    }

    private static void step(Scene scene, NavigationService nav, Path out, List<Screen> order, int i) {
        if (i >= order.size()) {
            System.out.println("SELFCHECK terminado");
            Platform.exit();
            return;
        }
        Screen s = order.get(i);
        nav.go(s);
        PauseTransition pt = new PauseTransition(Duration.millis(s == Screen.VISOR ? 2500 : 1200));
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
            step(scene, nav, out, order, i + 1);
        });
        pt.play();
    }
}
