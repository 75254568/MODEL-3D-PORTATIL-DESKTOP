package com.laptopexplorer;

import javafx.application.Application;

/**
 * Punto de entrada. No extiende Application a propósito: así el programa
 * empaquetado puede ejecutarse sin exigir los módulos de JavaFX en la línea de comandos.
 */
public class AMJLauncher {
    public static void main(String[] args) {
        Application.launch(AMJJavaFxApplication.class, args);
    }
}
