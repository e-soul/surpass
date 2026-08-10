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

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import org.esoul.surpass.secgen.api.CharClass;

final class SecretEditorDialog {

    @FunctionalInterface
    interface SecretWriter {
        void write(char[] secret, char[] identifier, char[] note) throws Exception;
    }

    private SecretEditorDialog() {
    }

    static boolean showAdd(Window owner, Collection<String> identifiers,
            BiConsumer<char[], Collection<CharClass>> generator, SecretWriter writer) {
        return show(owner, "Add secret", "Add a new secret to your vault", "Add secret", "", "", false,
                identifiers, generator, writer);
    }

    static boolean showEdit(Window owner, String identifier, String note, Collection<String> identifiers,
            BiConsumer<char[], Collection<CharClass>> generator, SecretWriter writer) {
        return show(owner, "Edit secret", "Update this vault entry", "Save changes", identifier, note, true,
                identifiers, generator, writer);
    }

    private static boolean show(Window owner, String title, String header, String actionText, String initialIdentifier,
            String initialNote, boolean editing, Collection<String> identifiers,
            BiConsumer<char[], Collection<CharClass>> generator, SecretWriter writer) {
        Dialog<Void> dialog = UiDialogs.dialog(owner, title);
        dialog.setHeaderText(header);

        ComboBox<String> identifier = new ComboBox<>(FXCollections.observableArrayList(identifiers));
        identifier.setEditable(true);
        identifier.setMaxWidth(Double.MAX_VALUE);
        identifier.setValue(initialIdentifier);
        identifier.setPromptText("Email, username, account, or purpose");

        PasswordField secret = new PasswordField();
        secret.setPromptText(editing ? "Leave empty to keep the current secret" : "Secret");
        secret.setMaxWidth(Double.MAX_VALUE);
        Button generate = new Button("Generate");
        generate.setOnAction(_ -> SecretGeneratorDialog.show(dialog.getDialogPane().getScene().getWindow(), generator)
                .ifPresent(value -> {
                    secret.setText(new String(value));
                    Arrays.fill(value, '\0');
                }));

        TextArea note = new TextArea(initialNote);
        note.setPromptText("Optional notes");
        note.setWrapText(true);
        note.setPrefRowCount(5);

        GridPane form = new GridPane(10, 10);
        form.setPadding(new Insets(4, 0, 0, 0));
        form.add(new Label("Identifier"), 0, 0);
        form.add(identifier, 1, 0, 2, 1);
        form.add(new Label("Secret"), 0, 1);
        form.add(secret, 1, 1);
        form.add(generate, 2, 1);
        form.add(new Label("Note"), 0, 2);
        form.add(note, 1, 2, 2, 1);
        GridPane.setHgrow(identifier, Priority.ALWAYS);
        GridPane.setHgrow(secret, Priority.ALWAYS);
        GridPane.setHgrow(note, Priority.ALWAYS);
        GridPane.setVgrow(note, Priority.ALWAYS);
        form.setPrefWidth(600);
        dialog.getDialogPane().setContent(form);

        ButtonType save = new ButtonType(actionText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        AtomicBoolean saved = new AtomicBoolean();
        dialog.getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
            char[] secretValue = secret.getText().toCharArray();
            char[] identifierValue = identifier.getEditor().getText().trim().toCharArray();
            char[] noteValue = note.getText().toCharArray();
            try {
                writer.write(secretValue, identifierValue, noteValue);
                saved.set(true);
            } catch (Exception error) {
                event.consume();
                UiDialogs.error(dialog.getDialogPane().getScene().getWindow(), "Cannot save secret",
                        "Check the entry and try again.", error);
            } finally {
                secret.clear();
            }
        });
        dialog.setOnShown(_ -> identifier.requestFocus());
        dialog.setOnHidden(_ -> secret.clear());
        dialog.showAndWait();
        return saved.get();
    }
}
