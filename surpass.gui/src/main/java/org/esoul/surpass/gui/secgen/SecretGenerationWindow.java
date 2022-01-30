/*
   Copyright 2017-2022 e-soul.org
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

import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Collection;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.esoul.surpass.secgen.api.CharClass;

/**
 * Brings up a dialog that allows the user to generate random secrets based on various parameters.
 *
 * @author mgp
 *
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
     * @param parentFrame The owner of this dialog.
     * @param secretGenerator Lambda to generate the secret.
     * @return The generated secret or an empty array.
     */
    public static char[] createAndShow(JFrame parentFrame, BiConsumer<char[], Collection<CharClass>> secretGenerator) {
        SecretGenerationComponents components = new SecretGenerationComponents();

        JDialog secretGenerationDialog = new JDialog(parentFrame, "Secret Generation", true);
        secretGenerationDialog.setLayout(new BoxLayout(secretGenerationDialog.getContentPane(), BoxLayout.PAGE_AXIS));
        setupCharClassesPanel(secretGenerationDialog, components);
        setupLengthPanel(secretGenerationDialog, components);
        setupSecretPanel(secretGenerationDialog, components, secretGenerator);
        setupCommandPanel(secretGenerationDialog, components);

        secretGenerationDialog.pack();
        secretGenerationDialog.setLocationRelativeTo(parentFrame);
        secretGenerationDialog.setVisible(true);
        secretGenerationDialog.dispose();

        return components.secretField.getText().toCharArray();
    }

    private static void setupCharClassesPanel(JDialog secretGenerationDialog, SecretGenerationComponents components) {
        Box charClassesBox = new Box(BoxLayout.PAGE_AXIS);
        components.alphaUpperCheckBox = new JCheckBox("Upper case latin characters, A-Z", true);
        charClassesBox.add(components.alphaUpperCheckBox);
        components.alphaLowerCheckBox = new JCheckBox("Lower case latin characters, a-z", true);
        charClassesBox.add(components.alphaLowerCheckBox);
        components.digitsCheckBox = new JCheckBox("Digits, 0-9", true);
        charClassesBox.add(components.digitsCheckBox);
        components.specialCharsCheckBox = new JCheckBox("Special characters", true);
        charClassesBox.add(components.specialCharsCheckBox);
        JPanel charClassesPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        charClassesPanel.setBorder(BorderFactory.createTitledBorder("Character classes"));
        charClassesPanel.add(charClassesBox);
        secretGenerationDialog.getContentPane().add(charClassesPanel);
    }

    private static void setupLengthPanel(JDialog secretGenerationDialog, SecretGenerationComponents components) {
        JPanel lengthPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        lengthPanel.setBorder(BorderFactory.createTitledBorder("Length"));
        components.lengthSlider = new JSlider(MIN_SECRET_LEN, MAX_SECRET_LEN, DEFAULT_SECRET_LEN);
        components.lengthSlider.setPaintTicks(true);
        components.lengthSlider.setMinorTickSpacing(1);
        lengthPanel.add(components.lengthSlider);
        components.lengthLabel = new JLabel(components.getLengthLabelValue());
        lengthPanel.add(components.lengthLabel);
        components.lengthSlider.addChangeListener(l -> components.updateLengthLabel());
        secretGenerationDialog.getContentPane().add(lengthPanel);
    }

    private static void setupSecretPanel(JDialog secretGenerationDialog, SecretGenerationComponents components, BiConsumer<char[], Collection<CharClass>> secretGenerator) {
        JPanel secretPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        secretPanel.setBorder(BorderFactory.createTitledBorder("Secret"));

        components.generateButton = new JButton("Generate");
        components.generateButton.addActionListener(l -> {
            char[] secret = new char[components.lengthSlider.getValue()];
            secretGenerator.accept(secret, components.getSelectedCharClasses());
            components.secretField.setText(new String(secret));
        });
        secretPanel.add(components.generateButton);
        components.secretField = new JTextField(SECRET_FIELD_COLS);
        components.secretField.setEditable(false);
        components.secretField.setHorizontalAlignment(JTextField.CENTER);
        components.secretField.setFont(new Font(Font.MONOSPACED, Font.BOLD, SECRET_FIELD_FONT_SIZE));
        secretPanel.add(components.secretField);
        secretGenerationDialog.getContentPane().add(secretPanel);
    }

    private static void setupCommandPanel(JDialog secretGenerationDialog, SecretGenerationComponents components) {
        JPanel commandPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(l -> secretGenerationDialog.setVisible(false));
        commandPanel.add(okButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(l -> {
            secretGenerationDialog.setVisible(false);
            components.secretField.setText("");
        });
        commandPanel.add(cancelButton);
        secretGenerationDialog.getContentPane().add(commandPanel);
    }
}
