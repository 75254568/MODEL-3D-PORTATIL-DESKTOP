package com.laptopstudio.ui;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Objects;
import java.util.Optional;

/** Dialogos con el mismo aspecto que el resto de la aplicacion. */
public final class Dialogs {

    private Dialogs() {
    }

    private static Alert build(Alert.AlertType type, Node owner, String title, String message, ButtonType... buttons) {
        Alert a = new Alert(type, message, buttons);
        a.setTitle(title);
        a.setHeaderText(title);
        if (owner != null && owner.getScene() != null) {
            a.initOwner(owner.getScene().getWindow());
        }
        a.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(Dialogs.class.getResource("/css/app.css")).toExternalForm());
        a.getDialogPane().setMinWidth(440);
        return a;
    }

    public static void info(Node owner, String title, String message) {
        build(Alert.AlertType.INFORMATION, owner, title, message, ButtonType.OK).showAndWait();
    }

    public static void warn(Node owner, String title, String message) {
        build(Alert.AlertType.WARNING, owner, title, message, ButtonType.OK).showAndWait();
    }

    public static void error(Node owner, String title, String message) {
        build(Alert.AlertType.ERROR, owner, title, message, ButtonType.OK).showAndWait();
    }

    /** Pide confirmacion con textos de boton propios. Devuelve verdadero si se acepta. */
    public static boolean confirm(Node owner, String title, String message, String acceptText, String cancelText) {
        ButtonType yes = new ButtonType(acceptText, javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType(cancelText, javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> r = build(Alert.AlertType.CONFIRMATION, owner, title, message, no, yes).showAndWait();
        return r.isPresent() && r.get() == yes;
    }
}
