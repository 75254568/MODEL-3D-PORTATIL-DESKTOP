package com.laptopstudio.ui;

import com.laptopstudio.model.Laptop;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Tarjeta de producto compartida por la busqueda y el catalogo: marca y modelo, precio y las especificaciones
 * de compra en una rejilla de etiqueta/valor. Todos los datos salen del propio {@link Laptop}, sin adornos.
 */
public final class LaptopCard {

    /** Acciones de la tarjeta. Las que sean nulas no muestran boton. */
    public record Actions(Consumer<Laptop> ver3d, Consumer<Laptop> similares, Consumer<Laptop> abrir) {
    }

    private LaptopCard() {
    }

    /**
     * @param note     texto opcional bajo las especificaciones (por que no cumple, en que se parece)
     * @param noteGood verdadero para pintar la nota como positiva; falso, como advertencia
     */
    public static VBox build(Laptop l, String note, boolean noteGood, SelectionState selection, Actions actions) {
        Label marca = new Label(l.getBrand().toUpperCase() + "  ·  " + l.getCategory()
                + (l.getReleaseYear() == null ? "" : "  ·  " + l.getReleaseYear()));
        marca.getStyleClass().add("section-label");
        javafx.scene.Node cabecera = marca;
        if ("Anunciado".equalsIgnoreCase(l.getAvailability())) {
            // un equipo solo anunciado no se puede comprar todavia: se distingue a simple vista
            Label anunciado = new Label("Anunciado, aún no a la venta");
            anunciado.getStyleClass().addAll("tag", "warn");
            cabecera = new HBox(8, marca, anunciado);
        }
        Label nombre = new Label(l.getModel());
        nombre.getStyleClass().add("model-name");
        nombre.setWrapText(true);
        Label precio = new Label(Fmt.price(l.getPriceUsd(), l.getPriceEstimated()));
        precio.getStyleClass().add("price");

        GridPane specs = new GridPane();
        specs.setHgap(10);
        specs.setVgap(5);
        ColumnConstraints c0 = new ColumnConstraints(104);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        specs.getColumnConstraints().addAll(c0, c1);
        String[][] filas = {{"Procesador", Fmt.text(l.getCpu())}, {"Gráficos", SpecSheet.gpu(l)},
                {"Memoria RAM", SpecSheet.ram(l)}, {"Almacenamiento", SpecSheet.storage(l)}, {"Pantalla", SpecSheet.screen(l)}};
        for (int i = 0; i < filas.length; i++) {
            Label k = new Label(filas[i][0]);
            k.getStyleClass().addAll("muted", "small");
            Label v = new Label(filas[i][1]);
            v.getStyleClass().add("small");
            v.setWrapText(true);
            specs.add(k, 0, i);
            specs.add(v, 1, i);
        }

        VBox card = new VBox(6, cabecera, nombre, precio, specs);
        if (note != null && !note.isBlank()) {
            Label n = new Label(note);
            n.getStyleClass().addAll("tag", noteGood ? "ok" : "warn");
            n.setWrapText(true);
            card.getChildren().add(n);
        }

        HBox acciones = new HBox(6);
        acciones.setPadding(new Insets(6, 0, 0, 0));
        if (actions.ver3d() != null) {
            Button b = boton("Ver en 3D", acciones);
            b.getStyleClass().add("primary");
            b.setOnAction(e -> actions.ver3d().accept(l));
        }
        Button ficha = boton(actions.abrir() != null ? "Editar" : "Ficha", acciones);
        ficha.setOnAction(e -> {
            if (actions.abrir() != null) {
                actions.abrir().accept(l);
            } else {
                SpecSheet.show(card, l);
            }
        });
        if (actions.similares() != null) {
            boton("Similares", acciones).setOnAction(e -> actions.similares().accept(l));
        }
        Button comparar = boton(selection.isMarked(l) ? "Quitar" : "Comparar", acciones);
        comparar.setOnAction(e -> {
            boolean marcar = !selection.isMarked(l);
            if (selection.setMarked(l, marcar)) {
                comparar.setText(marcar ? "Quitar" : "Comparar");
            } else {
                selection.say("Solo se pueden comparar " + SelectionState.MAX_COMPARE + " laptops a la vez");
            }
        });
        Region empuje = new Region();
        VBox.setVgrow(empuje, Priority.ALWAYS);
        card.getChildren().addAll(empuje, acciones);
        card.getStyleClass().add("result-card");
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setPrefWidth(396);
        card.setMinHeight(262);
        Motion.hoverLift(card);
        return card;
    }

    private static Button boton(String texto, HBox into) {
        Button b = new Button(texto);
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
