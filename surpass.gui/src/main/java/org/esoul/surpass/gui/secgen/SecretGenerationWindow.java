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
package org.esoul.surpass.gui.secgen;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collection;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.esoul.surpass.gui.Layout;
import org.esoul.surpass.gui.dialog.Dialogs;
import org.esoul.surpass.secgen.api.CharClass;

/**
 * Brings up a dialog that allows the user to generate random secrets based on various parameters.
 *
 * @author mgp
 */
public class SecretGenerationWindow {

    private static final int MIN_SECRET_LEN = 4;

    private static final int DEFAULT_SECRET_LEN = 12;

    private static final int MAX_SECRET_LEN = 30;

    private static final int SECRET_FIELD_FONT_SIZE = 16;

    private static final int SECRET_FIELD_COLS = 16;

    /**
     * Shows the secret generation dialog and returns the generated secret.
     *
     * @param parentDialog The owner of this dialog.
     * @param secretGenerator Lambda to generate the secret.
     * @return The generated secret or an empty array.
     */
    public static char[] createAndShow(JDialog parentDialog, BiConsumer<char[], Collection<CharClass>> secretGenerator) {
        SecretGenerationComponents components = new SecretGenerationComponents();

        components.frame = new JDialog(parentDialog, "Secret Generation", true);
        components.frame.setLayout(new BoxLayout(components.frame.getContentPane(), BoxLayout.PAGE_AXIS));

        setupCharClassesPanel(components);
        setupLengthPanel(components);
        setupSecretPanel(components, secretGenerator);
        setupCommandPanel(components);

        Dialogs.show(parentDialog, components.frame);

        return components.secretField.getText().toCharArray();
    }

    private static void setupCharClassesPanel(SecretGenerationComponents components) {
        Box charClassesBox = new Box(BoxLayout.PAGE_AXIS);
        charClassesBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        charClassesBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6), "Character classes"));

        components.alphaUpperCheckBox = new JCheckBox("Upper case latin characters, A-Z", true);
        components.alphaUpperCheckBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        charClassesBox.add(components.alphaUpperCheckBox);
        components.alphaLowerCheckBox = new JCheckBox("Lower case latin characters, a-z", true);
        charClassesBox.add(components.alphaLowerCheckBox);
        components.digitsCheckBox = new JCheckBox("Digits, 0-9", true);
        charClassesBox.add(components.digitsCheckBox);
        components.specialCharsCheckBox = new JCheckBox("Special characters", true);
        charClassesBox.add(components.specialCharsCheckBox);
        components.frame.add(charClassesBox);
    }

    private static void setupLengthPanel(SecretGenerationComponents components) {
        Box lengthBox = new Box(BoxLayout.LINE_AXIS);
        lengthBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        lengthBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 6, 10, 6), "Length"));

        components.lengthSlider = new JSlider(MIN_SECRET_LEN, MAX_SECRET_LEN, DEFAULT_SECRET_LEN);
        components.lengthSlider.setPaintTicks(true);
        components.lengthSlider.setMinorTickSpacing(1);
        lengthBox.add(components.lengthSlider);

        components.lengthLabel = new JLabel(components.getLengthLabelValue());
        lengthBox.add(components.lengthLabel);
        components.lengthSlider.addChangeListener(_ -> components.updateLengthLabel());
        components.frame.add(lengthBox);
    }

    private static void setupSecretPanel(SecretGenerationComponents components, BiConsumer<char[], Collection<CharClass>> secretGenerator) {
        Box secretBox = new Box(BoxLayout.LINE_AXIS);
        secretBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        secretBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 6, 10, 6), "Secret"));

        components.secretField = new JTextField(SECRET_FIELD_COLS);
        components.secretField.setEditable(false);
        components.secretField.setHorizontalAlignment(JTextField.CENTER);
        components.secretField.setFont(new Font(Font.MONOSPACED, Font.BOLD, SECRET_FIELD_FONT_SIZE));
        secretBox.add(components.secretField);

        secretBox.add(Layout.createHSpacer());

        components.generateButton = Layout.createFixedSizeButton("Generate", 90);
        components.generateButton.addActionListener(_ -> {
            char[] secret = new char[components.lengthSlider.getValue()];
            secretGenerator.accept(secret, components.getSelectedCharClasses());
            components.secretField.setText(new String(secret));
        });
        secretBox.add(components.generateButton);
        components.frame.add(secretBox);
    }

    private static void setupCommandPanel(SecretGenerationComponents components) {
        Box commandBox = new Box(BoxLayout.LINE_AXIS);
        commandBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        commandBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        commandBox.add(Box.createHorizontalGlue());

        JButton okButton = Layout.createFixedSizeButton("OK", 90);
        okButton.addActionListener(_ -> components.frame.setVisible(false));
        commandBox.add(okButton);

        commandBox.add(Layout.createHSpacer());

        JButton cancelButton = Layout.createFixedSizeButton("Cancel", 90);
        cancelButton.addActionListener(_ -> {
            components.frame.setVisible(false);
            components.secretField.setText("");
        });
        commandBox.add(cancelButton);
        components.frame.add(commandBox);
    }
}
