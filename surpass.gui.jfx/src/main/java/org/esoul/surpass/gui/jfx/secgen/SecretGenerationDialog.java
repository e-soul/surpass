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
package org.esoul.surpass.gui.jfx.secgen;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.esoul.surpass.secgen.api.CharClass;

/**
 * Brings up a dialog that allows the user to generate random secrets based on various parameters.
 */
public class SecretGenerationDialog {

    private static final int MIN_SECRET_LEN = 4;
    private static final int DEFAULT_SECRET_LEN = 12;
    private static final int MAX_SECRET_LEN = 30;
    private static final int SECRET_FIELD_FONT_SIZE = 16;

    private SecretGenerationDialog() {
        // no instances
    }

    public static char[] createAndShow(Window owner, BiConsumer<char[], Collection<CharClass>> secretGenerator) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Secret Generation");

        // Character classes
        CheckBox alphaUpperCheckBox = new CheckBox("Upper case latin characters, A-Z");
        alphaUpperCheckBox.setSelected(true);
        CheckBox alphaLowerCheckBox = new CheckBox("Lower case latin characters, a-z");
        alphaLowerCheckBox.setSelected(true);
        CheckBox digitsCheckBox = new CheckBox("Digits, 0-9");
        digitsCheckBox.setSelected(true);
        CheckBox specialCharsCheckBox = new CheckBox("Special characters");
        specialCharsCheckBox.setSelected(true);

        VBox charClassesContent = new VBox(5, alphaUpperCheckBox, alphaLowerCheckBox, digitsCheckBox, specialCharsCheckBox);
        charClassesContent.setPadding(new Insets(5));

        TitledPane charClassesPane = new TitledPane("Character classes", charClassesContent);
        charClassesPane.setCollapsible(false);

        // Length
        Slider lengthSlider = new Slider(MIN_SECRET_LEN, MAX_SECRET_LEN, DEFAULT_SECRET_LEN);
        lengthSlider.setMajorTickUnit(1);
        lengthSlider.setMinorTickCount(0);
        lengthSlider.setSnapToTicks(true);

        Label lengthLabel = new Label(String.format("%2d", DEFAULT_SECRET_LEN));
        lengthSlider.valueProperty().addListener((_, _, newValue) ->
                lengthLabel.setText(String.format("%2d", newValue.intValue())));

        HBox lengthContent = new HBox(10, lengthSlider, lengthLabel);
        lengthContent.setPadding(new Insets(5));
        HBox.setHgrow(lengthSlider, Priority.ALWAYS);

        TitledPane lengthPane = new TitledPane("Length", lengthContent);
        lengthPane.setCollapsible(false);

        // Secret
        TextField secretField = new TextField();
        secretField.setEditable(false);
        secretField.setAlignment(Pos.CENTER);
        secretField.setFont(Font.font("Monospaced", FontWeight.BOLD, SECRET_FIELD_FONT_SIZE));

        Button generateButton = createFixedButton("Generate", 90);
        generateButton.setOnAction(_ -> {
            int length = (int) lengthSlider.getValue();
            char[] secret = new char[length];
            Collection<CharClass> selectedClasses = getSelectedCharClasses(
                    alphaUpperCheckBox, alphaLowerCheckBox, digitsCheckBox, specialCharsCheckBox);
            secretGenerator.accept(secret, selectedClasses);
            secretField.setText(new String(secret));
        });

        HBox secretContent = new HBox(5, secretField, generateButton);
        secretContent.setPadding(new Insets(5));
        HBox.setHgrow(secretField, Priority.ALWAYS);

        TitledPane secretPane = new TitledPane("Secret", secretContent);
        secretPane.setCollapsible(false);

        // Command buttons
        Button okButton = createFixedButton("OK", 90);
        okButton.setOnAction(_ -> dialog.close());

        Button cancelButton = createFixedButton("Cancel", 90);
        cancelButton.setOnAction(_ -> {
            secretField.setText("");
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox commandBox = new HBox(5, spacer, okButton, cancelButton);
        commandBox.setPadding(new Insets(0, 10, 10, 10));

        VBox root = new VBox(5, charClassesPane, lengthPane, secretPane, commandBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.showAndWait();

        return secretField.getText().toCharArray();
    }

    private static Collection<CharClass> getSelectedCharClasses(CheckBox alphaUpper, CheckBox alphaLower,
            CheckBox digits, CheckBox special) {
        return Stream.of(
                Map.entry(CharClass.ALPHA_UPPER, alphaUpper),
                Map.entry(CharClass.ALPHA_LOWER, alphaLower),
                Map.entry(CharClass.DIGIT, digits),
                Map.entry(CharClass.SPECIAL, special))
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static Button createFixedButton(String text, double width) {
        Button button = new Button(text);
        button.setPrefWidth(width);
        button.setMinWidth(width);
        button.setMaxWidth(width);
        return button;
    }
}
