package com.laptopexplorer.ui;

import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/** Permite que cualquier pantalla pida cambiar a otra sin conocer el contenedor principal. */
@Component
public class AMJNavigationService {

    private Consumer<AMJScreen> handler;

    /** Modo inmersivo: la ventana pasa a pantalla completa y solo queda la pantalla activa, sin menús. */
    private final javafx.beans.property.BooleanProperty immersive = new javafx.beans.property.SimpleBooleanProperty(false);

    public javafx.beans.property.BooleanProperty amjImmersiveProperty() {
        return immersive;
    }

    public void amjRegister(Consumer<AMJScreen> handler) {
        this.handler = handler;
    }

    public void amjGo(AMJScreen screen) {
        if (handler != null) {
            handler.accept(screen);
        }
    }
}
