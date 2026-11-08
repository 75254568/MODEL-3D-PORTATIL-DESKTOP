package com.laptopstudio.ui;

import com.laptopstudio.model.Laptop;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.stereotype.Component;

/**
 * Estado compartido entre pantallas: laptops que muestra el visor 3D (A y B), laptops marcados para comparar
 * (de 2 a 4) y el ultimo mensaje de la barra de estado.
 */
@Component
public class SelectionState {

    public static final int MAX_COMPARE = 4;

    private final ObjectProperty<Laptop> a = new SimpleObjectProperty<>();
    private final ObjectProperty<Laptop> b = new SimpleObjectProperty<>();
    private final ObservableList<Laptop> compare = FXCollections.observableArrayList();
    private final StringProperty message = new SimpleStringProperty("Listo");

    public ObjectProperty<Laptop> aProperty() {
        return a;
    }

    public ObjectProperty<Laptop> bProperty() {
        return b;
    }

    public Laptop getA() {
        return a.get();
    }

    public Laptop getB() {
        return b.get();
    }

    public void setA(Laptop laptop) {
        a.set(laptop);
    }

    public void setB(Laptop laptop) {
        b.set(laptop);
    }

    public ObservableList<Laptop> getCompare() {
        return compare;
    }

    public boolean isMarked(Laptop l) {
        return l != null && compare.contains(l);
    }

    /** Marca o desmarca un laptop. Devuelve falso si se pidio marcarlo y ya hay {@value #MAX_COMPARE}. */
    public boolean setMarked(Laptop l, boolean marked) {
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

    public StringProperty messageProperty() {
        return message;
    }

    public void say(String text) {
        message.set(text);
    }

    /** Sustituye por la version guardada las copias del mismo laptop que mantienen las pantallas. */
    public void refresh(Laptop saved) {
        if (saved == null) {
            return;
        }
        if (getA() != null && saved.equals(getA())) {
            setA(saved);
        }
        if (getB() != null && saved.equals(getB())) {
            setB(saved);
        }
        int i = compare.indexOf(saved);
        if (i >= 0) {
            compare.set(i, saved);
        }
    }

    /** Quita un laptop eliminado de todas las selecciones. */
    public void forget(Laptop gone) {
        if (gone == null) {
            return;
        }
        if (gone.equals(getA())) {
            setA(null);
        }
        if (gone.equals(getB())) {
            setB(null);
        }
        compare.remove(gone);
    }
}
