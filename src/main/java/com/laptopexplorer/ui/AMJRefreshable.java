package com.laptopexplorer.ui;

/** Controladores que necesitan reaccionar cuando su pantalla pasa a primer plano. */
public interface AMJRefreshable {

    void amjOnShow();

    default void amjOnHide() {
    }
}
