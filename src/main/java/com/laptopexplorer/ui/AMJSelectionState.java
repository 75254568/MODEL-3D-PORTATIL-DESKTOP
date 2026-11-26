package com.laptopexplorer.ui;

import com.laptopexplorer.model.AMJLaptop;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.stereotype.Component;

/**
 * Estado compartido entre pantallas: laptops que muestra el visor 3D (A y B), laptops marcados para comparar
 * (de 2 a 4) y el último mensaje de la barra de estado.
 */
@Component
public class AMJSelectionState {

    public static final int MAX_COMPARE = 4;

    private final ObjectProperty<AMJLaptop> a = new SimpleObjectProperty<>();
    private final ObjectProperty<AMJLaptop> b = new SimpleObjectProperty<>();
    private final ObservableList<AMJLaptop> compare = FXCollections.observableArrayList();
    private final StringProperty message = new SimpleStringProperty("Listo");

    public ObjectProperty<AMJLaptop> amjAProperty() {
        return a;
    }

    public ObjectProperty<AMJLaptop> amjBProperty() {
        return b;
    }

    public AMJLaptop amjGetA() {
        return a.get();
    }

    public AMJLaptop amjGetB() {
        return b.get();
    }

    public void amjSetA(AMJLaptop laptop) {
        a.set(laptop);
    }

    public void amjSetB(AMJLaptop laptop) {
        b.set(laptop);
    }

    public ObservableList<AMJLaptop> amjGetCompare() {
        return compare;
    }

    public boolean amjIsMarked(AMJLaptop l) {
        return l != null && compare.contains(l);
    }

    /** Marca o desmarca un laptop. Devuelve falso si se pidió marcarlo y ya hay {@value #MAX_COMPARE}. */
    public boolean amjSetMarked(AMJLaptop l, boolean marked) {
        if (marked) {
            if (compare.contains(l)) {
                return true;
            }
            if (compare.size() >= MAX_COMPARE) {
                return false;
            }
            compare.add(l);
        } else {
            compare.remove(l);
        }
        return true;
    }

    public StringProperty amjMessageProperty() {
        return message;
    }

    public void amjSay(String text) {
        message.set(text);
    }

    /** Sustituye por la versión guardada las copias del mismo laptop que mantienen las pantallas. */
    public void amjRefresh(AMJLaptop saved) {
        if (saved == null) {
            return;
        }
        if (amjGetA() != null && saved.equals(amjGetA())) {
            amjSetA(saved);
        }
        if (amjGetB() != null && saved.equals(amjGetB())) {
            amjSetB(saved);
        }
        int i = compare.indexOf(saved);
        if (i >= 0) {
            compare.set(i, saved);
        }
    }

    /** Quita un laptop eliminado de todas las selecciones. */
    public void amjForget(AMJLaptop gone) {
        if (gone == null) {
            return;
        }
        if (gone.equals(amjGetA())) {
            amjSetA(null);
        }
        if (gone.equals(amjGetB())) {
            amjSetB(null);
        }
        compare.remove(gone);
    }
}
