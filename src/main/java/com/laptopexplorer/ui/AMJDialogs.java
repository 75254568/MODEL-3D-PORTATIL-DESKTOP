package com.laptopexplorer.ui;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Objects;
import java.util.Optional;

/** Diálogos con el mismo aspecto que el resto de la aplicación. */
public final class AMJDialogs {

    private AMJDialogs() {
    }

    private static Alert amjBuild(Alert.AlertType type, Node owner, String title, String message, ButtonType... buttons) {
        Alert a = new Alert(type, message, buttons);
        a.setTitle(title);
        a.setHeaderText(title);
        if (owner != null && owner.getScene() != null) {
            a.initOwner(owner.getScene().getWindow());
        }
        a.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(AMJDialogs.class.getResource("/css/app.css")).toExternalForm());
        a.getDialogPane().setMinWidth(440);
        return a;
    }

    public static void amjInfo(Node owner, String title, String message) {
        amjBuild(Alert.AlertType.INFORMATION, owner, title, message, ButtonType.OK).showAndWait();
    }

    public static void amjWarn(Node owner, String title, String message) {
        amjBuild(Alert.AlertType.WARNING, owner, title, message, ButtonType.OK).showAndWait();
    }

    public static void amjError(Node owner, String title, String message) {
        amjBuild(Alert.AlertType.ERROR, owner, title, message, ButtonType.OK).showAndWait();
    }

    /** Pide confirmación con textos de botón propios. Devuelve verdadero si se acepta. */
    public static boolean amjConfirm(Node owner, String title, String message, String acceptText, String cancelText) {
        ButtonType yes = new ButtonType(acceptText, javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType(cancelText, javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> r = amjBuild(Alert.AlertType.CONFIRMATION, owner, title, message, no, yes).showAndWait();
        return r.isPresent() && r.get() == yes;
    }
}
