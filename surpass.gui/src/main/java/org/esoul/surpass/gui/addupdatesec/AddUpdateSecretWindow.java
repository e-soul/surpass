/*
   Copyright 2017-2023 e-soul.org
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
package org.esoul.surpass.gui.addupdatesec;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Collection;
import java.util.function.BiConsumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.esoul.surpass.gui.Layout;
import org.esoul.surpass.gui.dialog.Dialogs;
import org.esoul.surpass.gui.event.AddUpdateSecretListener;
import org.esoul.surpass.gui.secgen.SecretGenerationWindow;
import org.esoul.surpass.secgen.api.CharClass;

public class AddUpdateSecretWindow {

    public static void createAndShowAdd(JFrame parentFrame, AddUpdateSecretListener listener, BiConsumer<char[], Collection<CharClass>> secretGenerator) {
        AddUpdateSecretComponents components = new AddUpdateSecretComponents();
        components.frame = Layout.createDialogFrame(parentFrame, "Add Secret");
        setupIdentifierLine(components);
        setupSecretLine(components, secretGenerator);
        setupNoteLine(components);
        setupCommandPanel(components, listener, "Add");
        Dialogs.show(parentFrame, components.frame);
    }

    public static void createAndShowUpdate(JFrame parentFrame, AddUpdateSecretListener listener, BiConsumer<char[], Collection<CharClass>> secretGenerator,
            String identifier, String note) {
        AddUpdateSecretComponents components = new AddUpdateSecretComponents();
        components.frame = Layout.createDialogFrame(parentFrame, "Update Secret");
        setupIdentifierLine(components);
        components.identifierTextField.setText(identifier);
        setupSecretLine(components, secretGenerator);
        setupNoteLine(components);
        components.noteTextArea.setText(note);
        setupCommandPanel(components, listener, "Update");
        Dialogs.show(parentFrame, components.frame);
    }

    private static void setupIdentifierLine(AddUpdateSecretComponents components) {
        JLabel identifierLabel = new JLabel("Identifier: ");
        identifierLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.frame.add(identifierLabel);

        components.identifierTextField = new JTextField(30);
        components.identifierTextField.setPreferredSize(new Dimension(400, 26));
        components.identifierTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JButton selectIdButton = Layout.createFixedSizeButton("Select existing", 120);
        selectIdButton.setEnabled(false);

        Box idBox = new Box(BoxLayout.LINE_AXIS);
        idBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        idBox.add(components.identifierTextField);
        idBox.add(Box.createRigidArea(new Dimension(3, 0)));
        idBox.add(selectIdButton);
        components.frame.add(idBox);
        components.frame.add(Layout.createVSpacer());
    }

    private static void setupSecretLine(AddUpdateSecretComponents components, BiConsumer<char[], Collection<CharClass>> secretGenerator) {
        JLabel secretLabel = new JLabel("Secret: ");
        secretLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.frame.add(secretLabel);

        components.secretPasswordField = new JPasswordField(30);
        components.secretPasswordField.setPreferredSize(new Dimension(400, 26));
        components.secretPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JButton generateSecretButton = Layout.createFixedSizeButton("Generate", 120);
        generateSecretButton.addActionListener(l -> {
            char[] secret = SecretGenerationWindow.createAndShow(components.frame, secretGenerator);
            if (secret.length > 0) {
                components.secretPasswordField.setText(new String(secret));
            }
        });

        Box secretBox = new Box(BoxLayout.LINE_AXIS);
        secretBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        secretBox.add(components.secretPasswordField);
        secretBox.add(Box.createRigidArea(new Dimension(3, 0)));
        secretBox.add(generateSecretButton);
        components.frame.add(secretBox);
        components.frame.add(Layout.createVSpacer());
    }

    private static void setupNoteLine(AddUpdateSecretComponents components) {
        JLabel noteLabel = new JLabel("Note: ");
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.frame.add(noteLabel);

        components.noteTextArea = new JTextArea(3, 39);
        JScrollPane noteScrollPane = new JScrollPane(components.noteTextArea);
        noteScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.frame.add(noteScrollPane);
        components.frame.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private static void setupCommandPanel(AddUpdateSecretComponents components, AddUpdateSecretListener listener, String actionName) {
        JButton addUpdateButton = Layout.createFixedSizeButton(actionName, 80);
        addUpdateButton.addActionListener(l -> addSecret(components, listener));

        JButton cancelButton = Layout.createFixedSizeButton("Cancel", 80);
        cancelButton.addActionListener(l -> components.frame.setVisible(false));

        Box addUpdateBox = new Box(BoxLayout.LINE_AXIS);
        addUpdateBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        addUpdateBox.add(Box.createHorizontalGlue());
        addUpdateBox.add(addUpdateButton);
        addUpdateBox.add(Layout.createHSpacer());
        addUpdateBox.add(cancelButton);
        components.frame.add(addUpdateBox);
    }

    private static void addSecret(AddUpdateSecretComponents components, AddUpdateSecretListener listener) {
        char[] secret = components.secretPasswordField.getPassword();
        char[] identifier = components.identifierTextField.getText().toCharArray();
        char[] note = components.noteTextArea.getText().toCharArray();
        try {
            listener.actionPerformed(secret, identifier, note);
            components.identifierTextField.setText("");
            components.noteTextArea.setText("");
            components.frame.setVisible(false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(components.frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Always clear the password. Clear the other fields only on success.
            components.secretPasswordField.setText("");
        }
    }
}
