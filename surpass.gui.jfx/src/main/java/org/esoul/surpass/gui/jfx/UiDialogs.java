/*
   Copyright 2017-2026 e-soul.org
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted
   provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright notice, this list of conditions
      and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
      and the following disclaimer in the documentation and/or other materials provided with the distribution.

   THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.esoul.surpass.gui.jfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

final class UiDialogs {

    record MasterPasswordChange(char[] currentPassword, char[] newPassword, Collection<String> serviceIds) {
        void clear() {
            Arrays.fill(currentPassword, '\0');
            Arrays.fill(newPassword, '\0');
        }
    }

    private UiDialogs() {
    }

    static String stylesheet() {
        return UiDialogs.class.getResource("surpass.css").toExternalForm();
    }

    static Optional<char[]> password(Window owner, String title, String prompt) {
        Dialog<char[]> dialog = dialog(owner, title);
        dialog.setHeaderText(prompt);
        PasswordField field = new PasswordField();
        field.setPromptText("Master password");
        field.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(field);

        ButtonType continueButton = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(continueButton, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == continueButton ? field.getText().toCharArray() : null);
        dialog.setOnShown(_ -> field.requestFocus());
        dialog.setOnHidden(_ -> field.clear());
        return dialog.showAndWait();
    }

    static Optional<String> loadService(Window owner, Map<String, String> services) {
        Dialog<String> dialog = dialog(owner, "Load secrets");
        dialog.setHeaderText("Choose where to load your encrypted vault from");
        ToggleGroup group = new ToggleGroup();
        VBox choices = new VBox(10);
        sortedServices(services).forEach(entry -> {
            RadioButton choice = new RadioButton(entry.getValue());
            choice.setUserData(entry.getKey());
            choice.setToggleGroup(group);
            choices.getChildren().add(choice);
        });
        if (!group.getToggles().isEmpty()) {
            group.selectToggle(group.getToggles().getFirst());
        }
        choices.getStyleClass().add("choice-list");
        dialog.getDialogPane().setContent(choices);

        ButtonType load = new ButtonType("Load", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(load, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(load).disableProperty().bind(group.selectedToggleProperty().isNull());
        dialog.setResultConverter(button -> button == load ? (String) group.getSelectedToggle().getUserData() : null);
        return dialog.showAndWait();
    }

    static Optional<Collection<String>> storeServices(Window owner, Map<String, String> services) {
        Dialog<Collection<String>> dialog = dialog(owner, "Store secrets");
        dialog.setHeaderText("Choose every location that should receive the encrypted vault");
        VBox choices = new VBox(10);
        List<CheckBox> checkBoxes = new ArrayList<>();
        sortedServices(services).forEach(entry -> {
            CheckBox choice = new CheckBox(entry.getValue());
            choice.setUserData(entry.getKey());
            choice.setSelected(true);
            checkBoxes.add(choice);
            choices.getChildren().add(choice);
        });
        choices.getStyleClass().add("choice-list");
        dialog.getDialogPane().setContent(choices);

        ButtonType store = new ButtonType("Store", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(store, ButtonType.CANCEL);
        Node storeButton = dialog.getDialogPane().lookupButton(store);
        storeButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (checkBoxes.stream().noneMatch(CheckBox::isSelected)) {
                event.consume();
                warning(dialog.getDialogPane().getScene().getWindow(), "Choose a location", "Select at least one persistence service.");
            }
        });
        dialog.setResultConverter(button -> button == store
                ? checkBoxes.stream().filter(CheckBox::isSelected).map(box -> (String) box.getUserData()).toList()
                : null);
        return dialog.showAndWait();
    }

    static Optional<MasterPasswordChange> masterPassword(Window owner, Map<String, String> services) {
        Dialog<MasterPasswordChange> dialog = dialog(owner, "Change master password");
        dialog.setHeaderText("Protect the vault with a new master password");

        PasswordField current = passwordField("Current master password");
        PasswordField replacement = passwordField("New master password");
        PasswordField repeated = passwordField("Repeat new master password");
        VBox serviceChoices = new VBox(8);
        List<CheckBox> checkBoxes = new ArrayList<>();
        sortedServices(services).forEach(entry -> {
            CheckBox box = new CheckBox(entry.getValue());
            box.setUserData(entry.getKey());
            box.setSelected(true);
            checkBoxes.add(box);
            serviceChoices.getChildren().add(box);
        });

        GridPane form = new GridPane(10, 12);
        form.add(new Label("Current"), 0, 0);
        form.add(current, 1, 0);
        form.add(new Label("New"), 0, 1);
        form.add(replacement, 1, 1);
        form.add(new Label("Repeat"), 0, 2);
        form.add(repeated, 1, 2);
        form.add(new Label("Store in"), 0, 3);
        form.add(serviceChoices, 1, 3);
        GridPane.setHgrow(current, Priority.ALWAYS);
        GridPane.setHgrow(replacement, Priority.ALWAYS);
        GridPane.setHgrow(repeated, Priority.ALWAYS);
        form.setPadding(new Insets(4, 0, 0, 0));
        dialog.getDialogPane().setContent(form);

        ButtonType change = new ButtonType("Change password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(change, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(change).addEventFilter(ActionEvent.ACTION, event -> {
            String message = null;
            if (current.getText().isBlank() || replacement.getText().isEmpty()) {
                message = "Both the current and new master passwords are required.";
            } else if (!replacement.getText().equals(repeated.getText())) {
                message = "The new master passwords do not match.";
            } else if (checkBoxes.stream().noneMatch(CheckBox::isSelected)) {
                message = "Select at least one persistence service.";
            }
            if (message != null) {
                event.consume();
                warning(dialog.getDialogPane().getScene().getWindow(), "Check your input", message);
            }
        });
        dialog.setResultConverter(button -> button == change
                ? new MasterPasswordChange(current.getText().toCharArray(), replacement.getText().toCharArray(),
                        checkBoxes.stream().filter(CheckBox::isSelected).map(box -> (String) box.getUserData()).toList())
                : null);
        dialog.setOnHidden(_ -> {
            current.clear();
            replacement.clear();
            repeated.clear();
        });
        return dialog.showAndWait();
    }

    static boolean confirm(Window owner, String title, String header, String actionText, boolean destructive) {
        Alert alert = alert(owner, Alert.AlertType.CONFIRMATION, title, header, null);
        ButtonType action = new ButtonType(actionText, destructive ? ButtonBar.ButtonData.NO : ButtonBar.ButtonData.YES);
        alert.getButtonTypes().setAll(action, ButtonType.CANCEL);
        if (destructive) {
            alert.getDialogPane().lookupButton(action).getStyleClass().add("danger-button");
        }
        return alert.showAndWait().filter(action::equals).isPresent();
    }

    static void info(Window owner, String title, String header, String content) {
        alert(owner, Alert.AlertType.INFORMATION, title, header, content).showAndWait();
    }

    static void warning(Window owner, String header, String content) {
        alert(owner, Alert.AlertType.WARNING, "Surpass", header, content).showAndWait();
    }

    static void error(Window owner, String title, String header, Throwable error) {
        String detail = error == null || error.getMessage() == null || error.getMessage().isBlank()
                ? "No additional details are available."
                : error.getMessage();
        alert(owner, Alert.AlertType.ERROR, title, header, detail).showAndWait();
    }

    static void secretCopied(Window owner, String secret, long seconds) {
        Alert alert = alert(owner, Alert.AlertType.INFORMATION, "Secret copied",
                "Copied to the clipboard for " + seconds + " seconds", null);
        TextField value = new TextField(secret);
        value.setEditable(false);
        value.getStyleClass().add("secret-preview");
        alert.getDialogPane().setContent(value);
        alert.showAndWait();
    }

    static void about(Window owner, Runnable openHomepage) {
        Dialog<Void> dialog = dialog(owner, "About Surpass");
        dialog.setHeaderText("Surpass");
        Label version = new Label("Version 1.6  •  JavaFX edition");
        Label description = new Label("A simple, secure, and focused password manager.");
        description.setWrapText(true);
        Hyperlink homepage = new Hyperlink("surpass.e-soul.org");
        homepage.setOnAction(_ -> openHomepage.run());
        Label copyright = new Label("© 2017–2026 e-soul.org");
        VBox content = new VBox(10, version, description, homepage, copyright);
        content.setPrefWidth(380);
        content.getStyleClass().add("about-content");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static PasswordField passwordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static List<Map.Entry<String, String>> sortedServices(Map<String, String> services) {
        return services.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.naturalOrder())).toList();
    }

    static <T> Dialog<T> dialog(Window owner, String title) {
        Dialog<T> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        WindowChrome.configure(dialog, title);
        dialog.getDialogPane().getStylesheets().add(stylesheet());
        dialog.getDialogPane().getStyleClass().add("surpass-dialog");
        return dialog;
    }

    private static Alert alert(Window owner, Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        WindowChrome.configure(alert, title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(stylesheet());
        alert.getDialogPane().getStyleClass().add("surpass-dialog");
        return alert;
    }
}
