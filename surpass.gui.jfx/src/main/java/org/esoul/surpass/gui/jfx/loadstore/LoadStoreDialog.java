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
package org.esoul.surpass.gui.jfx.loadstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class LoadStoreDialog {

    private LoadStoreDialog() {
        // no instances
    }

    public static String showLoad(Window owner, Map<String, String> supportedPersistenceServices) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Loading secrets");

        ToggleGroup group = new ToggleGroup();
        VBox servicesBox = new VBox(5);
        for (Map.Entry<String, String> entry : supportedPersistenceServices.entrySet()) {
            RadioButton rb = new RadioButton(entry.getValue());
            rb.setToggleGroup(group);
            rb.setUserData(entry.getKey());
            rb.setSelected(true);
            servicesBox.getChildren().add(rb);
        }

        TitledPane titledPane = new TitledPane("Supported persistence services", servicesBox);
        titledPane.setCollapsible(false);

        String[] result = { null };

        Button okButton = new Button("OK");
        okButton.setOnAction(_ -> {
            Toggle selected = group.getSelectedToggle();
            if (selected != null) {
                result[0] = (String) selected.getUserData();
            }
            dialog.close();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(_ -> dialog.close());

        HBox buttonBox = new HBox(5, okButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        VBox root = new VBox(10, titledPane, buttonBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.showAndWait();

        return result[0];
    }

    public static Collection<String> showStore(Window owner, Map<String, String> supportedPersistenceServices) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Storing secrets");

        List<CheckBox> checkBoxes = new ArrayList<>();
        VBox servicesBox = new VBox(5);
        for (Map.Entry<String, String> entry : supportedPersistenceServices.entrySet()) {
            CheckBox cb = new CheckBox(entry.getValue());
            cb.setUserData(entry.getKey());
            cb.setSelected(true);
            checkBoxes.add(cb);
            servicesBox.getChildren().add(cb);
        }

        TitledPane titledPane = new TitledPane("Supported persistence services", servicesBox);
        titledPane.setCollapsible(false);

        List<String> result = new ArrayList<>();

        Button okButton = new Button("OK");
        okButton.setOnAction(_ -> {
            result.addAll(checkBoxes.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> (String) cb.getUserData())
                    .collect(Collectors.toList()));
            dialog.close();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(_ -> dialog.close());

        HBox buttonBox = new HBox(5, okButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        VBox root = new VBox(10, titledPane, buttonBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.showAndWait();

        return result;
    }

    public static List<CheckBox> createServiceCheckBoxes(Map<String, String> supportedPersistenceServices) {
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (Map.Entry<String, String> entry : supportedPersistenceServices.entrySet()) {
            CheckBox cb = new CheckBox(entry.getValue());
            cb.setUserData(entry.getKey());
            cb.setSelected(true);
            checkBoxes.add(cb);
        }
        return checkBoxes;
    }

    public static Collection<String> getSelectedServices(List<CheckBox> checkBoxes) {
        return checkBoxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (String) cb.getUserData())
                .collect(Collectors.toSet());
    }
}
