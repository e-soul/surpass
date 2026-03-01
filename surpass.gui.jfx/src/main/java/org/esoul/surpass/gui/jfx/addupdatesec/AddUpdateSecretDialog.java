/*
   Copyright 2017-2025 e-soul.org
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
package org.esoul.surpass.gui.jfx.addupdatesec;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.esoul.surpass.gui.jfx.dialog.Dialogs;
import org.esoul.surpass.gui.jfx.event.AddUpdateSecretListener;
import org.esoul.surpass.gui.jfx.secgen.SecretGenerationDialog;
import org.esoul.surpass.secgen.api.CharClass;

public class AddUpdateSecretDialog {

    private AddUpdateSecretDialog() {
        // no instances
    }

    public static void createAndShowAdd(Window owner, AddUpdateSecretListener listener,
            BiConsumer<char[], Collection<CharClass>> secretGenerator, Supplier<Collection<String>> uniqueIdsSupplier) {
        createAndShow(owner, listener, secretGenerator, uniqueIdsSupplier, "Add Secret", "", "", "Add");
    }

    public static void createAndShowUpdate(Window owner, AddUpdateSecretListener listener,
            BiConsumer<char[], Collection<CharClass>> secretGenerator, Supplier<Collection<String>> uniqueIdsSupplier,
            String identifier, String note) {
        createAndShow(owner, listener, secretGenerator, uniqueIdsSupplier, "Update Secret", identifier, note, "Update");
    }

    private static void createAndShow(Window owner, AddUpdateSecretListener listener,
            BiConsumer<char[], Collection<CharClass>> secretGenerator, Supplier<Collection<String>> uniqueIdsSupplier,
            String title, String initialIdentifier, String initialNote, String actionName) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(title);

        VBox root = new VBox(5);
        root.setPadding(new Insets(10));

        // Identifier
        Label identifierLabel = new Label("Identifier:");
        TextField identifierField = new TextField(initialIdentifier);
        identifierField.setMaxHeight(26);

        Button selectIdButton = createFixedButton("Select existing", 125);
        selectIdButton.setDisable(uniqueIdsSupplier.get().isEmpty());
        selectIdButton.setOnAction(_ -> {
            String selected = Dialogs.showComboSelectionDialog(dialog, "Existing identifiers", uniqueIdsSupplier.get());
            if (selected != null) {
                identifierField.setText(selected);
            }
        });

        HBox idBox = new HBox(3, identifierField, selectIdButton);
        HBox.setHgrow(identifierField, Priority.ALWAYS);

        // Secret
        Label secretLabel = new Label("Secret:");
        PasswordField secretField = new PasswordField();
        secretField.setMaxHeight(26);

        Button generateButton = createFixedButton("Generate", 125);
        generateButton.setOnAction(_ -> {
            char[] secret = SecretGenerationDialog.createAndShow(dialog, secretGenerator);
            if (secret.length > 0) {
                secretField.setText(new String(secret));
            }
        });

        HBox secretBox = new HBox(3, secretField, generateButton);
        HBox.setHgrow(secretField, Priority.ALWAYS);

        // Note
        Label noteLabel = new Label("Note:");
        TextArea noteArea = new TextArea(initialNote);
        noteArea.setPrefRowCount(3);
        noteArea.setPrefColumnCount(39);

        // Command buttons
        Button actionButton = createFixedButton(actionName, 80);
        actionButton.setOnAction(_ -> performAction(dialog, listener, secretField, identifierField, noteArea));

        Button cancelButton = createFixedButton("Cancel", 80);
        cancelButton.setOnAction(_ -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBox = new HBox(5, spacer, actionButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(identifierLabel, idBox, secretLabel, secretBox, noteLabel, noteArea, buttonBox);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.showAndWait();
    }

    private static void performAction(Stage dialog, AddUpdateSecretListener listener,
            PasswordField secretField, TextField identifierField, TextArea noteArea) {
        char[] secret = secretField.getText().toCharArray();
        char[] identifier = identifierField.getText().trim().toCharArray();
        char[] note = noteArea.getText().toCharArray();
        try {
            listener.actionPerformed(secret, identifier, note);
            identifierField.setText("");
            noteArea.setText("");
            dialog.close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialog);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } finally {
            secretField.setText("");
        }
    }

    private static Button createFixedButton(String text, double width) {
        Button button = new Button(text);
        button.setPrefWidth(width);
        button.setMinWidth(width);
        button.setMaxWidth(width);
        return button;
    }
}
