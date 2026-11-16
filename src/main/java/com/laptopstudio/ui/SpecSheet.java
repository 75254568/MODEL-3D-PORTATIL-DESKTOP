package com.laptopstudio.ui;

import com.laptopstudio.model.Laptop;
import com.laptopstudio.viewer.LaptopModel;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/** Resumen de compra y ficha tecnica completa de un laptop, reutilizados por la busqueda y el visor 3D. */
public final class SpecSheet {

    private SpecSheet() {
    }

    /** Las siete especificaciones que se miran al comprar, como pares etiqueta/valor. */
    public static String[][] summary(Laptop l) {
        return new String[][]{
                {"Marca y modelo", l.getDisplayName()},
                {"Procesador", Fmt.text(l.getCpu())},
                {"Memoria RAM", ram(l)},
                {"Almacenamiento", storage(l)},
                {"Gráficos", gpu(l)},
                {"Pantalla", screen(l)},
                {"Precio", Fmt.price(l.getPriceUsd(), l.getPriceEstimated())}};
    }

    public static String ram(Laptop l) {
        String gb = Fmt.num(l.getRamGb(), "GB");
        return l.getRamType() == null || l.getRamType().isBlank() || Fmt.ND.equals(gb) ? gb : gb + " " + l.getRamType();
    }

    public static String storage(Laptop l) {
        if (l.getStorageGb() == null || l.getStorageGb() <= 0) {
            return Fmt.ND;
        }
        int gb = l.getStorageGb();
        return (gb >= 1024 && gb % 1024 == 0 ? gb / 1024 + " TB" : gb + " GB") + " (SSD)";
    }

    public static String gpu(Laptop l) {
        return Fmt.text(l.getGpu());
    }

    public static String screen(Laptop l) {
        StringBuilder sb = new StringBuilder(Fmt.num(l.getScreenInches(), "in"));
        if (l.getResolution() != null && !l.getResolution().isBlank()) {
            sb.append(", ").append(l.getResolution());
        }
        if (l.getRefreshRateHz() != null && l.getRefreshRateHz() > 0) {
            sb.append(", ").append(l.getRefreshRateHz()).append(" Hz");
        }
        return sb.toString();
    }

    /** Ficha tecnica completa en un dialogo. */
    public static void show(Node owner, Laptop l) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(l.getDisplayName());
        dlg.setHeaderText(l.getDisplayName() + " — ficha técnica");
        if (owner != null && owner.getScene() != null) {
            dlg.initOwner(owner.getScene().getWindow());
        }
        dlg.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(SpecSheet.class.getResource("/css/app.css")).toExternalForm());
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ScrollPane sp = new ScrollPane(content(l));
        sp.setFitToWidth(true);
        sp.setPrefViewportWidth(560);
        sp.setPrefViewportHeight(480);
        dlg.getDialogPane().setContent(sp);
        dlg.showAndWait();
    }

    /** Todas las especificaciones del laptop por secciones, para incrustar en cualquier panel. */
    public static VBox content(Laptop l) {
        VBox body = new VBox(10);
        body.setPadding(new Insets(4, 4, 4, 4));
        section(body, "General", new String[][]{
                {"Marca", l.getBrand()}, {"Modelo", l.getModel()}, {"Categoría", l.getCategory()},
                {"Año", Fmt.num(l.getReleaseYear(), "")}, {"Disponibilidad", Fmt.text(l.getAvailability())},
                {"Precio", Fmt.price(l.getPriceUsd(), l.getPriceEstimated())
                        + (Boolean.TRUE.equals(l.getPriceEstimated()) ? " (referencia, no verificado)" : "")}});
        section(body, "Procesador", new String[][]{
                {"Modelo", Fmt.text(l.getCpu())}, {"Núcleos", Fmt.num(l.getCpuCores(), "")},
                {"Potencia base", Fmt.num(l.getCpuBaseWatts(), "W")}, {"Potencia máxima", Fmt.num(l.getCpuBoostWatts(), "W")}});
        section(body, "Gráficos", new String[][]{
                {"Modelo", gpu(l)}, {"VRAM", Fmt.num(l.getGpuVramGb(), "GB")}, {"TGP", Fmt.num(l.getGpuTgpWatts(), "W")}});
        section(body, "Memoria y almacenamiento", new String[][]{
                {"RAM", ram(l)}, {"RAM ampliable", Fmt.bool(l.getRamUpgradable(), "Sí", "No, soldada")},
                {"Almacenamiento", storage(l)}, {"Ranuras M.2", Fmt.num(l.getStorageSlots(), "")}});
        section(body, "Pantalla", new String[][]{
                {"Tamaño", Fmt.num(l.getScreenInches(), "in")}, {"Resolución", Fmt.text(l.getResolution())},
                {"Frecuencia", Fmt.num(l.getRefreshRateHz(), "Hz")}, {"Panel", Fmt.text(l.getPanelType())}});
        section(body, "Chasis y batería", new String[][]{
                {"Material", Fmt.text(l.getChassisMaterial())}, {"Ancho", Fmt.num(l.getWidthMm(), "mm")},
                {"Fondo", Fmt.num(l.getDepthMm(), "mm")}, {"Grosor", Fmt.num(l.getThicknessMm(), "mm")},
                {"Peso", Fmt.num(l.getWeightKg(), "kg")}, {"Batería", Fmt.num(l.getBatteryWh(), "Wh")}});
        LaptopModel.InteriorInfo info = LaptopModel.interiorInfo(l);
        section(body, "Modelo 3D", new String[][]{
                {"Exterior", com.laptopstudio.service.ModelStore.fileFor(l) != null ? "Modelo 3D importado (OBJ)" : "Generado por la aplicación"},
                {"Interior", info.reconstructed() ? "Reconstruido pieza por pieza" : "Aproximado, sin reconstruir"}});
        section(body, "Origen de los datos", new String[][]{
                {"Fuente", Fmt.text(l.getSourceUrl())}, {"Notas", Fmt.text(l.getNotes())}});
        return body;
    }

    private static void section(VBox into, String title, String[][] rows) {
        Label h = new Label(title.toUpperCase());
        h.getStyleClass().add("section-label");
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(4);
        ColumnConstraints c0 = new ColumnConstraints(140);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        int r = 0;
        for (String[] row : rows) {
            Label k = new Label(row[0]);
            k.getStyleClass().add("muted");
            Label v = new Label(row[1]);
            v.setWrapText(true);
            g.add(k, 0, r);
            g.add(v, 1, r++);
        }
        into.getChildren().addAll(h, g);
    }
}
