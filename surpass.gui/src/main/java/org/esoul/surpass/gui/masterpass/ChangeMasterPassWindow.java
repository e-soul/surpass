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
package org.esoul.surpass.gui.masterpass;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;

import org.esoul.surpass.gui.Layout;
import org.esoul.surpass.gui.dialog.Dialogs;
import org.esoul.surpass.gui.loadstore.LoadStoreWindow;

public class ChangeMasterPassWindow {

    public static void createAndShow(JFrame parentFrame, JProgressBar operationProgressBar, ChangeMasterPassPolicy policy) {
        ChangeMasterPassComponents components = new ChangeMasterPassComponents();
        components.frame = Layout.createDialogFrame(parentFrame, "Change Master Password");

        JLabel currentMasterPassLabel = new JLabel("Current Master Password: ");
        currentMasterPassLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.frame.add(currentMasterPassLabel);

        components.currentMasterPasswordField = new JPasswordField(30);
        components.currentMasterPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.currentMasterPasswordField.setPreferredSize(new Dimension(400, 26));
        components.currentMasterPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        components.frame.add(components.currentMasterPasswordField);

        components.frame.add(Layout.createVSpacer());

        JLabel newMasterPassLabel = new JLabel("New Master Password: ");
        newMasterPassLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.frame.add(newMasterPassLabel);

        components.newMasterPasswordField = new JPasswordField(30);
        components.newMasterPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.newMasterPasswordField.setPreferredSize(new Dimension(400, 26));
        components.newMasterPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        components.frame.add(components.newMasterPasswordField);

        components.frame.add(Layout.createVSpacer());

        JLabel repeatedMasterPassLabel = new JLabel("Repeat New Master Password: ");
        repeatedMasterPassLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.frame.add(repeatedMasterPassLabel);

        components.repeatedNewMasterPasswordField = new JPasswordField(30);
        components.repeatedNewMasterPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.repeatedNewMasterPasswordField.setPreferredSize(new Dimension(400, 26));
        components.repeatedNewMasterPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        components.frame.add(components.repeatedNewMasterPasswordField);

        components.frame.add(Layout.createVSpacer());

        components.servicesBox = LoadStoreWindow.createServicesBox(policy.getSupportedPersistenceServices(), LoadStoreWindow::createServiceCheckBox);
        components.servicesBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        components.servicesBox.setPreferredSize(new Dimension(400, 70));
        components.servicesBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        components.servicesBox.setBorder(BorderFactory.createTitledBorder("Change Master Password in"));
        components.frame.add(components.servicesBox);

        components.frame.add(Layout.createVSpacer(10));

        JButton changeButton = Layout.createFixedSizeButton("Change", 80);
        changeButton.addActionListener(l -> changeMasterPassword(parentFrame, operationProgressBar, policy, components));

        JButton cancelButton = Layout.createFixedSizeButton("Cancel", 80);
        cancelButton.addActionListener(l -> components.frame.setVisible(false));

        Box changeBox = new Box(BoxLayout.LINE_AXIS);
        changeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        changeBox.add(Box.createHorizontalGlue());
        changeBox.add(changeButton);
        changeBox.add(Layout.createHSpacer());
        changeBox.add(cancelButton);
        components.frame.add(changeBox);

        Dialogs.show(parentFrame, components.frame);
    }

    private static void changeMasterPassword(JFrame parentFrame, JProgressBar operationProgressBar, ChangeMasterPassPolicy policy,
            ChangeMasterPassComponents components) {
        Collection<String> selectedServices = LoadStoreWindow.getSelectedServices(components.servicesBox.getComponents());
        new ChangeMasterPassOperation(parentFrame, operationProgressBar, components, policy, selectedServices).execute();
        components.frame.setVisible(false);
    }
}
