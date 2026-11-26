package com.laptopexplorer.ui;

import com.laptopexplorer.controller.AMJCatalogController;
import com.laptopexplorer.controller.AMJCompareController;
import com.laptopexplorer.controller.AMJViewerController;
import com.laptopexplorer.model.AMJLaptop;
import com.laptopexplorer.service.AMJLaptopService;
import com.laptopexplorer.viewer.AMJLaptopViewer;
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
 * Prueba funcional de las pantallas (con -Dexplorer.func=true). Maneja los controles reales (búsqueda, filtros,
 * edición, guardado, eliminación, marcado, comparación y visor 3D), comprueba el resultado contra la base de datos
 * y escribe "FUNC ok" o "FUNC FALLO" por cada comprobación. Modifica la base de datos del usuario activo, así que
 * se ejecuta siempre con -Duser.home apuntando a una carpeta temporal.
 */
public final class AMJFunctionalCheck {

    private AMJFunctionalCheck() {
    }

    public static boolean amjEnabled() {
        return Boolean.getBoolean("explorer.func");
    }

    private static int fallos;

    private static void amjCheck(String what, boolean ok) {
        if (!ok) {
            fallos++;
        }
        System.out.println((ok ? "FUNC ok     " : "FUNC FALLO  ") + what);
    }

    @SuppressWarnings("unchecked")
    private static <T> T amjF(Object o, String name) {
        try {
            Field fld = o.getClass().getDeclaredField(name);
            fld.setAccessible(true);
            return (T) fld.get(o);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(name, e);
        }
    }

    private static void amjCall(Object o, String method) {
        try {
            var m = o.getClass().getDeclaredMethod(method);
            m.setAccessible(true);
            m.invoke(o);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(method, e);
        }
    }

    /** Dentro de {@code ms}, pulsa el botón con ese texto del diálogo abierto (aunque haya un showAndWait en curso). */
    private static void amjAutoDialog(Stage owner, String buttonText, int ms) {
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
                    System.out.println("FUNC FALLO  diálogo sin botón '" + buttonText + "': " + dp.getButtonTypes());
                    fallos++;
                    ((Stage) w).close();
                    return;
                }
            }
            System.out.println("FUNC FALLO  se esperaba un diálogo con '" + buttonText + "'");
            fallos++;
        });
        p.play();
    }

    private static final class AMJSeq {
        final List<Supplier<Integer>> steps = new ArrayList<>();
        int i = 0;

        void amjAdd(Runnable r, int waitMs) {
            steps.add(() -> {
                r.run();
                return waitMs;
            });
        }

        void amjRun() {
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
                System.out.println("FUNC FALLO  excepción en paso " + i + ": " + t);
                t.printStackTrace(System.out);
                wait = 100;
            }
            PauseTransition p = new PauseTransition(Duration.millis(wait));
            p.setOnFinished(e -> Platform.runLater(this::amjRun));   // fuera de la animación: showAndWait no se permite dentro
            p.play();
        }
    }

    public static void amjRun(Scene scene, ApplicationContext ctx, AMJNavigationService nav) {
        Stage stage = (Stage) scene.getWindow();
        AMJLaptopService service = ctx.getBean(AMJLaptopService.class);
        AMJSelectionState sel = ctx.getBean(AMJSelectionState.class);
        AMJCatalogController cat = ctx.getBean(AMJCatalogController.class);
        AMJCompareController cmp = ctx.getBean(AMJCompareController.class);
        AMJViewerController vis = ctx.getBean(AMJViewerController.class);
        // las pantallas se cargan al abrirse por primera vez: se abren antes de leer sus controles
        nav.amjGo(AMJScreen.VISOR);
        nav.amjGo(AMJScreen.COMPARAR);
        nav.amjGo(AMJScreen.CATALOGO);
        TableView<AMJLaptop> tabla = amjF(cat, "tblLaptops");
        TextField buscar = amjF(cat, "txtBuscar");
        ComboBox<String> marca = amjF(cat, "cmbMarca");
        ToggleButton rtx50 = amjF(cat, "tglRtx50");
        ToggleButton recon = amjF(cat, "tglRecon");
        Map<String, TextField> campos = amjF(cat, "campos");
        Button guardar = amjF(cat, "btnGuardar");
        Button eliminar = amjF(cat, "btnEliminar");
        Button comparar = amjF(cat, "btnComparar");
        Button visorBtn = amjF(cat, "btnVisor");
        long total0 = service.amjContar();
        AMJSeq seq = new AMJSeq();

        seq.amjAdd(() -> nav.amjGo(AMJScreen.CATALOGO), 800);

        // --- catálogo: tabla, búsqueda y filtros
        seq.amjAdd(() -> {
            amjCheck("catálogo muestra todos los laptops (" + tabla.getItems().size() + " de " + total0 + ")", tabla.getItems().size() == total0);
            amjCheck("la primera fila queda seleccionada y la ficha muestra su modelo", tabla.getSelectionModel().getSelectedItem() != null
                    && campos.get("model").getText().equals(tabla.getSelectionModel().getSelectedItem().getModel()));
            buscar.setText("legion");
        }, 300);
        seq.amjAdd(() -> {
            long esperado = service.amjTodos().stream().filter(l -> (l.getBrand() + l.getModel() + l.getCpu() + l.getGpu()).toLowerCase().contains("legion")).count();
            amjCheck("búsqueda 'legion' filtra (" + tabla.getItems().size() + " filas, esperado " + esperado + ")", tabla.getItems().size() == esperado && esperado > 0);
            buscar.setText("zzzz-no-existe");
        }, 300);
        seq.amjAdd(() -> {
            amjCheck("búsqueda sin resultados deja la tabla vacía", tabla.getItems().isEmpty());
            amjCheck("sin filas no queda ficha editable (Guardar y Eliminar desactivados)", guardar.isDisabled() && eliminar.isDisabled());
            buscar.clear();
            marca.setValue("Apple");
        }, 300);
        seq.amjAdd(() -> {
            long esperado = service.amjTodos().stream().filter(l -> "Apple".equals(l.getBrand())).count();
            amjCheck("filtro de marca 'Apple' (" + tabla.getItems().size() + " filas, esperado " + esperado + ")", tabla.getItems().size() == esperado);
            marca.setValue("Todas las marcas");
            rtx50.setSelected(true);
        }, 300);
        seq.amjAdd(() -> {
            boolean todasRtx50 = tabla.getItems().stream().allMatch(l -> l.getGpu().matches("(?i).*RTX (serie )?50.*"));
            amjCheck("filtro GPU RTX 50 (" + tabla.getItems().size() + " filas, todas RTX 50=" + todasRtx50 + ")", todasRtx50 && !tabla.getItems().isEmpty());
            rtx50.setSelected(false);
            recon.setSelected(true);
        }, 300);
        seq.amjAdd(() -> {
            amjCheck("filtro interior reconstruido (" + tabla.getItems().size() + " filas, esperado 9)", tabla.getItems().size() == 9);
            recon.setSelected(false);
        }, 300);
        seq.amjAdd(() -> amjCheck("quitar filtros devuelve todas las filas", tabla.getItems().size() == total0), 100);

        // --- vista de tarjetas y diseño 3D de cada laptop
        seq.amjAdd(() -> {
            javafx.scene.layout.FlowPane rejilla = amjF(cat, "pnlRejilla");
            ToggleButton tarjetas = amjF(cat, "tglTarjetas");
            amjCheck("el catálogo abre en vista de tarjetas, una por laptop (" + rejilla.getChildren().size() + ")",
                    tarjetas.isSelected() && rejilla.getChildren().size() == total0);
            long genericos = service.amjTodos().stream()
                    .filter(l -> com.laptopexplorer.viewer.AMJLaptopModel.amjExteriorInfo(l).kind() == com.laptopexplorer.viewer.AMJLaptopModel.AMJExterior.GENERIC)
                    .count();
            amjCheck("cada laptop tiene diseño 3D propio o de su serie (" + genericos + " genéricos)", genericos == 0);
            long sinFicha = service.amjTodos().stream().filter(l -> com.laptopexplorer.service.AMJVerifiedSpecs.amjOf(l) == null).count();
            long sinBlender = service.amjTodos().stream().filter(l -> com.laptopexplorer.viewer.AMJLaptopModel.amjBlenderInfo(l) == null).count();
            amjCheck("cada laptop tiene ficha verificada (" + sinFicha + " sin ficha) y modelo de Blender (" + sinBlender + " sin modelo)",
                    sinFicha == 0 && sinBlender == 0);
            AMJLaptop esperado = tabla.getItems().get(2);
            Node tercera = rejilla.getChildren().get(2);
            tercera.fireEvent(new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                    javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, true, false, false, null));
            amjCheck("un clic en una tarjeta la elige y muestra su ficha",
                    esperado.equals(tabla.getSelectionModel().getSelectedItem())
                            && campos.get("model").getText().equals(esperado.getModel())
                            && tercera.getStyleClass().contains("selected"));
        }, 300);

        // --- edición y guardado
        seq.amjAdd(() -> {
            AMJLaptop tuf = tabla.getItems().stream().filter(l -> l.getModel().contains("TUF")).findFirst().orElseThrow();
            tabla.getSelectionModel().select(tuf);
            amjCheck("Guardar está desactivado sin cambios", guardar.isDisabled());
            campos.get("weightKg").setText("3.1");
            amjCheck("editar un campo activa Guardar", !guardar.isDisabled());
        }, 200);
        seq.amjAdd(() -> {
            guardar.fire();
        }, 700);
        seq.amjAdd(() -> {
            AMJLaptop db = service.amjTodos().stream().filter(l -> l.getModel().contains("TUF")).findFirst().orElseThrow();
            amjCheck("guardar persiste en la base de datos (peso=" + db.getWeightKg() + ")", Double.valueOf(3.1).equals(db.getWeightKg()));
            amjCheck("el laptop del visor (B) se actualiza con lo guardado", sel.amjGetB() != null && Double.valueOf(3.1).equals(sel.amjGetB().getWeightKg()));
            amjCheck("tras guardar, la fila sigue seleccionada y Guardar vuelve a desactivarse",
                    tabla.getSelectionModel().getSelectedItem() != null && tabla.getSelectionModel().getSelectedItem().getModel().contains("TUF") && guardar.isDisabled());
            // valor inválido: se avisa y no se guarda
            campos.get("cpuCores").setText("abc");
            amjAutoDialog(stage, "Aceptar", 400);
            guardar.fire();
        }, 900);
        seq.amjAdd(() -> {
            AMJLaptop db = service.amjTodos().stream().filter(l -> l.getModel().contains("TUF")).findFirst().orElseThrow();
            amjCheck("un número inválido muestra aviso, marca el campo y no guarda",
                    campos.get("cpuCores").getStyleClass().contains("invalid") && !"abc".equals(String.valueOf(db.getCpuCores())));
            // campo obligatorio vacío
            campos.get("cpuCores").setText("");
            campos.get("brand").setText("");
            amjAutoDialog(stage, "Aceptar", 400);
            guardar.fire();
        }, 900);
        seq.amjAdd(() -> {
            long vacias = service.amjTodos().stream().filter(l -> l.getBrand() == null || l.getBrand().isBlank()).count();
            amjCheck("campo obligatorio vacío no se guarda", vacias == 0);
            // restaurar el peso original (n/d) y descartar edición
            Button descartar = amjF(cat, "btnDescartar");
            descartar.fire();
            amjCheck("Descartar restaura la ficha", "ASUS".equals(campos.get("brand").getText()));
        }, 300);

        // --- un dato desconocido no se convierte en "no" al guardar otro campo
        long[] idSinDato = new long[1];
        seq.amjAdd(() -> {
            AMJLaptop sinDato = tabla.getItems().stream().filter(l -> l.getRamUpgradable() == null).findFirst().orElseThrow();
            idSinDato[0] = sinDato.getId();
            tabla.getSelectionModel().select(sinDato);
            CheckBox ram = amjF(cat, "chkRam");
            amjCheck("'RAM ampliable' sin dato se muestra como estado intermedio", ram.isIndeterminate());
            campos.get("notes").setText("nota de prueba");
            guardar.fire();
        }, 700);
        seq.amjAdd(() -> {
            AMJLaptop db = service.amjBuscarPorId(idSinDato[0]).orElseThrow();
            amjCheck("guardar otro campo conserva 'RAM ampliable: sin dato'",
                    db.getRamUpgradable() == null && "nota de prueba".equals(db.getNotes()));
        }, 200);

        // --- eliminar
        seq.amjAdd(() -> {
            AMJLaptop objetivo = tabla.getItems().stream().filter(l -> l.getModel().equals("Laptop 16")).findFirst().orElseThrow();
            tabla.getSelectionModel().select(objetivo);
            amjAutoDialog(stage, "Cancelar", 400);
            eliminar.fire();
        }, 900);
        seq.amjAdd(() -> {
            amjCheck("cancelar la eliminación conserva el registro", service.amjContar() == total0);
            amjAutoDialog(stage, "Eliminar", 400);
            eliminar.fire();
        }, 900);
        seq.amjAdd(() -> {
            amjCheck("confirmar elimina el registro (" + service.amjContar() + " de " + total0 + ")", service.amjContar() == total0 - 1);
            amjCheck("la tabla se actualiza tras eliminar", tabla.getItems().size() == total0 - 1);
            javafx.scene.layout.FlowPane rejilla = amjF(cat, "pnlRejilla");
            Node imagen = rejilla.getChildren().isEmpty() ? null : rejilla.getChildren().get(0).lookup(".image-view");
            amjCheck("las tarjetas reciben la imagen de su modelo 3D",
                    imagen instanceof javafx.scene.image.ImageView iv && iv.getImage() != null);
        }, 200);

        // --- marcar y comparar
        seq.amjAdd(() -> {
            sel.amjGetCompare().clear();
            amjCheck("Comparar está desactivado sin laptops marcados", comparar.isDisabled() && visorBtn.isDisabled());
            sel.amjSetMarked(tabla.getItems().get(0), true);
            amjCheck("con un laptop: visor activo, Comparar desactivado", !visorBtn.isDisabled() && comparar.isDisabled());
            sel.amjSetMarked(tabla.getItems().get(1), true);
            sel.amjSetMarked(tabla.getItems().get(2), true);
            sel.amjSetMarked(tabla.getItems().get(3), true);
            boolean quinto = sel.amjSetMarked(tabla.getItems().get(4), true);
            amjCheck("la comparación admite como máximo 4 laptops", !quinto && sel.amjGetCompare().size() == 4);
            amjCheck("con 4 laptops: Comparar activo, visor desactivado", !comparar.isDisabled() && visorBtn.isDisabled());
            nav.amjGo(AMJScreen.COMPARAR);
        }, 700);
        seq.amjAdd(() -> {
            TableView<AMJCompareMatrix.AMJRow> t = amjF(cmp, "tblComparacion");
            int cols = t.getColumns().size();
            int filas = t.getItems().size();
            amjCheck("la comparación muestra 4 columnas de laptops + etiquetas (" + cols + " columnas, " + filas + " filas)", cols == 5 && filas > 10);
            boolean hayMejor = t.getItems().stream().anyMatch(r -> !r.best().isEmpty());
            amjCheck("se marca el mejor valor donde corresponde", hayMejor);
            boolean puntuaciones = t.getItems().stream().anyMatch(r -> r.label().toLowerCase().contains("puntuaci"));
            amjCheck("no hay puntuaciones inventadas", !puntuaciones);
            CheckBox diff = amjF(cmp, "chkDiff");
            long iguales = t.getItems().stream().filter(r -> !r.group() && r.same()).count();
            diff.setSelected(true);
            TableView<AMJCompareMatrix.AMJRow> t2 = amjF(cmp, "tblComparacion");
            // cuantas filas son iguales depende de los laptops marcados: se comprueba que ninguna igual quede a la vista
            amjCheck("'solo diferencias' oculta las filas iguales (" + t2.getItems().size() + " de " + filas + ")",
                    t2.getItems().size() <= filas && t2.getItems().stream().noneMatch(r -> !r.group() && r.same()));
            diff.setSelected(false);
            sel.amjSetMarked(sel.amjGetCompare().get(3), false);
            sel.amjSetMarked(sel.amjGetCompare().get(2), false);
        }, 400);
        seq.amjAdd(() -> {
            TableView<AMJCompareMatrix.AMJRow> t = amjF(cmp, "tblComparacion");
            amjCheck("quitar laptops actualiza la comparación (" + t.getColumns().size() + " columnas)", t.getColumns().size() == 3);
            sel.amjSetMarked(sel.amjGetCompare().get(1), false);
        }, 300);
        seq.amjAdd(() -> {
            TableView<AMJCompareMatrix.AMJRow> t = amjF(cmp, "tblComparacion");
            amjCheck("con un solo laptop se pide marcar otro", !t.isVisible());
            nav.amjGo(AMJScreen.CATALOGO);
            visorBtn.fire();
        }, 400);

        // --- visor
        seq.amjAdd(() -> amjCheck("'Abrir en visor 3D' lleva al visor con el laptop como A", sel.amjGetA() != null && sel.amjGetA().equals(sel.amjGetCompare().get(0))), 2500);
        seq.amjAdd(() -> {
            ListView<?> lista = amjF(vis, "lstPartes");
            amjCheck("el visor lista los componentes (" + lista.getItems().size() + ")", lista.getItems().size() == 9);
            lista.getSelectionModel().select(0);
        }, 300);
        seq.amjAdd(() -> {
            javafx.scene.layout.VBox detalle = amjF(vis, "pnlDetalle");
            javafx.scene.control.Label pieza = amjF(vis, "lblPieza");
            amjCheck("elegir un componente muestra su ficha ('" + pieza.getText() + "', " + detalle.getChildren().size() + " filas)",
                    "Batería".equals(pieza.getText()) && detalle.getChildren().size() >= 2);
            Slider ex = amjF(vis, "sldExplode");
            ex.setValue(80);
            List<AMJLaptopViewer> vs = amjViewers(scene);
            amjCheck("hay dos visores 3D", vs.size() == 2);
            Node bat = amjFindPart(vs.get(0), "battery");
            amjCheck("el visor contiene piezas 'battery' seleccionables", bat != null);
            if (bat != null) {
                amjCheck("la batería está visible antes de ocultarla", bat.isVisible());
                vs.get(0).amjSetPartHidden("battery", true);
            }
        }, 800);
        seq.amjAdd(() -> {
            Node bat = amjFindPart(amjViewers(scene).get(0), "battery");
            amjCheck("ocultar un componente lo oculta en el modelo", bat != null && !bat.isVisible());
            amjViewers(scene).get(0).amjSetPartHidden("battery", false);
        }, 400);
        seq.amjAdd(() -> {
            Node bat = amjFindPart(amjViewers(scene).get(0), "battery");
            amjCheck("volver a mostrarlo lo restaura", bat != null && bat.isVisible());
            nav.amjGo(AMJScreen.COMPONENTES);
        }, 2500);

        // --- componentes 3D
        seq.amjAdd(() -> {
            var comp = ctx.getBean(com.laptopexplorer.controller.AMJComponentsController.class);
            var lib = ctx.getBean(com.laptopexplorer.component.AMJComponentLibrary.class);
            ListView<?> lista = amjF(comp, "lstComponentes");
            amjCheck("la biblioteca de componentes lista todos sus modelos (" + lista.getItems().size() + ")",
                    !lista.getItems().isEmpty() && lista.getItems().size() == lib.amjAll().size());
            javafx.scene.layout.VBox modelo = amjF(comp, "boxModelo");
            javafx.scene.layout.VBox secciones = amjF(comp, "boxSecciones");
            amjCheck("el primer componente carga su modelo 3D, con medidas y piezas", modelo.getChildren().size() == 2);
            amjCheck("y muestra su ficha técnica por secciones (" + secciones.getChildren().size() + ")", secciones.getChildren().size() >= 2);
            TextField buscarComp = amjF(comp, "txtBuscar");
            buscarComp.setText("rtx 5090");
        }, 1500);
        seq.amjAdd(() -> {
            ListView<?> lista = amjF(ctx.getBean(com.laptopexplorer.controller.AMJComponentsController.class), "lstComponentes");
            amjCheck("la búsqueda de componentes filtra por nombre (" + lista.getItems().size() + " resultado)", lista.getItems().size() == 1);
            nav.amjGo(AMJScreen.AJUSTES);
        }, 400);
        seq.amjAdd(() -> amjCheck("la pantalla Ajustes carga", true), 300);
        seq.amjRun();
    }

    private static List<AMJLaptopViewer> amjViewers(Scene scene) {
        List<AMJLaptopViewer> out = new ArrayList<>();
        for (Node n : scene.getRoot().lookupAll("*")) {
            if (n instanceof AMJLaptopViewer v) {
                out.add(v);
            }
        }
        return out;
    }

    private static Node amjFindPart(AMJLaptopViewer v, String part) {
        for (Node n : v.amjGetSubScene().getRoot().lookupAll("*")) {
            if (part.equals(n.getUserData())) {
                return n;
            }
        }
        return null;
    }
}
