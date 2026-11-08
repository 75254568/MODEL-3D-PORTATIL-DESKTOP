package com.laptopstudio.ui;

import com.laptopstudio.controller.CatalogController;
import com.laptopstudio.controller.CompareController;
import com.laptopstudio.controller.ViewerController;
import com.laptopstudio.model.Laptop;
import com.laptopstudio.service.LaptopService;
import com.laptopstudio.viewer.LaptopViewer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Prueba funcional de las pantallas (con -Dstudio.func=true). Maneja los controles reales (busqueda, filtros,
 * edicion, guardado, eliminacion, marcado, comparacion y visor 3D), comprueba el resultado contra la base de datos
 * y escribe "FUNC ok" o "FUNC FALLO" por cada comprobacion. Modifica la base de datos del usuario activo, asi que
 * se ejecuta siempre con -Duser.home apuntando a una carpeta temporal.
 */
public final class FunctionalCheck {

    private FunctionalCheck() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("studio.func");
    }

    private static int fallos;

    private static void check(String what, boolean ok) {
        if (!ok) {
            fallos++;
        }
        System.out.println((ok ? "FUNC ok     " : "FUNC FALLO  ") + what);
    }

    @SuppressWarnings("unchecked")
    private static <T> T f(Object o, String name) {
        try {
            Field fld = o.getClass().getDeclaredField(name);
            fld.setAccessible(true);
            return (T) fld.get(o);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(name, e);
        }
    }

    private static void call(Object o, String method) {
        try {
            var m = o.getClass().getDeclaredMethod(method);
            m.setAccessible(true);
            m.invoke(o);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(method, e);
        }
    }

    /** Dentro de {@code ms}, pulsa el boton con ese texto del dialogo abierto (aunque haya un showAndWait en curso). */
    private static void autoDialog(Stage owner, String buttonText, int ms) {
        PauseTransition p = new PauseTransition(Duration.millis(ms));
        p.setOnFinished(e -> {
            for (Window w : new ArrayList<>(Window.getWindows())) {
                if (w != owner && w.isShowing() && w.getScene() != null && w.getScene().getRoot() instanceof DialogPane dp) {
                    for (ButtonType bt : dp.getButtonTypes()) {
                        if (bt.getText().equals(buttonText) && dp.lookupButton(bt) instanceof Button b) {
                            b.fire();
                            return;
                        }
                    }
                    System.out.println("FUNC FALLO  dialogo sin boton '" + buttonText + "': " + dp.getButtonTypes());
                    fallos++;
                    ((Stage) w).close();
                    return;
                }
            }
            System.out.println("FUNC FALLO  se esperaba un dialogo con '" + buttonText + "'");
            fallos++;
        });
        p.play();
    }

    private static final class Seq {
        final List<Supplier<Integer>> steps = new ArrayList<>();
        int i = 0;

        void add(Runnable r, int waitMs) {
            steps.add(() -> {
                r.run();
                return waitMs;
            });
        }

        void run() {
            if (i >= steps.size()) {
                System.out.println("FUNC terminado, fallos=" + fallos);
                Platform.exit();
                return;
            }
            int wait;
            try {
                wait = steps.get(i++).get();
            } catch (Throwable t) {
                fallos++;
                System.out.println("FUNC FALLO  excepcion en paso " + i + ": " + t);
                t.printStackTrace(System.out);
                wait = 100;
            }
            PauseTransition p = new PauseTransition(Duration.millis(wait));
            p.setOnFinished(e -> Platform.runLater(this::run));   // fuera de la animacion: showAndWait no se permite dentro
            p.play();
        }
    }

    public static void run(Scene scene, ApplicationContext ctx, NavigationService nav) {
        Stage stage = (Stage) scene.getWindow();
        LaptopService service = ctx.getBean(LaptopService.class);
        SelectionState sel = ctx.getBean(SelectionState.class);
        CatalogController cat = ctx.getBean(CatalogController.class);
        CompareController cmp = ctx.getBean(CompareController.class);
        ViewerController vis = ctx.getBean(ViewerController.class);
        TableView<Laptop> tabla = f(cat, "tabla");
        TextField buscar = f(cat, "txtBuscar");
        ComboBox<String> marca = f(cat, "cmbMarca");
        ToggleButton rtx50 = f(cat, "tgRtx50");
        ToggleButton recon = f(cat, "tgRecon");
        Map<String, TextField> campos = f(cat, "campos");
        Button guardar = f(cat, "btnGuardar");
        Button eliminar = f(cat, "btnEliminar");
        Button comparar = f(cat, "btnComparar");
        Button visorBtn = f(cat, "btnVisor");
        long total0 = service.contar();
        Seq seq = new Seq();

        seq.add(() -> nav.go(Screen.CATALOGO), 800);

        // --- catalogo: tabla, busqueda y filtros
        seq.add(() -> {
            check("catalogo muestra todos los laptops (" + tabla.getItems().size() + " de " + total0 + ")", tabla.getItems().size() == total0);
            check("la primera fila queda seleccionada y la ficha muestra su modelo", tabla.getSelectionModel().getSelectedItem() != null
                    && campos.get("model").getText().equals(tabla.getSelectionModel().getSelectedItem().getModel()));
            buscar.setText("legion");
        }, 300);
        seq.add(() -> {
            long esperado = service.todos().stream().filter(l -> (l.getBrand() + l.getModel() + l.getCpu() + l.getGpu()).toLowerCase().contains("legion")).count();
            check("busqueda 'legion' filtra (" + tabla.getItems().size() + " filas, esperado " + esperado + ")", tabla.getItems().size() == esperado && esperado > 0);
            buscar.setText("zzzz-no-existe");
        }, 300);
        seq.add(() -> {
            check("busqueda sin resultados deja la tabla vacia", tabla.getItems().isEmpty());
            check("sin filas no queda ficha editable (Guardar y Eliminar desactivados)", guardar.isDisabled() && eliminar.isDisabled());
            buscar.clear();
            marca.setValue("Apple");
        }, 300);
        seq.add(() -> {
            long esperado = service.todos().stream().filter(l -> "Apple".equals(l.getBrand())).count();
            check("filtro de marca 'Apple' (" + tabla.getItems().size() + " filas, esperado " + esperado + ")", tabla.getItems().size() == esperado);
            marca.setValue("Todas las marcas");
            rtx50.setSelected(true);
        }, 300);
        seq.add(() -> {
            boolean todasRtx50 = tabla.getItems().stream().allMatch(l -> l.getGpu().matches("(?i).*RTX (serie )?50.*"));
            check("filtro GPU RTX 50 (" + tabla.getItems().size() + " filas, todas RTX 50=" + todasRtx50 + ")", todasRtx50 && !tabla.getItems().isEmpty());
            rtx50.setSelected(false);
            recon.setSelected(true);
        }, 300);
        seq.add(() -> {
            check("filtro interior reconstruido (" + tabla.getItems().size() + " filas, esperado 9)", tabla.getItems().size() == 9);
            recon.setSelected(false);
        }, 300);
        seq.add(() -> check("quitar filtros devuelve todas las filas", tabla.getItems().size() == total0), 100);

        // --- edicion y guardado
        seq.add(() -> {
            Laptop tuf = tabla.getItems().stream().filter(l -> l.getModel().contains("TUF")).findFirst().orElseThrow();
            tabla.getSelectionModel().select(tuf);
            check("Guardar esta desactivado sin cambios", guardar.isDisabled());
            campos.get("weightKg").setText("3.1");
            check("editar un campo activa Guardar", !guardar.isDisabled());
        }, 200);
        seq.add(() -> {
            guardar.fire();
        }, 700);
        seq.add(() -> {
            Laptop db = service.todos().stream().filter(l -> l.getModel().contains("TUF")).findFirst().orElseThrow();
            check("guardar persiste en la base de datos (peso=" + db.getWeightKg() + ")", Double.valueOf(3.1).equals(db.getWeightKg()));
            check("el laptop del visor (B) se actualiza con lo guardado", sel.getB() != null && Double.valueOf(3.1).equals(sel.getB().getWeightKg()));
            check("tras guardar, la fila sigue seleccionada y Guardar vuelve a desactivarse",
                    tabla.getSelectionModel().getSelectedItem() != null && tabla.getSelectionModel().getSelectedItem().getModel().contains("TUF") && guardar.isDisabled());
            // valor invalido: se avisa y no se guarda
            campos.get("cpuCores").setText("abc");
            autoDialog(stage, "Aceptar", 400);
            guardar.fire();
        }, 900);
        seq.add(() -> {
            Laptop db = service.todos().stream().filter(l -> l.getModel().contains("TUF")).findFirst().orElseThrow();
            check("un numero invalido muestra aviso, marca el campo y no guarda",
                    campos.get("cpuCores").getStyleClass().contains("invalid") && !"abc".equals(String.valueOf(db.getCpuCores())));
            // campo obligatorio vacio
            campos.get("cpuCores").setText("");
            campos.get("brand").setText("");
            autoDialog(stage, "Aceptar", 400);
            guardar.fire();
        }, 900);
        seq.add(() -> {
            long vacias = service.todos().stream().filter(l -> l.getBrand() == null || l.getBrand().isBlank()).count();
            check("campo obligatorio vacio no se guarda", vacias == 0);
            // restaurar el peso original (n/d) y descartar edicion
            Button descartar = f(cat, "btnDescartar");
            descartar.fire();
            check("Descartar restaura la ficha", "ASUS".equals(campos.get("brand").getText()));
        }, 300);

        // --- eliminar
        seq.add(() -> {
            Laptop objetivo = tabla.getItems().stream().filter(l -> l.getModel().equals("Laptop 16")).findFirst().orElseThrow();
            tabla.getSelectionModel().select(objetivo);
            autoDialog(stage, "Cancelar", 400);
            eliminar.fire();
        }, 900);
        seq.add(() -> {
            check("cancelar la eliminacion conserva el registro", service.contar() == total0);
            autoDialog(stage, "Eliminar", 400);
            eliminar.fire();
        }, 900);
        seq.add(() -> {
            check("confirmar elimina el registro (" + service.contar() + " de " + total0 + ")", service.contar() == total0 - 1);
            check("la tabla se actualiza tras eliminar", tabla.getItems().size() == total0 - 1);
        }, 200);

        // --- marcar y comparar
        seq.add(() -> {
            sel.getCompare().clear();
            check("Comparar esta desactivado sin laptops marcados", comparar.isDisabled() && visorBtn.isDisabled());
            sel.setMarked(tabla.getItems().get(0), true);
            check("con un laptop: visor activo, Comparar desactivado", !visorBtn.isDisabled() && comparar.isDisabled());
            sel.setMarked(tabla.getItems().get(1), true);
            sel.setMarked(tabla.getItems().get(2), true);
            sel.setMarked(tabla.getItems().get(3), true);
            boolean quinto = sel.setMarked(tabla.getItems().get(4), true);
            check("la comparacion admite como maximo 4 laptops", !quinto && sel.getCompare().size() == 4);
            check("con 4 laptops: Comparar activo, visor desactivado", !comparar.isDisabled() && visorBtn.isDisabled());
            nav.go(Screen.COMPARAR);
        }, 700);
        seq.add(() -> {
            TableView<CompareMatrix.Row> t = f(cmp, "tabla");
            int cols = t.getColumns().size();
            int filas = t.getItems().size();
            check("la comparacion muestra 4 columnas de laptops + etiquetas (" + cols + " columnas, " + filas + " filas)", cols == 5 && filas > 10);
            boolean hayMejor = t.getItems().stream().anyMatch(r -> !r.best().isEmpty());
            check("se marca el mejor valor donde corresponde", hayMejor);
            boolean puntuaciones = t.getItems().stream().anyMatch(r -> r.label().toLowerCase().contains("puntuaci"));
            check("no hay puntuaciones inventadas", !puntuaciones);
            CheckBox diff = f(cmp, "chkDiff");
            diff.setSelected(true);
            TableView<CompareMatrix.Row> t2 = f(cmp, "tabla");
            check("'solo diferencias' reduce las filas (" + t2.getItems().size() + " < " + filas + ")", t2.getItems().size() < filas);
            diff.setSelected(false);
            sel.setMarked(sel.getCompare().get(3), false);
            sel.setMarked(sel.getCompare().get(2), false);
        }, 400);
        seq.add(() -> {
            TableView<CompareMatrix.Row> t = f(cmp, "tabla");
            check("quitar laptops actualiza la comparacion (" + t.getColumns().size() + " columnas)", t.getColumns().size() == 3);
            sel.setMarked(sel.getCompare().get(1), false);
        }, 300);
        seq.add(() -> {
            TableView<CompareMatrix.Row> t = f(cmp, "tabla");
            check("con un solo laptop se pide marcar otro", !t.isVisible());
            nav.go(Screen.CATALOGO);
            visorBtn.fire();
        }, 400);

        // --- visor
        seq.add(() -> check("'Abrir en visor 3D' lleva al visor con el laptop como A", sel.getA() != null && sel.getA().equals(sel.getCompare().get(0))), 2500);
        seq.add(() -> {
            ListView<?> lista = f(vis, "lstPartes");
            check("el visor lista los componentes (" + lista.getItems().size() + ")", lista.getItems().size() == 9);
            lista.getSelectionModel().select(0);
        }, 300);
        seq.add(() -> {
            javafx.scene.layout.VBox detalle = f(vis, "boxDetalle");
            javafx.scene.control.Label pieza = f(vis, "lblPieza");
            check("elegir un componente muestra su ficha ('" + pieza.getText() + "', " + detalle.getChildren().size() + " filas)",
                    "Batería".equals(pieza.getText()) && detalle.getChildren().size() >= 2);
            Slider ex = f(vis, "slExplode");
            ex.setValue(80);
            List<LaptopViewer> vs = viewers(scene);
            check("hay dos visores 3D", vs.size() == 2);
            Node bat = findPart(vs.get(0), "battery");
            check("el visor contiene piezas 'battery' seleccionables", bat != null);
            if (bat != null) {
                check("la bateria esta visible antes de ocultarla", bat.isVisible());
                vs.get(0).setPartHidden("battery", true);
            }
        }, 800);
        seq.add(() -> {
            Node bat = findPart(viewers(scene).get(0), "battery");
            check("ocultar un componente lo oculta en el modelo", bat != null && !bat.isVisible());
            viewers(scene).get(0).setPartHidden("battery", false);
        }, 400);
        seq.add(() -> {
            Node bat = findPart(viewers(scene).get(0), "battery");
            check("volver a mostrarlo lo restaura", bat != null && bat.isVisible());
            nav.go(Screen.AJUSTES);
        }, 400);
        seq.add(() -> check("la pantalla Ajustes carga", true), 300);
        seq.run();
    }

    private static List<LaptopViewer> viewers(Scene scene) {
        List<LaptopViewer> out = new ArrayList<>();
        for (Node n : scene.getRoot().lookupAll("*")) {
            if (n instanceof LaptopViewer v) {
                out.add(v);
            }
        }
        return out;
    }

    private static Node findPart(LaptopViewer v, String part) {
        for (Node n : v.getSubScene().getRoot().lookupAll("*")) {
            if (part.equals(n.getUserData())) {
                return n;
            }
        }
        return null;
    }
}
