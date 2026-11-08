package com.laptopstudio;

import javafx.application.Application;

/**
 * Punto de entrada. No extiende Application a proposito: asi el programa
 * empaquetado puede ejecutarse sin exigir los modulos de JavaFX en la linea de comandos.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}
