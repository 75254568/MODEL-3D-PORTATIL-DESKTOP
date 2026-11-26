package com.laptopexplorer.ui;

import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Autocompletado para campos de texto: al escribir (o al enfocar el campo vacío) despliega las opciones que
 * contienen lo escrito, primero las que empiezan igual. Las opciones se piden cada vez al proveedor, de modo
 * que pueden depender de lo que el usuario ya eligio en otros campos.
 */
public final class AMJSuggest {

    private static final int MAX = 9;

    private AMJSuggest() {
    }

    /**
     * @param options proveedor de opciones en el momento de mostrar la lista
     * @param onPick  se ejecuta después de elegir una opción (por ejemplo, relanzar la búsqueda)
     */
    public static void amjAttach(TextField field, Supplier<List<String>> options, Runnable onPick) {
        ContextMenu menu = new ContextMenu();
        menu.setAutoHide(true);
        boolean[] picking = {false};
        Runnable show = () -> {
            if (picking[0] || !field.isFocused() || field.getScene() == null) {
                return;
            }
            String typed = amjNorm(field.getText());
            List<String> starts = new ArrayList<>();
            List<String> contains = new ArrayList<>();
            for (String o : options.get()) {
                String n = amjNorm(o);
                if (n.equals(typed)) {
                    continue;
                }
                if (typed.isEmpty() || n.startsWith(typed) || amjWordStarts(n, typed)) {
                    starts.add(o);
                } else if (n.contains(typed)) {
                    contains.add(o);
                }
            }
            starts.addAll(contains);
            if (starts.isEmpty()) {
                menu.hide();
                return;
            }
            List<MenuItem> items = new ArrayList<>();
            for (String o : starts.subList(0, Math.min(MAX, starts.size()))) {
                MenuItem it = new MenuItem(o);
                it.setMnemonicParsing(false);
                it.setOnAction(e -> {
                    picking[0] = true;
                    field.setText(o);
                    field.positionCaret(o.length());
                    picking[0] = false;
                    menu.hide();
                    if (onPick != null) {
                        onPick.run();
                    }
                });
                items.add(it);
            }
            menu.getItems().setAll(items);
            if (!menu.isShowing()) {
                menu.show(field, Side.BOTTOM, 0, 2);
            }
        };
        field.textProperty().addListener((o, a, b) -> show.run());
        field.focusedProperty().addListener((o, a, focused) -> {
            if (focused) {
                show.run();
            } else {
                menu.hide();
            }
        });
        field.setOnMouseClicked(e -> show.run());
    }

    private static boolean amjWordStarts(String text, String typed) {
        for (String w : text.split("[\\s/()-]+")) {
            if (w.startsWith(typed)) {
                return true;
            }
        }
        return false;
    }

    private static String amjNorm(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
