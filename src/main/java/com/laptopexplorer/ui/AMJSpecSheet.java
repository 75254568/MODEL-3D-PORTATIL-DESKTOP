package com.laptopexplorer.ui;

import com.laptopexplorer.model.AMJLaptop;
import com.laptopexplorer.service.AMJVerifiedSpecs;
import com.laptopexplorer.viewer.AMJLaptopModel;
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

/** Resumen de compra y ficha técnica completa de un laptop, reutilizados por la búsqueda y el visor 3D. */
public final class AMJSpecSheet {

    private AMJSpecSheet() {
    }

    /** Las siete especificaciones que se miran al comprar, como pares etiqueta/valor. */
    public static String[][] amjSummary(AMJLaptop l) {
        return new String[][]{
                {"Marca y modelo", l.amjGetDisplayName()},
                {"Procesador", AMJFmt.amjText(l.getCpu())},
                {"Memoria RAM", amjRam(l)},
                {"Almacenamiento", amjStorage(l)},
                {"Gráficos", amjGpu(l)},
                {"Pantalla", amjScreen(l)},
                {"Precio", AMJFmt.amjPrice(l.getPriceUsd(), l.getPriceEstimated())}};
    }

    public static String amjRam(AMJLaptop l) {
        String gb = AMJFmt.amjNum(l.getRamGb(), "GB");
        return l.getRamType() == null || l.getRamType().isBlank() || AMJFmt.ND.equals(gb) ? gb : gb + " " + l.getRamType();
    }

    public static String amjStorage(AMJLaptop l) {
        if (l.getStorageGb() == null || l.getStorageGb() <= 0) {
            return AMJFmt.ND;
        }
        int gb = l.getStorageGb();
        return (gb >= 1024 && gb % 1024 == 0 ? gb / 1024 + " TB" : gb + " GB") + " (SSD)";
    }

    public static String amjGpu(AMJLaptop l) {
        return AMJFmt.amjText(l.getGpu());
    }

    public static String amjScreen(AMJLaptop l) {
        StringBuilder sb = new StringBuilder(AMJFmt.amjNum(l.getScreenInches(), "in"));
        if (l.getResolution() != null && !l.getResolution().isBlank()) {
            sb.append(", ").append(l.getResolution());
        }
        if (l.getRefreshRateHz() != null && l.getRefreshRateHz() > 0) {
            sb.append(", ").append(l.getRefreshRateHz()).append(" Hz");
        }
        return sb.toString();
    }

    /** Ficha técnica completa en un diálogo. */
    public static void amjShow(Node owner, AMJLaptop l) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(l.amjGetDisplayName());
        dlg.setHeaderText(l.amjGetDisplayName() + " — ficha técnica");
        if (owner != null && owner.getScene() != null) {
            dlg.initOwner(owner.getScene().getWindow());
        }
        dlg.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(AMJSpecSheet.class.getResource("/css/app.css")).toExternalForm());
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ScrollPane sp = new ScrollPane(amjContent(l));
        sp.setFitToWidth(true);
        sp.setPrefViewportWidth(560);
        sp.setPrefViewportHeight(480);
        dlg.getDialogPane().setContent(sp);
        dlg.showAndWait();
    }

    /** Todas las especificaciones del laptop por secciones, para incrustar en cualquier panel. */
    public static VBox amjContent(AMJLaptop l) {
        VBox body = new VBox(10);
        body.setPadding(new Insets(4, 4, 4, 4));
        amjSection(body, "General", new String[][]{
                {"Marca", l.getBrand()}, {"Modelo", l.getModel()}, {"Categoría", l.getCategory()},
                {"Año", AMJFmt.amjNum(l.getReleaseYear(), "")}, {"Disponibilidad", AMJFmt.amjText(l.getAvailability())},
                {"Precio", AMJFmt.amjPrice(l.getPriceUsd(), l.getPriceEstimated())
                        + (Boolean.TRUE.equals(l.getPriceEstimated()) ? " (referencia, no verificado)" : "")}});
        amjSection(body, "Procesador", new String[][]{
                {"Modelo", AMJFmt.amjText(l.getCpu())}, {"Núcleos", AMJFmt.amjNum(l.getCpuCores(), "")},
                {"Potencia base", AMJFmt.amjNum(l.getCpuBaseWatts(), "W")}, {"Potencia máxima", AMJFmt.amjNum(l.getCpuBoostWatts(), "W")}});
        amjSection(body, "Gráficos", new String[][]{
                {"Modelo", amjGpu(l)}, {"VRAM", AMJFmt.amjNum(l.getGpuVramGb(), "GB")}, {"TGP", AMJFmt.amjNum(l.getGpuTgpWatts(), "W")}});
        amjSection(body, "Memoria y almacenamiento", new String[][]{
                {"RAM", amjRam(l)}, {"RAM ampliable", AMJFmt.amjBool(l.getRamUpgradable(), "Sí", "No, soldada")},
                {"Almacenamiento", amjStorage(l)}, {"Ranuras M.2", AMJFmt.amjNum(l.getStorageSlots(), "")}});
        amjSection(body, "Pantalla", new String[][]{
                {"Tamaño", AMJFmt.amjNum(l.getScreenInches(), "in")}, {"Resolución", AMJFmt.amjText(l.getResolution())},
                {"Frecuencia", AMJFmt.amjNum(l.getRefreshRateHz(), "Hz")}, {"Panel", AMJFmt.amjText(l.getPanelType())}});
        amjSection(body, "Chasis y batería", new String[][]{
                {"Material", AMJFmt.amjText(l.getChassisMaterial())}, {"Ancho", AMJFmt.amjNum(l.getWidthMm(), "mm")},
                {"Fondo", AMJFmt.amjNum(l.getDepthMm(), "mm")}, {"Grosor", AMJFmt.amjNum(l.getThicknessMm(), "mm")},
                {"Peso", AMJFmt.amjNum(l.getWeightKg(), "kg")}, {"Batería", AMJFmt.amjNum(l.getBatteryWh(), "Wh")}});
        AMJLaptopModel.AMJInteriorInfo info = AMJLaptopModel.amjInteriorInfo(l);
        amjSection(body, "Modelo 3D", new String[][]{
                {"Exterior", com.laptopexplorer.service.AMJModelStore.amjFileFor(l) != null ? "Modelo 3D importado (OBJ)"
                        : AMJLaptopModel.amjExteriorInfo(l).amjDetail()},
                {"Interior", info.reconstructed() ? "Reconstruido pieza por pieza" : "Aproximado, sin reconstruir"}});
        AMJLaptopModel.AMJBlenderInfo blender = AMJLaptopModel.amjBlenderInfo(l);
        if (blender != null) {
            amjSection(body, "Fidelidad del modelo 3D", new String[][]{
                    {"Puertos colocados", blender.portsPlaced() + " con su lado confirmado"},
                    {"Rasgos confirmados", amjList(blender.verified())},
                    {"Puertos sin colocar", amjList(blender.portsNotPlaced())},
                    {"Valores supuestos", amjList(blender.assumed())},
                    {"Sin información", amjList(blender.notVerified())}});
        }
        AMJVerifiedSpecs.AMJFicha ficha = AMJVerifiedSpecs.amjOf(l);
        if (ficha != null) {
            amjSection(body, "Verificación de la ficha", new String[][]{
                    {"Configuración", AMJFmt.amjText(ficha.config())},
                    {"Sin confirmar", amjList(ficha.notVerified())},
                    {"Discrepancias", ficha.conflicts().isEmpty() ? "Ninguna detectada" : String.join("\n", ficha.conflicts())},
                    {"Fuentes", String.join("\n", ficha.sources())}});
        }
        amjSection(body, "Origen de los datos", new String[][]{
                {"Fuente", AMJFmt.amjText(l.getSourceUrl())}, {"Notas", AMJFmt.amjText(l.getNotes())}});
        return body;
    }

    private static String amjList(java.util.List<String> items) {
        return items.isEmpty() ? "Ninguno" : String.join(", ", items);
    }

    private static void amjSection(VBox into, String title, String[][] rows) {
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
