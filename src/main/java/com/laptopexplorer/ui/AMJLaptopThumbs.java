package com.laptopexplorer.ui;

import com.laptopexplorer.model.AMJLaptop;
import com.laptopexplorer.service.AMJAppConfig;
import com.laptopexplorer.viewer.AMJLaptopShot;
import javafx.animation.PauseTransition;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Imágenes de producto de los laptops para las tarjetas, dibujadas con su modelo 3D ({@link AMJLaptopShot}).
 * Dibujar una cuesta entre una y varias décimas de segundo y tiene que hacerse en el hilo de la interfaz, así
 * que se hacen de una en una, con una pausa entre dos para que la ventana siga respondiendo: la tarjeta aparece
 * al momento y su imagen llega poco después. Cada imagen se guarda en memoria y en disco
 * ({@code ~/.laptop-explorer/cache/laptops}), de modo que solo se dibuja la primera vez o cuando cambia la ficha.
 * Salvo la escritura en disco, todo ocurre en el hilo de JavaFX.
 */
@Component
@Slf4j
public class AMJLaptopThumbs {

    /** Tamaño de la imagen en la tarjeta. */
    public static final int WIDTH = 240, HEIGHT = 168;
    /** Versión del dibujo (y su tamaño): al cambiarla, las imágenes guardadas con la anterior dejan de usarse. */
    private static final String STYLE = "v7-" + WIDTH + "x" + HEIGHT;
    private static final int MAX_CACHED = 240;

    private final Path dir;
    private final Map<String, Image> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > MAX_CACHED;
        }
    };
    private final Map<String, AMJLaptop> pending = new LinkedHashMap<>();
    private final Map<String, List<Consumer<Image>>> waiting = new LinkedHashMap<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "guardar-imagenes-laptop");
        t.setDaemon(true);
        return t;
    });
    private boolean running;

    public AMJLaptopThumbs(AMJAppConfig config) {
        this.dir = config.amjDataDir().resolve("cache").resolve("laptops");
    }

    /**
     * Imagen del laptop si ya existe (en memoria o en disco); si no, devuelve null, la encarga y la entrega
     * después a {@code onReady}. La clave incluye lo que cambia el dibujo: al editar la ficha se vuelve a dibujar.
     */
    public Image amjRequest(AMJLaptop l, Consumer<Image> onReady) {
        String key = amjKey(l);
        Image ready = cache.get(key);
        if (ready != null) {
            return ready;
        }
        Path file = amjFile(l, key);
        if (file != null && Files.isRegularFile(file)) {
            // se lee en segundo plano: la tarjeta la muestra en cuanto termina de cargar
            Image stored = new Image(file.toUri().toString(), true);
            cache.put(key, stored);
            stored.errorProperty().addListener((o, a, failed) -> {
                if (failed) {
                    cache.remove(key);          // archivo danado: se dibujara de nuevo la próxima vez
                }
            });
            return stored;
        }
        waiting.computeIfAbsent(key, k -> new ArrayList<>()).add(onReady);
        pending.putIfAbsent(key, l);
        if (!running) {
            running = true;
            amjNext();
        }
        return null;
    }

    private void amjNext() {
        PauseTransition gap = new PauseTransition(Duration.millis(25));
        gap.setOnFinished(e -> {
            Iterator<Map.Entry<String, AMJLaptop>> it = pending.entrySet().iterator();
            if (!it.hasNext()) {
                running = false;
                return;
            }
            Map.Entry<String, AMJLaptop> job = it.next();
            it.remove();
            List<Consumer<Image>> listeners = waiting.remove(job.getKey());
            try {
                Image img = AMJLaptopShot.amjRender(job.getValue(), WIDTH, HEIGHT);
                cache.put(job.getKey(), img);
                if (listeners != null) {
                    listeners.forEach(c -> c.accept(img));
                }
                amjSave(job.getValue(), job.getKey(), img);
            } catch (RuntimeException ex) {
                // la tarjeta se queda sin imagen; el resto de la interfaz no depende de ella
                log.warn("No se pudo dibujar la imagen de {}: {}", job.getValue().amjGetDisplayName(), ex.toString());
            }
            amjNext();
        });
        gap.play();
    }

    /** Los píxeles se copian aquí (hilo de JavaFX); comprimir y escribir el PNG se hace en otro hilo. */
    private void amjSave(AMJLaptop l, String key, Image img) {
        Path file = amjFile(l, key);
        if (file == null) {
            return;
        }
        int w = (int) img.getWidth(), h = (int) img.getHeight();
        int[] px = new int[w * h];
        img.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
        String prefix = l.getId() + "_";
        writer.submit(() -> {
            try {
                Files.createDirectories(dir);
                // imágenes anteriores de este mismo laptop (ficha editada u otra versión del dibujo)
                try (DirectoryStream<Path> old = Files.newDirectoryStream(dir, prefix + "*.png")) {
                    for (Path p : old) {
                        Files.deleteIfExists(p);
                    }
                }
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                out.setRGB(0, 0, w, h, px, 0, w);
                ImageIO.write(out, "png", file.toFile());
            } catch (IOException | RuntimeException ex) {
                log.warn("No se pudo guardar la imagen {}: {}", file.getFileName(), ex.getMessage());
            }
        });
    }

    /** Archivo de la imagen; null para un laptop aun sin guardar, que no tiene identificador estable. */
    private Path amjFile(AMJLaptop l, String key) {
        return l.getId() == null ? null : dir.resolve(key.replace('|', '_') + "_" + STYLE + ".png");
    }

    private static String amjKey(AMJLaptop l) {
        return (l.getId() == null ? "nuevo" + System.identityHashCode(l) : String.valueOf(l.getId())) + "|"
                + Integer.toHexString(Objects.hash(l.getBrand(), l.getModel(), l.getCategory(), l.getColorHex(), l.getWidthMm(),
                l.getDepthMm(), l.getThicknessMm(), l.getScreenInches(), l.getResolution(),
                // si cambia de donde sale su 3D (por ejemplo, pasa a usar fotos), la imagen se dibuja de nuevo
                com.laptopexplorer.viewer.AMJLaptopModel.amjExteriorInfo(l).kind()));
    }
}
