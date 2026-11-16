package com.laptopstudio.ui;

import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/** Permite que cualquier pantalla pida cambiar a otra sin conocer el contenedor principal. */
@Component
public class NavigationService {

    private Consumer<Screen> handler;

    /** Modo inmersivo: la ventana pasa a pantalla completa y solo queda la pantalla activa, sin menus. */
    private final javafx.beans.property.BooleanProperty immersive = new javafx.beans.property.SimpleBooleanProperty(false);

    public javafx.beans.property.BooleanProperty immersiveProperty() {
        return immersive;
    }

    public void register(Consumer<Screen> handler) {
        this.handler = handler;
    }

    public void go(Screen screen) {
        if (handler != null) {
            handler.accept(screen);
        }
    }
}
