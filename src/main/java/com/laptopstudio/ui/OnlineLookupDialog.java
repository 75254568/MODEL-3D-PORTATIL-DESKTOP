package com.laptopstudio.ui;

import com.laptopstudio.exception.NegocioException;
import com.laptopstudio.model.Laptop;
import com.laptopstudio.service.LaptopService;
import com.laptopstudio.service.ModelStore;
import com.laptopstudio.service.OnlineLookupService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Dialogo de consulta en internet: busca un laptop por marca, modelo o caracteristicas, muestra la ficha
 * encontrada con sus fuentes para que el usuario la revise y, si acepta, la incorpora al catalogo local
 * (y descarga su modelo 3D cuando la consulta encontro uno). La red se usa siempre en segundo plano.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OnlineLookupDialog {

    private final OnlineLookupService lookup;
    private final LaptopService laptops;
    private final ModelStore models;
    private final SelectionState selection;

    public void open(Node owner, String hint, Consumer<Laptop> onSaved) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Buscar en internet");
        dlg.setHeaderText("Buscar un laptop en internet");
        if (owner != null && owner.getScene() != null) {
            dlg.initOwner(owner.getScene().getWindow());
        }
        dlg.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextField query = new TextField(hint == null ? "" : hint);
        query.setPromptText("Marca y modelo, por ejemplo: Lenovo Legion 5i Gen 9 RTX 4060");
        HBox.setHgrow(query, Priority.ALWAYS);
        Button go = new Button("Buscar");
        go.getStyleClass().add("primary");
        ProgressIndicator spin = new ProgressIndicator();
        spin.setPrefSize(22, 22);
        spin.setVisible(false);
        HBox top = new HBox(8, query, go, spin);
        top.setAlignment(Pos.CENTER_LEFT);

        Label status = new Label(lookup.hasKey()
                ? "Se consultan fichas del fabricante y análisis técnicos. Puede tardar hasta un minuto."
                : "Falta la clave de la API de Claude. Escríbela en Ajustes > Consulta en internet.");
        status.getStyleClass().add("muted");
        status.setWrapText(true);

        VBox result = new VBox(10);
        ScrollPane scroll = new ScrollPane(result);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(600);
        scroll.setPrefViewportHeight(420);

        VBox body = new VBox(10, top, status, scroll);
        body.setPadding(new Insets(4));
        dlg.getDialogPane().setContent(body);

        Runnable search = () -> {
            result.getChildren().clear();
            go.setDisable(true);
            spin.setVisible(true);
            status.setText("Consultando en internet…");
            String q = query.getText();
            Task<OnlineLookupService.Found> task = new Task<>() {
                @Override
                protected OnlineLookupService.Found call() {
                    return lookup.buscar(q);
                }
            };
            task.setOnSucceeded(e -> {
                go.setDisable(false);
                spin.setVisible(false);
                status.setText("Revisa los datos antes de incorporarlos. Podrás corregirlos después en el Catálogo.");
                showFound(result, task.getValue(), dlg, onSaved);
            });
            task.setOnFailed(e -> {
                go.setDisable(false);
                spin.setVisible(false);
                Throwable ex = task.getException();
                status.setText(ex instanceof NegocioException ? ex.getMessage() : "La consulta falló: " + ex.getMessage());
            });
            background(task);
        };
        go.setOnAction(e -> search.run());
        query.setOnAction(e -> search.run());
        go.setDisable(!lookup.hasKey());
        dlg.showAndWait();
    }

    private void showFound(VBox into, OnlineLookupService.Found f, Dialog<Void> dlg, Consumer<Laptop> onSaved) {
        Laptop l = f.laptop();
        Label name = new Label(l.getDisplayName());
        name.getStyleClass().add("model-name");
        Label match = new Label(f.exactMatch() ? "Coincide con el modelo pedido" : "Sin confirmar que sea exactamente el modelo pedido");
        match.getStyleClass().addAll("tag", f.exactMatch() ? "ok" : "warn");
        Label note = new Label(f.note());
        note.getStyleClass().addAll("muted", "small");
        note.setWrapText(true);

        VBox sources = new VBox(2);
        Label st = new Label("FUENTES");
        st.getStyleClass().add("section-label");
        sources.getChildren().add(st);
        for (String s : f.sources()) {
            TextField link = new TextField(s);            // campo de solo lectura: permite seleccionar y copiar
            link.setEditable(false);
            sources.getChildren().add(link);
        }

        // El enlace del modelo 3D sale de una pagina web, no del usuario: se muestra entero, con su servidor, y la
        // descarga queda desmarcada hasta que el usuario la elija.
        CheckBox with3d = new CheckBox("Descargar también su modelo 3D (.obj) desde este enlace");
        with3d.setSelected(false);
        VBox box3d = new VBox(4, with3d);
        if (f.model3dUrl() != null) {
            TextField link3d = new TextField(f.model3dUrl());
            link3d.setEditable(false);
            Label aviso3d = new Label("Servidor: " + host(f.model3dUrl()) + ". El enlace lo propuso la búsqueda y no "
                    + "está verificado: márcalo solo si reconoces el sitio. También puedes importar un .obj después, desde el Catálogo.");
            aviso3d.getStyleClass().addAll("muted", "small");
            aviso3d.setWrapText(true);
            box3d.getChildren().addAll(link3d, aviso3d);
        }
        Label no3d = new Label("No se encontró un modelo 3D descargable de este equipo. El visor mostrará la "
                + "representación generada por la aplicación, marcada como aproximada. Puedes importar un .obj después.");
        no3d.getStyleClass().addAll("muted", "small");
        no3d.setWrapText(true);

        Button add = new Button("Agregar al catálogo");
        add.getStyleClass().add("primary");
        Label saveMsg = new Label();
        saveMsg.setWrapText(true);
        add.setOnAction(e -> {
            try {
                Laptop saved = laptops.guardar(l);
                selection.say("Incorporado al catálogo: " + saved.getDisplayName());
                if (f.model3dUrl() != null && with3d.isSelected()) {
                    add.setDisable(true);
                    saveMsg.setText("Guardado. Descargando el modelo 3D…");
                    Task<ModelStore.Report> t = new Task<>() {
                        @Override
                        protected ModelStore.Report call() {
                            return models.importUrl(saved, f.model3dUrl());
                        }
                    };
                    t.setOnSucceeded(ev -> {
                        String w = t.getValue().warning();
                        selection.say("Modelo 3D descargado para " + saved.getDisplayName());
                        if (w != null) {
                            Dialogs.warn(add, "Revisa el modelo 3D", w);
                        }
                        dlg.close();
                        onSaved.accept(saved);
                    });
                    t.setOnFailed(ev -> {
                        Dialogs.warn(add, "El laptop se guardó, pero sin modelo 3D", t.getException().getMessage());
                        dlg.close();
                        onSaved.accept(saved);
                    });
                    background(t);
                } else {
                    dlg.close();
                    onSaved.accept(saved);
                }
            } catch (NegocioException ex) {
                saveMsg.setText(ex.getMessage());
            } catch (ConstraintViolationException ex) {
                saveMsg.setText("No se puede guardar: " + ex.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage).findFirst().orElse("datos no válidos"));
            } catch (RuntimeException ex) {
                // cualquier otro fallo (por ejemplo, de la base de datos) se dice aqui y queda en el registro
                log.error("No se pudo incorporar {} al catálogo", l.getDisplayName(), ex);
                saveMsg.setText("No se pudo guardar: " + ex.getMessage());
            }
        });

        into.getChildren().addAll(name, match, note, SpecSheet.content(l), sources,
                f.model3dUrl() != null ? box3d : no3d, new HBox(10, add, saveMsg));
    }

    /** Servidor de un enlace, para que el usuario vea de donde se descargaria. */
    private static String host(String url) {
        try {
            String h = java.net.URI.create(url.trim()).getHost();
            return h == null ? "desconocido" : h;
        } catch (IllegalArgumentException e) {
            return "desconocido";
        }
    }

    private static void background(Task<?> task) {
        Thread t = new Thread(task, "consulta-internet");
        t.setDaemon(true);
        t.start();
    }
}
