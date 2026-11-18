package com.laptopstudio.ui;

import com.laptopstudio.model.Laptop;
import com.laptopstudio.viewer.LaptopModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tarjeta de producto compartida por la busqueda y el catalogo, al estilo de una tienda: arriba la imagen del
 * laptop (su propio modelo 3D) con las especificaciones clave superpuestas en etiquetas; debajo la marca, el
 * modelo, el precio y las acciones. Todos los datos salen del propio {@link Laptop}, sin adornos.
 */
public final class LaptopCard {

    /** Ancho de la tarjeta: el de su imagen. */
    public static final double WIDTH = LaptopThumbs.WIDTH;

    /**
     * Acciones de la tarjeta. Las que sean nulas no muestran boton. {@code abrir} se ejecuta al hacer clic en
     * cualquier punto de la tarjeta que no sea un boton (en el catalogo, elegirla para editar su ficha).
     */
    public record Actions(Consumer<Laptop> ver3d, Consumer<Laptop> similares, Consumer<Laptop> abrir) {
    }

    private static final Pattern CPU = Pattern.compile(
            "(Core Ultra X?\\d \\w+(?: Plus)?|Core Ultra X?\\d|Core i\\d[- ]\\w+|Ryzen AI \\d+(?: \\w+)? \\d+|Ryzen \\d \\w+|"
                    + "M\\d(?: Pro| Max| Ultra)?|Snapdragon X\\d? \\w+|A\\d+ Pro)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GPU = Pattern.compile(
            "(RTX PRO \\d{4}|RTX \\d{4}(?: Ti)?(?: Ada)?|GTX \\d{3,4}(?: Ti)?|RX \\d{4}\\w*|Arc \\w+|Radeon \\d{3}M)", Pattern.CASE_INSENSITIVE);

    private LaptopCard() {
    }

    /**
     * @param note     texto opcional bajo el precio (por que no cumple, en que se parece)
     * @param noteGood verdadero para pintar la nota como positiva; falso, como advertencia
     */
    public static VBox build(Laptop l, String note, boolean noteGood, SelectionState selection, Actions actions,
                             LaptopThumbs thumbs) {
        VBox card = new VBox(media(l, thumbs), info(l, note, noteGood, selection, actions));
        card.getStyleClass().addAll("result-card", "product-card");
        card.setPrefWidth(WIDTH);
        card.setMinWidth(WIDTH);
        card.setMaxWidth(WIDTH);
        if (actions.abrir() != null) {
            // un clic en un boton de la tarjeta hace lo suyo; no debe ademas elegir la tarjeta
            card.setOnMouseClicked(e -> {
                if (!insideButton(e.getTarget(), card)) {
                    actions.abrir().accept(l);
                }
            });
        }
        Motion.hoverLift(card);
        return card;
    }

    private static boolean insideButton(Object target, Node card) {
        for (Node n = target instanceof Node node ? node : null; n != null && n != card; n = n.getParent()) {
            if (n instanceof javafx.scene.control.ButtonBase) {
                return true;
            }
        }
        return false;
    }

    /* ------------------------------------------------------------------ imagen con etiquetas */

    private static StackPane media(Laptop l, LaptopThumbs thumbs) {
        ImageView image = new ImageView();
        image.setFitWidth(LaptopThumbs.WIDTH);
        image.setFitHeight(LaptopThumbs.HEIGHT);
        image.setPreserveRatio(true);
        image.setSmooth(true);
        // mientras se dibuja su modelo 3D, la tarjeta muestra un indicador en el sitio de la imagen
        javafx.scene.control.ProgressIndicator wait = new javafx.scene.control.ProgressIndicator();
        wait.setMaxSize(26, 26);
        wait.setMouseTransparent(true);
        Image ready = thumbs.request(l, img -> {
            image.setImage(img);
            wait.setVisible(false);
        });
        if (ready != null) {
            image.setImage(ready);
            wait.setVisible(false);
        }

        VBox left = pills(Pos.TOP_LEFT, screenBadge(l), l.getRamGb() == null || l.getRamGb() <= 0 ? null : l.getRamGb() + " GB RAM",
                storageBadge(l));
        VBox right = pills(Pos.TOP_RIGHT, cpuBadge(l), l.getCpuCores() == null || l.getCpuCores() <= 0 ? null : l.getCpuCores() + " núcleos",
                gpuBadge(l));

        // la imagen se apoya abajo; las etiquetas quedan arriba, sobre la pantalla, como en una foto de tienda
        StackPane.setAlignment(image, Pos.BOTTOM_CENTER);
        StackPane.setMargin(image, new Insets(0, 0, 4, 0));
        StackPane media = new StackPane(wait, image, left, right);
        if ("Anunciado".equalsIgnoreCase(l.getAvailability())) {
            // un equipo solo anunciado no se puede comprar todavia: se distingue a simple vista
            Label soon = new Label("Anunciado");
            soon.getStyleClass().addAll("tag", "warn", "media-tag");
            StackPane.setAlignment(soon, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(soon, new Insets(0, 8, 7, 0));
            media.getChildren().add(soon);
        }
        media.getStyleClass().add("product-media");
        media.setMinHeight(LaptopThumbs.HEIGHT + 8);
        media.setMaxHeight(LaptopThumbs.HEIGHT + 8);
        return media;
    }

    /** Columna de etiquetas superpuesta a la imagen; las que no tienen dato no se dibujan. */
    private static VBox pills(Pos corner, String... texts) {
        VBox box = new VBox(3);
        box.setAlignment(corner);
        for (String t : texts) {
            if (t != null && !t.isBlank()) {
                Label p = new Label(t);
                p.getStyleClass().add("spec-pill");
                p.setMaxWidth(LaptopThumbs.WIDTH * 0.47);
                box.getChildren().add(p);
            }
        }
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setMouseTransparent(true);
        StackPane.setAlignment(box, corner);
        StackPane.setMargin(box, new Insets(8));
        return box;
    }

    private static String screenBadge(Laptop l) {
        if (l.getScreenInches() == null || l.getScreenInches() <= 0) {
            return null;
        }
        String size = Fmt.num(l.getScreenInches(), "").trim() + "\"";
        return l.getResolution() == null || l.getResolution().isBlank() ? size : size + " " + l.getResolution().replace("x", "×");
    }

    private static String storageBadge(Laptop l) {
        if (l.getStorageGb() == null || l.getStorageGb() <= 0) {
            return null;
        }
        int gb = l.getStorageGb();
        return (gb >= 1024 && gb % 1024 == 0 ? gb / 1024 + " TB" : gb + " GB") + " SSD";
    }

    /** Familia y modelo del procesador sin la marca ("Core Ultra 9 290HX Plus"); el texto entero si no se reconoce. */
    static String cpuBadge(Laptop l) {
        return shortName(l.getCpu(), CPU);
    }

    /** Modelo de la grafica ("RTX 5090"); "Gráficos integrados" si no es dedicada. */
    static String gpuBadge(Laptop l) {
        String g = l.getGpu();
        if (g != null && GPU.matcher(g).find()) {
            return shortName(g, GPU);
        }
        return g != null && g.toLowerCase(Locale.ROOT).contains("integr") ? "Gráficos integrados" : shortName(g, GPU);
    }

    private static String shortName(String full, Pattern known) {
        if (full == null || full.isBlank()) {
            return null;
        }
        Matcher m = known.matcher(full);
        if (m.find()) {
            return m.group(1);
        }
        String s = full.replaceFirst("(?i)^(intel|amd|apple|qualcomm|nvidia)\\s+", "");
        return s.length() > 24 ? s.substring(0, 23) + "…" : s;
    }

    /* ------------------------------------------------------------------ datos y acciones */

    private static VBox info(Laptop l, String note, boolean noteGood, SelectionState selection, Actions actions) {
        Label brand = new Label(String.valueOf(l.getBrand()).toUpperCase(Locale.ROOT));
        brand.getStyleClass().add("product-brand");
        brand.setMinWidth(Region.USE_PREF_SIZE);
        // de donde sale el aspecto de su modelo 3D: fotos del equipo, de su familia o rasgos de su serie
        LaptopModel.ExteriorInfo exterior = LaptopModel.exteriorInfo(l);
        Label origin = new Label("3D: " + exterior.label().toLowerCase(Locale.ROOT));
        origin.getStyleClass().addAll("origin-tag", exterior.kind() == LaptopModel.Exterior.PHOTOS || exterior.kind() == LaptopModel.Exterior.BLENDER ? "ok" : "approx");
        origin.setMinWidth(0);
        Tooltip.install(origin, new Tooltip(exterior.detail()));
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox head = new HBox(6, brand, origin, grow, compareToggle(l, selection));
        head.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(l.getModel());
        title.getStyleClass().add("product-title");
        title.setWrapText(true);
        title.setAlignment(Pos.TOP_LEFT);
        title.setMinHeight(58);                      // tres lineas: todas las tarjetas de una fila miden lo mismo
        title.setMaxHeight(58);
        title.setTooltip(new Tooltip(l.getDisplayName()));

        Label kind = new Label(l.getCategory() + (l.getReleaseYear() == null ? "" : "  ·  " + l.getReleaseYear()));
        kind.getStyleClass().addAll("muted", "small");

        Label price = new Label(Money.format(l.getPriceUsd(), null));
        price.getStyleClass().add("price");
        HBox priceRow = new HBox(8, price);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        if (Fmt.ND.equals(price.getText())) {
            price.setText("Sin precio");
            price.getStyleClass().add("no-price");
        } else if (Boolean.TRUE.equals(l.getPriceEstimated())) {
            Label approx = new Label("aprox.");
            approx.getStyleClass().add("tag");
            Tooltip.install(approx, new Tooltip("Precio de referencia, no verificado"));
            priceRow.getChildren().add(approx);
        }

        VBox box = new VBox(5, head, title, kind, priceRow);
        if (note != null && !note.isBlank()) {
            Label n = new Label(note);
            n.getStyleClass().addAll("tag", noteGood ? "ok" : "warn");
            n.setWrapText(true);
            n.setMaxWidth(WIDTH - 24);
            box.getChildren().add(n);
        }

        HBox buttons = new HBox(6);
        buttons.setPadding(new Insets(4, 0, 0, 0));
        if (actions.ver3d() != null) {
            Button b = button("Ver en 3D", buttons);
            b.getStyleClass().add("primary");
            b.setOnAction(e -> actions.ver3d().accept(l));
        }
        button("Ficha", buttons).setOnAction(e -> SpecSheet.show(box, l));
        if (actions.similares() != null) {
            button("Similares", buttons).setOnAction(e -> actions.similares().accept(l));
        }
        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);
        box.getChildren().addAll(push, buttons);
        box.setPadding(new Insets(10, 12, 12, 12));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    /** Boton redondo para marcar o desmarcar el laptop para comparar (el corazon de una tienda, aqui con otro fin). */
    private static Node compareToggle(Laptop l, SelectionState selection) {
        Button b = new Button();
        b.getStyleClass().add("compare-toggle");
        Tooltip tip = new Tooltip();
        b.setTooltip(tip);
        Runnable paint = () -> {
            boolean on = selection.isMarked(l);
            b.setGraphic(Icons.of(on ? Icons.CHECK : Icons.COMPARE, 15));
            b.getStyleClass().remove("on");
            if (on) {
                b.getStyleClass().add("on");
            }
            tip.setText(on ? "Quitar de la comparación" : "Marcar para comparar");
            b.setAccessibleText(tip.getText() + ": " + l.getDisplayName());
        };
        paint.run();
        b.setOnAction(e -> {
            if (!selection.setMarked(l, !selection.isMarked(l))) {
                selection.say("Solo se pueden comparar " + SelectionState.MAX_COMPARE + " laptops a la vez");
            }
            paint.run();
            e.consume();
        });
        return b;
    }

    private static Button button(String text, HBox into) {
        Button b = new Button(text);
        b.setMinWidth(Region.USE_PREF_SIZE);
        b.getStyleClass().add("compact");
        into.getChildren().add(b);
        return b;
    }

    /** Une notas cortas en una sola linea con un prefijo. */
    public static String note(String prefix, List<String> notes) {
        return notes == null || notes.isEmpty() ? null : prefix + String.join("; ", notes);
    }
}
