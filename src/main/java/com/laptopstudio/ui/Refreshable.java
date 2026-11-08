package com.laptopstudio.ui;

/** Controladores que necesitan reaccionar cuando su pantalla pasa a primer plano. */
public interface Refreshable {

    void onShow();

    default void onHide() {
    }
}
