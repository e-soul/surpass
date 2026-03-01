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
package org.esoul.surpass.gui.jfx.masterpass;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.esoul.surpass.app.Session;
import org.esoul.surpass.gui.jfx.loadstore.LoadStoreDialog;

public class ChangeMasterPassDialog {

    private ChangeMasterPassDialog() {
        // no instances
    }

    public static void createAndShow(Window owner, ProgressBar progressBar, Session session) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Change Master Password");

        VBox root = new VBox(5);
        root.setPadding(new Insets(10));

        Label currentLabel = new Label("Current Master Password:");
        PasswordField currentField = new PasswordField();
        currentField.setMaxHeight(26);

        Label newLabel = new Label("New Master Password:");
        PasswordField newField = new PasswordField();
        newField.setMaxHeight(26);

        Label repeatLabel = new Label("Repeat New Master Password:");
        PasswordField repeatField = new PasswordField();
        repeatField.setMaxHeight(26);

        // Services checkboxes
        List<CheckBox> serviceCheckBoxes = LoadStoreDialog.createServiceCheckBoxes(session.getSupportedPersistenceServices());
        VBox servicesBox = new VBox(5);
        servicesBox.getChildren().addAll(serviceCheckBoxes);

        TitledPane servicesTitledPane = new TitledPane("Change Master Password in", servicesBox);
        servicesTitledPane.setCollapsible(false);

        // Buttons
        Button changeButton = createFixedButton("Change", 80);
        changeButton.setOnAction(_ -> {
            Collection<String> selectedServices = serviceCheckBoxes.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> (String) cb.getUserData())
                    .collect(Collectors.toList());
            new ChangeMasterPassOperation(owner, progressBar, session,
                    currentField.getText().toCharArray(),
                    newField.getText().toCharArray(),
                    repeatField.getText().toCharArray(),
                    selectedServices).execute();
            dialog.close();
        });

        Button cancelButton = createFixedButton("Cancel", 80);
        cancelButton.setOnAction(_ -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBox = new HBox(5, spacer, changeButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(5, 0, 0, 0));

        root.getChildren().addAll(
                currentLabel, currentField,
                newLabel, newField,
                repeatLabel, repeatField,
                servicesTitledPane,
                buttonBox);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.showAndWait();
    }

    private static Button createFixedButton(String text, double width) {
        Button button = new Button(text);
        button.setPrefWidth(width);
        button.setMinWidth(width);
        button.setMaxWidth(width);
        return button;
    }
}
