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

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import org.esoul.surpass.secgen.api.CharClass;

final class SecretGeneratorDialog {

    private static final int MIN_LENGTH = 4;
    private static final int DEFAULT_LENGTH = 16;
    private static final int MAX_LENGTH = 64;

    private SecretGeneratorDialog() {
    }

    static Optional<char[]> show(Window owner, BiConsumer<char[], Collection<CharClass>> generator) {
        Dialog<char[]> dialog = UiDialogs.dialog(owner, "Generate secret");
        dialog.setHeaderText("Create a strong, random secret");

        CheckBox upper = selectedCheckBox("Uppercase", "A–Z");
        CheckBox lower = selectedCheckBox("Lowercase", "a–z");
        CheckBox digits = selectedCheckBox("Numbers", "0–9");
        CheckBox special = selectedCheckBox("Symbols", "!@#…");
        HBox classes = new HBox(10, upper, lower, digits, special);
        classes.setAlignment(Pos.CENTER_LEFT);
        classes.getStyleClass().add("generator-choices");

        Slider length = new Slider(MIN_LENGTH, MAX_LENGTH, DEFAULT_LENGTH);
        length.setBlockIncrement(1);
        length.setMajorTickUnit(12);
        length.setMinorTickCount(11);
        length.setShowTickMarks(true);
        length.setSnapToTicks(true);
        HBox.setHgrow(length, Priority.ALWAYS);
        Label lengthValue = new Label();
        lengthValue.textProperty().bind(Bindings.format("%.0f", length.valueProperty()));
        lengthValue.getStyleClass().add("length-value");
        HBox lengthRow = new HBox(14, length, lengthValue);
        lengthRow.setAlignment(Pos.CENTER);

        TextField output = new TextField();
        output.setEditable(false);
        output.setPromptText("Select character sets and generate");
        output.getStyleClass().add("generated-secret");
        Button generate = new Button("Generate");
        generate.getStyleClass().add("accent-button");
        HBox outputRow = new HBox(10, output, generate);
        HBox.setHgrow(output, Priority.ALWAYS);

        VBox content = new VBox(18,
                section("Character sets", classes),
                section("Length", lengthRow),
                section("Preview", outputRow));
        content.setPadding(new Insets(4, 0, 0, 0));
        content.setPrefWidth(580);
        dialog.getDialogPane().setContent(content);

        var noneSelected = upper.selectedProperty().not()
                .and(lower.selectedProperty().not())
                .and(digits.selectedProperty().not())
                .and(special.selectedProperty().not());
        generate.disableProperty().bind(noneSelected);
        generate.setOnAction(_ -> {
            char[] value = new char[(int) length.getValue()];
            generator.accept(value, selectedClasses(upper, lower, digits, special));
            output.setText(new String(value));
        });

        ButtonType use = new ButtonType("Use secret", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(use, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(use).disableProperty().bind(output.textProperty().isEmpty());
        dialog.setResultConverter(button -> button == use ? output.getText().toCharArray() : null);
        dialog.setOnShown(_ -> generate.fire());
        dialog.setOnHidden(_ -> output.clear());
        return dialog.showAndWait();
    }

    private static VBox section(String title, javafx.scene.Node content) {
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        return new VBox(8, label, content);
    }

    private static CheckBox selectedCheckBox(String text, String detail) {
        CheckBox checkBox = new CheckBox(text + "  " + detail);
        checkBox.setSelected(true);
        return checkBox;
    }

    private static Collection<CharClass> selectedClasses(CheckBox upper, CheckBox lower, CheckBox digits, CheckBox special) {
        return Stream.of(
                new Choice(CharClass.ALPHA_UPPER, upper),
                new Choice(CharClass.ALPHA_LOWER, lower),
                new Choice(CharClass.DIGIT, digits),
                new Choice(CharClass.SPECIAL, special))
                .filter(choice -> choice.control().isSelected())
                .map(Choice::charClass)
                .toList();
    }

    private record Choice(CharClass charClass, CheckBox control) {
    }
}
